package com.intrumentoev.demo.service.account;

import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.service.service.account.AccountNumberGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountNumberGenerator generator;

    @Test
    @DisplayName("Genera número de cuenta único de 10 dígitos sin colisión")
    void testGenerarNumeroCuentaUnico() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        String cuenta = generator.generarNumeroCuentaUnico();

        assertThat(cuenta).isNotNull();
        assertThat(cuenta).hasSize(10).matches("\\d{10}");
        assertThat(cuenta.charAt(0)).isNotEqualTo('0');
    }

    @Test
    @DisplayName("Reintenta generación si el primer número ya existe")
    void testGenerarNumeroCuenta_ReintentoPorColision() {
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String cuenta = generator.generarNumeroCuentaUnico();

        assertThat(cuenta).hasSize(10);
    }
}

