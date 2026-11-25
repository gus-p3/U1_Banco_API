package com.intrumentoev.demo.controller;

import com.intrumentoev.demo.config.GlobalExceptionHandler;
import com.intrumentoev.demo.controller.account.AccountController;
import com.intrumentoev.demo.model.account.AccountBalanceMovementResponse;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BearerTokenValidator bearerTokenValidator;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /v1/cuentas/{accountNumber}/movimientos devuelve movimientos si la cuenta pertenece al cliente")
    void obtenerMovimientosExitoso() throws Exception {
        when(bearerTokenValidator.validarCliente(anyString()))
                .thenReturn(AuthenticatedClient.builder()
                        .idClient(1L)
                        .email("cliente@banco.com")
                        .authMethod(AuthMethod.PASSWORD)
                        .build());

        when(accountService.obtenerCuentaPorNumero("9876543210"))
                .thenReturn(AccountResponse.builder()
                        .idAccount(10L)
                        .idClient(1L)
                        .accountNumber("9876543210")
                        .balance(new BigDecimal("25000.00"))
                        .status("ACTIVA")
                        .build());

        when(accountService.obtenerMovimientosPorNumeroCuenta("9876543210"))
                .thenReturn(List.of(AccountBalanceMovementResponse.builder()
                        .idBalance(1L)
                        .accountNumber("9876543210")
                        .movementType("APERTURA")
                        .amount(new BigDecimal("25000.00"))
                        .build()));

        mockMvc.perform(get("/v1/cuentas/9876543210/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("APERTURA"));
    }

    @Test
    @DisplayName("GET /v1/cuentas/{accountNumber}/movimientos devuelve 403 si la cuenta pertenece a otro cliente")
    void obtenerMovimientosCuentaAjena() throws Exception {
        when(bearerTokenValidator.validarCliente(anyString()))
                .thenReturn(AuthenticatedClient.builder()
                        .idClient(1L)
                        .email("cliente@banco.com")
                        .authMethod(AuthMethod.PASSWORD)
                        .build());

        when(accountService.obtenerCuentaPorNumero("9876543210"))
                .thenReturn(AccountResponse.builder()
                        .idAccount(10L)
                        .idClient(99L)
                        .accountNumber("9876543210")
                        .build());

        mockMvc.perform(get("/v1/cuentas/9876543210/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isForbidden());
    }
}

