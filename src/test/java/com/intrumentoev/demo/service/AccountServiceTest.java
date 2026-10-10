package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.exception.ClientInactiveException;
import com.intrumentoev.demo.mapper.account.AccountMapper;
import com.intrumentoev.demo.model.account.AccountBalanceResponse;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.service.impl.account.AccountServiceImpl;
import com.intrumentoev.demo.service.service.account.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private com.intrumentoev.demo.repository.account.AccountBalanceRepository accountBalanceRepository;

    @Mock
    private com.intrumentoev.demo.service.service.auth.ServerSessionManager serverSessionManager;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Client activeClient;
    private Client inactiveClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountService, "defaultInitialBalance", new BigDecimal("1000.00"));

        activeClient = new Client();
        activeClient.setIdClient(1L);
        activeClient.setIsActive(true);

        inactiveClient = new Client();
        inactiveClient.setIdClient(2L);
        inactiveClient.setIsActive(false);
    }

    @Test
    @DisplayName("Crear cuenta bancaria exitosa para cliente activo")
    void testCrearCuentaExitoso() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(activeClient));
        when(accountNumberGenerator.generarNumeroCuentaUnico()).thenReturn("1234567890");

        Account savedAccount = Account.builder()
                .idAccount(1L)
                .accountNumber("1234567890")
                .idClient(1L)
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVA")
                .build();
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponse mockResponse = AccountResponse.builder()
                .idAccount(1L)
                .accountNumber("1234567890")
                .idClient(1L)
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVA")
                .build();
        when(accountMapper.toResponse(savedAccount)).thenReturn(mockResponse);

        AccountResponse response = accountService.crearCuenta(1L, new BigDecimal("1000.00"));

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("1234567890");
        assertThat(response.getStatus()).isEqualTo("ACTIVA");
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        verify(accountBalanceRepository, times(1)).save(argThat(balance ->
                balance.getIdAccount().equals(1L) &&
                balance.getAmount().compareTo(new BigDecimal("1000.00")) == 0 &&
                balance.getCurrentBalance().compareTo(new BigDecimal("1000.00")) == 0 &&
                "APERTURA".equals(balance.getMovementType())
        ));
    }

    @Test
    @DisplayName("Crear cuenta bancaria rechaza cliente inactivo")
    void testCrearCuentaClienteInactivo() {
        when(clientRepository.findById(2L)).thenReturn(Optional.of(inactiveClient));

        assertThatThrownBy(() -> accountService.crearCuenta(2L, new BigDecimal("500.00")))
                .isInstanceOf(ClientInactiveException.class)
                .hasMessageContaining("cliente inactivo");
    }

    @Test
    @DisplayName("Crear cuenta rechaza saldo inicial negativo")
    void testCrearCuentaSaldoNegativo() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(activeClient));

        assertThatThrownBy(() -> accountService.crearCuenta(1L, new BigDecimal("-100.00")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("saldo inicial no puede ser negativo");
    }

    @Test
    @DisplayName("Consultar saldo de cuenta existente")
    void testConsultarSaldo() {
        Account account = Account.builder()
                .accountNumber("1234567890")
                .balance(new BigDecimal("2500.50"))
                .status("ACTIVA")
                .build();
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(account));
        when(accountMapper.toBalanceResponse(account)).thenReturn(
                AccountBalanceResponse.builder()
                        .accountNumber("1234567890")
                        .balance(new BigDecimal("2500.50"))
                        .status("ACTIVA")
                        .build()
        );

        AccountBalanceResponse saldo = accountService.obtenerSaldoPorNumeroCuenta("1234567890");

        assertThat(saldo).isNotNull();
        assertThat(saldo.getBalance()).isEqualByComparingTo("2500.50");
    }

    @Test
    @DisplayName("Consultar movimientos de la tabla de saldo de cuenta")
    void testConsultarMovimientosSaldo() {
        Account account = Account.builder()
                .idAccount(1L)
                .accountNumber("1234567890")
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVA")
                .build();
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(account));

        com.intrumentoev.demo.entity.account.AccountBalance movApertura = com.intrumentoev.demo.entity.account.AccountBalance.builder()
                .idBalance(10L)
                .idAccount(1L)
                .previousBalance(BigDecimal.ZERO)
                .amount(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00"))
                .movementType("APERTURA")
                .description("Apertura de cuenta")
                .build();
        when(accountBalanceRepository.findByIdAccountOrderByCreatedAtDesc(1L)).thenReturn(List.of(movApertura));

        List<com.intrumentoev.demo.model.account.AccountBalanceMovementResponse> movimientos =
                accountService.obtenerMovimientosPorNumeroCuenta("1234567890");

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).getIdBalance()).isEqualTo(10L);
        assertThat(movimientos.get(0).getAccountNumber()).isEqualTo("1234567890");
        assertThat(movimientos.get(0).getMovementType()).isEqualTo("APERTURA");
        assertThat(movimientos.get(0).getCurrentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Desactivar cuentas bancarias de un cliente")
    void testDesactivarCuentasCliente() {
        Account cuenta1 = Account.builder().idAccount(1L).status("ACTIVA").build();
        Account cuenta2 = Account.builder().idAccount(2L).status("ACTIVA").build();
        when(accountRepository.findByIdClient(1L)).thenReturn(List.of(cuenta1, cuenta2));

        accountService.desactivarCuentasDeCliente(1L);

        assertThat(cuenta1.getStatus()).isEqualTo("INACTIVA");
        assertThat(cuenta2.getStatus()).isEqualTo("INACTIVA");
        verify(accountRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Consultar cuentas por cliente exitoso para usuario autorizado")
    void testObtenerCuentasPorClienteExitoso() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(activeClient));
        when(serverSessionManager.isUserLoggedIn()).thenReturn(true);
        when(serverSessionManager.getActiveClientId()).thenReturn(1L);

        Account c = Account.builder().idAccount(10L).accountNumber("1234567890").idClient(1L).build();
        when(accountRepository.findByIdClient(1L)).thenReturn(List.of(c));
        when(accountMapper.toResponseList(List.of(c))).thenReturn(List.of(
                AccountResponse.builder().idAccount(10L).accountNumber("1234567890").build()
        ));

        List<AccountResponse> result = accountService.obtenerCuentasPorCliente(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("Consultar cuentas por cliente lanza ClientNotFoundException (404) si el cliente no existe")
    void testObtenerCuentasPorClienteNoExiste() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.obtenerCuentasPorCliente(999L))
                .isInstanceOf(com.intrumentoev.demo.exception.ClientNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Consultar cuentas por cliente lanza AccessDeniedException (403) si el usuario intenta consultar cuentas ajenas")
    void testObtenerCuentasPorClienteAccesoDenegado() {
        when(serverSessionManager.isUserLoggedIn()).thenReturn(true);
        when(serverSessionManager.getActiveClientId()).thenReturn(1L);
        when(serverSessionManager.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> accountService.obtenerCuentasPorCliente(2L))
                .isInstanceOf(com.intrumentoev.demo.exception.AccessDeniedException.class)
                .hasMessageContaining("No tiene autorización");
    }
}
