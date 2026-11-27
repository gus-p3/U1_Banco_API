-- Verificación de correo electrónico (Brevo) y 2FA en inicio de sesión (código por correo o huella en móvil).
ALTER TABLE auth
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

CREATE TABLE auth_challenge (
    id                  VARCHAR(36)  NOT NULL,
    id_client           BIGINT       NOT NULL,
    email               VARCHAR(255) NOT NULL,
    purpose             VARCHAR(30)  NOT NULL,
    channel             VARCHAR(20)  NOT NULL,
    poll_token          VARCHAR(36)  NOT NULL,
    code_hash           VARCHAR(128),
    code_sent_at        TIMESTAMPTZ,
    allow_biometric     BOOLEAN      NOT NULL DEFAULT FALSE,
    biometric_requested BOOLEAN      NOT NULL DEFAULT FALSE,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    attempts            INTEGER      NOT NULL DEFAULT 0,
    expira_en           TIMESTAMPTZ  NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_auth_challenge PRIMARY KEY (id),
    CONSTRAINT fk_auth_challenge_client FOREIGN KEY (id_client)
        REFERENCES client (id_client) ON DELETE CASCADE,
    CONSTRAINT ck_auth_challenge_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'LOGIN_2FA')),
    CONSTRAINT ck_auth_challenge_channel CHECK (channel IN ('WEB', 'MOBILE')),
    CONSTRAINT ck_auth_challenge_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'EXPIRADO', 'CANCELADO', 'CONSUMIDO'))
);

CREATE INDEX idx_auth_challenge_client_estado
    ON auth_challenge (id_client, purpose, estado, fecha_creacion DESC);
