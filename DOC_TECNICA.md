# 🏛️ Documento de Arquitectura y Especificación Técnica (Banco API)

**Proyecto:** Banco API - Onboarding de Clientes Personas Físicas y Servicios Financieros  
**Versión:** 1.0.0  
**Fecha de Publicación:** Octubre 2026  
**Tecnología Base:** Java 21 • Spring Boot 3.5.6 • PostgreSQL 16+ • Redis 7+ • Flyway  

---

## 📑 Tabla de Contenidos

1. [Visión General y Objetivos del Sistema](#1-visión-general-y-objetivos-del-sistema)
2. [Arquitectura de Software](#2-arquitectura-de-software)
   - 2.1. [Patrón de Arquitectura Limpia](#21-patrón-de-arquitectura-limpia)
   - 2.2. [Estructura de Paquetes](#22-estructura-de-paquetes)
3. [Diseño y Modelo de Base de Datos](#3-diseño-y-modelo-de-base-de-datos)
   - 3.1. [Directiva de Tipos de Datos (Cero VARCHAR)](#31-directiva-de-tipos-de-datos-cero-varchar)
   - 3.2. [Diagrama Entidad-Relación](#32-diagrama-entidad-relación)
   - 3.3. [Diccionario de Tablas y Restricciones (12 Entidades)](#33-diccionario-de-tablas-y-restricciones-12-entidades)
   - 3.4. [Evolución de Esquema con Flyway](#34-evolución-de-esquema-con-flyway)
4. [Módulo de Seguridad y Criptografía](#4-módulo-de-seguridad-y-criptografía)
   - 4.1. [Hashing de Contraseñas (PBKDF2)](#41-hashing-de-contraseñas-pbkdf2)
   - 4.2. [Cifrado de Biometría (AES-256-GCM)](#42-cifrado-de-biometría-aes-256-gcm)
   - 4.3. [Tokens JWT y Refresh Tokens](#43-tokens-jwt-y-refresh-tokens)
   - 4.4. [Control de Sesión en el Servidor (Booleano e Inactividad)](#44-control-de-sesión-en-el-servidor-booleano-e-inactividad)
5. [Estrategia de Integración Externa y Caché](#5-estrategia-de-integración-externa-y-caché)
   - 5.1. [Consumo de API Oficial INEGI](#51-consumo-de-api-oficial-inegi)
   - 5.2. [Capa de Caché con Redis y Resiliencia Fallback](#52-capa-de-caché-con-redis-y-resiliencia-fallback)
6. [Catálogo Completo de Endpoints REST](#6-catálogo-completo-de-endpoints-rest)
   - 6.1. [Módulo de Clientes y Onboarding](#61-módulo-de-clientes-y-onboarding)
   - 6.2. [Módulo de Cuentas Bancarias y Saldos](#62-módulo-de-cuentas-bancarias-y-saldos)
   - 6.3. [Módulo de Autenticación y Seguridad](#63-módulo-de-autenticación-y-seguridad)
   - 6.4. [Módulo de Catálogos](#64-módulo-de-catálogos)
   - 6.5. [Módulos Satélite (Contacto, Domicilio, Empleo)](#65-módulos-satélite-contacto-domicilio-empleo)
7. [Manejo Global de Errores y Estandarización](#7-manejo-global-de-errores-y-estandarización)
8. [Configuración, Despliegue y Pruebas](#8-configuración-despliegue-y-pruebas)

---

## 1. Visión General y Objetivos del Sistema

La **Banco API** es una solución bancaria backend empresarial diseñada para orquestar de forma integral el proceso de **Onboarding Digital de Personas Físicas**. Permite registrar la identidad del cliente, normalizar y auditar sus canales de contacto, validar su ubicación geográfica contra catálogos oficiales del INEGI, corroborar solvencia económica y aperturar automáticamente una cuenta bancaria con saldo inicial y registro en el libro mayor contable.

### Metas Técnicas Primordiales:
* **Integridad de Datos Bancarios:** Políticas de base de datos sin tipos truncables arbitrarios (`VARCHAR`), implementando tipos fijos y `TEXT` con validación Regex estricta.
* **Seguridad Criptográfica Grado Financiero:** Protección de contraseñas mediante derivación lenta de claves (PBKDF2) y protección de biometría en reposo con cifrado autenticado simétrico (AES-256-GCM).
* **Control Centralizado de Sesión:** Regulación estricta en el servidor para impedir accesos o consultas no autorizadas si no hay una sesión activa, con revocación automática tras 300 segundos (5 minutos) de inactividad.
* **Resiliencia Operativa:** Arquitectura desacoplada para sincronización de catálogos demográficos y geográficos con fallback a base de datos relacional y caché en Redis.

---

## 2. Arquitectura de Software

### 2.1. Patrón de Arquitectura Limpia

La aplicación se estructura siguiendo los preceptos de **Clean Architecture** y principios **SOLID**, garantizando que el dominio y las reglas de negocio permanezcan independientes de los mecanismos de entrega HTTP y de la persistencia física.

```
┌────────────────────────────────────────────────────────┐
│                   CONTROLADORES REST                   │
│        (Client, Account, Auth, Catalog, Satellite)     │
└──────────────────────────┬─────────────────────────────┘
                           │ DTOs (Request / Response)
                           ▼
┌────────────────────────────────────────────────────────┐
│                   INTERCEPTORES WEB                    │
│      (ServerSessionInterceptor - Valida login = true)   │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                   CAPA DE SERVICIOS                    │
│       (Lógica transaccional, PBKDF2, AES-GCM, Reglas)   │
└──────────────────────────┬─────────────────────────────┘
                           │ Entidades JPA
                           ▼
┌────────────────────────────────────────────────────────┐
│                 REPOSITORIOS JPA & REDIS               │
│   (Spring Data JPA / PostgreSQL)  (Redis Cache / Jedis)│
└────────────────────────────────────────────────────────┘
```

### 2.2. Estructura de Paquetes

```
com.intrumentoev.demo/
├── client/                     # Clientes HTTP y conectores externos (INEGI)
├── config/                     # Configuraciones Spring (DB, WebMvc, Jackson, Redis, OpenAPI, Interceptores)
├── controller/                 # Controladores REST organizados por dominio
│   ├── account/                # Cuentas bancarias y movimientos
│   ├── auth/                   # Autenticación, biometría y estado de sesión
│   ├── catalog/                # Catálogos INEGI y demográficos
│   ├── client/                 # Onboarding y gestión de clientes
│   ├── contactDetail/          # Teléfonos y correo
│   ├── employment/             # Datos laborales
│   └── home/                   # Domicilio fiscal
├── entity/                     # Entidades JPA (@Table, @Entity) mapeadas al modelo PostgreSQL
├── exception/                  # Jerarquía de excepciones de negocio (RFC 7807)
├── mapper/                     # Mappers declarativos MapStruct
├── model/                      # DTOs, Requests, Responses y esquemas JSON
├── repository/                 # Interfaces Spring Data JPA
└── service/                    # Lógica de negocio (Interfaces en service/ e Implementaciones en impl/)
```

---

## 3. Diseño y Modelo de Base de Datos

### 3.1. Directiva de Tipos de Datos (Cero VARCHAR)

Cumpliendo con la directiva arquitectónica del sistema de **NO UTILIZAR `VARCHAR`**:

| Tipo PostgreSQL | Casos de Uso | Beneficio Técnico |
| :--- | :--- | :--- |
| `TEXT` | Nombres, apellidos, descripciones, correos, hashes, tokens | Almacenamiento idéntico a `VARCHAR` sin costos de reescritura en migraciones; gobernado por restricciones `CHECK` con expresiones regulares. |
| `CHAR(18)` | Clave Única de Registro de Población (`CURP`) | Longitud fija invariable. |
| `CHAR(10)` | Número de cuenta bancaria y teléfonos (móvil y alternativo) | Longitud fija estandarizada de 10 dígitos. |
| `CHAR(5)` | Código postal mexicano | 5 dígitos exactos según norma de Correos de México. |
| `CHAR(3)` | Clave de municipio (`cve_mun`) | 3 dígitos oficiales según INEGI. |
| `CHAR(2)` | Clave de entidad federativa (`cve_ent`) | 2 dígitos oficiales según INEGI. |
| `NUMERIC(15,2)` | Saldos de cuentas y montos de transacciones | Precisión decimal contable sin pérdida de precisión binaria de coma flotante. |
| `NUMERIC(12,2)` | Ingresos mensuales del cliente | Registro financiero positivo validado por `CHECK (monthly_income > 0)`. |
| `BYTEA` | Plantillas o firmas biométricas | Soporte binario para almacenamiento directo de datos cifrados. |
| `SMALLINT` | Llaves de catálogos e intentos fallidos | Ahorro sustancial de almacenamiento (2 bytes). |
| `BIGINT` | Llaves primarias de entidades transaccionales | Capacidad de direccionamiento a gran escala (`GENERATED ALWAYS AS IDENTITY`). |
| `TIMESTAMPTZ` | Tiempos de creación, actualización y login | Almacenamiento con zona horaria universal (UTC). |

### 3.2. Diagrama Entidad-Relación

A continuación se muestra el diagrama de arquitectura de datos completo del sistema:

<p align="center">
  <img src="./modelo_entidad_relacion.svg" alt="Diagrama Entidad-Relación - Banco API" width="100%">
</p>

* **Imagen en Alta Definición:** [`modelo_entidad_relacion.png`](./modelo_entidad_relacion.png) (2820 × 2070 px).
* **Gráfico Vectorial:** [`modelo_entidad_relacion.svg`](./modelo_entidad_relacion.svg).
* **Visor Web con Zoom Interactivo:** [`modelo_entidad_relacion.html`](./modelo_entidad_relacion.html).

---

### 3.3. Diccionario de Tablas y Restricciones (12 Entidades)

#### 1. `client` (Entidad Central)
* **Propósito:** Registro principal de la persona física.
* **Columnas:** `id_client` (PK, BIGINT), `name` (TEXT), `second_name` (TEXT), `last_name` (TEXT), `second_last_name` (TEXT), `birth_date` (DATE), `curp` (CHAR(18), UQ), `rfc` (TEXT, UQ), `id_gender` (FK, SMALLINT), `id_nationality` (FK, SMALLINT), `id_marital_status` (FK, SMALLINT), `is_active` (BOOLEAN), `deactivated_at` (TIMESTAMPTZ), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
* **Restricciones:** Validaciones Regex en nombres (`^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$`), CURP oficial, RFC con homoclave y coherencia de desactivación (`(is_active = TRUE AND deactivated_at IS NULL) OR (is_active = FALSE AND deactivated_at IS NOT NULL)`).

#### 2. `contact_details` (Relación 1 : 1 con `client`)
* **Propósito:** Canales de comunicación directa del cliente.
* **Columnas:** `id_contact_detail` (PK, BIGINT), `id_client` (FK, BIGINT, UQ), `email` (TEXT, UQ), `mobile_phone` (CHAR(10), UQ), `alternative_phone` (CHAR(10)), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
* **Restricciones:** Correo electrónico con estructura válida (`CHECK`), teléfonos de 10 dígitos.

#### 3. `home` (Relación 1 : 1 con `client`, N : 1 con `municipality`)
* **Propósito:** Ubicación geográfica y domicilio fiscal.
* **Columnas:** `id_home` (PK, BIGINT), `id_client` (FK, BIGINT, UQ), `street` (TEXT), `exterior_number` (TEXT), `interior_number` (TEXT), `neighborhood` (TEXT), `id_municipality` (FK, INTEGER), `postal_code` (CHAR(5)), `country` (TEXT), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).

#### 4. `employment_information` (Relación 1 : 1 con `client`)
* **Propósito:** Solvencia y situación laboral del cuentahabiente.
* **Columnas:** `id_employment` (PK, BIGINT), `id_client` (FK, BIGINT, UQ), `occupation` (TEXT), `company` (TEXT), `monthly_income` (NUMERIC(12,2)), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
* **Restricciones:** `CHECK (monthly_income > 0)`.

#### 5. `account` (Relación 1 : N con `client`)
* **Propósito:** Cuentas bancarias aperturadas a nombre del cliente.
* **Columnas:** `id_account` (PK, BIGINT), `account_number` (CHAR(10), UQ), `id_client` (FK, BIGINT), `balance` (NUMERIC(15,2)), `status` (TEXT: `'ACTIVA'`, `'INACTIVA'`), `opened_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
* **Restricciones:** Cuenta de 10 dígitos, saldo no negativo (`balance >= 0`).

#### 6. `account_balance` (Relación 1 : N con `account`)
* **Propósito:** Libro mayor bancario e historial auditado de variaciones de saldo.
* **Columnas:** `id_balance` (PK, BIGINT), `id_account` (FK, BIGINT), `previous_balance` (NUMERIC(15,2)), `amount` (NUMERIC(15,2)), `current_balance` (NUMERIC(15,2)), `movement_type` (TEXT: `'APERTURA'`, `'DEPOSITO'`, `'RETIRO'`, `'AJUSTE'`), `description` (TEXT), `created_at` (TIMESTAMPTZ).

#### 7. `auth` (Relación 1 : N con `client`)
* **Propósito:** Credenciales de autenticación, biometría y tokens de refresco.
* **Columnas:** `id_login` (PK, BIGINT), `id_client` (FK, BIGINT), `email` (TEXT, UQ), `password_hash` (TEXT), `biometric_type` (TEXT: `'HUELLA'`, `'FACIAL'`), `biometric_template` (BYTEA), `biometric_registered_at` (TIMESTAMPTZ), `refresh_token` (TEXT), `refresh_token_expires_at` (TIMESTAMPTZ), `is_active` (BOOLEAN), `failed_attempts` (SMALLINT), `last_login_at` (TIMESTAMPTZ), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
* **Restricciones:** Si `biometric_type` está presente, `biometric_template` es obligatorio y viceversa. Bloqueo al alcanzar 5 intentos fallidos.

#### 8. Catálogos Normalizados (`state`, `municipality`, `gender`, `nationality`, `marital_status`)
* Claves únicas, claves compuestas (`id_state, cve_mun`), banderas lógicas `is_active` y auditoría temporal.

---

### 3.4. Evolución de Esquema con Flyway

El ciclo de vida de la base de datos se versiona a través de scripts de migración SQL inmutables en `src/main/resources/db/`:

* `V1__create_gender_table.sql`: Creación del catálogo de sexos.
* `V2__create_nationality_table.sql`: Catálogo de nacionalidades.
* `V3__create_marital_status_table.sql`: Catálogo de estado civil.
* `V4__create_state_table.sql`: Entidades federativas con clave INEGI de 2 dígitos.
* `V5__create_municipality_table.sql`: Municipios vinculados al estado federativo.
* `V6__create_client_table.sql`: Tabla maestra de clientes personas físicas.
* `V7__create_contact_details_table.sql`: Datos de contacto y teléfonos únicos.
* `V8__create_home_table.sql`: Domicilio fiscal con clave postal y municipio.
* `V9__create_employment_information_table.sql`: Información laboral y solvencia.
* `V10__create_account_table.sql`: Cuentas bancarias de 10 dígitos.
* `V11__seed_catalogs.sql`: Poblado inicial de datos maestros.
* `V12__alter_contact_details_remove_email_lower.sql`: Migración de compatibilidad para emails.
* `V13__create_login_client_table.sql`: Autenticación, contraseñas y biometría cifrada.
* `V14__create_account_balance_table.sql`: Libro mayor inmutable para historial de saldos.

---

## 4. Módulo de Seguridad y Criptografía

### 4.1. Hashing de Contraseñas (PBKDF2)
* **Algoritmo:** `PBKDF2WithHmacSHA256`.
* **Configuración:** **10,000 iteraciones**, clave resultante de **256 bits**, con **Salt criptográfico aleatorio de 16 bytes** generado mediante `SecureRandom`.
* **Formato Serializado:** `iteraciones:salt_base64:hash_base64`.
* **Resistencia:** Elimina ataques por tablas Rainbow y mitiga ataques de fuerza bruta por GPU.

### 4.2. Cifrado de Biometría (AES-256-GCM)
* **Algoritmo:** `AES/GCM/NoPadding` (Galois/Counter Mode).
* **Parámetros:** Vector de Inicialización (IV) de **96 bits** único por registro y Etiqueta de Autenticación (Tag) de **128 bits**.
* **Integridad:** Detecta cualquier manipulación en la plantilla biométrica almacenada.
* **Mitigación de Canales Laterales:** Comparación en **tiempo constante** (`MessageDigest.isEqual`) para neutralizar *timing attacks*.

### 4.3. Tokens JWT y Refresh Tokens
* **Access Token:** JWT firmado con HMAC-SHA256, con tiempo de expiración de **300 segundos (5 minutos)**.
* **Refresh Token:** Token opaco de larga duración con fecha de expiración, con rotación en cada solicitud exitosa al endpoint `/v1/auth/refresh`.

### 4.4. Control de Sesión en el Servidor (Booleano e Inactividad)
Para garantizar estricto control de acceso:
* **Componente `ServerSessionManager`:** Mantiene un estado en memoria mediante `AtomicBoolean loggedIn`, rastreando la última actividad mediante `AtomicReference<Instant>`.
* **Interceptor `ServerSessionInterceptor`:**
  * **Rutas Públicas (No requieren sesión):**
    * Onboarding de clientes (`POST /v1/clientes/onboarding`).
    * Creación de cuenta bancaria (`POST /v1/cuentas`).
    * Autenticación (`POST /v1/auth/login`, `POST /v1/auth/login-biometrico`, `POST /v1/auth/refresh`).
    * Estado de la sesión del servidor (`GET /v1/auth/session-status`, `GET /v1/auth/estado-servidor`).
    * Consulta y sincronización de catálogos (`GET /v1/catalogos/**`, `POST /v1/catalogos/sincronizar`).
  * **Rutas Protegidas (Requieren `loggedIn == true`):**
    * Consultas y listados de clientes (`GET /v1/clientes/**`).
    * Consultas de cuentas bancarias y saldos (`GET /v1/cuentas/**`).
    * Modificaciones y bajas lógicas (`PUT`, `PATCH`, `DELETE`).
* **Temporizador de Inactividad de 300 segundos:** Si el servidor detecta que han transcurrido más de 300 segundos sin solicitudes en rutas protegidas, el booleano `loggedIn` se conmuta automáticamente a `false` y subsiguientes peticiones son rechazadas con HTTP `401 Unauthorized`.

---

## 5. Estrategia de Integración Externa y Caché

### 5.1. Consumo de API Oficial INEGI
* **Servicio Gaia INEGI:** `https://gaia.inegi.org.mx/wscatgeo/v2`
* **Endpoints consumidos:**
  * Marco Geoestadístico Estatal (`mgee`): Catálogo de las 32 entidades federativas.
  * Marco Geoestadístico Municipal (`mgem`): Municipios clasificados por clave de entidad.
* **Resiliencia:** Configuración de timeouts de conexión (5s) y lectura (10s).

### 5.2. Capa de Caché con Redis y Resiliencia Fallback
* Las consultas de catálogos geográficos (`/v1/catalogos/estados` y `/v1/catalogos/municipios/{cveEnt}`) consultan primero la base en memoria **Redis**.
* **Estrategia Fallback:** Si Redis no está disponible o la clave expira, el sistema consulta PostgreSQL; en caso de ausencia local, contacta la API del INEGI y repuebla de forma transparente la base de datos y la memoria caché.

---

## 6. Catálogo Completo de Endpoints REST

### 6.1. Módulo de Clientes y Onboarding (`/v1/clientes`)

| Método | Ruta | Requiere Sesión | Descripción | Códigos HTTP |
| :--- | :--- | :---: | :--- | :--- |
| `POST` | `/v1/clientes/onboarding` | ❌ No | Onboarding integral: persona, domicilio, contacto, empleo y apertura automática de cuenta bancaria ($1000 saldo inicial). | `201`, `400`, `409`, `422` |
| `GET` | `/v1/clientes` | ✅ Sí | Búsqueda filtrada de clientes (curp, rfc, email, numeroCuenta, activo, rango de fechas). | `200`, `400`, `401` |
| `GET` | `/v1/clientes/{id}` | ✅ Sí | Consulta de detalle integral de cliente por su ID numérico positivo. | `200`, `400`, `401`, `404` |
| `GET` | `/v1/clientes/curp/{curp}` | ✅ Sí | Consulta de cliente por su CURP de 18 caracteres. | `200`, `400`, `401`, `404` |
| `GET` | `/v1/clientes/rfc/{rfc}` | ✅ Sí | Consulta de cliente por su RFC de 12 o 13 caracteres. | `200`, `400`, `401`, `404` |
| `PUT` | `/v1/clientes/{id}` | ✅ Sí | Actualización de datos generales del cliente. | `200`, `400`, `401`, `404` |
| `DELETE` | `/v1/clientes/{id}` | ✅ Sí | Baja lógica del cliente (`is_active = false`, registra `deactivated_at`). | `204`, `400`, `401`, `404` |

---

### 6.2. Módulo de Cuentas Bancarias y Saldos (`/v1/cuentas`)

| Método | Ruta | Requiere Sesión | Descripción | Códigos HTTP |
| :--- | :--- | :---: | :--- | :--- |
| `POST` | `/v1/cuentas` | ❌ No | Apertura de cuenta bancaria adicional para un cliente activo. | `201`, `400`, `404`, `422` |
| `GET` | `/v1/cuentas/{numeroCuenta}` | ✅ Sí | Detalle de cuenta bancaria por su número de 10 dígitos. | `200`, `400`, `401`, `404` |
| `GET` | `/v1/cuentas/{numeroCuenta}/saldo`| ✅ Sí | Consulta de saldo disponible y estatus de la cuenta. | `200`, `400`, `401`, `404` |
| `GET` | `/v1/cuentas/activas` | ✅ Sí | Listado de todas las cuentas con estado ACTIVA. | `200`, `401` |
| `GET` | `/v1/cuentas/cliente/{idClient}` | ✅ Sí | Consulta de cuentas asociadas a un cliente. | `200`, `400`, `401` |
| `GET` | `/v1/cuentas/{numeroCuenta}/movimientos` | ✅ Sí | Historial de auditoría contable en el libro mayor (`account_balance`). | `200`, `400`, `401`, `404` |

---

### 6.3. Módulo de Autenticación y Seguridad (`/v1/auth`)

| Método | Ruta | Requiere Sesión | Descripción | Códigos HTTP |
| :--- | :--- | :---: | :--- | :--- |
| `GET` | `/v1/auth/session-status` | ❌ No | Consulta el booleano en el servidor (`loggedIn`), segundos de inactividad y segundos restantes de expiración. | `200` |
| `POST` | `/v1/auth/login` | ❌ No | Autenticación mediante email y contraseña (PBKDF2). Establece `login = true` en servidor. | `200`, `400`, `401`, `423` |
| `POST` | `/v1/auth/login-biometrico` | ❌ No | Login mediante huella dactilar o reconocimiento facial descifrado. | `200`, `400`, `401`, `423` |
| `POST` | `/v1/auth/biometria` | ✅ Sí | Enrolamiento o actualización de biometría del cliente. | `200`, `400`, `401`, `404` |
| `POST` | `/v1/auth/refresh` | ❌ No | Renovación de token de acceso JWT y rotación de Refresh Token. | `200`, `401` |
| `POST` | `/v1/auth/logout` | ❌ No | Cierre de sesión voluntario. Invalida tokens y pasa el booleano a `false`. | `204`, `400` |

---

### 6.4. Módulo de Catálogos (`/v1/catalogos`)

| Método | Ruta | Requiere Sesión | Descripción | Códigos HTTP |
| :--- | :--- | :---: | :--- | :--- |
| `GET` | `/v1/catalogos/generos` | ❌ No | Listado de sexos biológicos para CURP (Hombre, Mujer). | `200` |
| `GET` | `/v1/catalogos/nacionalidades` | ❌ No | Listado de nacionalidades registradas. | `200` |
| `GET` | `/v1/catalogos/estados-civiles`| ❌ No | Listado de estados civiles válidos. | `200` |
| `GET` | `/v1/catalogos/estados` | ❌ No | Entidades federativas mexicanas (con caché en Redis y fallback). | `200` |
| `GET` | `/v1/catalogos/municipios/{cveEnt}` | ❌ No | Municipios clasificados por clave de entidad federativa de 2 dígitos. | `200`, `400` |
| `POST` | `/v1/catalogos/sincronizar` | ❌ No | Sincronización batch con la API oficial del INEGI. | `200` |

---

### 6.5. Módulos Satélite (Contacto, Domicilio, Empleo)

* **Detalles de Contacto (`/v1/contact-details`):**
  * `GET /v1/contact-details/{id}`
  * `GET /v1/contact-details/client/{idClient}`
  * `PUT /v1/contact-details/{id}`
  * `PATCH /v1/contact-details/{id}`
* **Domicilios (`/v1/domicilios`):**
  * `GET /v1/domicilios/{id}`
  * `GET /v1/domicilios/cliente/{idClient}`
  * `PUT /v1/domicilios/{id}`
  * `PATCH /v1/domicilios/{id}`
* **Información Laboral (`/v1/laboral`):**
  * `GET /v1/laboral/{id}`
  * `GET /v1/laboral/cliente/{idClient}`
  * `PUT /v1/laboral/{id}`
  * `PATCH /v1/laboral/{id}`

---

## 7. Manejo Global de Errores y Estandarización

El sistema adopta las directrices de diseño de APIs de Microsoft y la especificación **RFC 7807** mediante la clase centralizada [`GlobalExceptionHandler`](./src/main/java/com/intrumentoev/demo/config/GlobalExceptionHandler.java):

### Estructura de Respuesta de Error Unificada:
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Error de validación en los datos de la solicitud",
    "target": "requestBody",
    "details": [
      {
        "code": "INVALID_FIELD",
        "target": "rfc",
        "message": "El formato del RFC es inválido"
      }
    ]
  }
}
```

### Principales Excepciones Manejadas:
* `BusinessValidationException` (HTTP 400 / 422): Reglas de negocio incumplidas (menor de edad, fechas incoherentes, monto no positivo).
* `ResourceNotFoundException` (HTTP 404): Entidad solicitada inexistente.
* `DuplicateResourceException` (HTTP 409): Violación de unicidad en CURP, RFC, Email o Teléfono.
* `LockedException` (HTTP 423): Cuenta suspendida por exceso de intentos fallidos.
* `ConstraintViolationException` (HTTP 400): Violación de validaciones a nivel de parámetros de URL o PathVariables.
* `HttpMessageNotReadableException` (HTTP 400): JSON malformado o campos desconocidos rechazados gracias a `fail-on-unknown-properties=true`.

---

## 8. Configuración, Despliegue y Pruebas

### 8.1. Requisitos de Infraestructura
* **Java SDK:** 21 LTS (Oracle OpenJDK o Eclipse Temurin).
* **Motor de Base de Datos:** PostgreSQL 15 o superior.
* **Caché en Memoria:** Redis 6 o superior.
* **Gestor de Compilación:** Gradle 8.x con Wrapper integrado.

### 8.2. Variables y Propiedades Principales (`application.properties`)
```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/banco
spring.datasource.username=postgres
spring.datasource.password=linux

# Reglas de negocio
bank.account.initial-balance=1000.00
server.session.inactivity-limit-seconds=300
jwt.access-token-expiration-seconds=300

# Serialización segura
spring.jackson.deserialization.fail-on-unknown-properties=true
spring.jackson.mapper.allow-coercion-of-scalars=false
spring.jackson.default-property-inclusion=non_null
```

### 8.3. Comandos de Compilación y Ejecución

* **Compilar y ejecutar suite completa de pruebas:**
  ```bash
  ./gradlew test
  ```
* **Levantar la aplicación en entorno local:**
  ```bash
  ./gradlew bootRun
  ```
* **Acceso a la Documentación Interactiva OpenAPI / Swagger:**
  ```
  http://localhost:8080/swagger-ui.html
  ```
