package com.intrumentoev.demo.web;

import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AllEndpointsSecurityAndSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @Nested
    @DisplayName("1. Rutas Públicas - Permitidas sin iniciar sesión (login = false)")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /v1/auth/session-status es público")
        void testSessionStatusPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/auth/session-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isLoggedIn", is(false)));
        }

        @Test
        @DisplayName("GET /v1/catalogos/generos es público")
        void testCatalogosGenerosPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/catalogos/generos"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /v1/catalogos/nacionalidades es público")
        void testCatalogosNacionalidadesPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/catalogos/nacionalidades"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /v1/catalogos/estados-civiles es público")
        void testCatalogosEstadosCivilesPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/catalogos/estados-civiles"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/clientes/onboarding es público (no devuelve 401 por sesión)")
        void testOnboardingPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            // Se envía un cuerpo vacío para comprobar que llega al controlador/validador (400) y no es bloqueado con 401
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /v1/cuentas es público (no devuelve 401 por sesión)")
        void testCuentasPublic() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(post("/v1/cuentas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("2. Rutas Protegidas - Bloqueadas con 401 cuando login = false")
    class ProtectedEndpointsWithoutLoginTests {

        @Test
        @DisplayName("GET /v1/clientes devuelve 401 si no hay sesión")
        void testGetClientesBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/clientes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", containsString("Acceso No Autorizado")));
        }

        @Test
        @DisplayName("GET /v1/clientes/{identificador} devuelve 401 si no hay sesión")
        void testGetClienteByIdBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/clientes/TEST850101HDFRRN01"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /v1/clientes/buscar devuelve 401 si no hay sesión")
        void testBuscarClientesBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(post("/v1/clientes/buscar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"activo\":true}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /v1/cuentas/activas devuelve 401 si no hay sesión")
        void testGetCuentasActivasBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/cuentas/activas"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /v1/contact-details/cliente/{identificador} devuelve 401 si no hay sesión")
        void testGetContactDetailBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/contact-details/cliente/TEST850101HDFRRN01"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /v1/laboral/cliente/{identificador} devuelve 401 si no hay sesión")
        void testGetLaboralBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/laboral/cliente/TEST850101HDFRRN01"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /v1/domicilios/cliente/{identificador} devuelve 401 si no hay sesión")
        void testGetDomiciliosBlocked() throws Exception {
            serverSessionManager.setLoggedIn(false, null, null);

            mockMvc.perform(get("/v1/domicilios/cliente/TEST850101HDFRRN01"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("3. Rutas Protegidas - Acceso Permitido cuando login = true")
    class ProtectedEndpointsWithLoginTests {

        @Test
        @DisplayName("GET /v1/clientes devuelve 200 cuando hay sesión activa")
        void testGetClientesAllowed() throws Exception {
            serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);

            mockMvc.perform(get("/v1/clientes"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /v1/cuentas/activas devuelve 200 cuando hay sesión activa")
        void testGetCuentasActivasAllowed() throws Exception {
            serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);

            mockMvc.perform(get("/v1/cuentas/activas"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("4. Control de Autorización - Prevención de manipulación de ID (403 Forbidden)")
    class AuthorizationIdTamperingTests {

        @Test
        @DisplayName("Cliente autenticado no puede consultar cuentas de otro cliente modificando el ID en la URL (403 Forbidden)")
        void testCannotAccessOtherClientAccountsById() throws Exception {
            // Usuario regular logueado con ID 10
            serverSessionManager.setLoggedIn(true, "cliente10@banco.com", 10L);

            // Intenta consultar cuentas del cliente 20
            mockMvc.perform(get("/v1/cuentas/cliente/20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.error.message", containsString("No tiene autorización")));
        }

        @Test
        @DisplayName("Administrador sí tiene autorización para consultar cualquier registro")
        void testAdminCanAccessAnyClientAccounts() throws Exception {
            // Sesión admin
            serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);

            // Al consultar un cliente inexistente, debe dar 404 (no 403 por autorización)
            mockMvc.perform(get("/v1/cuentas/cliente/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code", is("CLIENT_NOT_FOUND")));
        }
    }
}
