-- Ampliar longitud de los 4 campos de nombre/apellidos a 62 caracteres (soporte nombres compuestos en México)
ALTER TABLE client
    ALTER COLUMN name TYPE VARCHAR(62),
    ALTER COLUMN second_name TYPE VARCHAR(62),
    ALTER COLUMN last_name TYPE VARCHAR(62),
    ALTER COLUMN second_last_name TYPE VARCHAR(62);

-- Catálogo de profesiones / ocupaciones
CREATE TABLE IF NOT EXISTS profession (
    id_profession SMALLSERIAL PRIMARY KEY,
    name          VARCHAR(80)  NOT NULL UNIQUE,
    description   VARCHAR(150),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO profession (name, description, is_active) VALUES
    ('Abogado(a)', 'Servicios jurídicos y asesoría legal', TRUE),
    ('Administrador(a) de empresas', 'Gestión administrativa y dirección', TRUE),
    ('Agente de ventas', 'Comercialización y atención comercial', TRUE),
    ('Agricultor(a) / Ganadero(a)', 'Actividades agropecuarias', TRUE),
    ('Ama de casa', 'Administración del hogar', TRUE),
    ('Analista financiero', 'Finanzas, presupuestos y riesgos', TRUE),
    ('Arquitecto(a)', 'Diseño arquitectónico y construcción', TRUE),
    ('Artista / Músico(a)', 'Actividades artísticas y culturales', TRUE),
    ('Chef / Gastrónomo(a)', 'Alimentos, bebidas y gastronomía', TRUE),
    ('Comerciante', 'Comercio minorista o mayorista', TRUE),
    ('Contador(a) público(a)', 'Contabilidad, fiscal y auditoría', TRUE),
    ('Dentista / Odontólogo(a)', 'Salud bucal y odontología', TRUE),
    ('Desarrollador(a) / Ingeniero(a) de software', 'Desarrollo de sistemas y TI', TRUE),
    ('Diseñador(a) gráfico / UX', 'Diseño visual y experiencia digital', TRUE),
    ('Docente / Profesor(a)', 'Educación e investigación académica', TRUE),
    ('Economista', 'Análisis económico y planeación', TRUE),
    ('Emprendedor(a) / Empresario(a)', 'Dirección de negocio propio', TRUE),
    ('Enfermero(a)', 'Atención clínica y cuidados de salud', TRUE),
    ('Estudiante', 'Estudiante de nivel medio superior o superior', TRUE),
    ('Ingeniero(a) civil', 'Infraestructura y obra civil', TRUE),
    ('Ingeniero(a) industrial', 'Procesos, logística y manufactura', TRUE),
    ('Jubilado(a) / Pensionado(a)', 'Ingresos por pensión o retiro', TRUE),
    ('Médico(a)', 'Medicina general o especialidad médica', TRUE),
    ('Mercadólogo(a)', 'Marketing, publicidad y estrategia', TRUE),
    ('Operador(a) industrial / Técnico(a)', 'Operación técnica y mantenimiento', TRUE),
    ('Periodista / Comunicólogo(a)', 'Medios y comunicación', TRUE),
    ('Psicólogo(a)', 'Salud mental y psicología', TRUE),
    ('Servidor(a) público(a)', 'Administración pública y gobierno', TRUE),
    ('Técnico(a) en sistemas / Soporte TI', 'Infraestructura y soporte técnico', TRUE),
    ('Trabajador(a) independiente / Freelance', 'Servicios profesionales independientes', TRUE),
    ('Transportista / Operador(a) logístico(a)', 'Transporte y distribución', TRUE),
    ('Veterinario(a)', 'Medicina veterinaria y zootecnia', TRUE),
    ('Otra ocupación', 'Otra actividad económica lícita', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Catálogo de montos de operación (rangos de $5,000 MXN: perfil transaccional KYC + límite por operación)
CREATE TABLE IF NOT EXISTS operation_amount_range (
    id_operation_amount SMALLSERIAL    PRIMARY KEY,
    label               VARCHAR(60)    NOT NULL UNIQUE,
    min_amount          NUMERIC(12, 2) NOT NULL,
    max_amount          NUMERIC(12, 2) NOT NULL,
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO operation_amount_range (label, min_amount, max_amount, is_active) VALUES
    ('Hasta $5,000 MXN', 0.01, 5000.00, TRUE),
    ('$5,001 a $10,000 MXN', 5000.01, 10000.00, TRUE),
    ('$10,001 a $15,000 MXN', 10000.01, 15000.00, TRUE),
    ('$15,001 a $20,000 MXN', 15000.01, 20000.00, TRUE),
    ('$20,001 a $25,000 MXN', 20000.01, 25000.00, TRUE),
    ('$25,001 a $30,000 MXN', 25000.01, 30000.00, TRUE),
    ('$30,001 a $35,000 MXN', 30000.01, 35000.00, TRUE),
    ('$35,001 a $40,000 MXN', 35000.01, 40000.00, TRUE),
    ('$40,001 a $45,000 MXN', 40000.01, 45000.00, TRUE),
    ('$45,001 a $50,000 MXN', 45000.01, 50000.00, TRUE),
    ('Más de $50,000 MXN (hasta $100,000)', 50000.01, 100000.00, TRUE)
ON CONFLICT (label) DO NOTHING;

-- Relacionar profesión y rango de monto de operación en employment_information
ALTER TABLE employment_information
    ADD COLUMN IF NOT EXISTS id_profession       SMALLINT REFERENCES profession(id_profession),
    ADD COLUMN IF NOT EXISTS id_operation_amount SMALLINT REFERENCES operation_amount_range(id_operation_amount);
