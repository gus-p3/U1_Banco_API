package com.intrumentoev.demo.model.gestopago.pago;

/**
 * PENDIENTE: registrado y saldo descontado, aún sin respuesta de sendTx.
 * APLICADO: GestoPago confirmó el pago (sendTx 01 o confirmTx 06).
 * RECHAZADO: GestoPago no lo aplicó; el saldo se regresó.
 * POR_CONFIRMAR: timeout, sin respuesta o código 82; lo resuelve el job de confirmTx.
 */
public enum EstadoPago {
    PENDIENTE,
    APLICADO,
    RECHAZADO,
    POR_CONFIRMAR
}