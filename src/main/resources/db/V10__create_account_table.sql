-- Cuenta bancaria (1 cliente : 1 o muchas cuentas)
CREATE TABLE account (
    id_account     BIGINT GENERATED ALWAYS AS IDENTITY,
    account_number CHAR(10)      NOT NULL,
    id_client      BIGINT        NOT NULL,
    balance        NUMERIC(15,2) NOT NULL DEFAULT 0,
    status         TEXT   NOT NULL DEFAULT 'ACTIVA',
    opened_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_account PRIMARY KEY (id_account),
    CONSTRAINT uq_account_number UNIQUE (account_number),
    CONSTRAINT fk_account_client FOREIGN KEY (id_client) REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT ck_account_number CHECK (account_number ~ '^[0-9]{10}$'),
    CONSTRAINT ck_account_balance CHECK (balance >= 0),
    CONSTRAINT ck_account_status CHECK (status IN ('ACTIVA', 'INACTIVA'))
);

CREATE INDEX idx_account_client ON account (id_client);
CREATE INDEX idx_account_status ON account (status);
