package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.entity.auth.AuthChallenge;
import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;

public interface PushNotificationService {

    /**
     * Envía una notificación push (FCM o Expo) a los dispositivos activos del cliente
     * para que abra la app móvil y autorice el pago con su huella digital.
     */
    void enviarNotificacionAutorizacionPago(Long idCliente, PagoAutorizacion autorizacion);

    /**
     * Envía una notificación push a los dispositivos activos del cliente
     * para aprobar con su huella un inicio de sesión pendiente en la Web.
     */
    default void enviarNotificacionLogin2fa(Long idCliente, AuthChallenge challenge) {
        // Implementación opcional por defecto para compatibilidad en pruebas unitarias
    }
}
