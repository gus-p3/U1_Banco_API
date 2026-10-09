package com.intrumentoev.demo.web;

import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
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
class AllEndpointsPathAndParamValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @BeforeEach
    void setUp() {
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
    }

    @Nested
    @DisplayName("Validaciones de Parámetros de Ruta (@PathVariable)")
    class PathVariableValidations {

        @Test
        @DisplayName("Rechaza texto en lugar de ID numérico con INVALID_TYPE")
        void testNonNumericIdReturnsInvalidType() throws Exception {
            mockMvc.perform(get("/v1/clientes/no-es-un-numero")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("id")))
                    .andExpect(jsonPath("$.error.message", containsString("número entero")));
        }

        @Test
        @DisplayName("Rechaza ID menor o igual a cero con mensaje descriptivo")
        void testNegativeOrZeroIdReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes/-5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[0].message", containsString("mayor a 0")));
        }

        @Test
        @DisplayName("Rechaza CURP con formato inválido en ruta")
        void testInvalidCurpInPathReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes/curp/CURPINVALIDA123")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[0].message", containsString("CURP")));
        }

        @Test
        @DisplayName("Rechaza RFC con formato inválido en ruta")
        void testInvalidRfcInPathReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes/rfc/RFC_MALO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[0].message", containsString("RFC")));
        }

        @Test
        @DisplayName("Rechaza número de cuenta que no tenga 10 dígitos")
        void testInvalidAccountNumberInPathReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/cuentas/12345")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[0].message", containsString("10 dígitos numéricos")));
        }

        @Test
        @DisplayName("Rechaza clave de entidad federativa que no tenga 2 dígitos")
        void testInvalidCveEntInPathReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/catalogos/municipios/1234")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[0].message", containsString("2 dígitos")));
        }
    }

    @Nested
    @DisplayName("Validaciones de Parámetros de Consulta (@RequestParam)")
    class QueryParamValidations {

        @Test
        @DisplayName("Rechaza rango de fechas si solo se proporciona 'desde'")
        void testOnlyDesdeProvidedReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes?desde=2026-01-01T00:00:00Z")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.target", is("rangoFechas")))
                    .andExpect(jsonPath("$.error.message", containsString("ambos parámetros")));
        }

        @Test
        @DisplayName("Rechaza rango de fechas si 'desde' es posterior a 'hasta'")
        void testDesdeAfterHastaReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes?desde=2026-12-31T23:59:59Z&hasta=2026-01-01T00:00:00Z")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.target", is("rangoFechas")))
                    .andExpect(jsonPath("$.error.message", containsString("no puede ser posterior")));
        }

        @Test
        @DisplayName("Rechaza formato de email inválido en query param")
        void testInvalidEmailQueryParamReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/v1/clientes?email=no-es-correo")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[0].message", containsString("correo electrónico")));
        }

        @Test
        @DisplayName("Rechaza logout con email malformado")
        void testLogoutWithMalformedEmailReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/v1/auth/logout?email=invalido")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[0].message", containsString("correo electrónico")));
        }
    }

    @Nested
    @DisplayName("Validaciones de Cuerpo de Petición (@RequestBody)")
    class RequestBodyValidations {

        @Test
        @DisplayName("Rechaza campos desconocidos / no permitidos con INVALID_FIELD")
        void testUnknownFieldInBodyReturnsInvalidField() throws Exception {
            String bodyWithUnknownField = """
                {
                  "name": "Juan",
                  "lastName": "Pérez",
                  "campoInexistente": "hacker"
                }
                """;

            mockMvc.perform(patch("/v1/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithUnknownField))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_FIELD")))
                    .andExpect(jsonPath("$.error.target", is("campoInexistente")))
                    .andExpect(jsonPath("$.error.message", containsString("no está permitido")));
        }

        @Test
        @DisplayName("Rechaza cuerpo de PATCH completamente vacío")
        void testEmptyPatchBodyReturnsBadRequest() throws Exception {
            mockMvc.perform(patch("/v1/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.target", is("requestBody")))
                    .andExpect(jsonPath("$.error.message", containsString("al menos un campo válido")));
        }
    }
}
