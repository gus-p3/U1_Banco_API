-- Domicilio (1 a 1 con client). El estado se obtiene a través del municipio.
CREATE TABLE home (
    id_home         BIGINT GENERATED ALWAYS AS IDENTITY,
    id_client       BIGINT       NOT NULL,
    street          TEXT NOT NULL,
    exterior_number TEXT  NOT NULL,
    interior_number TEXT,
    neighborhood    TEXT  NOT NULL,
    id_municipality INTEGER      NOT NULL,
    postal_code     CHAR(5)      NOT NULL,
    country         TEXT  NOT NULL DEFAULT 'México',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_home PRIMARY KEY (id_home),
    CONSTRAINT uq_home_client UNIQUE (id_client),
    CONSTRAINT fk_home_client FOREIGN KEY (id_client) REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT fk_home_municipality FOREIGN KEY (id_municipality) REFERENCES municipality (id_municipality),
    CONSTRAINT ck_home_postal_code CHECK (postal_code ~ '^[0-9]{5}$')
);

CREATE INDEX idx_home_municipality ON home (id_municipality);
