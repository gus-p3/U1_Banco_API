-- Información laboral (1 a 1 con client)
CREATE TABLE employment_information (
    id_employment  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_client      BIGINT        NOT NULL,
    occupation     TEXT   NOT NULL,
    company        TEXT  NOT NULL,
    monthly_income NUMERIC(12,2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_employment_information PRIMARY KEY (id_employment),
    CONSTRAINT uq_employment_information_client UNIQUE (id_client),
    CONSTRAINT fk_employment_information_client FOREIGN KEY (id_client) REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT ck_employment_information_income CHECK (monthly_income > 0)
);
