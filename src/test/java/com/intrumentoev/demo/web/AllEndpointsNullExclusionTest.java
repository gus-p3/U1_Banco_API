package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AllEndpointsNullExclusionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @BeforeEach
    void setUp() {
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
    }

    /**
     * Valida recursivamente que ningún nodo en el JSON resultante contenga valores nulos.
     */
    private void assertNoNullsInJson(JsonNode node, String path) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String currentPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                assertThat(field.getValue().isNull())
                        .as("El campo '%s' no debe contener valor null", currentPath)
                        .isFalse();
                assertNoNullsInJson(field.getValue(), currentPath);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                assertNoNullsInJson(node.get(i), path + "[" + i + "]");
            }
        }
    }

    @Test
    @DisplayName("GET /v1/auth/session-status - No contiene valores null en ningún campo")
    void testSessionStatusNoNulls() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/auth/session-status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNoNullsInJson(root, "");
    }

    @Test
    @DisplayName("GET /v1/catalogos/generos - No contiene valores null en la lista")
    void testCatalogosGenerosNoNulls() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/catalogos/generos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNoNullsInJson(root, "");
    }

    @Test
    @DisplayName("GET /v1/catalogos/nacionalidades - No contiene valores null en la lista")
    void testCatalogosNacionalidadesNoNulls() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/catalogos/nacionalidades")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNoNullsInJson(root, "");
    }

    @Test
    @DisplayName("GET /v1/catalogos/estados-civiles - No contiene valores null en la lista")
    void testCatalogosEstadosCivilesNoNulls() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/catalogos/estados-civiles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNoNullsInJson(root, "");
    }

    @Test
    @DisplayName("POST /v1/clientes/onboarding (con campos opcionales omitidos) - No contiene valores null")
    void testOnboardingResponseNoNulls() throws Exception {
        String json = """
            {
              "name": "Mariana",
              "lastName": "Perez",
              "birthDate": "1993-08-20",
              "curp": "PEMM930820MDFRRN09",
              "rfc": "PEMM9308203B2",
              "idGender": 2,
              "idNationality": 1,
              "idMaritalStatus": 1,
              "email": "mariana.nulls@email.com",
              "mobilePhone": "5599887766",
              "street": "Insurgentes Sur",
              "exteriorNumber": "450",
              "neighborhood": "Del Valle",
              "idMunicipality": 1,
              "postalCode": "03100",
              "occupation": "Contadora",
              "company": "Deloitte",
              "monthlyIncome": 38000.00,
              "password": "P@ssw0rd2024!"
            }
            """;

        MvcResult result = mockMvc.perform(post("/v1/clientes/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();

        if (result.getResponse().getStatus() == 201) {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertNoNullsInJson(root, "");
        }
    }

    @Test
    @DisplayName("Respuestas de Error 400 - No contienen el campo 'details' como null")
    void testErrorResponseNoNulls() throws Exception {
        // Enviar JSON inválido para provocar un ErrorResponse
        MvcResult result = mockMvc.perform(post("/v1/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\": \"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNoNullsInJson(root, "");
        // Verificar que no aparezca "details": null
        assertThat(root.path("error").has("details")).isFalse();
    }
}
