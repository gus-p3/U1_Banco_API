# Estatus de Verificación y Funcionamiento del Sistema Bancario

**Fecha de verificación:** 30 de septiembre de 2026  
**Proyecto:** API Bancaria de Clientes, Cuentas y Autenticación  
**Tecnología:** Spring Boot 3, Java 21, Gradle, PostgreSQL, Flyway  

---

## 1. Resumen Ejecutivo

Se completó la verificación integral de la aplicación bancaria conforme a las reglas de negocio y especificaciones de seguridad:
- **Creación de Cuenta Bancaria Única:** Garantizada por lógica de servicio y restricciones a nivel de base de datos.
- **Tabla de Saldos y Movimientos (`account_balance`):** Se genera y persiste automáticamente el registro inicial (`APERTURA`) con el saldo de bienvenida en cada alta de cuenta.
- **Módulo de Login y Seguridad:** Operativo con soporte de autenticación tradicional (PBKDF2), autenticación biométrica (AES-256-GCM), refresh tokens y control de sesión activa en el servidor con temporizador de inactividad.
- **Validación Técnica:** Pruebas unitarias e integración ejecutadas al 100% con resultado `BUILD SUCCESSFUL` y empaquetado del artefacto `bootJar` sin errores.

---

## 2. Creación Automática de Cuenta Única y Tabla de Saldos

### 2.1 Flujo en Onboarding Integral (`POST /v1/clientes/onboarding`)
1. El usuario se registra proporcionando datos personales, de contacto, domicilio, ocupación y credenciales de acceso.
2. Tras persistir el cliente, [`ClientServiceImpl`](src/main/java/com/intrumentoev/demo/service/impl/client/ClientServiceImpl.java) invoca a [`AccountService.crearCuenta`](src/main/java/com/intrumentoev/demo/service/impl/account/AccountServiceImpl.java) enviando el ID del cliente y el saldo inicial.

### 2.2 Generación y Unicidad del Número de Cuenta
- **Componente:** [`AccountNumberGenerator.java`](src/main/java/com/intrumentoev/demo/service/service/account/AccountNumberGenerator.java).
- **Mecanismo:** Generación con `SecureRandom` de números de 10 dígitos numéricos (no inicia en 0). Se valida contra la base de datos mediante `existsByAccountNumber` en bucle hasta asegurar unicidad.
- **Restricciones en BD ([V10__create_account_table.sql](src/main/resources/db/V10__create_account_table.sql)):**
  - `CONSTRAINT uq_account_number UNIQUE (account_number)`
  - `CONSTRAINT ck_account_number CHECK (account_number ~ '^[0-9]{10}$')`
  - `CONSTRAINT ck_account_balance CHECK (balance >= 0)`
  - `CONSTRAINT ck_account_status CHECK (status IN ('ACTIVA', 'INACTIVA'))`

### 2.3 Libro Mayor y Registro de Saldos ([V14__create_account_balance_table.sql](src/main/resources/db/V14__create_account_balance_table.sql))
- En cada apertura de cuenta, se inserta inmediatamente un registro en la tabla `account_balance`:
  - `id_account`: Identificador de la cuenta recién generada.
  - `previous_balance`: `0.00`.
  - `amount`: Saldo inicial asignado (por defecto `$1,000.00` o el configurado).
  - `current_balance`: Saldo resultante.
  - `movement_type`: `'APERTURA'` (validado por restricción check `ck_balance_movement_type`).
  - `description`: `'Apertura de cuenta con asignación de saldo inicial de bienvenida'`.
  - `created_at`: Marca de tiempo UTC (`now()`).
- Los movimientos pueden ser consultados mediante el endpoint:
  - `GET /v1/cuentas/{numeroCuenta}/movimientos`
  - `GET /v1/cuentas/{numeroCuenta}/saldo`

---

## 3. Verificación del Módulo de Login y Autenticación

El controlador [`AuthController`](src/main/java/com/intrumentoev/demo/controller/auth/AuthController.java) y el servicio [`AuthServiceImpl`](src/main/java/com/intrumentoev/demo/service/impl/auth/AuthServiceImpl.java) gestionan el ciclo completo de identidad bancaria tipo Mercado Libre / Mercado Pago.

### 3.1 Login Tradicional (`POST /v1/auth/login`)
1. **Normalización:** El correo se normaliza a minúsculas y se valida la existencia de la cuenta.
2. **Protección contra ataques de fuerza bruta:** Bloqueo tras 5 intentos erróneos (`AccountLockedException`, código HTTP 423).
3. **Criptografía de contraseña:** Validación mediante `PasswordEncoder` con algoritmo **PBKDF2** con sal aleatoria.
4. **Reseteo de intentos:** Tras contraseña correcta, `failedAttempts` se reinicia a 0 y se actualiza `last_login_at`.
5. **Generación de Tokens:** Retorna un JWT Access Token firmado y un Refresh Token de rotación.
6. **Sesión en el Servidor:** Activa el booleano en el servidor (`login = true`) a través de [`ServerSessionManager`](src/main/java/com/intrumentoev/demo/service/service/auth/ServerSessionManager.java).

### 3.2 Login Biométrico (`POST /v1/auth/login-biometrico`)
1. Valida que el cliente tenga enrolamiento biométrico previo (`HUELLA` o `FACIAL`).
2. Desencripta la plantilla almacenada con **AES-256-GCM** mediante [`AesEncryptionService`](src/main/java/com/intrumentoev/demo/service/service/auth/AesEncryptionService.java).
3. Realiza la comparación en tiempo constante (`MessageDigest.isEqual`) para proteger contra ataques de canal lateral (*timing attacks*).

### 3.3 Renovación y Cierre de Sesión
- `POST /v1/auth/refresh`: Renueva el JWT y rota el Refresh Token sin solicitar credenciales nuevamente.
- `POST /v1/auth/logout`: Revoca el Refresh Token en base de datos y pasa el estado del servidor a `login = false`.

### 3.4 Interceptor de Seguridad y Control de Inactividad
- Componente: [`ServerSessionInterceptor`](src/main/java/com/intrumentoev/demo/config/ServerSessionInterceptor.java).
- **Rutas Públicas Permitidas:** Onboarding (`POST /v1/clientes/onboarding`), creación de cuentas (`POST /v1/cuentas`), catálogos y login/refresh.
- **Rutas Protegidas:** Consultas de clientes, expedientes, saldos y movimientos requieren que el servidor tenga `login = true` y menos de 5 segundos de inactividad.
- **Endpoint de Monitoreo:** `GET /v1/auth/session-status` o `GET /v1/auth/estado-servidor` para auditar el estado del booleano, segundos de inactividad transcurridos y segundos restantes.

---

## 4. Estado de las Pruebas y Compilación

### 4.1 Correcciones Realizadas
- En [`ClientServiceTest.java`](src/test/java/com/intrumentoev/demo/service/ClientServiceTest.java), se corrigió la cabecera faltante del método `testObtenerClientePorIdConIncludes()`.
- En [`AccountServiceTest.java`](src/test/java/com/intrumentoev/demo/service/AccountServiceTest.java), se agregó:
  1. Verificación del guardado automático en `accountBalanceRepository` al crear una cuenta.
  2. Prueba unitaria para la consulta de movimientos de saldo (`testConsultarMovimientosSaldo`).

### 4.2 Resultados de Ejecución
- `./gradlew test`: **BUILD SUCCESSFUL** (0 errores, todas las suites aprobadas).
- `./gradlew assemble`: **BUILD SUCCESSFUL** (construcción del ejecutable `bootJar` exitosa).
