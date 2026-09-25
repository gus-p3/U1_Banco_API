package com.intrumentoev.demo.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Table(
        name = "client",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_client_curp", columnNames = "curp"),
                @UniqueConstraint(name = "uq_client_rfc", columnNames = "rfc")
        },
        indexes = {
                @Index(name = "idx_client_is_active", columnList = "is_active"),
                @Index(name = "idx_client_created_at", columnList = "created_at")
        }
)
@Entity
@Getter
@Setter
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long idClient;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "second_name", length = 50)
    private String secondName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "second_last_name", nullable = false, length = 50)
    private String secondLastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "curp", nullable = false, length = 18, unique = true)
    private String curp;

    @Column(name = "rfc", nullable = false, length = 13, unique = true)
    private String rfc;

    @Column(name = "id_gender", nullable = false)
    private Short idGender;

    @Column(name = "id_nationality", nullable = false)
    private Short idNationality;

    @Column(name = "id_marital_status", nullable = false)
    private Short idMaritalStatus;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}