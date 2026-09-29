package com.intrumentoev.demo.service;

import com.intrumentoev.demo.service.service.auth.AesEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AesEncryptionServiceTest {

    private AesEncryptionService aesEncryptionService;

    @BeforeEach
    void setUp() {
        aesEncryptionService = new AesEncryptionService("ClaveSecretaDePruebaParaAes256Gcm2026!");
    }

    @Test
    @DisplayName("Cifrado y descifrado de bytes biométricos con AES-256-GCM")
    void testCifradoYDescifradoBytes() {
        byte[] original = "vector-biometrico-huella-dactilar-minutiae".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = aesEncryptionService.encryptBytes(original);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(original);
        assertThat(encrypted.length).isGreaterThan(original.length);

        byte[] decrypted = aesEncryptionService.decryptBytes(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("Cifrado y descifrado de texto en Base64")
    void testCifradoYDescifradoTexto() {
        String original = "DatoConfidencialBancario12345";

        String encrypted = aesEncryptionService.encryptText(original);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(original);

        String decrypted = aesEncryptionService.decryptText(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }
}
