package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredOrInvalidException extends BaseBusinessException {
    public TokenExpiredOrInvalidException(String message) {
        super(message, "INVALID_OR_EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED, "token");
    }
}
