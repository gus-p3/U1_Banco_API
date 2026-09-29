package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class BiometricAuthenticationException extends BaseBusinessException {
    public BiometricAuthenticationException(String message) {
        super(message, "BIOMETRIC_AUTH_FAILED", HttpStatus.UNAUTHORIZED, "biometric");
    }
}
