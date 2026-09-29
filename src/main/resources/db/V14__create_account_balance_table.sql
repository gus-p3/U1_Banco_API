-- Tabla de saldos y movimientos de cuentas bancarias (historial y libro mayor de saldo).
-- Prohibido el uso de VARCHAR: solo se utiliza TEXT, NUMERIC, BIGINT y TIMESTAMPTZ.
CREATE TABLE account_balance (
    id_balance       BIGINT GENERATED ALWAYS AS IDENTITY,
    id_account       BIGINT        NOT NULL,
    previous_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
    amount           NUMERIC(15,2) NOT NULL,
    current_balance  NUMERIC(15,2) NOT NULL,
    movement_type    TEXT          NOT NULL DEFAULT 'APERTURA',
    description      TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_account_balance PRIMARY KEY (id_balance),
    CONSTRAINT fk_balance_account FOREIGN KEY (id_account)
        REFERENCES account (id_account) ON DELETE RESTRICT,
    CONSTRAINT ck_balance_movement_type CHECK (movement_type IN ('APERTURA', 'DEPOSITO', 'RETIRO', 'AJUSTE')),
    CONSTRAINT ck_balance_current CHECK (current_balance >= 0)
);

CREATE INDEX idx_balance_account ON account_balance (id_account);
CREATE INDEX idx_balance_created_at ON account_balance (created_at);
