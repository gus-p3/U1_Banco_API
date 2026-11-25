package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.PagoDetalleResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoHistorialPageResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.gestopago.PagoGestoPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Tag(name = "Pagos GestoPago", description = "Pago de recargas y servicios con saldo de la cuenta del cliente vía GestoPago (sendTx / confirmTx)")
@RestController
@RequestMapping("/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    public static final String HEADER_CONFIRMATION_TOKEN = "X-Confirmation-Token";

    private final BearerTokenValidator bearerTokenValidator;
    private final PagoGestoPagoService pagoService;

    @Operation(
            summary = "Pagar una recarga o un servicio (exclusivo app móvil con huella digital)",
            description = """
                    Requiere `Authorization: Bearer <JWT>` y el encabezado `X-Confirmation-Token` emitido por \
                    `POST /v1/auth/confirmar-operacion` mediante **huella digital (`BIOMETRIC_HUELLA`)** \
                    (vida corta, de un solo uso y ligado al cliente). No se permiten pagos desde la web ni con contraseña.

                    Validaciones en orden: Bearer/firma/expiración (401) → token de confirmación biométrico de huella y único (403 STRONG_AUTH_REQUIRED) → \
                    cuenta del cliente del token (403, mensaje genérico) → cliente y cuenta activos (422) → producto en el catálogo de GestoPago → saldo (422).

                    `codigo`: 1 = aplicado (CODIGO "01"), 0 = no se pudo o en proceso. Un pago `POR_CONFIRMAR` devuelve codigo 0 y el `upc`; \
                    el sistema lo confirma automáticamente con confirmTx. La comisión la absorbe el banco: solo se descuenta el monto.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PagoRequest.class),
                    examples = {
                            @ExampleObject(name = "Recarga",
                                    value = "{\n  \"numCuenta\": \"9876543210\",\n  \"idServicio\": 133,\n  \"idProducto\": 582,\n  \"tipoPago\": \"RECARGA\",\n  \"telefono\": \"5512345678\"\n}"),
                            @ExampleObject(name = "Servicio",
                                    value = "{\n  \"numCuenta\": \"9876543210\",\n  \"idServicio\": 79,\n  \"idProducto\": 209,\n  \"tipoPago\": \"SERVICIO\",\n  \"referencia\": \"40425475190118187271\",\n  \"monto\": 150.00\n}")
                    }))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado del pago (revisar codigo/estado): aplicado, rechazado, por confirmar o producto no disponible"),
            @ApiResponse(responseCode = "400", description = "Cuerpo inválido (campos faltantes, telefono y referencia juntos, etc.)"),
            @ApiResponse(responseCode = "401", description = "Sin Bearer, token inválido/expirado o sesión del servidor inactiva"),
            @ApiResponse(responseCode = "403", description = "El token no es de huella, o la cuenta no es del cliente"),
            @ApiResponse(responseCode = "422", description = "Cliente/cuenta inactivos, producto que no coincide con tipoPago, o saldo insuficiente")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoResponse> pagar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = HEADER_CONFIRMATION_TOKEN, required = false) String confirmationToken,
            @RequestBody PagoRequest request) {
        // El idClient sale SOLO del JWT de esta petición, nunca del body
        AuthenticatedClient cliente = bearerTokenValidator.validarParaPago(authorization, confirmationToken);
        return ResponseEntity.ok(pagoService.procesarPago(cliente, request));
    }

    @Operation(
            summary = "Listar historial de pagos del cliente autenticado (paginado y filtrable)",
            description = "Devuelve únicamente los pagos pertenecientes al cliente del JWT, paginados y filtrables por `estado` y rango de fechas (`desde`, `hasta`), con la referencia/teléfono parcialmente enmascarados."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoHistorialPageResponse> listarHistorial(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El parámetro 'page' no puede ser negativo");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "El parámetro 'size' debe estar entre 1 y 100");
        }
        EstadoPago estadoEnum = parsearEstado(estado);
        OffsetDateTime desdeDt = parsearFecha(desde, false);
        OffsetDateTime hastaDt = parsearFecha(hasta, true);
        if (desdeDt != null && hastaDt != null && desdeDt.isAfter(hastaDt)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "La fecha inicial 'desde' no puede ser posterior a 'hasta'");
        }
        return ResponseEntity.ok(pagoService.listarPagosCliente(cliente, estadoEnum, desdeDt, hastaDt, page, size));
    }

    @Operation(
            summary = "Consultar estado y detalle de un pago por ID o UPC",
            description = "Devuelve el estado actual (APLICADO, RECHAZADO, POR_CONFIRMAR, PENDIENTE) y el detalle de un pago perteneciente exclusivamente al cliente autenticado en el JWT."
    )
    @GetMapping(value = "/{idOrUpc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoDetalleResponse> consultarPago(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("idOrUpc") String idOrUpc) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        if (idOrUpc == null || idOrUpc.isBlank() || idOrUpc.trim().length() > 40 || !idOrUpc.trim().matches("^[A-Za-z0-9\\-_]+$")) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "Identificador de pago inválido");
        }
        return ResponseEntity.ok(pagoService.consultarPagoCliente(cliente, idOrUpc));
    }

    private EstadoPago parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPago.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "Estado de pago inválido: " + estado);
        }
    }

    private OffsetDateTime parsearFecha(String valor, boolean finDelDia) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String trimmed = valor.trim();
        try {
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed);
                return finDelDia
                        ? date.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                        : date.atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(trimmed);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "Formato de fecha inválido: " + valor);
        }
    }
}
