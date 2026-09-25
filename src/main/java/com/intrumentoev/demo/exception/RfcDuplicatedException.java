package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class RfcDuplicatedException extends BaseBusinessException {
    public RfcDuplicatedException(String rfc) {
        super("Ya existe un cliente registrado con el RFC: " + rfc, "RFC_DUPLICATED", HttpStatus.CONFLICT, "rfc");
    }
}
