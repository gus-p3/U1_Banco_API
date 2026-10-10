-- Cliente persona física.
-- La regla de mayoría de edad (>= 18 años) se valida en la capa de servicio,
-- porque un CHECK con CURRENT_DATE no es inmutable y rompe las restauraciones de respaldo.
CREATE TABLE client (
    id_client         BIGINT GENERATED ALWAYS AS IDENTITY,
    name              TEXT  NOT NULL,
    second_name       TEXT,
    last_name         TEXT  NOT NULL,
    second_last_name  TEXT,
    birth_date        DATE         NOT NULL,
    curp              CHAR(18)     NOT NULL,
    rfc               TEXT  NOT NULL,
    id_gender         SMALLINT     NOT NULL,
    id_nationality    SMALLINT     NOT NULL,
    id_marital_status SMALLINT     NOT NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deactivated_at    TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_client PRIMARY KEY (id_client),
    CONSTRAINT uq_client_curp UNIQUE (curp),
    CONSTRAINT uq_client_rfc UNIQUE (rfc),
    CONSTRAINT fk_client_gender FOREIGN KEY (id_gender) REFERENCES gender (id_gender),
    CONSTRAINT fk_client_nationality FOREIGN KEY (id_nationality) REFERENCES nationality (id_nationality),
    CONSTRAINT fk_client_marital_status FOREIGN KEY (id_marital_status) REFERENCES marital_status (id_marital_status),
    CONSTRAINT ck_client_name CHECK (name ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_client_second_name CHECK (second_name ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_client_last_name CHECK (last_name ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_client_second_last_name CHECK (second_last_name ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_client_curp CHECK (curp ~ '^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$'),
    CONSTRAINT ck_client_rfc CHECK (rfc ~ '^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$'),
    CONSTRAINT ck_client_deactivation CHECK (
        (is_active = TRUE  AND deactivated_at IS NULL) OR
        (is_active = FALSE AND deactivated_at IS NOT NULL)
    )
);

-- Consultas de clientes activos y por rango de fecha de registro
CREATE INDEX idx_client_is_active  ON client (is_active);
CREATE INDEX idx_client_created_at ON client (created_at);
