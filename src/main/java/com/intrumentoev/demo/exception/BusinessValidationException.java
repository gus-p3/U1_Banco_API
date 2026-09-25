package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BaseBusinessException {
    public BusinessValidationException(String message, String target) {
        super(message, "BUSINESS_VALIDATION_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, target);
    }

    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, "body");
    }
}
