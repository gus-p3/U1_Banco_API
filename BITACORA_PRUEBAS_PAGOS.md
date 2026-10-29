# 🧪 Bitácora y Plan de Pruebas: Integración de Pagos GestoPago (`POST /v1/pagos`)

**Proyecto:** Banco API - Módulo de Pagos y Servicios Externos  
**Fecha de Ejecución:** Octubre 2026  
**Ambiente:** Local (`http://localhost:8080`) / Producción (`https://u1bancoapi-production.up.railway.app`)  
**Colección Postman:** `Banco_API_Postman_Collection.json` (Carpeta: `07. Pagos GestoPago (POST /v1/pagos)`)  
**Evaluador:** _______________________________  

---

## 📌 Guía Rápida de Configuración en Postman

1. **Importar Colección:** Abre Postman -> **Import** -> Selecciona el archivo [`Banco_API_Postman_Collection.json`](./Banco_API_Postman_Collection.json) ubicado en la raíz del proyecto.
2. **Variables de Colección:** Verifica que la variable `baseUrl` apunte a tu entorno:
   - Local: `http://localhost:8080`
   - Producción/Nube: `https://u1bancoapi-production.up.railway.app`
3. **Flujo de Ejecución:** Ejecuta las pruebas en orden numérico correlativo (`07.1` hasta `07.20`) o utiliza el **Collection Runner** de Postman para correr la carpeta `07` completa. Los scripts de prueba guardan automáticamente los tokens en variables (`authToken`, `pagoTokenHuella`, `pagoIdServicioRecarga`, etc.).

---

## 🗺️ Matriz del Flujo de Pruebas

| ID | Nombre de la Prueba | Método / Endpoint | HTTP Esp. | Código / Error Esperado | Criterio de Aceptación | Estado (PASS / FAIL / BUG) |
|---|---|---|---|---|---|:---:|
| **07.1** | Login con contraseña | `POST /v1/auth/login` | 200 OK | `access_token` emitido | Genera token con `authMethod=PASSWORD` (este token NO debe tener permiso de pagar). | [ ] |
| **07.2** | Login Biométrico HUELLA | `POST /v1/auth/biometric/login` | 200 OK | `access_token` emitido | Genera token con `authMethod=BIOMETRIC_HUELLA`. Guarda `pagoTokenHuella`. | [ ] |
| **07.3** | Consultar Catálogo GestoPago | `GET /v1/productos/redis` | 200 OK | Lista JSON de productos | Autodetecta y guarda en variables un producto de Recarga (`tipoFront=1`) y uno de Servicio (`tipoFront=2`). | [ ] |
| **07.4** | Consultar Saldo Inicial | `GET /v1/cuentas/9876543210/saldo` | 200 OK | `currentBalance` | Valida el saldo antes de pagar (ej. $1,000.00). | [ ] |
| **07.5** | Error 401 - Sin Header Authorization | `POST /v1/pagos` | 401 Unauth | `TOKEN_MISSING` | Petición rechazada sin token en la cabecera. | [ ] |
| **07.6** | Error 401 - JWT Inválido / Malformado | `POST /v1/pagos` | 401 Unauth | `TOKEN_EXPIRED_OR_INVALID` | Petición con firma alterada o formato erróneo es bloqueada. | [ ] |
| **07.7** | Error 403 - Token de Contraseña | `POST /v1/pagos` | 403 Forbid | `STRONG_AUTH_REQUIRED` | Intentar pagar con token de login tradicional es denegado (requiere huella). | [ ] |
| **07.8** | Error 403 - Cuenta Inexistente / Ajena | `POST /v1/pagos` | 403 Forbid | `PAYMENT_FORBIDDEN` | Mismo mensaje genérico para cuenta ajena o inexistente (evita enumeración de cuentas). | [ ] |
| **07.9** | Error 400 - Teléfono y Referencia Juntos | `POST /v1/pagos` | 400 Bad Req | `INVALID_PAYMENT_REQUEST` | No permite enviar ambos campos simultáneamente. | [ ] |
| **07.10** | Error 400 - Sin Teléfono ni Referencia | `POST /v1/pagos` | 400 Bad Req | `INVALID_PAYMENT_REQUEST` | Requiere al menos el identificador correspondiente al tipo de pago. | [ ] |
| **07.11** | Error 400 - SERVICIO sin Monto | `POST /v1/pagos` | 400 Bad Req | `INVALID_PAYMENT_REQUEST` | Los pagos de servicios exigen especificar el monto a liquidar. | [ ] |
| **07.12** | Error 400 - tipoPago Inválido | `POST /v1/pagos` | 400 Bad Req | `INVALID_PAYMENT_REQUEST` | Solo permite valores `RECARGA` o `SERVICIO`. | [ ] |
| **07.13** | Error 400 - idClient Inyectado en Body | `POST /v1/pagos` | 400 Bad Req | `INVALID_FIELD` | Bloquea Mass Assignment (el cliente solo se toma del JWT). | [ ] |
| **07.14** | Error 422 - Saldo Insuficiente | `POST /v1/pagos` | 422 Unproc | `INSUFFICIENT_FUNDS` | Intento de pago mayor al saldo disponible (ej. $99,999.00). | [ ] |
| **07.15** | Error 422 - RECARGA con Producto Servicio | `POST /v1/pagos` | 422 Unproc | `PAYMENT_PRODUCT_MISMATCH` | Conflicto entre `tipoPago` y `tipoFront` del catálogo. | [ ] |
| **07.16** | Error 422 - Monto Manipulado en RECARGA | `POST /v1/pagos` | 422 Unproc | `PAYMENT_PRODUCT_MISMATCH` | El monto no coincide exactamente con el precio oficial de la recarga en catálogo. | [ ] |
| **07.17** | Producto Inexistente en Catálogo | `POST /v1/pagos` | 200 OK | `codigo: 0` | Responde mensaje controlado `"producto o servicio no disponible"` y activa refresco en background. | [ ] |
| **07.18** | Pago de RECARGA Exitoso | `POST /v1/pagos` | 200 OK | `codigo: 1` / `estado: APLICADO` | Genera UPC único, descuenta el saldo exacto y registra autorización de GestoPago. | [ ] |
| **07.19** | Pago de SERVICIO Exitoso | `POST /v1/pagos` | 200 OK | `codigo: 1` o `0` (según saldo GestoPago) | Envía referencia y monto. Si el simulador GestoPago devuelve éxito o rechazo controlado, actualiza estado. | [ ] |
| **07.20** | Verificación de Saldo y Comisión | `GET /v1/cuentas/9876543210/saldo` | 200 OK | `currentBalance` exacto | Comprueba que **solo se descontó el importe del pago** y que la comisión bancaria no fue cobrada al cliente. | [ ] |

---

## 🐞 Registro y Seguimiento de Hallazgos / Bugs

Usa esta sección para anotar cualquier comportamiento anómalo detectado durante tus pruebas:

### Reporte 1: [Título Corto del Fallo]
* **Prueba Asociada:** (Ej. `07.16` o prueba exploratoria)
* **Datos Enviados (Payload / Headers):**
```json
{
}
```
* **Respuesta Obtenida (HTTP Status + Body):**
```json
{
}
```
* **Comportamiento Esperado:** 
* **Severidad:** (Crítica / Alta / Media / Baja)
* **Notas / Causa Probable:** 

---

### Reporte 2: [Título Corto del Fallo]
* **Prueba Asociada:** 
* **Datos Enviados:**
```json
{
}
```
* **Respuesta Obtenida:**
```json
{
}
```
* **Comportamiento Esperado:** 
* **Severidad:** 
* **Notas / Causa Probable:** 

---

## 📋 Resumen Final de Ejecución

- **Total de Pruebas Ejecutadas:** ___ / 20
- **Pruebas Exitosas (PASS):** ___
- **Pruebas Fallidas (FAIL):** ___
- **Bugs Encontrados:** ___
- **Dictamen Final:** [ ] APROBADO PARA PRODUCCIÓN  /  [ ] REQUIERE AJUSTES

