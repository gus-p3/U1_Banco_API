package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.model.gestopago.pago.PaymentTicketEmailRequest;

public interface EmailService {

    /**
     * Envía un código de verificación de un solo uso mediante Brevo.
     */
    void enviarCodigoVerificacion(String destinatarioEmail, String nombre, String codigo, String proposito, long minutosVigencia);

    /**
     * Envía el comprobante / ticket de pago por correo con la leyenda del producto e información de aclaraciones.
     */
    default void enviarTicketPago(PaymentTicketEmailRequest ticket) {
        // Implementación por defecto vacía para mocks/stubs en pruebas que no requieren envío de ticket.
    }
}
