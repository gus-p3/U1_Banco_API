package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;

public interface PushNotificationService {

    /**
     * Envía una notificación push (FCM o Expo) a los dispositivos activos del cliente
     * para que abra la app móvil y autorice el pago con su huella digital.
     */
    void enviarNotificacionAutorizacionPago(Long idCliente, PagoAutorizacion autorizacion);
}
