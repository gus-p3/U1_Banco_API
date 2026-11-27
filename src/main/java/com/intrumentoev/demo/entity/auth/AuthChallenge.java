package com.intrumentoev.demo.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "auth_challenge",
        indexes = {
                @Index(name = "idx_auth_challenge_client_estado", columnList = "id_client, purpose, estado, fecha_creacion")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthChallenge {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "id_client", nullable = false, updatable = false)
    private Long idClient;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose; // EMAIL_VERIFICATION | LOGIN_2FA

    @Column(name = "channel", nullable = false, length = 20)
    private String channel; // WEB | MOBILE

    @Column(name = "poll_token", nullable = false, length = 36)
    private String pollToken;

    @Column(name = "code_hash", length = 128)
    private String codeHash;

    @Column(name = "code_sent_at")
    private OffsetDateTime codeSentAt;

    @Builder.Default
    @Column(name = "allow_biometric", nullable = false)
    private Boolean allowBiometric = false;

    @Builder.Default
    @Column(name = "biometric_requested", nullable = false)
    private Boolean biometricRequested = false;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE | APROBADO | RECHAZADO | EXPIRADO | CANCELADO | CONSUMIDO

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
