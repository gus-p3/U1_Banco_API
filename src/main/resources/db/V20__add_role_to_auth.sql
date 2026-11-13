-- Rol de acceso del login (CLIENT por defecto; ADMIN puede consultar y administrar a todos los clientes).
ALTER TABLE auth ADD COLUMN role TEXT NOT NULL DEFAULT 'CLIENT';
ALTER TABLE auth ADD CONSTRAINT ck_auth_role CHECK (role IN ('CLIENT', 'ADMIN'));
