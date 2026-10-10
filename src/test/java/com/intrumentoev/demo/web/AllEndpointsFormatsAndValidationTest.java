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
class AllEndpointsFormatsAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @BeforeEach
    void setUp() {
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
    }

    private String buildValidOnboardingJsonWithOverride(String key, String value) {
        return """
            {
              "name": "Fernando",
              "lastName": "Morales",
              "birthDate": "1991-05-15",
              "curp": "MOVF910515HDFRRN01",
              "rfc": "MOVF910515XY3",
              "idGender": 1,
              "idNationality": 1,
              "idMaritalStatus": 1,
              "email": "fernando.fmt@email.com",
              "mobilePhone": "5512345678",
              "street": "Av Insurgentes",
              "exteriorNumber": "100",
              "neighborhood": "Roma",
              "idMunicipality": 1,
              "postalCode": "06700",
              "occupation": "Ingeniero",
              "company": "Tech Corp",
              "monthlyIncome": 35000.00,
              "password": "P@ssw0rd2024!",
              "%s": %s
            }
            """.formatted(key, value);
    }

    @Nested
    @DisplayName("1. Validaciones de Formato en CURP")
    class CurpFormatTests {

        @Test
        @DisplayName("Rechaza CURP con 17 caracteres (muy corta)")
        void testCurpTooShort() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("curp", "\"MOVF910515HDFRRN0\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("curp")));
        }

        @Test
        @DisplayName("Rechaza CURP con 19 caracteres (muy larga)")
        void testCurpTooLong() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("curp", "\"MOVF910515HDFRRN01X\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("curp")));
        }

        @Test
        @DisplayName("Rechaza CURP con caracteres especiales")
        void testCurpSpecialChars() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("curp", "\"MOVF910515HDFRRN#1\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("curp")));
        }
    }

    @Nested
    @DisplayName("2. Validaciones de Formato en RFC")
    class RfcFormatTests {

        @Test
        @DisplayName("Rechaza RFC con menos de 12 caracteres")
        void testRfcTooShort() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("rfc", "\"MOVF910515\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("rfc")));
        }

        @Test
        @DisplayName("Rechaza RFC con más de 13 caracteres")
        void testRfcTooLong() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("rfc", "\"MOVF910515XY300\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("rfc")));
        }
    }

    @Nested
    @DisplayName("3. Validaciones de Formato en Correo Electrónico")
    class EmailFormatTests {

        @Test
        @DisplayName("Rechaza email sin dominio ni arroba")
        void testEmailInvalid() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("email", "\"not-a-valid-email\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("email")));
        }

        @Test
        @DisplayName("Rechaza email sin TLD (ej: user@dominio)")
        void testEmailMissingTld() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("email", "\"user@domain\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("email")));
        }
    }

    @Nested
    @DisplayName("4. Validaciones de Formato en Teléfonos")
    class PhoneFormatTests {

        @Test
        @DisplayName("Rechaza teléfono móvil con 9 dígitos")
        void testPhone9Digits() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("mobilePhone", "\"551234567\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("mobilePhone")));
        }

        @Test
        @DisplayName("Rechaza teléfono móvil con letras")
        void testPhoneWithLetters() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("mobilePhone", "\"551234567a\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("mobilePhone")));
        }

        @Test
        @DisplayName("PUT /v1/contact-details/{id} rechaza teléfono alternativo con letras")
        void testContactDetailPutPhoneInvalid() throws Exception {
            String json = """
                {
                  "idClient": 1,
                  "email": "valid@email.com",
                  "mobilePhone": "5511223344",
                  "alternativePhone": "55112233aa"
                }
                """;
            mockMvc.perform(put("/v1/contact-details/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("alternativePhone")));
        }
    }

    @Nested
    @DisplayName("5. Validaciones de Código Postal")
    class PostalCodeFormatTests {

        @Test
        @DisplayName("Rechaza código postal con 4 dígitos")
        void testPostalCode4Digits() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("postalCode", "\"0660\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("postalCode")));
        }

        @Test
        @DisplayName("Rechaza código postal con letras")
        void testPostalCodeLetters() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("postalCode", "\"0660A\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("postalCode")));
        }
    }

    @Nested
    @DisplayName("6. Validaciones de Fecha de Nacimiento")
    class BirthDateTests {

        @Test
        @DisplayName("Rechaza fecha de nacimiento en el futuro (2099-01-01)")
        void testFutureBirthDate() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("birthDate", "\"2099-01-01\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("birthDate")));
        }
    }

    @Nested
    @DisplayName("7. Validaciones de Contraseñas y Biometría")
    class PasswordAndBiometricTests {

        @Test
        @DisplayName("Rechaza contraseña sin caracteres especiales")
        void testPasswordMissingSpecialChar() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("password", "\"Password12345\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("password")));
        }

        @Test
        @DisplayName("Rechaza contraseña muy corta (< 12 caracteres)")
        void testPasswordTooShort() throws Exception {
            String json = buildValidOnboardingJsonWithOverride("password", "\"P@ss1\"");
            mockMvc.perform(post("/v1/clientes/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("password")));
        }

        @Test
        @DisplayName("Rechaza tipo biométrico desconocido (VOZ)")
        void testBiometricTypeInvalid() throws Exception {
            String json = """
                {
                  "idClient": 1,
                  "biometricType": "VOZ",
                  "biometricData": "dGVzdA=="
                }
                """;
            mockMvc.perform(post("/v1/auth/biometria")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("biometricType")));
        }
    }

    @Nested
    @DisplayName("8. Validaciones de Autenticación y Refresh Token")
    class AuthEndpointValidationTests {

        @Test
        @DisplayName("POST /v1/auth/login rechaza correo en formato incorrecto")
        void testLoginInvalidEmail() throws Exception {
            String json = "{\"email\": \"correo-invalido\", \"password\": \"P@ssw0rd2024!\"}";
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("email")));
        }

        @Test
        @DisplayName("POST /v1/auth/login rechaza contraseña vacía")
        void testLoginEmptyPassword() throws Exception {
            String json = "{\"email\": \"usuario@email.com\", \"password\": \"\"}";
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("password")));
        }

        @Test
        @DisplayName("POST /v1/auth/refresh rechaza refresh token en blanco")
        void testRefreshTokenBlank() throws Exception {
            String json = "{\"refreshToken\": \"   \"}";
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("refreshToken")));
        }
    }
}
