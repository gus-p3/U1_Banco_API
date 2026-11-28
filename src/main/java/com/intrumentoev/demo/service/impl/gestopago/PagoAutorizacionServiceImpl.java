package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.entity.catalogs.OperationAmountRange;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.gestopago.DispositivoPush;
import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.pago.EstadoAutorizacionPago;
import com.intrumentoev.demo.model.gestopago.pago.PagoAutorizacionResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;
import com.intrumentoev.demo.model.gestopago.pago.PushTokenRequest;
import com.intrumentoev.demo.model.gestopago.pago.TipoPago;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.repository.catalogs.OperationAmountRangeRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.gestopago.DispositivoPushRepository;
import com.intrumentoev.demo.repository.gestopago.PagoAutorizacionRepository;
import com.intrumentoev.demo.service.service.gestopago.PagoAutorizacionService;
import com.intrumentoev.demo.service.service.gestopago.PagoGestoPagoService;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import com.intrumentoev.demo.service.service.gestopago.PushNotificationService;
import com.intrumentoev.demo.service.service.gestopago.ReferenciaValidacionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PagoAutorizacionServiceImpl implements PagoAutorizacionService {

    private final DispositivoPushRepository dispositivoRepository;
    private final PagoAutorizacionRepository autorizacionRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final ProductoService productoService;
    private final ReferenciaValidacionService referenciaValidacionService;
    private final PagoGestoPagoService pagoGestoPagoService;
    private final PushNotificationService pushNotificationService;
    private final Validator validator;

    @Autowired(required = false)
    private EmploymentInformationRepository employmentRepository;

    @Autowired(required = false)
    private OperationAmountRangeRepository operationAmountRangeRepository;

    @Autowired(required = false)
    private AuthRepository authRepository;

    @Value("${pagos.autorizacion.ttl-seconds:120}")
    private long ttlSeconds = 120L;

    @Override
    public void registrarDispositivo(AuthenticatedClient cliente, PushTokenRequest request) {
        String token = request.getToken().trim();
        DispositivoPush entity = dispositivoRepository
                .findByIdClienteAndPushToken(cliente.getIdClient(), token)
                .orElseGet(() -> DispositivoPush.builder()
                        .idCliente(cliente.getIdClient())
                        .pushToken(token)
                        .build());
        entity.setPlataforma(request.getPlataforma().trim().toUpperCase());
        entity.setDeviceName(request.getDeviceName() != null ? request.getDeviceName().trim() : null);
        entity.setActivo(Boolean.TRUE);
        dispositivoRepository.save(entity);
    }

    @Override
    public PagoAutorizacionResponse solicitarAutorizacion(AuthenticatedClient cliente, PagoRequest request) {
        validarFormato(request);
        Long idCliente = cliente.getIdClient();
        TipoPago tipoPago = TipoPago.valueOf(request.getTipoPago());

        Account cuenta = accountRepository.findByAccountNumber(request.getNumCuenta())
                .filter(a -> idCliente.equals(a.getIdClient()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FORBIDDEN));

        Client client = clientRepository.findById(idCliente)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FORBIDDEN));
        if (!Boolean.TRUE.equals(client.getIsActive())) {
            throw new BusinessException(ErrorCode.CLIENT_INACTIVE, String.valueOf(idCliente));
        }
        if (!"ACTIVA".equals(cuenta.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCOUNT_INACTIVE);
        }
        if (authRepository != null) {
            Auth auth = authRepository.findByIdClient(idCliente).orElse(null);
            boolean tieneHuella = auth != null
                    && auth.getBiometricTemplate() != null
                    && auth.getBiometricTemplate().length > 0
                    && "HUELLA".equalsIgnoreCase(auth.getBiometricType());
            if (!tieneHuella) {
                throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
            }
        }

        ProductoResponse producto = buscarProducto(request.getIdServicio(), request.getIdProducto());
        if (producto == null) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto o servicio no está disponible en el catálogo");
        }
        validarTipoPago(tipoPago, producto);
        BigDecimal monto = calcularMonto(tipoPago, request, producto);
        if (employmentRepository != null && operationAmountRangeRepository != null && monto != null) {
            EmploymentInformation emp = employmentRepository.findByIdClient(idCliente).orElse(null);
            if (emp != null && emp.getIdOperationAmount() != null) {
                OperationAmountRange rango = operationAmountRangeRepository.findById(emp.getIdOperationAmount()).orElse(null);
                if (rango != null && rango.getMaxAmount() != null && monto.compareTo(rango.getMaxAmount()) > 0) {
                    throw new BusinessException(
                            ErrorCode.OPERATION_LIMIT_EXCEEDED,
                            monto.toPlainString(),
                            rango.getLabel()
                    );
                }
            }
        }

        if (cuenta.getBalance().compareTo(monto) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        if (tipoPago == TipoPago.SERVICIO && !referenciaValidacionService.esReferenciaValidada(
                idCliente, request.getIdServicio(), request.getIdProducto(), request.getReferencia(), monto)) {
            throw new BusinessException(ErrorCode.REFERENCE_NOT_VALIDATED);
        }

        String telefono = request.getTelefono() != null ? request.getTelefono().trim() : null;
        String referencia = request.getReferencia() != null ? request.getReferencia().trim() : null;
        String destinoMasked = PagoGestoPagoServiceImpl.enmascararReferencia(
                tipoPago == TipoPago.RECARGA ? telefono : referencia);

        PagoAutorizacion entity = PagoAutorizacion.builder()
                .id(UUID.randomUUID().toString())
                .idCliente(idCliente)
                .numCuenta(cuenta.getAccountNumber())
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .tipoPago(tipoPago)
                .nombreServicio(producto.getServicio() != null ? producto.getServicio() : "Servicio #" + request.getIdServicio())
                .nombreProducto(producto.getProducto() != null ? producto.getProducto() : "Producto #" + request.getIdProducto())
                .telefono(telefono)
                .referencia(referencia)
                .destinoEnmascarado(destinoMasked)
                .monto(monto)
                .estado(EstadoAutorizacionPago.PENDIENTE)
                .expiraEn(OffsetDateTime.now().plusSeconds(Math.max(30L, ttlSeconds)))
                .build();

        PagoAutorizacion saved = autorizacionRepository.save(entity);
        pushNotificationService.enviarNotificacionAutorizacionPago(idCliente, saved);
        return aRespuesta(saved);
    }

    @Override
    public List<PagoAutorizacionResponse> listarPendientes(AuthenticatedClient cliente) {
        List<PagoAutorizacion> pendientes = autorizacionRepository
                .findByIdClienteAndEstadoOrderByFechaCreacionDesc(cliente.getIdClient(), EstadoAutorizacionPago.PENDIENTE);
        OffsetDateTime ahora = OffsetDateTime.now();
        List<PagoAutorizacionResponse> vigentes = new ArrayList<>();
        for (PagoAutorizacion auth : pendientes) {
            if (auth.getExpiraEn().isBefore(ahora)) {
                auth.setEstado(EstadoAutorizacionPago.EXPIRADO);
                autorizacionRepository.save(auth);
            } else {
                vigentes.add(aRespuesta(auth));
            }
        }
        return vigentes;
    }

    @Override
    public PagoAutorizacionResponse consultarAutorizacion(AuthenticatedClient cliente, String idAutorizacion) {
        PagoAutorizacion auth = obtenerDeCliente(cliente, idAutorizacion);
        if (auth.getEstado() == EstadoAutorizacionPago.PENDIENTE && auth.getExpiraEn().isBefore(OffsetDateTime.now())) {
            auth.setEstado(EstadoAutorizacionPago.EXPIRADO);
            auth = autorizacionRepository.save(auth);
        }
        return aRespuesta(auth);
    }

    @Override
    public PagoAutorizacionResponse aprobarAutorizacion(AuthenticatedClient cliente, String idAutorizacion) {
        PagoAutorizacion auth = obtenerDeCliente(cliente, idAutorizacion);
        validarPendienteVigente(auth);

        PagoRequest pagoRequest = PagoRequest.builder()
                .numCuenta(auth.getNumCuenta())
                .idServicio(auth.getIdServicio())
                .idProducto(auth.getIdProducto())
                .tipoPago(auth.getTipoPago().name())
                .telefono(auth.getTelefono())
                .referencia(auth.getReferencia())
                .monto(auth.getTipoPago() == TipoPago.SERVICIO ? auth.getMonto() : null)
                .build();

        PagoResponse resultado = pagoGestoPagoService.procesarPago(cliente, pagoRequest);

        auth.setEstado(EstadoAutorizacionPago.APROBADO);
        auth.setPagoCodigo(resultado.getCodigo());
        auth.setPagoEstado(resultado.getEstado());
        auth.setPagoMensaje(resultado.getMensaje());
        auth.setPagoCodigoGestoPago(resultado.getCodigoGestoPago());
        auth.setPagoNumAutorizacion(resultado.getNumAutorizacion());
        auth.setPagoIdTx(resultado.getIdTx());
        auth.setPagoUpc(resultado.getUpc());
        PagoAutorizacion saved = autorizacionRepository.save(auth);
        return aRespuesta(saved);
    }

    @Override
    public PagoAutorizacionResponse rechazarAutorizacion(AuthenticatedClient cliente, String idAutorizacion) {
        PagoAutorizacion auth = obtenerDeCliente(cliente, idAutorizacion);
        validarPendienteVigente(auth);
        auth.setEstado(EstadoAutorizacionPago.RECHAZADO);
        return aRespuesta(autorizacionRepository.save(auth));
    }

    @Override
    public PagoAutorizacionResponse cancelarAutorizacion(AuthenticatedClient cliente, String idAutorizacion) {
        PagoAutorizacion auth = obtenerDeCliente(cliente, idAutorizacion);
        validarPendienteVigente(auth);
        auth.setEstado(EstadoAutorizacionPago.CANCELADO);
        return aRespuesta(autorizacionRepository.save(auth));
    }

    private PagoAutorizacion obtenerDeCliente(AuthenticatedClient cliente, String idAutorizacion) {
        PagoAutorizacion auth = autorizacionRepository.findById(idAutorizacion.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND, idAutorizacion));
        if (!cliente.getIdClient().equals(auth.getIdCliente())) {
            throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
        }
        return auth;
    }

    private void validarPendienteVigente(PagoAutorizacion auth) {
        if (auth.getEstado() != EstadoAutorizacionPago.PENDIENTE) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_INVALID_STATE,
                    "La solicitud de autorización ya fue procesada (" + auth.getEstado() + ")");
        }
        if (auth.getExpiraEn().isBefore(OffsetDateTime.now())) {
            auth.setEstado(EstadoAutorizacionPago.EXPIRADO);
            autorizacionRepository.save(auth);
            throw new BusinessException(ErrorCode.AUTHORIZATION_INVALID_STATE,
                    "La solicitud de autorización expiró. Inicia el pago nuevamente desde la web.");
        }
    }

    private void validarFormato(PagoRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El cuerpo de la solicitud de pago es obligatorio");
        }
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
        if ("RECARGA".equals(request.getTipoPago())) {
            if (!hayTelefono) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El telefono es obligatorio cuando tipoPago es RECARGA");
            }
            if (request.getTelefono().trim().matches("^0+$")) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El teléfono no puede consistir únicamente de ceros");
            }
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
            List<ProductoResponse> catalogo = productoService.getProductsRedis();
            if (catalogo == null) return null;
            return catalogo.stream()
                    .filter(p -> idServicio.equals(p.getIdServicio()) && idProducto.equals(p.getIdProducto()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error consultando catálogo para autorización: {}", e.getMessage());
            return null;
        }
    }

    private void validarTipoPago(TipoPago tipoPago, ProductoResponse producto) {
        Integer tipoFront = producto.getTipoFront();
        Integer cat = producto.getIdCatTipoServicio();
        boolean esCatalogoSinTelefono = Integer.valueOf(10).equals(cat) || Integer.valueOf(11).equals(cat);
        boolean esServicio = Integer.valueOf(2).equals(tipoFront) || esCatalogoSinTelefono;
        boolean esRecarga = Integer.valueOf(1).equals(tipoFront);
        if (tipoPago == TipoPago.RECARGA && !esRecarga) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto indicado no es una recarga");
        }
        if (tipoPago == TipoPago.SERVICIO && !esServicio) {
            throw new BusinessException(ErrorCode.PAYMENT_PRODUCT_MISMATCH, "El producto indicado no es un pago de servicio");
        }
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
        return precio;
    }

    private PagoAutorizacionResponse aRespuesta(PagoAutorizacion a) {
        PagoResponse resultadoPago = null;
        if (a.getEstado() == EstadoAutorizacionPago.APROBADO && a.getPagoCodigo() != null) {
            resultadoPago = PagoResponse.builder()
                    .codigo(a.getPagoCodigo())
                    .estado(a.getPagoEstado())
                    .mensaje(a.getPagoMensaje())
                    .codigoGestoPago(a.getPagoCodigoGestoPago())
                    .numAutorizacion(a.getPagoNumAutorizacion())
                    .idTx(a.getPagoIdTx())
                    .upc(a.getPagoUpc())
                    .build();
        }
        return PagoAutorizacionResponse.builder()
                .idAutorizacion(a.getId())
                .estado(a.getEstado())
                .tipoPago(a.getTipoPago())
                .numCuenta(a.getNumCuenta())
                .idServicio(a.getIdServicio())
                .idProducto(a.getIdProducto())
                .nombreServicio(a.getNombreServicio())
                .nombreProducto(a.getNombreProducto())
                .destinoEnmascarado(a.getDestinoEnmascarado())
                .monto(a.getMonto())
                .expiraEn(a.getExpiraEn())
                .fechaCreacion(a.getFechaCreacion())
                .resultadoPago(resultadoPago)
                .build();
    }
}
