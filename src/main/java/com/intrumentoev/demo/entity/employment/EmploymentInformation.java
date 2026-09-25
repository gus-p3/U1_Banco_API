package com.intrumentoev.demo.entity.employment;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "employment_information",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_employment_information_client", columnNames = "id_client")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_employment", nullable = false, updatable = false)
    private Long idEmployment;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 80, message = "La ocupación no puede superar los 80 caracteres")
    @Column(name = "occupation", nullable = false, length = 80)
    private String occupation;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede superar los 100 caracteres")
    @Column(name = "company", nullable = false, length = 100)
    private String company;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Column(name = "monthly_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
