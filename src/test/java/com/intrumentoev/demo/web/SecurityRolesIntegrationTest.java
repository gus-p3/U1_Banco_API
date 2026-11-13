package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.config.DemoUserInitializer;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.Role;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.service.impl.gestopago.CatalogoRefresher;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticación JWT stateless con roles: quién puede consultar a todos los clientes (ADMIN)
 * y quién solo a sí mismo (CLIENT).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRolesIntegrationTest {

    private static final String EMAIL = DemoUserInitializer.DEMO_EMAIL;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private AuthRepository authRepository;

    @MockitoBean private ProductoServiceRedis productoServiceRedis;
    @MockitoBean private GestoPagoTokenService tokenService;
    @MockitoBean private CatalogoRefresher catalogoRefresher;

    private Long idClienteDemo() {
        return authRepository.findByEmail(EMAIL).orElseThrow().getIdClient();
    }

    private String token(Role role) {
        return "Bearer " + jwtTokenService.generateAccessToken(idClienteDemo(), 1L, EMAIL, AuthMethod.PASSWORD, role);
    }

    private JsonNode login() throws Exception {
        String body = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", DemoUserInitializer.DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private MockHttpServletRequestBuilder buscar(String authorization) {
        MockHttpServletRequestBuilder req = post("/v1/clientes/buscar")
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        return authorization == null ? req : req.header("Authorization", authorization);
    }

    @Test
    @DisplayName("El login devuelve el rol y el JWT lo lleva como claim")
    void loginIncluyeRol() throws Exception {
        JsonNode json = login();

        // El usuario demo se promueve a ADMIN con security.bootstrap-admin-email
        org.assertj.core.api.Assertions.assertThat(json.get("role").asText()).isEqualTo("ADMIN");
        var claims = jwtTokenService.extractClaims(json.get("token").asText());
        org.assertj.core.api.Assertions.assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        org.assertj.core.api.Assertions.assertThat(claims.get("authMethod", String.class)).isEqualTo("PASSWORD");
    }

    @Test
    @DisplayName("Credenciales incorrectas → 401 y sin token")
    void loginInvalido() throws Exception {
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", "Incorrecta#123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("GET /v1/clientes/me devuelve solo al cliente del JWT; sin token → 401")
    void me() throws Exception {
        mockMvc.perform(get("/v1/clientes/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("TOKEN_MISSING")));

        mockMvc.perform(get("/v1/clientes/me").header("Authorization", token(Role.CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curp", is(DemoUserInitializer.DEMO_CURP)));
    }

    @Test
    @DisplayName("Ya no existe un GET que liste a todos los clientes")
    void noHayGetDeTodos() throws Exception {
        mockMvc.perform(get("/v1/clientes").header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Búsqueda global: CLIENT → 403, ADMIN → 200, sin token → 401, token basura → 401")
    void busquedaGlobalSoloAdmin() throws Exception {
        mockMvc.perform(buscar(token(Role.CLIENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
        mockMvc.perform(buscar(token(Role.ADMIN))).andExpect(status().isOk());
        mockMvc.perform(buscar(null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("TOKEN_MISSING")));
        mockMvc.perform(buscar("Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("TOKEN_EXPIRED_OR_INVALID")));
    }

    @Test
    @DisplayName("Un ADMIN autenticado por login real puede usar la búsqueda global")
    void adminPorLogin() throws Exception {
        String bearer = "Bearer " + login().get("token").asText();
        mockMvc.perform(buscar(bearer)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Actualizar y dar de baja clientes (PATCH/DELETE) solo para ADMIN")
    void modificarSoloAdmin() throws Exception {
        mockMvc.perform(patch("/v1/clientes").header("Authorization", token(Role.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/v1/clientes").header("Authorization", token(Role.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Productos: lectura autenticada; escritura, personas y sincronizar catálogos solo ADMIN")
    void rutasAdministrativas() throws Exception {
        mockMvc.perform(get("/v1/productos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/productos").header("Authorization", token(Role.CLIENT))).andExpect(status().isOk());
        mockMvc.perform(delete("/v1/productos/1").header("Authorization", token(Role.CLIENT))).andExpect(status().isForbidden());
        mockMvc.perform(post("/personas").header("Authorization", token(Role.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/catalogos/sincronizar").header("Authorization", token(Role.CLIENT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/catalogos/sincronizar")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rutas públicas: salud, catálogos, documentación y OPTIONS")
    void rutasPublicas() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/catalogos/generos")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(options("/v1/clientes/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un CLIENT solo enrola su propia biometría; ADMIN puede enrolar la de cualquiera")
    void biometriaPropia() throws Exception {
        String propia = objectMapper.writeValueAsString(Map.of("idClient", idClienteDemo(),
                "biometricType", "HUELLA", "biometricData", DemoUserInitializer.DEMO_BIOMETRIC_DATA));
        String ajena = objectMapper.writeValueAsString(Map.of("idClient", idClienteDemo() + 500,
                "biometricType", "HUELLA", "biometricData", DemoUserInitializer.DEMO_BIOMETRIC_DATA));

        mockMvc.perform(post("/v1/auth/biometria").header("Authorization", token(Role.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON).content(propia))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/auth/biometria").header("Authorization", token(Role.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON).content(ajena))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/auth/biometria").header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(ajena))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/auth/biometria").contentType(MediaType.APPLICATION_JSON).content(propia))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh renueva el access token conservando el rol y rota el refresh token")
    void refresh() throws Exception {
        JsonNode login = login();
        String refreshToken = login.get("refreshToken").asText();

        String body = mockMvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode renovado = objectMapper.readTree(body);

        var claims = jwtTokenService.extractClaims(renovado.get("token").asText());
        org.assertj.core.api.Assertions.assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        org.assertj.core.api.Assertions.assertThat(claims.get("authMethod", String.class)).isEqualTo("REFRESH");
        org.assertj.core.api.Assertions.assertThat(renovado.get("refreshToken").asText()).isNotEqualTo(refreshToken);

        // el refresh token anterior ya no sirve
        mockMvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout exige JWT y revoca el refresh token del usuario autenticado")
    void logout() throws Exception {
        JsonNode login = login();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/v1/auth/logout")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ya no existe el endpoint de estado de sesión del servidor")
    void sinSessionStatus() throws Exception {
        mockMvc.perform(get("/v1/auth/session-status")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/auth/session-status").header("Authorization", token(Role.ADMIN)))
                .andExpect(status().is4xxClientError());
    }
}
