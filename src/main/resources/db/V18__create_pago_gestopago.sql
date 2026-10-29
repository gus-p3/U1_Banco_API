-- Pagos de recargas y servicios vía GestoPago (sendTx / confirmTx).
-- Guarda request y response de cada operación (bitácora obligatoria por GestoPago).
CREATE TABLE pago_gestopago (
                                id                    BIGINT GENERATED ALWAYS AS IDENTITY,
                                id_cliente            BIGINT        NOT NULL,
                                num_cuenta            CHAR(10)      NOT NULL,
                                id_servicio           INTEGER       NOT NULL,
                                id_producto           INTEGER       NOT NULL,
                                tipo_pago             TEXT          NOT NULL,
                                telefono              TEXT,
                                referencia            TEXT,
                                monto                 NUMERIC(15,2) NOT NULL,
                                comision              NUMERIC(15,2),
                                upc                   TEXT          NOT NULL,
                                estado                TEXT          NOT NULL DEFAULT 'PENDIENTE',
                                codigo_respuesta      TEXT,
                                texto_respuesta       TEXT,
                                num_autorizacion      TEXT,
                                id_tx                 BIGINT,
                                payload_json          TEXT          NOT NULL,
                                request_cifrado       TEXT,
                                response_raw          TEXT,
                                intentos_confirmacion INTEGER       NOT NULL DEFAULT 0,
                                fecha_envio           TIMESTAMPTZ,
                                fecha_creacion        TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                fecha_actualizacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),

                                CONSTRAINT pk_pago_gestopago PRIMARY KEY (id),
                                CONSTRAINT uq_pago_gestopago_upc UNIQUE (upc),
                                CONSTRAINT fk_pago_cliente FOREIGN KEY (id_cliente)
                                    REFERENCES client (id_client) ON DELETE RESTRICT,
                                CONSTRAINT fk_pago_cuenta FOREIGN KEY (num_cuenta)
                                    REFERENCES account (account_number) ON DELETE RESTRICT,
                                CONSTRAINT ck_pago_tipo CHECK (tipo_pago IN ('RECARGA', 'SERVICIO')),
                                CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE', 'APLICADO', 'RECHAZADO', 'POR_CONFIRMAR')),
                                CONSTRAINT ck_pago_monto CHECK (monto > 0),
                                CONSTRAINT ck_pago_upc CHECK (char_length(upc) BETWEEN 3 AND 15),
                                CONSTRAINT ck_pago_destino CHECK (
                                    (tipo_pago = 'RECARGA'  AND telefono IS NOT NULL AND referencia IS NULL) OR
                                    (tipo_pago = 'SERVICIO' AND referencia IS NOT NULL AND telefono IS NULL)
                                    )
);

CREATE INDEX idx_pago_cliente ON pago_gestopago (id_cliente);
-- Índice parcial: el job de confirmTx solo recorre los POR_CONFIRMAR
CREATE INDEX idx_pago_por_confirmar ON pago_gestopago (fecha_envio) WHERE estado = 'POR_CONFIRMAR';