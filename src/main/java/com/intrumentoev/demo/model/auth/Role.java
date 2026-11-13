package com.intrumentoev.demo.model.auth;

/**
 * Roles de acceso. Se guardan en la tabla auth y viajan como claim "role" en el JWT.
 * Spring Security los expone como authorities "ROLE_CLIENT" / "ROLE_ADMIN".
 */
public enum Role {
    CLIENT,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
