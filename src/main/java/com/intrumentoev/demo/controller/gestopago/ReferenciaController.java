package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaRequest;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaResponse;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.gestopago.ReferenciaValidacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Referencias GestoPago", description = "Validación previa de referencias de servicios contra el catálogo y formato de GestoPago")
@RestController
@RequestMapping("/v1/referencias")
@RequiredArgsConstructor
public class ReferenciaController {

    private final BearerTokenValidator bearerTokenValidator;
    private final ReferenciaValidacionService referenciaValidacionService;

    @Operation(
            summary = "Validar una referencia de pago de servicio",
            description = "Verifica que el producto exista en GestoPago, valida el formato y dígito verificador de la referencia, y registra la validación en Redis con TTL corto para autorizar el pago posterior."
    )
    @PostMapping(value = "/validar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidarReferenciaResponse> validarReferencia(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ValidarReferenciaRequest request) {
        AuthenticatedClient cliente = bearerTokenValidator.validarCliente(authorization);
        return ResponseEntity.ok(referenciaValidacionService.validarReferencia(cliente, request));
    }
}

