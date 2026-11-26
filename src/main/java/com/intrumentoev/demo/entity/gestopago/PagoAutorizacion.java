package com.intrumentoev.demo.entity.gestopago;

import com.intrumentoev.demo.model.gestopago.pago.EstadoAutorizacionPago;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.TipoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pago_autorizacion",
        indexes = {
                @Index(name = "idx_pago_autorizacion_cliente_estado", columnList = "id_cliente, estado, fecha_creacion")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoAutorizacion {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "id_cliente", nullable = false, updatable = false)
    private Long idCliente;

    @Column(name = "num_cuenta", nullable = false, length = 10, updatable = false)
    private String numCuenta;

    @Column(name = "id_servicio", nullable = false, updatable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false, updatable = false)
    private Integer idProducto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 10, updatable = false)
    private TipoPago tipoPago;

    @Column(name = "nombre_servicio", length = 150)
    private String nombreServicio;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(name = "telefono", length = 20, updatable = false)
    private String telefono;

    @Column(name = "referencia", length = 100, updatable = false)
    private String referencia;

    @Column(name = "destino_enmascarado", length = 60)
    private String destinoEnmascarado;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoAutorizacionPago estado = EstadoAutorizacionPago.PENDIENTE;

    @Column(name = "pago_codigo")
    private Integer pagoCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "pago_estado", length = 20)
    private EstadoPago pagoEstado;

    @Column(name = "pago_mensaje", columnDefinition = "TEXT")
    private String pagoMensaje;

    @Column(name = "pago_codigo_gestopago", length = 20)
    private String pagoCodigoGestoPago;

    @Column(name = "pago_num_autorizacion", length = 40)
    private String pagoNumAutorizacion;

    @Column(name = "pago_id_tx")
    private Long pagoIdTx;

    @Column(name = "pago_upc", length = 20)
    private String pagoUpc;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
