package com.intrumentoev.demo.model.gestopago.pago;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Datos relevantes del XML de respuesta de sendTx/confirmTx.
 * ID_TX, NUM_AUTORIZACION y COMISION cuelgan de RESPONSE; CODIGO, TEXTO y REFERENCIA de MENSAJE.
 */
@Getter
@Builder
public class GestoPagoTxResult {
    private final String codigo;
    private final String texto;
    private final Long idTx;
    private final String numAutorizacion;
    private final BigDecimal comision;
}
