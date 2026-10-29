package com.intrumentoev.demo.model.gestopago.pago;

/**
 * Respuesta HTTP cruda de sendTx/confirmTx.
 * sinRespuesta = true cuando hubo timeout o corte de conexión (no se sabe si GestoPago aplicó la transacción).
 */
public record GestoPagoHttpResponse(Integer httpStatus, String body, boolean sinRespuesta, String error) {

    public static GestoPagoHttpResponse conRespuesta(int httpStatus, String body) {
        return new GestoPagoHttpResponse(httpStatus, body, false, null);
    }

    public static GestoPagoHttpResponse sinRespuesta(String error) {
        return new GestoPagoHttpResponse(null, null, true, error);
    }

    public boolean esTokenExpirado() {
        return httpStatus != null && httpStatus == 403;
    }
}
