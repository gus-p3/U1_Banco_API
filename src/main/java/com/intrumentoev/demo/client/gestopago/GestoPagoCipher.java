package com.intrumentoev.demo.client.gestopago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Cifrado del payload que exige GestoPago ("Security on payload"):
 * AES-256-CBC con PKCS5Padding, llave (32 bytes) e IV (16 bytes) en Base64.
 * El resultado en Base64 se envía en el parámetro "signed" de sendTx/confirmTx.
 */
@Component
public class GestoPagoCipher {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 16;

    private final SecretKeySpec secretKey;
    private final IvParameterSpec iv;

    public GestoPagoCipher(@Value("${gestopago.crypto.secret-key}") String secretKeyBase64,
                           @Value("${gestopago.crypto.iv}") String ivBase64) {
        byte[] keyBytes = decodificar(secretKeyBase64, "gestopago.crypto.secret-key", KEY_LENGTH_BYTES);
        byte[] ivBytes = decodificar(ivBase64, "gestopago.crypto.iv", IV_LENGTH_BYTES);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.iv = new IvParameterSpec(ivBytes);
    }

    public String cifrar(String textoPlano) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cifrado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar el payload de GestoPago", e);
        }
    }

    public String descifrar(String cifradoBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] plano = cipher.doFinal(Base64.getDecoder().decode(cifradoBase64));
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No fue posible descifrar el payload de GestoPago", e);
        }
    }

    private static byte[] decodificar(String base64, String propiedad, int longitudEsperada) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Falta configurar la propiedad " + propiedad);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La propiedad " + propiedad + " no es Base64 válido", e);
        }
        if (bytes.length != longitudEsperada) {
            throw new IllegalStateException("La propiedad " + propiedad + " debe medir " + longitudEsperada
                    + " bytes y mide " + bytes.length);
        }
        return bytes;
    }
}
