package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pagos GestoPago", description = "Pago de recargas y servicios con saldo de la cuenta del cliente vía GestoPago (sendTx / confirmTx)")
@RestController
@RequestMapping("/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final BearerTokenValidator bearerTokenValidator;
    private final PagoGestoPagoService pagoService;

    @Operation(
            summary = "Pagar una recarga o un servicio",
            description = """
                    Requiere `Authorization: Bearer <JWT>` obtenido con **login biométrico de HUELLA** (`authMethod = BIOMETRIC_HUELLA`). \
                    El login con contraseña, facial o un token refrescado NO permite pagar (403).

                    Validaciones en orden: Bearer/firma/expiración y sesión activa (401) → método huella (403) → \
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
            @RequestBody PagoRequest request) {
        // El idClient sale SOLO del JWT de esta petición, nunca del body
        AuthenticatedClient cliente = bearerTokenValidator.validarParaPago(authorization);
        return ResponseEntity.ok(pagoService.procesarPago(cliente, request));
    }
}
