package com.intrumentoev.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción única y centralizada para todas las reglas de negocio del sistema.
 * Se parametriza a través del enum ErrorCode.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCodeEnum;
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String target;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCodeEnum = errorCode;
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
        this.target = errorCode.getDefaultTarget();
    }

    public BusinessException(ErrorCode errorCode, String customMessage, String target) {
        super(customMessage != null ? customMessage : errorCode.format());
        this.errorCodeEnum = errorCode;
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
        this.target = target != null ? target : errorCode.getDefaultTarget();
    }
}
