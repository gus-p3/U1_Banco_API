package com.intrumentoev.demo.controller.client;

import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.service.service.client.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Clientes", description = "Gestión integral de clientes personas físicas y proceso de Onboarding bancario")
@RestController
@RequestMapping("/v1/clientes")
@RequiredArgsConstructor
@Validated
public class ClientController {

    private final ClientService clientService;

    private static final String IDENTIFICADOR_REGEX = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$";
    private static final String IDENTIFICADOR_MESSAGE = "El identificador debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido. No se permite el uso de IDs numéricos.";

    @Operation(summary = "Onboarding Integral de Cliente", description = "Registra los datos personales, contacto, domicilio, información laboral y crea automáticamente una cuenta bancaria con saldo inicial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente y cuenta bancaria creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o cliente menor de 18 años"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC, Email o Teléfono ya registrados"),
            @ApiResponse(responseCode = "422", description = "Violación de regla de negocio")
    })
    @PostMapping(
            value = "/onboarding",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientDetailResponse> registrarOnboarding(
            @Valid @RequestBody ClientOnboardingRequest request) {
        ClientDetailResponse response = clientService.registrarOnboarding(request);
        URI location = URI.create("/v1/clientes/" + response.getClient().getCurp());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Listar todos los clientes", description = "Obtiene la lista completa de todos los clientes registrados sin exponer identificadores internos numéricos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista completa de clientes obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientResponse>> obtenerTodosLosClientes() {
        return ResponseEntity.ok(clientService.obtenerTodosLosClientes());
    }

    @Operation(
            summary = "Búsqueda unificada de clientes",
            description = "Permite buscar clientes aplicando filtros en el cuerpo de la petición (curp, rfc, email, numeroCuenta, activo, desde, hasta). Si el cuerpo no contiene ningún filtro, retorna error 400."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda realizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Cuerpo vacío o sin filtros de búsqueda válidos"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "422", description = "Rango de fechas inválido"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(
            value = "/buscar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ClientResponse>> buscarClientes(
            @RequestBody(required = false) ClientSearchRequest request) {
        if (request == null || !request.hasAtLeastOneFilter()) {
            throw new BusinessValidationException(
                    "Debe proporcionar al menos un filtro de búsqueda (curp, rfc, email, numeroCuenta, activo o rango de fechas 'desde' y 'hasta')",
                    "filtros"
            );
        }
        List<ClientResponse> resultados = clientService.buscarClientes(request);
        return ResponseEntity.ok(resultados);
    }

    @Operation(
            summary = "Obtener cliente por CURP o RFC con soporte modular e includes",
            description = "Recupera la información del cliente identificado exclusivamente por su CURP o RFC. Soporta parámetro ?include=contact,home,employment,accounts,catalogs para cargar módulos específicos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del cliente obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "403", description = "No autorizado para consultar la información de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado con la CURP o RFC especificado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/{identificador}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDetailResponse> obtenerClientePorIdentificador(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Parameter(description = "Módulos a incluir separados por coma: contact,home,employment,accounts,catalogs,all")
            @RequestParam(value = "include", required = false) String include) {
        ClientDetailResponse detalle = clientService.obtenerClientePorIdentificadorConIncludes(identificador, include);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Reemplazo completo de cliente (PUT por CURP o RFC)", description = "Actualiza todos los datos personales permitidos identificado exclusivamente por CURP o RFC. CURP y RFC no pueden ser modificados. No se permite editar por ID numérico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "403", description = "No autorizado para modificar datos de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping(
            value = "/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientResponse> reemplazarCliente(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody ClientUpdateRequest request) {
        ClientResponse actualizado = clientService.reemplazarClientePorIdentificador(identificador, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Actualización parcial de cliente (PATCH por CURP o RFC)", description = "Actualiza los campos especificados identificado exclusivamente por CURP o RFC. CURP y RFC no pueden ser modificados. No se permite editar por ID numérico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado parcialmente con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "403", description = "No autorizado para modificar datos de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "422", description = "Cuerpo de solicitud sin campos válidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PatchMapping(
            value = "/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientResponse> actualizarParcialCliente(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody ClientPatchRequest request) {
        ClientResponse actualizado = clientService.actualizarParcialClientePorIdentificador(identificador, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Baja lógica de cliente (DELETE por CURP o RFC)", description = "Desactiva al cliente identificado exclusivamente por CURP o RFC y pasa todas sus cuentas bancarias asociadas a estatus INACTIVA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente y cuentas bancarias desactivados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{identificador}")
    public ResponseEntity<Void> eliminarCliente(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        clientService.eliminarClientePorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }
}