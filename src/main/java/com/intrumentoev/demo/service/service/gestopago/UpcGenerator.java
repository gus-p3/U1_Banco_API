package com.intrumentoev.demo.service.service.gestopago;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Genera el UPC (Unique Payment Code) de GestoPago: alfanumérico de máximo 15 caracteres
 * = prefijo + base36 del timestamp + relleno aleatorio base36.
 */
@Component
public class UpcGenerator {

    public static final int MAX_LENGTH = 15;
    private static final String PREFIX = "BX";
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generar() {
        String timestamp = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        StringBuilder upc = new StringBuilder(PREFIX).append(timestamp);
        while (upc.length() < MAX_LENGTH) {
            upc.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return upc.substring(0, MAX_LENGTH);
    }
}
