package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.PagoDetalleResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoHistorialPageResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;

import java.time.OffsetDateTime;

public interface PagoGestoPagoService {

    /**
     * Valida y ejecuta un pago (sendTx). El cliente viene del JWT ya validado, nunca del body.
     */
    PagoResponse procesarPago(AuthenticatedClient cliente, PagoRequest request);

    /**
     * Consulta el estado y detalle de un pago por ID o UPC verificando que pertenezca al cliente del JWT.
     */
    PagoDetalleResponse consultarPagoCliente(AuthenticatedClient cliente, String idOrUpc);

    /**
     * Lista paginada y filtrable del historial de pagos exclusivamente del cliente del JWT,
     * con referencia/teléfono parcialmente enmascarados.
     */
    PagoHistorialPageResponse listarPagosCliente(AuthenticatedClient cliente,
                                                 EstadoPago estado,
                                                 OffsetDateTime desde,
                                                 OffsetDateTime hasta,
                                                 int page,
                                                 int size);

    /**
     * Job: llama a confirmTx con el mismo payload para los pagos POR_CONFIRMAR (y PENDIENTE huérfanos).
     */
    void confirmarPendientes();

    /**
     * Registra en el log los pagos POR_CONFIRMAR con más de 24 h (confirmTx ya no es válido): revisión manual.
     */
    void reportarVencidos();
}
