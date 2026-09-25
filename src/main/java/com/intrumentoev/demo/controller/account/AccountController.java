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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Cuentas Bancarias", description = "Operaciones y consultas de cuentas bancarias asociadas a clientes")
@RestController
@RequestMapping("/v1/cuentas")
@RequiredArgsConstructor
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
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> obtenerCuentaPorNumero(
            @PathVariable("numeroCuenta") String numeroCuenta) {
        AccountResponse response = accountService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar saldo de cuenta bancaria", description = "Obtiene el saldo disponible y estatus de la cuenta.")
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountBalanceResponse> obtenerSaldoPorNumeroCuenta(
            @PathVariable("numeroCuenta") String numeroCuenta) {
        AccountBalanceResponse response = accountService.obtenerSaldoPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cuentas activas", description = "Lista todas las cuentas bancarias con estatus ACTIVA.")
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> obtenerCuentasActivas() {
        List<AccountResponse> response = accountService.obtenerCuentasActivas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar cuentas de un cliente", description = "Retorna el listado de cuentas bancarias asociadas al cliente.")
    @GetMapping(value = "/cliente/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> obtenerCuentasPorCliente(
            @PathVariable("idClient") Long idClient) {
        List<AccountResponse> response = accountService.obtenerCuentasPorCliente(idClient);
        return ResponseEntity.ok(response);
    }
}
