package com.intrumentoev.demo.controller.client;

import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.service.service.client.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "Clientes", description = "Gestión integral de clientes personas físicas y proceso de Onboarding bancario")
@RestController
@RequestMapping("/v1/clientes")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

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
        URI location = URI.create("/v1/clientes/" + response.getClient().getIdClient());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Consultar clientes con filtros (Microsoft REST Guidelines)", description = "Permite consultar todos los clientes o aplicar filtros vía parámetros de consulta (curp, rfc, email, numeroCuenta, activo, rango de fechas).")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerClientes(
            @Parameter(description = "Filtrar por CURP") @RequestParam(value = "curp", required = false) String curp,
            @Parameter(description = "Filtrar por RFC") @RequestParam(value = "rfc", required = false) String rfc,
            @Parameter(description = "Filtrar por correo electrónico") @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "Filtrar por número de cuenta asociada") @RequestParam(value = "numeroCuenta", required = false) String numeroCuenta,
            @Parameter(description = "Filtrar clientes activos") @RequestParam(value = "activo", required = false) Boolean activo,
            @Parameter(description = "Fecha inicial de registro (ISO-8601)") @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @Parameter(description = "Fecha final de registro (ISO-8601)") @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {

        if (curp != null && !curp.isBlank()) {
            return ResponseEntity.ok(clientService.obtenerClientePorCurp(curp));
        }
        if (rfc != null && !rfc.isBlank()) {
            return ResponseEntity.ok(clientService.obtenerClientePorRfc(rfc));
        }
        if (email != null && !email.isBlank()) {
            return ResponseEntity.ok(clientService.obtenerClientePorCorreo(email));
        }
        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            return ResponseEntity.ok(clientService.obtenerClientePorNumeroCuenta(numeroCuenta));
        }
        if (Boolean.TRUE.equals(activo)) {
            return ResponseEntity.ok(clientService.obtenerClientesActivos());
        }
        if (desde != null && hasta != null) {
            return ResponseEntity.ok(clientService.obtenerClientesPorRangoFechas(desde, hasta));
        }

        List<ClientResponse> clientes = clientService.obtenerTodosLosClientes();
        return ResponseEntity.ok(clientes);
    }

    @Operation(
            summary = "Obtener cliente por ID con soporte modular e includes",
            description = "Recupera la información del cliente. Soporta parámetro ?include=contact,home,employment,accounts,catalogs para cargar módulos específicos con sus IDs y catálogos asociados para edición modular en frontend. Si no se especifica 'include', devuelve todos los módulos por defecto."
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDetailResponse> obtenerClientePorId(
            @PathVariable("id") Long id,
            @Parameter(description = "Módulos a incluir separados por coma: contact,home,employment,accounts,catalogs,all")
            @RequestParam(value = "include", required = false) String include) {
        ClientDetailResponse detalle = clientService.obtenerClientePorIdConIncludes(id, include);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Obtener detalle completo de cliente", description = "Retorna el expediente integral: datos personales, contacto, domicilio, laboral y cuentas bancarias.")
    @GetMapping(value = "/{id}/detalle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDetailResponse> obtenerDetalleCompletoClientePorId(@PathVariable("id") Long id) {
        ClientDetailResponse detalle = clientService.obtenerDetalleCompletoClientePorId(id);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Buscar cliente por CURP", description = "Localiza un cliente específico mediante su clave CURP de 18 caracteres.")
    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientResponse> obtenerClientePorCurp(@PathVariable("curp") String curp) {
        ClientResponse cliente = clientService.obtenerClientePorCurp(curp);
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Buscar cliente por RFC", description = "Localiza un cliente específico mediante su clave RFC de 12 o 13 caracteres.")
    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientResponse> obtenerClientePorRfc(@PathVariable("rfc") String rfc) {
        ClientResponse cliente = clientService.obtenerClientePorRfc(rfc);
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Buscar cliente por Correo", description = "Localiza un cliente a partir de su dirección de correo electrónico registrada.")
    @GetMapping(value = "/correo/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientResponse> obtenerClientePorCorreo(@PathVariable("email") String email) {
        ClientResponse cliente = clientService.obtenerClientePorCorreo(email);
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Buscar cliente por Número de Cuenta", description = "Localiza al titular asociado a un número de cuenta bancaria de 10 dígitos.")
    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientResponse> obtenerClientePorNumeroCuenta(@PathVariable("numeroCuenta") String numeroCuenta) {
        ClientResponse cliente = clientService.obtenerClientePorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Consultar clientes activos", description = "Obtiene la lista de clientes que se encuentran activos en el sistema.")
    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientResponse>> obtenerClientesActivos() {
        List<ClientResponse> activos = clientService.obtenerClientesActivos();
        return ResponseEntity.ok(activos);
    }

    @Operation(summary = "Consultar clientes por rango de fechas", description = "Retorna los clientes dados de alta entre dos fechas especificadas.")
    @GetMapping(value = "/rango-fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientResponse>> obtenerClientesPorRangoFechas(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        List<ClientResponse> clientes = clientService.obtenerClientesPorRangoFechas(desde, hasta);
        return ResponseEntity.ok(clientes);
    }

    @Operation(summary = "Reemplazo completo de cliente (PUT)", description = "Actualiza todos los datos personales permitidos con DTO de edición. CURP y RFC no pueden ser modificados.")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientResponse> reemplazarCliente(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClientUpdateRequest request) {
        ClientResponse actualizado = clientService.reemplazarCliente(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Actualización parcial de cliente (PATCH)", description = "Actualiza los campos especificados. CURP y RFC no pueden ser modificados.")
    @PatchMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ClientResponse> actualizarParcialCliente(
            @PathVariable("id") Long id,
            @Valid @RequestBody ClientPatchRequest request) {
        ClientResponse actualizado = clientService.actualizarParcialCliente(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Baja lógica de cliente (DELETE)", description = "Desactiva al cliente y pasa todas sus cuentas bancarias asociadas a estatus INACTIVA sin borrar físicamente la información.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable("id") Long id) {
        clientService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}