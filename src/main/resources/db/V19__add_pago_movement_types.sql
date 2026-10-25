-- Movimientos generados por pagos GestoPago: cargo al pagar y reverso si se rechaza.
ALTER TABLE account_balance DROP CONSTRAINT ck_balance_movement_type;
ALTER TABLE account_balance ADD CONSTRAINT ck_balance_movement_type
    CHECK (movement_type IN ('APERTURA', 'DEPOSITO', 'RETIRO', 'AJUSTE', 'PAGO_SERVICIO', 'REVERSO_PAGO'));