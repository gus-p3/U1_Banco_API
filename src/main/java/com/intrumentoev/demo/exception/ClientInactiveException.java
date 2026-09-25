package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class ClientInactiveException extends BaseBusinessException {
    public ClientInactiveException(String message) {
        super(message, "CLIENT_INACTIVE", HttpStatus.BAD_REQUEST, "client");
    }
}
