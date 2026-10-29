package com.intrumentoev.demo.model.auth;

/**
 * Método con el que el cliente obtuvo su access token.
 * Se guarda como claim "authMethod" en el JWT.
 */
public enum AuthMethod {
    PASSWORD,
    BIOMETRIC_HUELLA,
    BIOMETRIC_FACIAL,
    REFRESH
}