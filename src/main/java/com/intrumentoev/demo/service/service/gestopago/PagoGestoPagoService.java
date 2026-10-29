package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;

public interface PagoGestoPagoService {

    /**
     * Valida y ejecuta un pago (sendTx). El cliente viene del JWT ya validado, nunca del body.
     */
    PagoResponse procesarPago(AuthenticatedClient cliente, PagoRequest request);

    /**
     * Job: llama a confirmTx con el mismo payload para los pagos POR_CONFIRMAR (y PENDIENTE huérfanos).
     */
    void confirmarPendientes();

    /**
     * Registra en el log los pagos POR_CONFIRMAR con más de 24 h (confirmTx ya no es válido): revisión manual.
     */
    void reportarVencidos();
}
