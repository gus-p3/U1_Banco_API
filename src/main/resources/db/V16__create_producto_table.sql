CREATE TABLE IF NOT EXISTS producto (
    "Id" SERIAL PRIMARY KEY,
    "producto" VARCHAR(255),
    "servicio" VARCHAR(255),
    "idServicio" INTEGER NOT NULL DEFAULT 0,
    "idProducto" INTEGER NOT NULL DEFAULT 0,
    "idCatTipoServicio" INTEGER NOT NULL DEFAULT 0,
    "tipoFront" INTEGER NOT NULL DEFAULT 0,
    "hasDigitoVerificador" BOOLEAN NOT NULL DEFAULT FALSE,
    "tipoReferencia" VARCHAR(50),
    "precio" VARCHAR(50),
    "showAyuda" BOOLEAN NOT NULL DEFAULT FALSE
);
