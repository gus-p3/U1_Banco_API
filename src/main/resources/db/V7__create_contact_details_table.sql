-- Detalle de contacto (1 a 1 con client).
-- La normalización de correo a minúsculas y recorte de espacios se delega a la capa de aplicación/backend
-- para mantener la consistencia de datos y separar responsabilidades del dominio.
CREATE TABLE contact_details (
    id_contact_detail BIGINT GENERATED ALWAYS AS IDENTITY,
    id_client         BIGINT       NOT NULL,
    email             TEXT NOT NULL,
    mobile_phone      CHAR(10)     NOT NULL,
    alternative_phone CHAR(10),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_contact_details PRIMARY KEY (id_contact_detail),
    CONSTRAINT uq_contact_details_client UNIQUE (id_client),
    CONSTRAINT uq_contact_details_phone UNIQUE (mobile_phone),
    CONSTRAINT uq_contact_details_email UNIQUE (email),
    CONSTRAINT fk_contact_details_client FOREIGN KEY (id_client)
        REFERENCES client (id_client) ON DELETE RESTRICT,

    CONSTRAINT ck_contact_details_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT ck_contact_details_mobile CHECK (mobile_phone ~ '^[0-9]{10}$'),
    CONSTRAINT ck_contact_details_alternative CHECK (alternative_phone IS NULL OR alternative_phone ~ '^[0-9]{10}$')
);