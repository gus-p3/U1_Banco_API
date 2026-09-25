package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class PhoneDuplicatedException extends BaseBusinessException {
    public PhoneDuplicatedException(String phone) {
        super("Ya existe un detalle de contacto registrado con el teléfono móvil: " + phone, "PHONE_DUPLICATED", HttpStatus.CONFLICT, "mobilePhone");
    }
}
