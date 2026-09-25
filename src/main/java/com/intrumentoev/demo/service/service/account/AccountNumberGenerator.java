package com.intrumentoev.demo.service.service.account;

import com.intrumentoev.demo.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un número de cuenta único de exactamente 10 dígitos numéricos.
     * Ejemplo: "1002345678"
     */
    public String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            // Garantizar que no empiece con 0 para mantener exactamente 10 dígitos
            long number = 1_000_000_000L + (long)(random.nextDouble() * 9_000_000_000L);
            numeroCuenta = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(numeroCuenta));

        return numeroCuenta;
    }
}
