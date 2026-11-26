package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.PagoAutorizacionResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PushTokenRequest;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.gestopago.PagoAutorizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Autorización Remota de Pagos (Web -> Móvil)", description = "Flujo tipo Mercado Libre: la web solicita autorización, Android recibe notificación y aprueba con huella digital")
@RestController
@RequiredArgsConstructor
public class PagoAutorizacionController {

    private final BearerTokenValidator bearerTokenValidator;
    private final PagoAutorizacionService autorizacionService;

    @Operation(summary = "Registrar token push del dispositivo móvil (FCM / Expo)")
    @PostMapping(value = "/v1/dispositivos/push-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registrarPushToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PushTokenRequest request) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        autorizacionService.registrarDispositivo(cliente, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar autorización remota de pago desde Web (envía notificación al móvil)")
    @PostMapping(value = "/v1/pagos/autorizaciones", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoAutorizacionResponse> solicitarAutorizacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody PagoRequest request) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(autorizacionService.solicitarAutorizacion(cliente, request));
    }

    @Operation(summary = "Listar solicitudes de autorización pendientes del cliente (para app móvil)")
    @GetMapping(value = "/v1/pagos/autorizaciones/pendientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PagoAutorizacionResponse>> listarPendientes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(autorizacionService.listarPendientes(cliente));
    }

    @Operation(summary = "Consultar estado y resultado de una solicitud de autorización (polling Web)")
    @GetMapping(value = "/v1/pagos/autorizaciones/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoAutorizacionResponse> consultarAutorizacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") String id) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(autorizacionService.consultarAutorizacion(cliente, id));
    }

    @Operation(summary = "Aprobar solicitud de pago desde Android con huella digital (X-Confirmation-Token)")
    @PostMapping(value = "/v1/pagos/autorizaciones/{id}/aprobar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoAutorizacionResponse> aprobarAutorizacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = PagoController.HEADER_CONFIRMATION_TOKEN, required = false) String confirmationToken,
            @PathVariable("id") String id) {
        AuthenticatedClient cliente = bearerTokenValidator.validarParaPago(authorization, confirmationToken);
        return ResponseEntity.ok(autorizacionService.aprobarAutorizacion(cliente, id));
    }

    @Operation(summary = "Rechazar solicitud de pago desde Android")
    @PostMapping(value = "/v1/pagos/autorizaciones/{id}/rechazar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoAutorizacionResponse> rechazarAutorizacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") String id) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(autorizacionService.rechazarAutorizacion(cliente, id));
    }

    @Operation(summary = "Cancelar solicitud de pago pendiente desde Web")
    @PostMapping(value = "/v1/pagos/autorizaciones/{id}/cancelar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoAutorizacionResponse> cancelarAutorizacion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") String id) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(autorizacionService.cancelarAutorizacion(cliente, id));
    }
}
