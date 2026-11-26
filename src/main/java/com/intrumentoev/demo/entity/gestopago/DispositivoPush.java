package com.intrumentoev.demo.entity.gestopago;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dispositivo_push",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dispositivo_push_cliente_token", columnNames = {"id_cliente", "push_token"})
        },
        indexes = {
                @Index(name = "idx_dispositivo_push_cliente", columnList = "id_cliente")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "id_cliente", nullable = false, updatable = false)
    private Long idCliente;

    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Column(name = "plataforma", nullable = false, length = 20)
    @Builder.Default
    private String plataforma = "ANDROID";

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
