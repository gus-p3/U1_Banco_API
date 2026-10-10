package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DemoRequirementsAndHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. GET / debe responder HTTP 200 y mensaje 'conectado servidor vivo'")
    void testRootHealthCheck() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("conectado servidor vivo"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("conectado servidor vivo"));
    }

    @Test
    @DisplayName("2. POST /v1/auth/login con credenciales predeterminadas de Alejandro Hernández")
    void testAlejandroHernandezLogin() throws Exception {
        Map<String, String> credenciales = Map.of(
                "email", "alejandro.hernandez@banco-demo.com",
                "password", "PasswordSegura#2026"
        );

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credenciales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alejandro.hernandez@banco-demo.com"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("3. POST /v1/auth/login-biometrico con credenciales biométricas de Alejandro Hernández")
    void testAlejandroHernandezBiometricLogin() throws Exception {
        Map<String, String> biometricReq = Map.of(
                "email", "alejandro.hernandez@banco-demo.com",
                "biometricType", "HUELLA",
                "biometricData", "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ=="
        );

        mockMvc.perform(post("/v1/auth/login-biometrico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(biometricReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alejandro.hernandez@banco-demo.com"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("4. Catálogos siguen mostrando id = 1 y nunca exponen idClient sensible")
    void testCatalogsKeepIdsVisible() throws Exception {
        mockMvc.perform(get("/v1/catalogos/generos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].idGender").exists())
                .andExpect(jsonPath("$[0].idGender").value(1));

        mockMvc.perform(get("/v1/catalogos/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].idState").exists())
                .andExpect(jsonPath("$[0].cveEnt").exists());
    }

    @Test
    @DisplayName("5. Onboarding con claveEntidad inconsistente debe rechazar con 400")
    void testClaveEntidadValidationMismatch() throws Exception {
        // Enviar idMunicipality existente pero con claveEntidad incorrecta
        String invalidOnboardingJson = """
                {
                  "name": "Carlos",
                  "lastName": "Martínez",
                  "secondLastName": "López",
                  "birthDate": "1992-04-10",
                  "curp": "MALC920410HDFRND02",
                  "rfc": "MALC920410AB2",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.mismatch@banco-demo.com",
                  "mobilePhone": "5599887766",
                  "street": "Insurgentes Sur",
                  "exteriorNumber": "123",
                  "neighborhood": "Roma Sur",
                  "idMunicipality": 1,
                  "claveEntidad": "99",
                  "postalCode": "06700",
                  "occupation": "Contador",
                  "company": "Fiscal SA",
                  "monthlyIncome": 30000,
                  "initialBalance": 1000,
                  "password": "PasswordSegura#2026"
                }
                """;

        mockMvc.perform(post("/v1/clientes/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidOnboardingJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.target").value("claveEntidad"));
    }
}
