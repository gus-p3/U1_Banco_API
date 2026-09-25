package com.intrumentoev.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseBusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String target;

    protected BaseBusinessException(String message, String errorCode, HttpStatus httpStatus, String target) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.target = target;
    }
}
