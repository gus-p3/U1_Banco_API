package com.intrumentoev.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un usuario autenticado intenta consultar o modificar un registro
 * que no le pertenece (prevención de manipulación de ID / IDOR / BOLA).
 */
public class AccessDeniedException extends BaseBusinessException {

    public AccessDeniedException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN, "authorization");
    }

    public AccessDeniedException(String message, String target) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN, target);
    }
}
