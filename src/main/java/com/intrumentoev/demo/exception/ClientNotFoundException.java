package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class ClientNotFoundException extends BaseBusinessException {
    public ClientNotFoundException(String message) {
        super(message, "CLIENT_NOT_FOUND", HttpStatus.NOT_FOUND, "client");
    }

    public ClientNotFoundException(Long id) {
        super("Cliente no encontrado con ID: " + id, "CLIENT_NOT_FOUND", HttpStatus.NOT_FOUND, "idClient");
    }
}
