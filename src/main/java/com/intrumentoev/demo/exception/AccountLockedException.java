package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BaseBusinessException {
    public AccountLockedException(String message) {
        super(message, "ACCOUNT_LOCKED", HttpStatus.LOCKED, "account");
    }
}
