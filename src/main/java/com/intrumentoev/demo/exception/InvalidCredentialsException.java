package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseBusinessException {
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "credentials");
    }
}
