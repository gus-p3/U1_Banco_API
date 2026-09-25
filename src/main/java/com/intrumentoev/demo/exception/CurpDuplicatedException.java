package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class CurpDuplicatedException extends BaseBusinessException {
    public CurpDuplicatedException(String curp) {
        super("Ya existe un cliente registrado con la CURP: " + curp, "CURP_DUPLICATED", HttpStatus.CONFLICT, "curp");
    }
}
