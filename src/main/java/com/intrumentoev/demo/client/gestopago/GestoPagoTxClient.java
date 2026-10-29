package com.intrumentoev.demo.client.gestopago;

import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;

/**
 * Cliente de las operaciones de transacción de GestoPago (form-urlencoded: random + signed).
 */
public interface GestoPagoTxClient {

    GestoPagoHttpResponse sendTx(String bearerToken, String payloadCifrado);

    GestoPagoHttpResponse confirmTx(String bearerToken, String payloadCifrado);
}
