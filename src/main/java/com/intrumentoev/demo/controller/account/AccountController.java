package com.intrumentoev.demo.controller.account;

import com.intrumentoev.demo.model.account.AccountBalanceResponse;
import com.intrumentoev.demo.model.account.AccountRequest;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.service.service.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Cuentas Bancarias", description = "Operaciones y consultas de cuentas bancarias asociadas a clientes")
@RestController
@RequestMapping("/v1/cuentas")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Apertura de cuenta bancaria", description = "Crea una cuenta bancaria asociada a un cliente activo con saldo inicial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cliente inactivo o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "422", description = "Saldo inicial negativo")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AccountResponse> crearCuenta(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.crearCuenta(request);
        URI location = URI.create("/v1/cuentas/" + response.getAccountNumber());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Consultar cuenta por número", description = "Obtiene los detalles de la cuenta bancaria por su número de 10 dígitos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalles de la cuenta bancaria obtenidos exitosamente"),
            @ApiResponse(responseCode = "400", description = "Número de cuenta inválido (debe contener exactamente 10 dígitos)"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> obtenerCuentaPorNumero(
            @PathVariable("numeroCuenta")
            @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos numéricos")
            String numeroCuenta) {
        AccountResponse response = accountService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar saldo de cuenta bancaria", description = "Obtiene el saldo disponible y estatus de la cuenta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo y estatus de cuenta obtenidos exitosamente"),
            @ApiResponse(responseCode = "400", description = "Número de cuenta inválido (debe contener 10 dígitos)"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountBalanceResponse> obtenerSaldoPorNumeroCuenta(
            @PathVariable("numeroCuenta")
            @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos numéricos")
            String numeroCuenta) {
        AccountBalanceResponse response = accountService.obtenerSaldoPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cuentas activas", description = "Lista todas las cuentas bancarias con estatus ACTIVA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de cuentas activas obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> obtenerCuentasActivas() {
        List<AccountResponse> response = accountService.obtenerCuentasActivas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cuentas de un cliente", description = "Retorna el listado de cuentas bancarias asociadas al cliente especificado por su ID positivo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de cuentas del cliente obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "ID de cliente inválido o menor a 1"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "403", description = "No autorizado para consultar registros o cuentas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado con el ID especificado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/cliente/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> obtenerCuentasPorCliente(
            @PathVariable("idClient")
            @Positive(message = "El identificador del cliente debe ser un número entero positivo mayor a 0")
            Long idClient) {
        List<AccountResponse> response = accountService.obtenerCuentasPorCliente(idClient);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar historial de la tabla de saldos", description = "Retorna el historial completo de saldos y movimientos registrados en la tabla de saldos (libro mayor).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de saldos y movimientos obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Número de cuenta inválido"),
            @ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = {"/{numeroCuenta}/saldos", "/{numeroCuenta}/movimientos"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.intrumentoev.demo.model.account.AccountBalanceMovementResponse>> obtenerMovimientosPorNumeroCuenta(
            @PathVariable("numeroCuenta")
            @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos numéricos")
            String numeroCuenta) {
        List<com.intrumentoev.demo.model.account.AccountBalanceMovementResponse> response = accountService.obtenerMovimientosPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(response);
    }
}
