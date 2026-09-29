package com.intrumentoev.demo.service.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio de cifrado autenticado de alta seguridad basado en AES-256-GCM.
 * Utilizado para cifrar datos biométricos (huellas y rostros) y credenciales sensibles
 * en la base de datos a nivel de aplicación (cifrado en reposo y en tránsito).
 */
@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recomendado para GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits de etiqueta de autenticación
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public AesEncryptionService(@Value("${security.encryption.aes-key:B4nc0$egur0Cl13nt3$P3rs0n4$F1s1c4$2026!}") String secretSeed) {
        try {
            // Derivación de clave simétrica de 256 bits mediante SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secretSeed.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", e);
        }
    }

    /**
     * Cifra un arreglo de bytes en texto plano utilizando AES-256-GCM.
     * Retorna una secuencia [IV (12 bytes) + Ciphertext + GCM Tag (16 bytes)].
     */
    public byte[] encryptBytes(byte[] plainData) {
        if (plainData == null || plainData.length == 0) {
            return plainData;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainData);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return byteBuffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Fallo en el cifrado AES-256 de datos biométricos", e);
        }
    }

    /**
     * Descifra un arreglo de bytes previamente cifrado con encryptBytes.
     */
    public byte[] decryptBytes(byte[] encryptedDataWithIv) {
        if (encryptedDataWithIv == null || encryptedDataWithIv.length < GCM_IV_LENGTH) {
            return encryptedDataWithIv;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedDataWithIv);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al descifrar datos biométricos cifrados con AES-256", e);
        }
    }

    /**
     * Cifra texto plano y devuelve representación Base64.
     */
    public String encryptText(String plainText) {
        if (plainText == null) return null;
        byte[] encrypted = encryptBytes(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Descifra un texto previamente cifrado en Base64.
     */
    public String decryptText(String cipherTextBase64) {
        if (cipherTextBase64 == null) return null;
        byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
        byte[] decrypted = decryptBytes(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
