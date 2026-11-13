package com.intrumentoev.demo.controller.account;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.account.AccountBalanceMovementResponse;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cuentas", description = "Consulta de movimientos de las cuentas del cliente autenticado")
@RestController
@RequestMapping("/v1/cuentas")
@RequiredArgsConstructor
public class AccountController {

    private final BearerTokenValidator bearerTokenValidator;
    private final AccountService accountService;

    @Operation(
            summary = "Movimientos de una cuenta",
            description = "Devuelve los movimientos (más recientes primero) de una cuenta del cliente dueño del JWT. "
                    + "Si la cuenta no es del cliente responde 403 con mensaje genérico."
    )
    @GetMapping(value = "/{accountNumber}/movimientos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountBalanceMovementResponse>> obtenerMovimientos(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("accountNumber") String accountNumber) {
        var cliente = bearerTokenValidator.validarCliente(authorization);
        var cuenta = accountService.obtenerCuentaPorNumero(accountNumber);
        if (!cliente.getIdClient().equals(cuenta.getIdClient())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return ResponseEntity.ok(accountService.obtenerMovimientosPorNumeroCuenta(accountNumber));
    }
}