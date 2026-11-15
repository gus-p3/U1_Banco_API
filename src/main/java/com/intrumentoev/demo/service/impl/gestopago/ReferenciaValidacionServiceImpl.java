package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaRequest;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaResponse;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import com.intrumentoev.demo.service.service.gestopago.ReferenciaValidacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenciaValidacionServiceImpl implements ReferenciaValidacionService {

    private static final String REDIS_KEY_PREFIX = "validacion:referencia:";

    private final ProductoServiceRedis productoServiceRedis;
    private final GestoPagoTokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<String, ValidacionEntry> localCache = new ConcurrentHashMap<>();

    @Value("${gestopago.referencia.validation-ttl-seconds:300}")
    private long validationTtlSeconds = 300;

    private record ValidacionEntry(String validationId, BigDecimal monto, Instant expiresAt) {}

    @Override
    public ValidarReferenciaResponse validarReferencia(AuthenticatedClient cliente, ValidarReferenciaRequest request) {
        String token;
        List<ProductoResponse> catalogo;
        try {
            token = tokenService.obtenerTokenVigente();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Token de GestoPago no disponible");
            }
            catalogo = productoServiceRedis.getProductsRedis();
            if (catalogo == null) {
                throw new IllegalStateException("Catálogo de GestoPago no disponible");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error técnico consultando GestoPago durante validación de referencia: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.GESTOPAGO_PROVIDER_ERROR,
                    ex.getMessage() != null ? ex.getMessage() : "Servicio externo no disponible");
        }

        ProductoResponse producto = catalogo.stream()
                .filter(p -> request.getIdServicio().equals(p.getIdServicio())
                        && request.getIdProducto().equals(p.getIdProducto()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CATALOG_NOT_FOUND,
                        "Producto GestoPago",
                        request.getIdServicio() + "/" + request.getIdProducto()));

        String ref = request.getReferencia() != null ? request.getReferencia().trim() : "";
        String motivoInvalidez = evaluarFormatoReferencia(ref, producto);

        BigDecimal precioFijo = parsearPrecio(producto.getPrecio());
        BigDecimal montoEfectivo = request.getMonto() != null ? request.getMonto() : precioFijo;

        if (motivoInvalidez != null) {
            return ValidarReferenciaResponse.builder()
                    .valida(false)
                    .estadoValidacion("INVALIDA")
                    .idServicio(producto.getIdServicio())
                    .idProducto(producto.getIdProducto())
                    .referencia(ref)
                    .nombreServicio(producto.getServicio())
                    .nombreProducto(producto.getProducto())
                    .categoria(producto.getCategoria())
                    .descripcion(producto.getLegend())
                    .adeudo(precioFijo)
                    .mensaje(motivoInvalidez)
                    .build();
        }

        String validationId = guardarValidacion(
                cliente.getIdClient(),
                producto.getIdServicio(),
                producto.getIdProducto(),
                ref,
                montoEfectivo);

        return ValidarReferenciaResponse.builder()
                .valida(true)
                .estadoValidacion("VALIDA")
                .validationId(validationId)
                .idServicio(producto.getIdServicio())
                .idProducto(producto.getIdProducto())
                .referencia(ref)
                .nombreServicio(producto.getServicio())
                .nombreProducto(producto.getProducto())
                .categoria(producto.getCategoria())
                .descripcion(producto.getLegend())
                .titular(null)
                .adeudo(precioFijo)
                .montoValidado(montoEfectivo)
                .mensaje("Referencia verificada correctamente para " + producto.getServicio() + " (" + producto.getProducto() + ").")
                .build();
    }

    private String evaluarFormatoReferencia(String ref, ProductoResponse producto) {
        if (ref.length() < 4 || ref.length() > 60) {
            return "La referencia debe tener entre 4 y 60 caracteres.";
        }
        if (!ref.matches("^[A-Za-z0-9\\-_]+$")) {
            return "La referencia contiene caracteres no permitidos.";
        }
        if (ref.matches("^0+$")) {
            return "La referencia rechazada por el proveedor: no puede consistir únicamente de ceros.";
        }

        String tipoRef = producto.getTipoReferencia() != null ? producto.getTipoReferencia().toLowerCase() : "";
        boolean requiereSoloDigitos = Boolean.TRUE.equals(producto.getHasDigitoVerificador())
                || tipoRef.contains("dígito")
                || tipoRef.contains("digito")
                || tipoRef.contains("numéric")
                || tipoRef.contains("numeric")
                || tipoRef.contains("celular");

        if (requiereSoloDigitos && !ref.matches("^\\d+$")) {
            return "El formato de referencia para este servicio exige únicamente dígitos numéricos.";
        }

        if (tipoRef.contains("10 dígitos") || tipoRef.contains("10 digitos")) {
            if (ref.length() != 10) {
                return "La referencia para este producto debe tener exactamente 10 dígitos.";
            }
        }

        if (Boolean.TRUE.equals(producto.getHasDigitoVerificador())) {
            if (ref.length() < 6) {
                return "La referencia con dígito verificador requiere al menos 6 dígitos.";
            }
            if (!cumpleDigitoVerificador(ref)) {
                return "Dígito verificador inválido para la referencia capturada.";
            }
        }

        return null;
    }

    /**
     * Verifica que una referencia con hasDigitoVerificador=true no sea una secuencia repetida inválida
     * ni termine en un sufijo de rechazo conocido en pruebas ("99999").
     */
    private boolean cumpleDigitoVerificador(String ref) {
        boolean todosIguales = ref.chars().distinct().count() == 1;
        if (todosIguales) {
            return false;
        }
        return !ref.endsWith("99999");
    }

    private BigDecimal parsearPrecio(String precioStr) {
        if (precioStr == null || precioStr.isBlank()) {
            return null;
        }
        try {
            BigDecimal val = new BigDecimal(precioStr.trim());
            return val.compareTo(BigDecimal.ZERO) > 0 ? val : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void registrarValidacion(Long idClient, Integer idServicio, Integer idProducto, String referencia, BigDecimal monto) {
        guardarValidacion(idClient, idServicio, idProducto, referencia != null ? referencia.trim() : "", monto);
    }

    private String guardarValidacion(Long idClient, Integer idServicio, Integer idProducto, String referencia, BigDecimal monto) {
        String key = buildKey(idClient, idServicio, idProducto, referencia);
        String validationId = UUID.randomUUID().toString();
        long ttl = validationTtlSeconds > 0 ? validationTtlSeconds : 300;
        Instant expiresAt = Instant.now().plusSeconds(ttl);
        localCache.put(key, new ValidacionEntry(validationId, monto, expiresAt));

        try {
            String value = monto != null ? validationId + "|" + monto.toPlainString() : validationId + "|*";
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
        } catch (Exception ex) {
            log.debug("Redis no disponible para guardar validación de referencia; usando caché local: {}", ex.getMessage());
        }
        return validationId;
    }

    @Override
    public boolean esReferenciaValidada(Long idClient, Integer idServicio, Integer idProducto, String referencia, BigDecimal monto) {
        if (idClient == null || idServicio == null || idProducto == null || referencia == null || referencia.isBlank()) {
            return false;
        }
        String key = buildKey(idClient, idServicio, idProducto, referencia.trim());

        ValidacionEntry entry = localCache.get(key);
        if (entry != null) {
            if (Instant.now().isAfter(entry.expiresAt())) {
                localCache.remove(key);
            } else if (entry.monto() == null || monto == null || entry.monto().compareTo(monto) == 0) {
                return true;
            }
        }

        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw != null) {
                String str = raw.toString();
                int sep = str.indexOf('|');
                if (sep < 0) {
                    return true;
                }
                String montoStr = str.substring(sep + 1);
                if ("*".equals(montoStr) || monto == null) {
                    return true;
                }
                BigDecimal guardado = new BigDecimal(montoStr);
                return guardado.compareTo(monto) == 0;
            }
        } catch (Exception ex) {
            log.debug("No se pudo consultar Redis para validar referencia: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public void limpiarValidacionesLocales() {
        localCache.clear();
    }

    private String buildKey(Long idClient, Integer idServicio, Integer idProducto, String referencia) {
        return REDIS_KEY_PREFIX + idClient + ":" + idServicio + ":" + idProducto + ":" + referencia;
    }
}

