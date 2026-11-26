-- Tokens de notificación push de dispositivos móviles (FCM / Expo) y solicitudes de autorización remota Web -> Móvil.
CREATE TABLE dispositivo_push (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente          BIGINT       NOT NULL,
    push_token          VARCHAR(512) NOT NULL,
    plataforma          VARCHAR(20)  NOT NULL DEFAULT 'ANDROID',
    device_name         VARCHAR(120),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dispositivo_push PRIMARY KEY (id),
    CONSTRAINT uq_dispositivo_push_cliente_token UNIQUE (id_cliente, push_token),
    CONSTRAINT fk_dispositivo_push_cliente FOREIGN KEY (id_cliente)
        REFERENCES client (id_client) ON DELETE CASCADE,
    CONSTRAINT ck_dispositivo_push_plataforma CHECK (plataforma IN ('ANDROID', 'IOS'))
);

CREATE INDEX idx_dispositivo_push_cliente ON dispositivo_push (id_cliente) WHERE activo = TRUE;

CREATE TABLE pago_autorizacion (
    id                      VARCHAR(36)    NOT NULL,
    id_cliente              BIGINT         NOT NULL,
    num_cuenta              CHAR(10)       NOT NULL,
    id_servicio             INTEGER        NOT NULL,
    id_producto             INTEGER        NOT NULL,
    tipo_pago               VARCHAR(10)    NOT NULL,
    nombre_servicio         VARCHAR(150),
    nombre_producto         VARCHAR(150),
    telefono                VARCHAR(20),
    referencia              VARCHAR(100),
    destino_enmascarado     VARCHAR(60),
    monto                   NUMERIC(15,2)  NOT NULL,
    estado                  VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    pago_codigo             INTEGER,
    pago_estado             VARCHAR(20),
    pago_mensaje            TEXT,
    pago_codigo_gestopago   VARCHAR(20),
    pago_num_autorizacion   VARCHAR(40),
    pago_id_tx              BIGINT,
    pago_upc                VARCHAR(20),
    expira_en               TIMESTAMPTZ    NOT NULL,
    fecha_creacion          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fecha_actualizacion     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_pago_autorizacion PRIMARY KEY (id),
    CONSTRAINT fk_pago_autorizacion_cliente FOREIGN KEY (id_cliente)
        REFERENCES client (id_client) ON DELETE RESTRICT,
    CONSTRAINT fk_pago_autorizacion_cuenta FOREIGN KEY (num_cuenta)
        REFERENCES account (account_number) ON DELETE RESTRICT,
    CONSTRAINT ck_pago_autorizacion_tipo CHECK (tipo_pago IN ('RECARGA', 'SERVICIO')),
    CONSTRAINT ck_pago_autorizacion_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'EXPIRADO', 'CANCELADO')),
    CONSTRAINT ck_pago_autorizacion_monto CHECK (monto > 0)
);

CREATE INDEX idx_pago_autorizacion_cliente_estado ON pago_autorizacion (id_cliente, estado, fecha_creacion DESC);
