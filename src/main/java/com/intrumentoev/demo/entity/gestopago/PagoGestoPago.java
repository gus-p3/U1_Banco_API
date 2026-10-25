package com.intrumentoev.demo.entity.gestopago;

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
        name = "pago_gestopago",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pago_gestopago_upc", columnNames = "upc")
        },
        indexes = {
                @Index(name = "idx_pago_cliente", columnList = "id_cliente"),
                @Index(name = "idx_pago_estado_envio", columnList = "estado, fecha_envio")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoGestoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

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

    @Column(name = "telefono", length = 20, updatable = false)
    private String telefono;

    @Column(name = "referencia", length = 100, updatable = false)
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal monto;

    @Column(name = "comision", precision = 15, scale = 2)
    private BigDecimal comision;

    @Column(name = "upc", nullable = false, length = 15, updatable = false)
    private String upc;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "texto_respuesta", columnDefinition = "TEXT")
    private String textoRespuesta;

    @Column(name = "num_autorizacion", length = 30)
    private String numAutorizacion;

    @Column(name = "id_tx")
    private Long idTx;

    // JSON sin cifrar: confirmTx debe reenviar exactamente el mismo payload (mismo upc y horaLocal)
    @Column(name = "payload_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "request_cifrado", columnDefinition = "TEXT")
    private String requestCifrado;

    @Column(name = "response_raw", columnDefinition = "TEXT")
    private String responseRaw;

    @Column(name = "intentos_confirmacion", nullable = false)
    @Builder.Default
    private Integer intentosConfirmacion = 0;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}