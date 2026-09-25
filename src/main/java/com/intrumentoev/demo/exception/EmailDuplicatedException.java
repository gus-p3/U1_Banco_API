package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class EmailDuplicatedException extends BaseBusinessException {
    public EmailDuplicatedException(String email) {
        super("Ya existe un detalle de contacto registrado con el correo electrónico: " + email, "EMAIL_DUPLICATED", HttpStatus.CONFLICT, "email");
    }
}
