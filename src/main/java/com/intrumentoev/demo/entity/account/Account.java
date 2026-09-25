package com.intrumentoev.demo.entity.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_account_number", columnNames = "account_number")
        },
        indexes = {
                @Index(name = "idx_account_client", columnList = "id_client"),
                @Index(name = "idx_account_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_account", nullable = false, updatable = false)
    private Long idAccount;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe tener exactamente 10 dígitos numéricos")
    @Column(name = "account_number", nullable = false, length = 10, unique = true)
    private String accountNumber;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @NotNull(message = "El saldo es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo no puede ser negativo")
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @NotBlank(message = "El estatus de la cuenta es obligatorio")
    @Pattern(regexp = "^(ACTIVA|INACTIVA)$", message = "El estatus de la cuenta debe ser ACTIVA o INACTIVA")
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "ACTIVA";

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
