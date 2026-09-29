-- Login: credenciales, datos biometricos y refresh token para clientes bancarios (tipo Mercado Libre / Mercado Pago).
-- auth tiene la FK hacia client
-- (un cliente puede existir sin login; un login siempre pertenece a un cliente,
-- y un cliente puede tener mas de un login o credenciales de acceso).
CREATE TABLE auth (
    id_login                 BIGINT GENERATED ALWAYS AS IDENTITY,
    id_client                BIGINT        NOT NULL,
    email                    TEXT          NOT NULL,
    password_hash            TEXT          NOT NULL,
    biometric_type           TEXT,
    biometric_template       BYTEA,
    biometric_registered_at  TIMESTAMPTZ,
    refresh_token            TEXT,
    refresh_token_expires_at TIMESTAMPTZ,
    is_active                BOOLEAN       NOT NULL DEFAULT TRUE,
    failed_attempts          SMALLINT      NOT NULL DEFAULT 0,
    last_login_at            TIMESTAMPTZ,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_login PRIMARY KEY (id_login),
    CONSTRAINT uq_auth_email UNIQUE (email),
    CONSTRAINT fk_login_client FOREIGN KEY (id_client) REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT ck_login_biometric_type CHECK (biometric_type IN ('HUELLA', 'FACIAL')),
    CONSTRAINT ck_login_biometric_pair CHECK (
        (biometric_type IS NULL AND biometric_template IS NULL) OR
        (biometric_type IS NOT NULL AND biometric_template IS NOT NULL)
    ),
    CONSTRAINT ck_login_failed_attempts CHECK (failed_attempts >= 0),
    CONSTRAINT ck_auth_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_auth_client ON auth (id_client);
CREATE INDEX idx_auth_refresh_token ON auth (refresh_token);
