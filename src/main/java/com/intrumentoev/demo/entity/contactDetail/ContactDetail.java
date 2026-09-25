package com.intrumentoev.demo.entity.contactDetail;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "contact_details",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_contact_details_client", columnNames = "id_client"),
                @UniqueConstraint(name = "uq_contact_details_phone", columnNames = "mobile_phone"),
                @UniqueConstraint(name = "uq_contact_details_email", columnNames = "email")
        }
)
@Getter
@Setter
public class ContactDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contact_detail", nullable = false, updatable = false)
    private Long idContactDetail;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Debe ser una dirección de correo electrónico válida")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico no es válido"
    )
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @NotBlank(message = "El teléfono móvil no puede estar vacío")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")
    @Column(name = "mobile_phone", nullable = false, length = 10)
    private String mobilePhone;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    @Column(name = "alternative_phone", length = 10)
    private String alternativePhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.normalizeData();
    }

    @PreUpdate
    protected void onUpdate() {
        this.normalizeData();
    }

    private void normalizeData() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}