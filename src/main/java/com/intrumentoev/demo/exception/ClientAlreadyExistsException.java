package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class ClientAlreadyExistsException extends BaseBusinessException {
    public ClientAlreadyExistsException(String message) {
        super(message, "CLIENT_ALREADY_EXISTS", HttpStatus.CONFLICT, "client");
    }
}
