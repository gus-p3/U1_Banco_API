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
class AllEndpointsNumericCoercionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @BeforeEach
    void setUp() {
        // Habilitar sesión para poder probar endpoints protegidos además de los públicos
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
    }

    @Nested
    @DisplayName("1. POST /v1/clientes/onboarding - Protección Numérica")
    class OnboardingNumericTests {

        @Test
        @DisplayName("Rechaza monthlyIncome cuando se envía como texto (string)")
        void testMonthlyIncomeAsString() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num1@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": 1,
                  "postalCode": "06600",
                  "country": "Mexico",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": "30000.00",
                  "initialBalance": 500.00,
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("monthlyIncome")))
                    .andExpect(jsonPath("$.error.message", containsString("monthlyIncome")));
        }

        @Test
        @DisplayName("Rechaza initialBalance cuando se envía como texto (string)")
        void testInitialBalanceAsString() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num2@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": 1,
                  "postalCode": "06600",
                  "country": "Mexico",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": 30000.00,
                  "initialBalance": "500.00",
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("initialBalance")));
        }

        @Test
        @DisplayName("Rechaza idGender cuando se envía como texto (string)")
        void testIdGenderAsString() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": "1",
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num3@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": 1,
                  "postalCode": "06600",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": 30000.00,
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idGender")));
        }

        @Test
        @DisplayName("Rechaza idMunicipality cuando se envía como texto (string)")
        void testIdMunicipalityAsString() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num4@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": "1",
                  "postalCode": "06600",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": 30000.00,
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idMunicipality")));
        }

        @Test
        @DisplayName("Rechaza monthlyIncome con valor alfabético 'abc'")
        void testMonthlyIncomeWithLetters() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num5@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": 1,
                  "postalCode": "06600",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": "mil pesos",
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("monthlyIncome")));
        }

        @Test
        @DisplayName("Rechaza monthlyIncome negativo (-500.00) por regla de validación")
        void testMonthlyIncomeNegative() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "birthDate": "1992-03-01",
                  "curp": "GALC920301HDFBRL02",
                  "rfc": "GALC920301XY3",
                  "idGender": 1,
                  "idNationality": 1,
                  "idMaritalStatus": 1,
                  "email": "carlos.num6@email.com",
                  "mobilePhone": "5511223344",
                  "street": "Calle Reforma",
                  "exteriorNumber": "500",
                  "neighborhood": "Centro",
                  "idMunicipality": 1,
                  "postalCode": "06600",
                  "occupation": "Analista",
                  "company": "Empresa SA",
                  "monthlyIncome": -500.00,
                  "password": "P@ssw0rd2024!"
                }
                """;

            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")));
        }
    }

    @Nested
    @DisplayName("2. POST /v1/cuentas - Protección Numérica")
    class AccountNumericTests {

        @Test
        @DisplayName("Rechaza idClient como String en apertura de cuenta")
        void testIdClientAsString() throws Exception {
            String json = "{\"idClient\": \"1\", \"initialBalance\": 1000.00}";

            mockMvc.perform(post("/v1/cuentas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idClient")));
        }

        @Test
        @DisplayName("Rechaza initialBalance como String en apertura de cuenta")
        void testInitialBalanceAsString() throws Exception {
            String json = "{\"idClient\": 1, \"initialBalance\": \"500.00\"}";

            mockMvc.perform(post("/v1/cuentas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("initialBalance")));
        }

        @Test
        @DisplayName("Rechaza initialBalance negativo (-100.00) en apertura de cuenta")
        void testInitialBalanceNegative() throws Exception {
            String json = "{\"idClient\": 1, \"initialBalance\": -100.00}";

            mockMvc.perform(post("/v1/cuentas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")));
        }
    }

    @Nested
    @DisplayName("3. PUT / PATCH /v1/laboral - Protección Numérica")
    class EmploymentNumericTests {

        @Test
        @DisplayName("PUT /v1/laboral/{id} rechaza monthlyIncome como String")
        void testLaboralPutMonthlyIncomeAsString() throws Exception {
            String json = "{\"idClient\": 1, \"occupation\": \"Ingeniero\", \"company\": \"Google\", \"monthlyIncome\": \"40000.00\"}";

            mockMvc.perform(put("/v1/laboral/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("monthlyIncome")));
        }

        @Test
        @DisplayName("PUT /v1/laboral/{id} rechaza idClient como String")
        void testLaboralPutIdClientAsString() throws Exception {
            String json = "{\"idClient\": \"1\", \"occupation\": \"Ingeniero\", \"company\": \"Google\", \"monthlyIncome\": 40000.00}";

            mockMvc.perform(put("/v1/laboral/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idClient")));
        }

        @Test
        @DisplayName("PATCH /v1/laboral/{id} rechaza monthlyIncome como String")
        void testLaboralPatchMonthlyIncomeAsString() throws Exception {
            String json = "{\"monthlyIncome\": \"45000.00\"}";

            mockMvc.perform(patch("/v1/laboral/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("monthlyIncome")));
        }
    }

    @Nested
    @DisplayName("4. PUT / PATCH /v1/domicilios - Protección Numérica")
    class HomeNumericTests {

        @Test
        @DisplayName("PUT /v1/domicilios/{id} rechaza idMunicipality como String")
        void testHomePutIdMunicipalityAsString() throws Exception {
            String json = """
                {
                  "idClient": 1,
                  "street": "Reforma",
                  "exteriorNumber": "123",
                  "neighborhood": "Juarez",
                  "idMunicipality": "1",
                  "postalCode": "06600"
                }
                """;

            mockMvc.perform(put("/v1/domicilios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idMunicipality")));
        }

        @Test
        @DisplayName("PATCH /v1/domicilios/{id} rechaza idMunicipality como String")
        void testHomePatchIdMunicipalityAsString() throws Exception {
            String json = "{\"idMunicipality\": \"1\"}";

            mockMvc.perform(patch("/v1/domicilios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idMunicipality")));
        }
    }

    @Nested
    @DisplayName("5. PUT / PATCH /v1/clientes - Protección Numérica")
    class ClientNumericTests {

        @Test
        @DisplayName("PUT /v1/clientes/{id} rechaza idGender como String")
        void testClientPutIdGenderAsString() throws Exception {
            String json = """
                {
                  "name": "Carlos",
                  "lastName": "Garcia",
                  "secondLastName": "Lopez",
                  "birthDate": "1990-01-01",
                  "curp": "GALC900101HDFRRN01",
                  "rfc": "GALC900101XY3",
                  "idGender": "1",
                  "idNationality": 1,
                  "idMaritalStatus": 1
                }
                """;

            mockMvc.perform(put("/v1/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idGender")));
        }

        @Test
        @DisplayName("PATCH /v1/clientes/{id} rechaza idNationality como String")
        void testClientPatchIdNationalityAsString() throws Exception {
            String json = "{\"idNationality\": \"1\"}";

            mockMvc.perform(patch("/v1/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idNationality")));
        }
    }

    @Nested
    @DisplayName("6. POST /v1/auth/biometria - Protección Numérica")
    class AuthNumericTests {

        @Test
        @DisplayName("POST /v1/auth/biometria rechaza idClient como String")
        void testBiometriaIdClientAsString() throws Exception {
            String json = """
                {
                  "idClient": "1",
                  "biometricType": "HUELLA",
                  "biometricData": "dGVzdC1kYXRh"
                }
                """;

            mockMvc.perform(post("/v1/auth/biometria")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("idClient")));
        }
    }

    @Nested
    @DisplayName("7. PathVariables Numéricos - Caracteres inválidos en URLs")
    class PathVariableNumericTests {

        @Test
        @DisplayName("GET /v1/clientes/{id} con 'abc' devuelve 400")
        void testClientIdPathVariableInvalid() throws Exception {
            mockMvc.perform(get("/v1/clientes/abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /v1/cuentas/cliente/{idClient} con 'xyz' devuelve 400")
        void testCuentaClienteIdPathVariableInvalid() throws Exception {
            mockMvc.perform(get("/v1/cuentas/cliente/xyz"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /v1/domicilios/{id} con 'no-numero' devuelve 400")
        void testDomicilioIdPathVariableInvalid() throws Exception {
            mockMvc.perform(get("/v1/domicilios/no-numero"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /v1/laboral/{id} con 'abc' devuelve 400")
        void testLaboralIdPathVariableInvalid() throws Exception {
            mockMvc.perform(get("/v1/laboral/abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /v1/contact-details/{id} con 'abc' devuelve 400")
        void testContactDetailIdPathVariableInvalid() throws Exception {
            mockMvc.perform(get("/v1/contact-details/abc"))
                    .andExpect(status().isBadRequest());
        }
    }
}
