package com.intrumentoev.demo.controller.client;

import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.service.service.client.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controlador REST para la administración de clientes y usuarios bancarios.
 * Implementa estrictamente las 4 APIs obligatorias:
 * 1. POST /buscar - Consulta y búsqueda unificada con ordenamiento (cuerpo DTO, sin PathVariable ni RequestParam)
 * 2. PATCH        - Actualización integral y granular (cuerpo DTO con identificador CURP/RFC)
 * 3. POST         - Creación integral (Onboarding bancario)
 * 4. DELETE       - Baja lógica (cuerpo DTO con identificador CURP/RFC)
 */
@Tag(name = "Clientes", description = "Administración integral de usuarios y clientes bancarios")
@RestController
@RequestMapping("/v1/clientes")
@RequiredArgsConstructor
@Validated
public class ClientController {

    private final ClientService clientService;

    // =========================================================================
    // 1. POST: Consulta y Búsqueda Unificada con Ordenamiento Dinámico
    // =========================================================================
    @Operation(
            summary = "Consulta unificada de clientes",
            description = "Permite buscar clientes filtrando por CURP, RFC, país/nacionalidad, estatus y fechas, aplicando ordenamiento dinámico. Cero @PathVariable y cero @RequestParam."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros o filtros de consulta inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(
            value = "/buscar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ClientResponse>> consultarClientes(
            @Valid @RequestBody(required = false) ClientUnifiedQueryRequest request,
            @RequestParam(name = "includes", required = false) List<String> includes) {
        List<ClientResponse> clientes = clientService.consultarClientes(request, includes);
        return ResponseEntity.ok(clientes);
    }

    // =========================================================================
    // 2. PATCH: Actualización Unificada y Modular (Súper PATCH)
    // =========================================================================
    @Operation(
            summary = "Actualización unificada de cliente (Súper PATCH)",
            description = "Actualiza de forma modular cualquier dato del cliente (personales, contacto, domicilio, laboral). El identificador (CURP/RFC) viaja en el cuerpo de la petición."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado con el identificador proporcionado"),
            @ApiResponse(responseCode = "409", description = "Correo o teléfono duplicado en otro cliente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PatchMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientDetailResponse> actualizarParcialCliente(
            @Valid @RequestBody ClientUnifiedPatchRequest request) {
        ClientDetailResponse actualizado = clientService.actualizarParcialUnificado(request);
        return ResponseEntity.ok(actualizado);
    }

    // =========================================================================
    // 3. POST: Creación Integral (Onboarding Bancario)
    // =========================================================================
    @Operation(
            summary = "Creación integral de cliente (Onboarding)",
            description = "Registra los datos personales, contacto, domicilio, información laboral y crea automáticamente una cuenta bancaria con saldo inicial."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente y cuenta bancaria creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o cliente menor de 18 años"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC, Email o Teléfono ya registrados"),
            @ApiResponse(responseCode = "422", description = "Violación de regla de negocio"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientDetailResponse> registrarCliente(
            @Valid @RequestBody ClientOnboardingRequest request) {
        ClientDetailResponse response = clientService.registrarOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // 4. DELETE: Baja Lógica Unificada
    // =========================================================================
    @Operation(
            summary = "Baja lógica de cliente",
            description = "Desactiva al cliente especificado por CURP o RFC en el cuerpo y pasa sus cuentas asociadas a estatus INACTIVA sin destruir registros físicos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente desactivado exitosamente (baja lógica)"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido en cuerpo"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "422", description = "El cliente ya se encuentra inactivo"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> desactivarCliente(
            @Valid @RequestBody ClientDeactivateRequest request) {
        clientService.desactivarClienteLogicamente(request);
        return ResponseEntity.noContent().build();
    }
}