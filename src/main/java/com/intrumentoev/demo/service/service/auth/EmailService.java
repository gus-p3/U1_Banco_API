package com.intrumentoev.demo.service.service.auth;

public interface EmailService {

    /**
     * Envía un código de verificación de un solo uso mediante Brevo.
     */
    void enviarCodigoVerificacion(String destinatarioEmail, String nombre, String codigo, String proposito, long minutosVigencia);
}
