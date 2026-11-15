package com.intrumentoev.demo.model.gestopago.referencia;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidarReferenciaResponse {

    private boolean valida;
    private String estadoValidacion;
    private String validationId;
    private Integer idServicio;
    private Integer idProducto;
    private String referencia;
    private String nombreServicio;
    private String nombreProducto;
    private String categoria;
    private String descripcion;
    /** Titular de la cuenta/servicio si el proveedor lo devuelve (nulo cuando GestoPago no lo incluye). */
    private String titular;
    /** Adeudo o importe fijo del producto cuando el catálogo de GestoPago lo define (> 0). */
    private BigDecimal adeudo;
    /** Monto validado para el pago. */
    private BigDecimal montoValidado;
    private String mensaje;
}

