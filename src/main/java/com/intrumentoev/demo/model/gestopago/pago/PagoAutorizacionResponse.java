package com.intrumentoev.demo.model.gestopago.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoAutorizacionResponse {

    private String idAutorizacion;
    private EstadoAutorizacionPago estado;
    private TipoPago tipoPago;
    private String numCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String nombreServicio;
    private String nombreProducto;
    private String destinoEnmascarado;
    private BigDecimal monto;
    private OffsetDateTime expiraEn;
    private OffsetDateTime fechaCreacion;
    private PagoResponse resultadoPago;
}
