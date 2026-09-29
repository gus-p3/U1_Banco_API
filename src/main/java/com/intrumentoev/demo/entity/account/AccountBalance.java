package com.intrumentoev.demo.entity.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "account_balance",
        indexes = {
                @Index(name = "idx_balance_account", columnList = "id_account"),
                @Index(name = "idx_balance_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_balance", nullable = false, updatable = false)
    private Long idBalance;

    @NotNull(message = "El ID de la cuenta es obligatorio")
    @Column(name = "id_account", nullable = false)
    private Long idAccount;

    @NotNull(message = "El saldo anterior es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo anterior no puede ser negativo")
    @Column(name = "previous_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal previousBalance = BigDecimal.ZERO;

    @NotNull(message = "El monto del movimiento es obligatorio")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "El saldo resultante es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo resultante no puede ser negativo")
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @NotBlank(message = "El tipo de movimiento es obligatorio")
    @Column(name = "movement_type", nullable = false)
    @Builder.Default
    private String movementType = "APERTURA";

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
