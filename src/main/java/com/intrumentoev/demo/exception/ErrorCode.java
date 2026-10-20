package com.intrumentoev.demo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de errores de negocio para toda la API.
 * Define el código de error unificado, el estatus HTTP y la plantilla del mensaje.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- 404 Not Found ---
    CLIENT_NOT_FOUND("CLIENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Cliente no encontrado con identificador: %s", "identificador"),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "Cuenta no encontrada con número: %s", "accountNumber"),
    CATALOG_NOT_FOUND("CATALOG_NOT_FOUND", HttpStatus.NOT_FOUND, "Registro de %s no encontrado con ID: %s", "catalog"),

    // --- 409 Conflict (Duplicados) ---
    CURP_DUPLICATED("CURP_DUPLICATED", HttpStatus.CONFLICT, "La CURP '%s' ya se encuentra registrada", "curp"),
    RFC_DUPLICATED("RFC_DUPLICATED", HttpStatus.CONFLICT, "El RFC '%s' ya se encuentra registrado", "rfc"),
    EMAIL_DUPLICATED("EMAIL_DUPLICATED", HttpStatus.CONFLICT, "El correo electrónico '%s' ya está en uso", "email"),
    PHONE_DUPLICATED("PHONE_DUPLICATED", HttpStatus.CONFLICT, "El teléfono móvil '%s' ya se encuentra registrado", "mobilePhone"),

    // --- 422 Unprocessable Entity (Reglas de Negocio) ---
    CLIENT_INACTIVE("CLIENT_INACTIVE", HttpStatus.UNPROCESSABLE_ENTITY, "El cliente '%s' ya se encuentra inactivo", "identificador"),
    INVALID_AGE("INVALID_AGE", HttpStatus.UNPROCESSABLE_ENTITY, "El cliente debe ser mayor de edad (al menos 18 años). Edad actual: %s años", "birthDate"),
    STATE_MISMATCH("STATE_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "La clave de entidad '%s' no coincide con la entidad federativa del municipio seleccionado ('%s' - %s)", "claveEntidad"),
    BUSINESS_VALIDATION("BUSINESS_VALIDATION", HttpStatus.UNPROCESSABLE_ENTITY, "%s", "rule"),

    // --- 401 & 403 & 423 Seguridad y Autenticación ---
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Credenciales de acceso inválidas", "password"),
    BIOMETRIC_AUTH_FAILED("BIOMETRIC_AUTH_FAILED", HttpStatus.UNAUTHORIZED, "%s", "biometrics"),
    TOKEN_EXPIRED_OR_INVALID("TOKEN_EXPIRED_OR_INVALID", HttpStatus.UNAUTHORIZED, "El token de sesión ha expirado o es inválido", "token"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", HttpStatus.LOCKED, "La cuenta se encuentra bloqueada por exceso de intentos fallidos", "auth"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Acceso no autorizado al recurso solicitado", "security");

    private final String code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;
    private final String defaultTarget;

    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return String.format(messageTemplate, args);
    }
}
