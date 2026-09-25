package com.intrumentoev.demo.service.impl.account;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.exception.AccountNotFoundException;
import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.exception.ClientInactiveException;
import com.intrumentoev.demo.exception.ClientNotFoundException;
import com.intrumentoev.demo.mapper.account.AccountMapper;
import com.intrumentoev.demo.model.account.AccountBalanceResponse;
import com.intrumentoev.demo.model.account.AccountRequest;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.service.service.account.AccountNumberGenerator;
import com.intrumentoev.demo.service.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;

    @Value("${bank.account.initial-balance:1000.00}")
    private BigDecimal defaultInitialBalance;

    @Override
    @Transactional
    public AccountResponse crearCuenta(AccountRequest request) {
        BigDecimal balance = request.getInitialBalance() != null ? request.getInitialBalance() : defaultInitialBalance;
        return crearCuenta(request.getIdClient(), balance);
    }

    @Override
    @Transactional
    public AccountResponse crearCuenta(Long idClient, BigDecimal saldoInicial) {
        log.info("Creando cuenta bancaria para cliente ID: {} con saldo inicial: {}", idClient, saldoInicial);

        // 1. Validar existencia del cliente
        Client client = clientRepository.findById(idClient)
                .orElseThrow(() -> new ClientNotFoundException(idClient));

        // 2. Regla de negocio: Solo los clientes activos podrán tener cuentas activas
        if (!Boolean.TRUE.equals(client.getIsActive())) {
            throw new ClientInactiveException("No es posible crear cuentas bancarias para un cliente inactivo con ID: " + idClient);
        }

        // 3. Regla de negocio: Saldo inicial no negativo
        if (saldoInicial == null) {
            saldoInicial = defaultInitialBalance;
        }
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("El saldo inicial no puede ser negativo", "balance");
        }

        // 4. Generar número de cuenta único de 10 dígitos
        String accountNumber = accountNumberGenerator.generarNumeroCuentaUnico();

        Account account = Account.builder()
                .idClient(idClient)
                .accountNumber(accountNumber)
                .balance(saldoInicial)
                .status("ACTIVA")
                .build();

        Account guardada = accountRepository.save(account);
        log.info("Cuenta bancaria creada exitosamente. Número: {}, ID: {}", guardada.getAccountNumber(), guardada.getIdAccount());

        return accountMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse obtenerCuentaPorNumero(String accountNumber) {
        log.info("Consultando cuenta bancaria con número: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse obtenerSaldoPorNumeroCuenta(String accountNumber) {
        log.info("Consultando saldo de cuenta bancaria con número: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return accountMapper.toBalanceResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> obtenerCuentasActivas() {
        log.info("Consultando todas las cuentas bancarias activas");
        List<Account> activas = accountRepository.findByStatus("ACTIVA");
        return accountMapper.toResponseList(activas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> obtenerCuentasPorCliente(Long idClient) {
        log.info("Consultando cuentas bancarias del cliente ID: {}", idClient);
        List<Account> cuentas = accountRepository.findByIdClient(idClient);
        return accountMapper.toResponseList(cuentas);
    }

    @Override
    @Transactional
    public void desactivarCuentasDeCliente(Long idClient) {
        log.info("Desactivando cuentas bancarias del cliente ID: {}", idClient);
        List<Account> cuentas = accountRepository.findByIdClient(idClient);
        for (Account account : cuentas) {
            account.setStatus("INACTIVA");
        }
        accountRepository.saveAll(cuentas);
    }
}
