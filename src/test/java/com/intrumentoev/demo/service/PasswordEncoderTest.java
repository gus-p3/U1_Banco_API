package com.intrumentoev.demo.service;

import com.intrumentoev.demo.service.service.auth.PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    void encodeAndMatches_Exito() {
        String pass = "PasswordSegura#2026";
        String encoded = encoder.encode(pass);

        assertNotNull(encoded);
        assertTrue(encoder.matches(pass, encoded));
        assertFalse(encoder.matches("WrongPassword", encoded));
    }

    @Test
    void matches_NullValues() {
        assertFalse(encoder.matches(null, "hash"));
        assertFalse(encoder.matches("pass", null));
        assertFalse(encoder.matches(null, null));
    }

    @Test
    void matches_FormatoInvalido() {
        assertFalse(encoder.matches("pass", "invalid_format"));
        assertFalse(encoder.matches("pass", "10000:not_base64:invalid"));
    }
}
