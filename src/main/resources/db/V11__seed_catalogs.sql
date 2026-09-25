-- Datos iniciales de catálogos (ampliables con nuevas migraciones)
INSERT INTO gender (name, description) VALUES
    ('Hombre', 'CURP: H'),
    ('Mujer',  'CURP: M');

INSERT INTO marital_status (name) VALUES
    ('Soltero(a)'),
    ('Casado(a)'),
    ('Unión libre'),
    ('Separado(a)'),
    ('Divorciado(a)'),
    ('Viudo(a)');

INSERT INTO nationality (name) VALUES
    ('Mexicana'),
    ('Estadounidense'),
    ('Canadiense'),
    ('Española'),
    ('Guatemalteca'),
    ('Hondureña'),
    ('Salvadoreña'),
    ('Cubana'),
    ('Colombiana'),
    ('Venezolana'),
    ('Argentina');
