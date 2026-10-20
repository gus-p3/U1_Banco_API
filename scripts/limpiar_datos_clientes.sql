-- ============================================================================
-- SCRIPT DE LIMPIEZA DE DATOS DE CLIENTES Y OPERACIONES BANCARIAS
-- Base de datos: PostgreSQL (banco)
-- ============================================================================
-- Este script elimina de forma segura todos los datos operativos de clientes:
--   1. account_balance (Movimientos e historial de saldos)
--   2. account (Cuentas bancarias)
--   3. auth (Credenciales de acceso, biometría y refresh tokens)
--   4. contact_details (Teléfonos y correos de contacto)
--   5. home (Direcciones y domicilios)
--   6. employment_information (Datos laborales e ingresos)
--   7. client (Datos personales de los clientes)
--
-- PRESERVA INTACTOS TODOS LOS CATÁLOGOS DEL SISTEMA:
--   - gender (Géneros)
--   - nationality (Nacionalidades)
--   - marital_status (Estados civiles)
--   - state (Estados de México / INEGI)
--   - municipality (Municipios / INEGI)
-- ============================================================================

\pset pager off

BEGIN;

-- Opción 1 (Recomendada): TRUNCATE con RESTART IDENTITY y CASCADE
-- Elimina registros al instante y reinicia las secuencias (auto-incrementales) a 1
TRUNCATE TABLE 
    account_balance,
    account,
    auth,
    contact_details,
    home,
    employment_information,
    client
RESTART IDENTITY CASCADE;

COMMIT;

-- ============================================================================
-- VERIFICACIÓN DE LIMPIEZA
-- Todos los conteos de clientes y cuentas deben ser 0.
-- Los catálogos deben mantener sus registros intactos.
-- ============================================================================
SELECT 'client' AS tabla, COUNT(*) AS total_registros FROM client
UNION ALL
SELECT 'contact_details', COUNT(*) FROM contact_details
UNION ALL
SELECT 'home', COUNT(*) FROM home
UNION ALL
SELECT 'employment_information', COUNT(*) FROM employment_information
UNION ALL
SELECT 'account', COUNT(*) FROM account
UNION ALL
SELECT 'account_balance', COUNT(*) FROM account_balance
UNION ALL
SELECT 'auth', COUNT(*) FROM auth
UNION ALL
SELECT '--- CATÁLOGOS (PRESERVADOS) ---', NULL
UNION ALL
SELECT 'gender', COUNT(*) FROM gender
UNION ALL
SELECT 'nationality', COUNT(*) FROM nationality
UNION ALL
SELECT 'marital_status', COUNT(*) FROM marital_status
UNION ALL
SELECT 'state', COUNT(*) FROM state
UNION ALL
SELECT 'municipality', COUNT(*) FROM municipality;
