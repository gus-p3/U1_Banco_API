package com.intrumentoev.demo.entity.home;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "home",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_home_client", columnNames = "id_client")
        },
        indexes = {
                @Index(name = "idx_home_municipality", columnList = "id_municipality")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Home {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_home", nullable = false, updatable = false)
    private Long idHome;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
    @Column(name = "street", nullable = false, length = 100)
    private String street;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no puede superar los 10 caracteres")
    @Column(name = "exterior_number", nullable = false, length = 10)
    private String exteriorNumber;

    @Size(max = 10, message = "El número interior no puede superar los 10 caracteres")
    @Column(name = "interior_number", length = 10)
    private String interiorNumber;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 80, message = "La colonia no puede superar los 80 caracteres")
    @Column(name = "neighborhood", nullable = false, length = 80)
    private String neighborhood;

    @NotNull(message = "El municipio es obligatorio")
    @Column(name = "id_municipality", nullable = false)
    private Integer idMunicipality;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    @Column(name = "postal_code", nullable = false, length = 5)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 50)
    @Builder.Default
    private String country = "México";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
