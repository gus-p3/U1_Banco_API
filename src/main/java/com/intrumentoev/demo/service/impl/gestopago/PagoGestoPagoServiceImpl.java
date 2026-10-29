package com.intrumentoev.demo.service.impl.gestopago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.client.gestopago.GestoPagoCipher;
import com.intrumentoev.demo.client.gestopago.GestoPagoTxClient;
import com.intrumentoev.demo.client.gestopago.GestoPagoXmlParser;
import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.account.AccountBalance;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.gestopago.PagoGestoPago;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoTxResult;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;
import com.intrumentoev.demo.model.gestopago.pago.TipoPago;
import com.intrumentoev.demo.repository.account.AccountBalanceRepository;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.gestopago.PagoGestoPagoRepository;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.PagoGestoPagoService;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import com.intrumentoev.demo.service.service.gestopago.UpcGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Flujo de pago vía GestoPago:
 *  1. Tx corta: validar, bloquear cuenta (FOR UPDATE), registrar pago PENDIENTE y descontar el saldo.
 *  2. sendTx fuera de transacción (hasta 65 s de espera).
 *  3. Tx corta según el XML (no según el HTTP status): 01/06 APLICADO, 82 o sin respuesta POR_CONFIRMAR, otro RECHAZADO.
 * El job de confirmTx resuelve los POR_CONFIRMAR con el mismo payload.
 */
@Service
@Slf4j
public class PagoGestoPagoServiceImpl implements PagoGestoPagoService {

    static final String CODIGO_APLICADO = "01";
    static final String CODIGO_DUPLICADA_APLICADA = "06";
    static final String CODIGO_TIMEOUT = "82";
    static final String CODIGO_CONFIRM_APLICADA = "06";

    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper();
    private static final String MSG_PRODUCTO_NO_DISPONIBLE = "No se pudo realizar el pago: producto o servicio no disponible";
    private static final String MSG_EN_PROCESO = "Pago en proceso de confirmación";
    private static final int PENDIENTE_HUERFANO_SEGUNDOS = 180;

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final ClientRepository clientRepository;
    private final PagoGestoPagoRepository pagoRepository;
    private final ProductoServiceRedis productoServiceRedis;
    private final CatalogoRefresher catalogoRefresher;
    private final GestoPagoCipher cipher;
    private final GestoPagoTxClient txClient;
    private final GestoPagoXmlParser xmlParser;
    private final GestoPagoTokenService tokenService;
    private final UpcGenerator upcGenerator;
    private final Validator validator;
    private final TransactionTemplate tx;

    @Value("${gestopago.tx.unidad:086}")
    private String unidad;

    @Value("${gestopago.tx.codigos-producto-inexistente:}")
    private String codigosProductoInexistente;

    @Value("${gestopago.tx.codigos-no-aplicada:70}")
    private String codigosNoAplicada;

    @Value("${gestopago.tx.espera-confirmacion-segundos:62}")
    private long esperaConfirmacionSegundos;

    @Value("${gestopago.tx.vigencia-confirmacion-horas:24}")
    private long vigenciaConfirmacionHoras;

    public PagoGestoPagoServiceImpl(AccountRepository accountRepository,
                                    AccountBalanceRepository accountBalanceRepository,
                                    ClientRepository clientRepository,
                                    PagoGestoPagoRepository pagoRepository,
                                    ProductoServiceRedis productoServiceRedis,
                                    CatalogoRefresher catalogoRefresher,
                                    GestoPagoCipher cipher,
                                    GestoPagoTxClient txClient,
                                    GestoPagoXmlParser xmlParser,
                                    GestoPagoTokenService tokenService,
                                    UpcGenerator upcGenerator,
                                    Validator validator,
                                    @Qualifier("sfTransactionManager") PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.clientRepository = clientRepository;
        this.pagoRepository = pagoRepository;
        this.productoServiceRedis = productoServiceRedis;
        this.catalogoRefresher = catalogoRefresher;
        this.cipher = cipher;
        this.txClient = txClient;
        this.xmlParser = xmlParser;
        this.tokenService = tokenService;
        this.upcGenerator = upcGenerator;
        this.validator = validator;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------ pago

    @Override
    public PagoResponse procesarPago(AuthenticatedClient cliente, PagoRequest request) {
        validarFormato(request);
        Long idCliente = cliente.getIdClient();
        TipoPago tipoPago = TipoPago.valueOf(request.getTipoPago());

        // c) propiedad de la cuenta: idClient solo del JWT; mismo mensaje si no existe o no es suya
        Account cuenta = accountRepository.findByAccountNumber(request.getNumCuenta())
                .filter(a -> idCliente.equals(a.getIdClient()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FORBIDDEN));

        // d) cliente activo y cuenta ACTIVA
        Client client = clientRepository.findById(idCliente)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FORBIDDEN));
        if (!Boolean.TRUE.equals(client.getIsActive())) {
            throw new BusinessException(ErrorCode.CLIENT_INACTIVE, String.valueOf(idCliente));
        }
        if (!"ACTIVA".equals(cuenta.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCOUNT_INACTIVE);
        }

        // e) producto en el catálogo de GestoPago (caché), no en el Id local de la tabla
        ProductoResponse producto = buscarProducto(request.getIdServicio(), request.getIdProducto());
        if (producto == null) {
            catalogoRefresher.refrescarEnSegundoPlano();
            return productoNoDisponible();
        }
        validarTipoPago(tipoPago, producto);
        BigDecimal monto = calcularMonto(tipoPago, request, producto);

        // payload (se guarda sin cifrar para poder repetir confirmTx idéntico)
        String upc = generarUpcUnico();
        String payloadJson = construirPayload(request, tipoPago, producto, monto, upc);
        String requestCifrado = cipher.cifrar(payloadJson);

        // 1) Tx corta: bloquear cuenta, validar saldo (f), registrar PENDIENTE y descontar
        PagoGestoPago pago = tx.execute(status -> registrarPagoYDescontar(idCliente, request, tipoPago, monto, upc,
                payloadJson, requestCifrado));

        // 2) sendTx fuera de transacción
        GestoPagoHttpResponse http = llamar(false, requestCifrado);

        // 3) Tx corta según el XML
        PagoGestoPago resuelto = resolverEnvio(pago.getId(), http);

        if (resuelto.getEstado() == EstadoPago.RECHAZADO && contiene(codigosProductoInexistente, resuelto.getCodigoRespuesta())) {
            catalogoRefresher.refrescarEnSegundoPlano();
            return productoNoDisponible();
        }
        return aRespuesta(resuelto);
    }

    private PagoGestoPago registrarPagoYDescontar(Long idCliente, PagoRequest request, TipoPago tipoPago, BigDecimal monto,
                                                  String upc, String payloadJson, String requestCifrado) {
        Account cuenta = accountRepository.findByAccountNumberForUpdate(request.getNumCuenta())
                .filter(a -> idCliente.equals(a.getIdClient()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FORBIDDEN));
        if (!"ACTIVA".equals(cuenta.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCOUNT_INACTIVE);
        }
        if (cuenta.getBalance().compareTo(monto) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        PagoGestoPago pago = pagoRepository.save(PagoGestoPago.builder()
                .idCliente(idCliente)
                .numCuenta(cuenta.getAccountNumber())
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .tipoPago(tipoPago)
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .monto(monto)
                .upc(upc)
                .estado(EstadoPago.PENDIENTE)
                .payloadJson(payloadJson)
                .requestCifrado(requestCifrado)
                .fechaEnvio(OffsetDateTime.now())
                .build());

        moverSaldo(cuenta, monto.negate(), "PAGO_SERVICIO", "Pago GestoPago UPC " + upc);
        return pago;
    }

    private PagoGestoPago resolverEnvio(Long idPago, GestoPagoHttpResponse http) {
        GestoPagoTxResult resultado = interpretar(http);
        return tx.execute(status -> {
            PagoGestoPago pago = pagoRepository.findByIdForUpdate(idPago).orElseThrow();
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                return pago; // el job de confirmTx ya lo resolvió
            }
            pago.setResponseRaw(bitacora(null, http));

            if (resultado == null || CODIGO_TIMEOUT.equals(resultado.getCodigo())) {
                pago.setEstado(EstadoPago.POR_CONFIRMAR);
                if (resultado != null) {
                    copiarResultado(pago, resultado);
                }
                log.warn("Pago {} queda POR_CONFIRMAR (sin respuesta o código 82)", pago.getUpc());
                return pagoRepository.save(pago);
            }

            copiarResultado(pago, resultado);
            if (CODIGO_APLICADO.equals(resultado.getCodigo()) || CODIGO_DUPLICADA_APLICADA.equals(resultado.getCodigo())) {
                pago.setEstado(EstadoPago.APLICADO);
            } else {
                pago.setEstado(EstadoPago.RECHAZADO);
                devolverSaldo(pago);
            }
            return pagoRepository.save(pago);
        });
    }

    // --------------------------------------------------------------- confirmTx

    @Override
    @Scheduled(fixedDelayString = "${gestopago.tx.confirmacion-intervalo-ms:60000}", initialDelayString = "${gestopago.tx.confirmacion-intervalo-ms:60000}")
    public void confirmarPendientes() {
        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime desde = ahora.minusHours(vigenciaConfirmacionHoras);
        List<PagoGestoPago> candidatos = new ArrayList<>(pagoRepository
                .findTop50ByEstadoAndFechaEnvioBetweenOrderByFechaEnvioAsc(
                        EstadoPago.POR_CONFIRMAR, desde, ahora.minusSeconds(esperaConfirmacionSegundos)));
        // PENDIENTE huérfano: se descontó el saldo pero la app se cayó antes de resolver sendTx
        candidatos.addAll(pagoRepository.findTop50ByEstadoAndFechaEnvioBetweenOrderByFechaEnvioAsc(
                EstadoPago.PENDIENTE, desde, ahora.minusSeconds(Math.max(esperaConfirmacionSegundos, PENDIENTE_HUERFANO_SEGUNDOS))));

        for (PagoGestoPago candidato : candidatos) {
            try {
                confirmar(candidato.getId());
            } catch (Exception e) {
                log.error("Error confirmando el pago id={}: {}", candidato.getId(), e.getMessage(), e);
            }
        }
    }

    private void confirmar(Long idPago) {
        PagoGestoPago pago = pagoRepository.findById(idPago).orElse(null);
        if (pago == null || !esPorResolver(pago.getEstado())) {
            return;
        }
        // MISMO payload (mismo upc y horaLocal) que en sendTx
        GestoPagoHttpResponse http = llamar(true, cipher.cifrar(pago.getPayloadJson()));
        GestoPagoTxResult resultado = interpretar(http);

        tx.executeWithoutResult(status -> {
            PagoGestoPago p = pagoRepository.findByIdForUpdate(idPago).orElseThrow();
            if (!esPorResolver(p.getEstado())) {
                return;
            }
            p.setIntentosConfirmacion(p.getIntentosConfirmacion() + 1);
            p.setResponseRaw(bitacora(p.getResponseRaw(), http));

            if (resultado == null) {
                log.warn("confirmTx sin respuesta para el pago {}; se reintenta en la siguiente vuelta", p.getUpc());
            } else if (CODIGO_CONFIRM_APLICADA.equals(resultado.getCodigo())) {
                copiarResultado(p, resultado);
                p.setEstado(EstadoPago.APLICADO);
                log.info("Pago {} confirmado como APLICADO", p.getUpc());
            } else if (contiene(codigosNoAplicada, resultado.getCodigo())) {
                copiarResultado(p, resultado);
                p.setEstado(EstadoPago.RECHAZADO);
                devolverSaldo(p);
                log.info("Pago {} confirmado como NO aplicado; saldo devuelto", p.getUpc());
            } else {
                log.warn("confirmTx devolvió un código no concluyente ({}) para el pago {}; se reintenta",
                        resultado.getCodigo(), p.getUpc());
            }
            pagoRepository.save(p);
        });
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    public void reportarVencidos() {
        OffsetDateTime limite = OffsetDateTime.now().minusHours(vigenciaConfirmacionHoras);
        for (EstadoPago estado : List.of(EstadoPago.POR_CONFIRMAR, EstadoPago.PENDIENTE)) {
            for (PagoGestoPago p : pagoRepository.findTop50ByEstadoAndFechaEnvioBeforeOrderByFechaEnvioAsc(estado, limite)) {
                log.error("REVISIÓN MANUAL: pago id={} upc={} cuenta={} monto={} sigue {} tras {} h; confirmTx ya no es válido. "
                                + "No se devuelve ni se cobra automáticamente.",
                        p.getId(), p.getUpc(), p.getNumCuenta(), p.getMonto(), p.getEstado(), vigenciaConfirmacionHoras);
            }
        }
    }

    // -------------------------------------------------------------- utilidades

    private void validarFormato(PagoRequest request) {
        Set<ConstraintViolation<PagoRequest>> errores = validator.validate(request);
        if (!errores.isEmpty()) {
            String mensaje = errores.stream()
                    .map(e -> e.getPropertyPath() + ": " + e.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, mensaje);
        }
        boolean hayTelefono = request.getTelefono() != null && !request.getTelefono().isBlank();
        boolean hayReferencia = request.getReferencia() != null && !request.getReferencia().isBlank();
        if (hayTelefono && hayReferencia) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "Envíe telefono o referencia, no ambos");
        }
        if (!hayTelefono && !hayReferencia) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "Debe enviar telefono (RECARGA) o referencia (SERVICIO)");
        }
        if ("RECARGA".equals(request.getTipoPago()) && !hayTelefono) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El telefono es obligatorio cuando tipoPago es RECARGA");
        }
        if ("SERVICIO".equals(request.getTipoPago())) {
            if (!hayReferencia) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "La referencia es obligatoria cuando tipoPago es SERVICIO");
            }
            if (request.getMonto() == null) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El monto es obligatorio cuando tipoPago es SERVICIO");
            }
        }
    }

    private ProductoResponse buscarProducto(Integer idServicio, Integer idProducto) {
        try {
            List<ProductoResponse> catalogo = productoServiceRedis.getProductsRedis();
            if (catalogo == null) {
                return null;
            }
            return catalogo.stream()
                    .filter(p -> idServicio.equals(p.getIdServicio()) && idProducto.equals(p.getIdProducto()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("No fue posible consultar el catálogo de GestoPago: {}", e.getMessage());
            return null;
        }
    }

    /** RECARGA ⇒ tipoFront 1. SERVICIO ⇒ tipoFront 2 o idCatTipoServicio 10/11. */
    private void validarTipoPago(TipoPago tipoPago, ProductoResponse producto) {
        Integer tipoFront = producto.getTipoFront();
        boolean esServicio = Integer.valueOf(2).equals(tipoFront) || esCatalogoSinTelefono(producto);
        boolean esRecarga = Integer.valueOf(1).equals(tipoFront);
        if (tipoPago == TipoPago.RECARGA && !esRecarga) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto indicado no es una recarga");
        }
        if (tipoPago == TipoPago.SERVICIO && !esServicio) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto indicado no es un pago de servicio");
        }
    }

    private boolean esCatalogoSinTelefono(ProductoResponse producto) {
        Integer cat = producto.getIdCatTipoServicio();
        return Integer.valueOf(10).equals(cat) || Integer.valueOf(11).equals(cat);
    }

    private BigDecimal calcularMonto(TipoPago tipoPago, PagoRequest request, ProductoResponse producto) {
        if (tipoPago == TipoPago.SERVICIO) {
            return request.getMonto();
        }
        BigDecimal precio;
        try {
            precio = new BigDecimal(producto.getPrecio().trim());
        } catch (Exception e) {
            precio = null;
        }
        if (precio == null || precio.signum() <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto no tiene un precio válido para la recarga");
        }
        if (request.getMonto() != null && request.getMonto().compareTo(precio) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH,
                    "El monto no coincide con el precio del producto (" + precio.toPlainString() + ")");
        }
        return precio;
    }

    private String generarUpcUnico() {
        for (int i = 0; i < 5; i++) {
            String upc = upcGenerator.generar();
            if (!pagoRepository.existsByUpc(upc)) {
                return upc;
            }
        }
        throw new IllegalStateException("No fue posible generar un UPC único");
    }

    private String construirPayload(PagoRequest request, TipoPago tipoPago, ProductoResponse producto, BigDecimal monto, String upc) {
        String telefono;
        if (esCatalogoSinTelefono(producto)) {
            telefono = "#1111111111";
        } else if (Integer.valueOf(2).equals(producto.getTipoFront())) {
            telefono = "1111111111";
        } else {
            telefono = request.getTelefono();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idProducto", request.getIdProducto());
        payload.put("idServicio", request.getIdServicio());
        payload.put("telefono", telefono);
        payload.put("horaLocal", ZonedDateTime.now(ZONA_MEXICO).format(HORA_LOCAL));
        payload.put("upc", upc);
        payload.put("unidad", unidad);
        if (tipoPago == TipoPago.SERVICIO) {
            payload.put("referencia", request.getReferencia());
            payload.put("montoPago", monto);
        }
        try {
            return PAYLOAD_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible construir el payload de GestoPago", e);
        }
    }

    /** sendTx/confirmTx con el token del día; ante 403 renueva el token UNA vez y reintenta. */
    private GestoPagoHttpResponse llamar(boolean confirmacion, String cifrado) {
        try {
            String token = tokenService.obtenerTokenVigente();
            GestoPagoHttpResponse respuesta = invocar(confirmacion, token, cifrado);
            if (respuesta.esTokenExpirado()) {
                token = tokenService.renovarTokenForzado();
                respuesta = invocar(confirmacion, token, cifrado);
            }
            return respuesta;
        } catch (Exception e) {
            log.error("No fue posible llamar a GestoPago: {}", e.getMessage());
            return GestoPagoHttpResponse.sinRespuesta("ERROR_LOCAL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private GestoPagoHttpResponse invocar(boolean confirmacion, String token, String cifrado) {
        return confirmacion ? txClient.confirmTx(token, cifrado) : txClient.sendTx(token, cifrado);
    }

    /** El resultado se decide por el XML, no por el status HTTP. */
    private GestoPagoTxResult interpretar(GestoPagoHttpResponse http) {
        if (http == null || http.sinRespuesta()) {
            return null;
        }
        return xmlParser.parse(http.body());
    }

    private void copiarResultado(PagoGestoPago pago, GestoPagoTxResult r) {
        pago.setCodigoRespuesta(r.getCodigo());
        pago.setTextoRespuesta(r.getTexto());
        pago.setNumAutorizacion(r.getNumAutorizacion());
        pago.setIdTx(r.getIdTx());
        if (r.getComision() != null) {
            pago.setComision(r.getComision()); // la absorbe el banco; solo se guarda para reportes
        }
    }

    private String bitacora(String anterior, GestoPagoHttpResponse http) {
        String actual = http.sinRespuesta()
                ? "SIN_RESPUESTA: " + http.error()
                : "HTTP " + http.httpStatus() + "\n" + http.body();
        if (anterior == null) {
            return actual;
        }
        return anterior + "\n--- confirmTx " + OffsetDateTime.now() + " ---\n" + actual;
    }

    private void devolverSaldo(PagoGestoPago pago) {
        Account cuenta = accountRepository.findByAccountNumberForUpdate(pago.getNumCuenta()).orElseThrow();
        moverSaldo(cuenta, pago.getMonto(), "REVERSO_PAGO", "Reverso de pago GestoPago UPC " + pago.getUpc());
    }

    private void moverSaldo(Account cuenta, BigDecimal importe, String tipoMovimiento, String descripcion) {
        BigDecimal anterior = cuenta.getBalance();
        BigDecimal nuevo = anterior.add(importe);
        cuenta.setBalance(nuevo);
        accountRepository.save(cuenta);
        accountBalanceRepository.save(AccountBalance.builder()
                .idAccount(cuenta.getIdAccount())
                .previousBalance(anterior)
                .amount(importe)
                .currentBalance(nuevo)
                .movementType(tipoMovimiento)
                .description(descripcion)
                .build());
    }

    private boolean esPorResolver(EstadoPago estado) {
        return estado == EstadoPago.POR_CONFIRMAR || estado == EstadoPago.PENDIENTE;
    }

    private boolean contiene(String csv, String codigo) {
        if (csv == null || csv.isBlank() || codigo == null) {
            return false;
        }
        return Arrays.stream(csv.split(",")).map(String::trim).anyMatch(codigo::equals);
    }

    private PagoResponse productoNoDisponible() {
        return PagoResponse.builder().codigo(0).mensaje(MSG_PRODUCTO_NO_DISPONIBLE).build();
    }

    private PagoResponse aRespuesta(PagoGestoPago pago) {
        PagoResponse.PagoResponseBuilder base = PagoResponse.builder()
                .estado(pago.getEstado())
                .codigoGestoPago(pago.getCodigoRespuesta())
                .numAutorizacion(pago.getNumAutorizacion())
                .idTx(pago.getIdTx())
                .upc(pago.getUpc());
        return switch (pago.getEstado()) {
            case APLICADO -> base.codigo(1)
                    .mensaje(pago.getTextoRespuesta() != null ? pago.getTextoRespuesta() : "Pago aplicado exitosamente")
                    .build();
            case RECHAZADO -> base.codigo(0)
                    .mensaje(pago.getTextoRespuesta() != null ? pago.getTextoRespuesta() : "El pago no fue aplicado")
                    .build();
            default -> base.codigo(0).mensaje(MSG_EN_PROCESO).build();
        };
    }
}
