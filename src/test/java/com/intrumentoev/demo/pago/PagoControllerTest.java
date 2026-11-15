package com.intrumentoev.demo.pago;

import com.intrumentoev.demo.controller.gestopago.PagoController;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.model.auth.AuthMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PagoControllerTest extends PagoIntegrationTestBase {

    private static final String RECARGA_JSON =
            "{\"numCuenta\":\"9876543210\",\"idServicio\":133,\"idProducto\":582,\"tipoPago\":\"RECARGA\",\"telefono\":\"5512345678\"}";
    private static final String SERVICIO_JSON =
            "{\"numCuenta\":\"9876543210\",\"idServicio\":79,\"idProducto\":209,\"tipoPago\":\"SERVICIO\",\"referencia\":\"40425475190118187271\",\"monto\":150.00}";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;

    private Long idCliente;

    @BeforeEach
    void cargarCliente() {
        idCliente = cuentaDemo().getIdClient();
    }

    private String bearer(AuthMethod metodo) {
        return "Bearer " + jwtTokenService.generateAccessToken(idCliente, 1L, "alejandro.hernandez@banco-demo.com", metodo);
    }

    private String confirmToken(Long clientId) {
        return jwtTokenService.generateConfirmationToken(clientId, 1L, "alejandro.hernandez@banco-demo.com", AuthMethod.PASSWORD);
    }

    private MockHttpServletRequestBuilder pagar(String authorization, String json) {
        return pagarConConfirmacion(authorization, confirmToken(idCliente), json);
    }

    private MockHttpServletRequestBuilder pagarConConfirmacion(String authorization, String confirmationToken, String json) {
        MockHttpServletRequestBuilder req = post("/v1/pagos").contentType(MediaType.APPLICATION_JSON).content(json);
        if (authorization != null) {
            req.header("Authorization", authorization);
        }
        if (confirmationToken != null) {
            req.header(PagoController.HEADER_CONFIRMATION_TOKEN, confirmationToken);
        }
        return req;
    }

    @Test
    @DisplayName("Sin header Authorization → 401 TOKEN_MISSING")
    void sinHeader() throws Exception {
        mockMvc.perform(pagar(null, RECARGA_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("TOKEN_MISSING")));
        verify(txClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Token con firma inválida → 401")
    void tokenInvalido() throws Exception {
        mockMvc.perform(pagar("Bearer abc.def.ghi", RECARGA_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("TOKEN_EXPIRED_OR_INVALID")));
    }

    @Test
    @DisplayName("Sin token de confirmación, expirado, reutilizado o de otro cliente → 403 STRONG_AUTH_REQUIRED")
    void confirmacionRequeridaYDeUnSoloUso() throws Exception {
        // 1. Ausente
        mockMvc.perform(pagarConConfirmacion(bearer(AuthMethod.PASSWORD), null, RECARGA_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("STRONG_AUTH_REQUIRED")));

        // 2. Expirado
        JwtTokenService expService = new JwtTokenService();
        org.springframework.test.util.ReflectionTestUtils.setField(expService, "jwtSecret",
                "test_jwt_secret_key_for_unit_tests_only_32_bytes_long_1234567890");
        org.springframework.test.util.ReflectionTestUtils.setField(expService, "confirmationTokenExpirationSeconds", -10L);
        String expirado = expService.generateConfirmationToken(idCliente, 1L, "alejandro.hernandez@banco-demo.com", AuthMethod.PASSWORD);
        mockMvc.perform(pagarConConfirmacion(bearer(AuthMethod.PASSWORD), expirado, RECARGA_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("STRONG_AUTH_REQUIRED")));

        // 3. De otro cliente
        String deOtro = confirmToken(idCliente + 777);
        mockMvc.perform(pagarConConfirmacion(bearer(AuthMethod.PASSWORD), deOtro, RECARGA_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("STRONG_AUTH_REQUIRED")));

        verify(txClient, never()).sendTx(anyString(), anyString());

        // 4. Válido con POST /v1/auth/confirmar-operacion y fallo al reutilizarlo
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "Transaccion aplicada", 291437188L, "100024988", "2.50"));
        String confirmResp = mockMvc.perform(post("/v1/auth/confirmar-operacion")
                        .header("Authorization", bearer(AuthMethod.PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"PasswordSegura#2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String tokenConfirmacion = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(confirmResp).get("confirmationToken").asText();

        // Primer uso: pago exitoso con JWT PASSWORD (web/móvil)
        mockMvc.perform(pagarConConfirmacion(bearer(AuthMethod.PASSWORD), tokenConfirmacion, RECARGA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.estado", is("APLICADO")));

        // Segundo uso del mismo token: rechazado con 403 STRONG_AUTH_REQUIRED
        mockMvc.perform(pagarConConfirmacion(bearer(AuthMethod.PASSWORD), tokenConfirmacion, RECARGA_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("STRONG_AUTH_REQUIRED")));
    }

    @Test
    @DisplayName("El idClient del body se rechaza: el cliente sale solo del JWT")
    void idClientEnBodyNoPermitido() throws Exception {
        String json = RECARGA_JSON.replace("{", "{\"idClient\":999,");
        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cuenta ajena e inexistente responden 403 con el mismo mensaje")
    void cuentaAjenaEIinexistente() throws Exception {
        String inexistente = RECARGA_JSON.replace("9876543210", "0000000001");
        String mensajeInexistente = mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), inexistente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("PAYMENT_FORBIDDEN")))
                .andReturn().getResponse().getContentAsString();

        // token válido pero de otro cliente
        Long otro = idCliente + 999;
        String tokenOtro = "Bearer " + jwtTokenService.generateAccessToken(otro, 2L, "otro@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        String mensajeAjena = mockMvc.perform(pagarConConfirmacion(tokenOtro, confirmToken(otro), RECARGA_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(mensajeAjena).isEqualTo(mensajeInexistente);
    }

    @Test
    @DisplayName("Telefono y referencia juntos → 400")
    void ambosCampos() throws Exception {
        String json = RECARGA_JSON.replace("}", ",\"referencia\":\"123\"}");
        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_PAYMENT_REQUEST")));
    }

    @Test
    @DisplayName("Saldo insuficiente → 422")
    void saldoInsuficiente() throws Exception {
        establecerCuenta(new BigDecimal("10.00"), "ACTIVA");
        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), SERVICIO_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("INSUFFICIENT_FUNDS")));
    }

    @Test
    @DisplayName("Recarga aplicada: 200 con codigo 1, estado APLICADO, codigoGestoPago 01, autorización, idTx y upc")
    void recargaAplicada() throws Exception {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "Transaccion aplicada", 291437188L, "100024988", "2.50"));

        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), RECARGA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.estado", is("APLICADO")))
                .andExpect(jsonPath("$.codigoGestoPago", is("01")))
                .andExpect(jsonPath("$.numAutorizacion", is("100024988")))
                .andExpect(jsonPath("$.idTx", is(291437188)))
                .andExpect(jsonPath("$.upc").isNotEmpty());
    }

    @Test
    @DisplayName("Servicio rechazado por GestoPago (02): 200 con codigo 0 y el TEXTO en mensaje")
    void servicioRechazado() throws Exception {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("02", "Saldo insuficiente del distribuidor", null, "0", null));

        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), SERVICIO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(0)))
                .andExpect(jsonPath("$.estado", is("RECHAZADO")))
                .andExpect(jsonPath("$.mensaje", is("Saldo insuficiente del distribuidor")));
    }

    @Test
    @DisplayName("Timeout 82: 200 con codigo 0, 'Pago en proceso de confirmación' y upc")
    void porConfirmar() throws Exception {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));

        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), RECARGA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(0)))
                .andExpect(jsonPath("$.estado", is("POR_CONFIRMAR")))
                .andExpect(jsonPath("$.mensaje", is("Pago en proceso de confirmación")))
                .andExpect(jsonPath("$.upc").isNotEmpty());
    }

    @Test
    @DisplayName("Producto fuera de catálogo: 200 con codigo 0 y mensaje de producto no disponible")
    void productoNoDisponible() throws Exception {
        String json = RECARGA_JSON.replace("582", "99999");
        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(0)))
                .andExpect(jsonPath("$.mensaje", is("No se pudo realizar el pago: producto o servicio no disponible")));
        verify(catalogoRefresher).refrescarEnSegundoPlano();
    }

    @Test
    @DisplayName("Fase 4: validar referencia correcta, referencia inválida, error técnico del proveedor e intento de pago sin validar (422)")
    void validacionDeReferenciasFlujoCompleto() throws Exception {
        String auth = bearer(AuthMethod.PASSWORD);

        // 1. Intento de pago de servicio con referencia NO validada -> 422 REFERENCE_NOT_VALIDATED
        String servicioSinValidarJson = "{\"numCuenta\":\"9876543210\",\"idServicio\":79,\"idProducto\":209,\"tipoPago\":\"SERVICIO\",\"referencia\":\"998877665544\",\"monto\":150.00}";
        mockMvc.perform(pagar(auth, servicioSinValidarJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("REFERENCE_NOT_VALIDATED")));

        // 2. Referencia inválida (solo ceros o caracteres inválidos) -> valida=false, estadoValidacion=INVALIDA
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"000000000000\",\"monto\":150.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)))
                .andExpect(jsonPath("$.estadoValidacion", is("INVALIDA")))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        // Intento de pagar con la referencia inválida sigue rechazando con 422
        String servicioInvalidoJson = "{\"numCuenta\":\"9876543210\",\"idServicio\":79,\"idProducto\":209,\"tipoPago\":\"SERVICIO\",\"referencia\":\"000000000000\",\"monto\":150.00}";
        mockMvc.perform(pagar(auth, servicioInvalidoJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("REFERENCE_NOT_VALIDATED")));

        // 3. Referencia válida -> valida=true, guarda en Redis/caché y habilita el pago
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"998877665544\",\"monto\":150.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(true)))
                .andExpect(jsonPath("$.estadoValidacion", is("VALIDA")))
                .andExpect(jsonPath("$.validationId").isNotEmpty())
                .andExpect(jsonPath("$.nombreServicio", is("CFE")));

        // Ahora el pago con esa referencia validada y monto 150.00 sí procede
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "Pago CFE aplicado", 5001L, "AUTH-CFE", "0.00"));
        mockMvc.perform(pagar(auth, servicioSinValidarJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.estado", is("APLICADO")));

        // 4. Error técnico del proveedor (timeout / sin token disponible) -> 502 GESTOPAGO_PROVIDER_ERROR
        when(tokenService.obtenerTokenVigente()).thenThrow(new RuntimeException("Timeout conectando a GestoPago"));
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"998877665544\",\"monto\":150.00}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code", is("GESTOPAGO_PROVIDER_ERROR")));
    }

    @Test
    @DisplayName("Fase 4: ramas adicionales de validación de formato (corta, caracteres especiales, dígito verificador y producto no encontrado)")
    void validacionDeReferenciasCasosBorde() throws Exception {
        String auth = bearer(AuthMethod.PASSWORD);

        when(productoServiceRedis.getProductsRedis()).thenReturn(java.util.List.of(
                com.intrumentoev.demo.model.gestopago.ProductoResponse.builder()
                        .idServicio(79).idProducto(209).producto("Luz CFE").servicio("CFE")
                        .idCatTipoServicio(2).tipoFront(2).hasDigitoVerificador(true)
                        .tipoReferencia("10 dígitos numéricos").precio("150.00").build()
        ));

        // Referencia demasiado corta
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)));

        // Caracteres especiales no permitidos
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"1234$5678!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)));

        // No numérica cuando exige dígitos
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"ABCDEF1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)));

        // Longitud distinta de 10 dígitos cuando tipoReferencia pide 10 dígitos
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)));

        // Dígito verificador inválido (todos los dígitos iguales o sufijo 99999)
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"1111111111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida", is(false)));

        // Producto inexistente -> 404 CATALOG_NOT_FOUND
        mockMvc.perform(post("/v1/referencias/validar")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idServicio\":999,\"idProducto\":999,\"referencia\":\"1234567890\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("CATALOG_NOT_FOUND")));
    }

    @Test
    @DisplayName("Fase 5: GET /v1/pagos/{idOrUpc} consulta estado de un pago POR_CONFIRMAR y refleja su resolución final; protege pagos de otro cliente (403) e inexistentes (404)")
    void consultarPagoPorUpcORefrescarPendiente() throws Exception {
        String auth = bearer(AuthMethod.PASSWORD);

        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        String respJson = mockMvc.perform(pagar(auth, RECARGA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("POR_CONFIRMAR")))
                .andExpect(jsonPath("$.codigo", is(0)))
                .andReturn().getResponse().getContentAsString();

        String upc = new com.fasterxml.jackson.databind.ObjectMapper().readTree(respJson).get("upc").asText();
        Long idPago = pagoRepository.findByUpc(upc).orElseThrow().getId();

        // Consultar por UPC mientras está POR_CONFIRMAR -> codigo 0, estado POR_CONFIRMAR
        mockMvc.perform(get("/v1/pagos/" + upc).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upc", is(upc)))
                .andExpect(jsonPath("$.estado", is("POR_CONFIRMAR")))
                .andExpect(jsonPath("$.codigo", is(0)))
                .andExpect(jsonPath("$.nombreServicio", is("Telcel")));

        // Consultar por ID numérico también funciona
        mockMvc.perform(get("/v1/pagos/" + idPago).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idPago.intValue())))
                .andExpect(jsonPath("$.upc", is(upc)));

        // Simular resolución por confirmTx -> pasa a APLICADO y el cliente refresca el comprobante
        var entidad = pagoRepository.findByUpc(upc).orElseThrow();
        entidad.setEstado(com.intrumentoev.demo.model.gestopago.pago.EstadoPago.APLICADO);
        entidad.setCodigoRespuesta("06");
        entidad.setTextoRespuesta("Confirmada exitosamente");
        entidad.setNumAutorizacion("AUTH-FINAL-99");
        entidad.setIdTx(888777L);
        pagoRepository.save(entidad);

        mockMvc.perform(get("/v1/pagos/" + upc).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("APLICADO")))
                .andExpect(jsonPath("$.codigo", is(1)))
                .andExpect(jsonPath("$.numAutorizacion", is("AUTH-FINAL-99")))
                .andExpect(jsonPath("$.idTx", is(888777)));

        // Otro cliente no puede consultar este pago -> 403 PAYMENT_FORBIDDEN
        String tokenOtro = "Bearer " + jwtTokenService.generateAccessToken(idCliente + 555, 2L, "otro@banco.com", AuthMethod.PASSWORD);
        mockMvc.perform(get("/v1/pagos/" + upc).header("Authorization", tokenOtro))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("PAYMENT_FORBIDDEN")));

        // UPC o ID inexistente -> 404 PAYMENT_NOT_FOUND
        mockMvc.perform(get("/v1/pagos/UPCNOEXISTE99").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("PAYMENT_NOT_FOUND")));
    }

    @Test
    @DisplayName("Fase 6: GET /v1/pagos devuelve historial paginado, filtrable por estado y fechas, solo del cliente del JWT y con referencia enmascarada; GET /v1/pagos/{id} devuelve el detalle")
    void historialPagosPaginadoFiltradoYAisladoPorCliente() throws Exception {
        String authCliente1 = bearer(AuthMethod.PASSWORD);
        Long otroId = idCliente + 888;
        String authCliente2 = "Bearer " + jwtTokenService.generateAccessToken(otroId, 2L, "otro@banco.com", AuthMethod.PASSWORD);

        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "Pago aplicado", 9001L, "AUTH-001", "1.50"));
        mockMvc.perform(pagar(authCliente1, SERVICIO_JSON)).andExpect(status().isOk());

        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("02", "Pago rechazado", null, "0", null));
        mockMvc.perform(pagar(authCliente1, RECARGA_JSON)).andExpect(status().isOk());

        // Insertar un pago de otro cliente directamente en el repositorio para verificar aislamiento
        var pagoAjeno = pagoRepository.save(com.intrumentoev.demo.entity.gestopago.PagoGestoPago.builder()
                .idCliente(otroId)
                .numCuenta("1111222233")
                .idServicio(133)
                .idProducto(582)
                .tipoPago(com.intrumentoev.demo.model.gestopago.pago.TipoPago.RECARGA)
                .telefono("5599998888")
                .monto(new BigDecimal("50.00"))
                .upc("UPCAJENO001")
                .estado(com.intrumentoev.demo.model.gestopago.pago.EstadoPago.APLICADO)
                .payloadJson("{}")
                .fechaEnvio(java.time.OffsetDateTime.now())
                .build());

        // 1. Cliente 1 ve solo sus 2 pagos (nunca el pago del cliente 2) y con referencia/teléfono enmascarados
        mockMvc.perform(get("/v1/pagos").header("Authorization", authCliente1)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.content[0].destinoEnmascarado", org.hamcrest.Matchers.startsWith("*")));

        // 2. Filtrar por estado APLICADO y rango de fechas (desde / hasta)
        String hoy = java.time.LocalDate.now().toString();
        String ayer = java.time.LocalDate.now().minusDays(1).toString();
        String manana = java.time.LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/v1/pagos").header("Authorization", authCliente1)
                        .param("estado", "APLICADO")
                        .param("desde", ayer)
                        .param("hasta", manana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].estado", is("APLICADO")))
                .andExpect(jsonPath("$.content[0].referencia", is("********7271")));

        // 3. Cliente 1 NO puede ver el detalle del pago del cliente 2 -> 403 PAYMENT_FORBIDDEN
        mockMvc.perform(get("/v1/pagos/" + pagoAjeno.getId()).header("Authorization", authCliente1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("PAYMENT_FORBIDDEN")));

        // 4. Cliente 2 ve su propio pago en detalle con su teléfono completo
        mockMvc.perform(get("/v1/pagos/" + pagoAjeno.getId()).header("Authorization", authCliente2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upc", is("UPCAJENO001")))
                .andExpect(jsonPath("$.telefono", is("5599998888")));

        // 5. Parámetros de filtro inválidos devuelven 400 INVALID_PAYMENT_REQUEST
        mockMvc.perform(get("/v1/pagos").header("Authorization", authCliente1).param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_PAYMENT_REQUEST")));
        mockMvc.perform(get("/v1/pagos").header("Authorization", authCliente1).param("desde", "fecha-mala"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_PAYMENT_REQUEST")));
    }
}
