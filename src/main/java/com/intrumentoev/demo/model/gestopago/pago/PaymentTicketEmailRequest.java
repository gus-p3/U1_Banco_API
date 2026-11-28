package com.intrumentoev.demo.model.gestopago.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTicketEmailRequest {
    private String destinatarioEmail;
    private String nombreCliente;
    private String estado;
    private String tipoOperacion;
    private String nombreServicio;
    private String nombreProducto;
    private String destino;
    private String cuentaEnmascarada;
    private BigDecimal monto;
    private String numAutorizacion;
    private Long idTx;
    private String upc;
    private String fechaHora;
    private String leyendaProducto;
    private String aclaraciones;
}
