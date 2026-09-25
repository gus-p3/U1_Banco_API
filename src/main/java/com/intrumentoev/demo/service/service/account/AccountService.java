package com.intrumentoev.demo.service.service.account;

import com.intrumentoev.demo.model.account.AccountBalanceResponse;
import com.intrumentoev.demo.model.account.AccountRequest;
import com.intrumentoev.demo.model.account.AccountResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    /**
     * Crea una cuenta bancaria asociada a un cliente activo con saldo inicial.
     */
    AccountResponse crearCuenta(AccountRequest request);

    /**
     * Crea una cuenta bancaria interna con idClient y saldo inicial.
     */
    AccountResponse crearCuenta(Long idClient, BigDecimal saldoInicial);

    /**
     * Consulta una cuenta por su número de cuenta de 10 dígitos.
     */
    AccountResponse obtenerCuentaPorNumero(String accountNumber);

    /**
     * Consulta el saldo disponible de una cuenta por su número de cuenta.
     */
    AccountBalanceResponse obtenerSaldoPorNumeroCuenta(String accountNumber);

    /**
     * Consulta todas las cuentas con estatus ACTIVA.
     */
    List<AccountResponse> obtenerCuentasActivas();

    /**
     * Consulta las cuentas asociadas a un cliente por su ID.
     */
    List<AccountResponse> obtenerCuentasPorCliente(Long idClient);

    /**
     * Desactiva todas las cuentas asociadas a un cliente (baja lógica).
     */
    void desactivarCuentasDeCliente(Long idClient);
}
