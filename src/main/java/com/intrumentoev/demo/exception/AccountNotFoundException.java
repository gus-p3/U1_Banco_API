package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BaseBusinessException {
    public AccountNotFoundException(String accountNumber) {
        super("Cuenta bancaria no encontrada con número: " + accountNumber, "ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "accountNumber");
    }
}
