package com.intrumentoev.demo.pago;

import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import com.intrumentoev.demo.model.auth.AuthMethod;
import org.junit.jupiter.api.AfterEach;
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
    @Autowired private ServerSessionManager sessionManager;

    private Long idCliente;

    @BeforeEach
    void abrirSesion() {
        idCliente = cuentaDemo().getIdClient();
        sessionManager.setLoggedIn(true, "alejandro.hernandez@banco-demo.com", idCliente);
    }

    @AfterEach
    void cerrarSesion() {
        sessionManager.setLoggedIn(false, null, null);
    }

    private String bearer(AuthMethod metodo) {
        return "Bearer " + jwtTokenService.generateAccessToken(idCliente, 1L, "alejandro.hernandez@banco-demo.com", metodo);
    }

    private MockHttpServletRequestBuilder pagar(String authorization, String json) {
        MockHttpServletRequestBuilder req = post("/v1/pagos").contentType(MediaType.APPLICATION_JSON).content(json);
        return authorization == null ? req : req.header("Authorization", authorization);
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
    @DisplayName("Sesión del servidor inactiva → 401 aunque el JWT sea válido")
    void sesionInactiva() throws Exception {
        sessionManager.setLoggedIn(false, null, null);
        mockMvc.perform(pagar(bearer(AuthMethod.BIOMETRIC_HUELLA), RECARGA_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login con contraseña, facial o refresh no puede pagar → 403 STRONG_AUTH_REQUIRED")
    void soloHuella() throws Exception {
        for (AuthMethod metodo : new AuthMethod[]{AuthMethod.PASSWORD, AuthMethod.BIOMETRIC_FACIAL, AuthMethod.REFRESH}) {
            mockMvc.perform(pagar(bearer(metodo), RECARGA_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code", is("STRONG_AUTH_REQUIRED")));
        }
        verify(txClient, never()).sendTx(anyString(), anyString());
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

        // token válido de HUELLA pero de otro cliente (la sesión también es de ese otro cliente)
        Long otro = idCliente + 999;
        sessionManager.setLoggedIn(true, "otro@banco.com", otro);
        String tokenOtro = "Bearer " + jwtTokenService.generateAccessToken(otro, 2L, "otro@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        String mensajeAjena = mockMvc.perform(pagar(tokenOtro, RECARGA_JSON))
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
}
