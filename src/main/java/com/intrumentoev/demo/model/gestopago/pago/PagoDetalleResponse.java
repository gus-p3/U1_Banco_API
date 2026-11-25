package com.intrumentoev.demo.model.gestopago.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoDetalleResponse {

    private Long id;
    private Integer codigo;
    private String mensaje;
    private EstadoPago estado;
    private TipoPago tipoPago;
    private String numCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String nombreServicio;
    private String nombreProducto;
    private String categoria;
    private String logoUrl;
    private String telefono;
    private String referencia;
    private String destinoEnmascarado;
    private BigDecimal monto;
    private BigDecimal comision;
    private String codigoGestoPago;
    private String numAutorizacion;
    private Long idTx;
    private String upc;
    private OffsetDateTime fechaEnvio;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
