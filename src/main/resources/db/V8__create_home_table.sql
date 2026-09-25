-- Domicilio (1 a 1 con client). El estado se obtiene a través del municipio.
CREATE TABLE home (
    id_home         BIGINT GENERATED ALWAYS AS IDENTITY,
    id_client       BIGINT       NOT NULL,
    street          VARCHAR(100) NOT NULL,
    exterior_number VARCHAR(10)  NOT NULL,
    interior_number VARCHAR(10),
    neighborhood    VARCHAR(80)  NOT NULL,
    id_municipality INTEGER      NOT NULL,
    postal_code     CHAR(5)      NOT NULL,
    country         VARCHAR(50)  NOT NULL DEFAULT 'México',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_home PRIMARY KEY (id_home),
    CONSTRAINT uq_home_client UNIQUE (id_client),
    CONSTRAINT fk_home_client FOREIGN KEY (id_client) REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT fk_home_municipality FOREIGN KEY (id_municipality) REFERENCES municipality (id_municipality),
    CONSTRAINT ck_home_postal_code CHECK (postal_code ~ '^[0-9]{5}$')
);

CREATE INDEX idx_home_municipality ON home (id_municipality);
