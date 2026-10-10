package com.intrumentoev.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Salud y Estado del Servidor", description = "Endpoints de verificación de disponibilidad y conectividad del servidor")
@RestController
public class RootController {

    @Operation(
            summary = "Verificación de salud del servidor (Ping / Health Check)",
            description = "Devuelve código HTTP 200 indicando que el servidor está conectado y vivo."
    )
    @ApiResponse(responseCode = "200", description = "Servidor en línea y respondiendo peticiones")
    @GetMapping(
            value = {"/", "/health"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> pingServidor() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("message", "conectado servidor vivo");
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("service", "Banco-API-REST");
        return ResponseEntity.ok(body);
    }
}
