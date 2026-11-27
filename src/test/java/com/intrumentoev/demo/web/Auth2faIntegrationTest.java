package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.config.DemoUserInitializer;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.Role;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.service.impl.gestopago.CatalogoRefresher;
import com.intrumentoev.demo.service.service.auth.EmailService;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Auth2faIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private AuthRepository authRepository;

    @MockitoBean private ProductoService productoService;
    @MockitoBean private GestoPagoTokenService tokenService;
    @MockitoBean private CatalogoRefresher catalogoRefresher;
    @MockitoBean private EmailService emailService;

    @Test
    @DisplayName("Login Web con 2FA ofrece 2 opciones y permite verificar código enviado por correo (Brevo)")
    void loginWebConCodigoCorreo() throws Exception {
        String loginBody = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DemoUserInitializer.DEMO_EMAIL,
                                "password", DemoUserInitializer.DEMO_PASSWORD,
                                "channel", "WEB"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresVerification").value(true))
                .andExpect(jsonPath("$.verificationPurpose").value("LOGIN_2FA"))
                .andReturn().getResponse().getContentAsString();

        JsonNode challenge = objectMapper.readTree(loginBody);
        String challengeId = challenge.get("challengeId").asText();
        String pollToken = challenge.get("pollToken").asText();

        mockMvc.perform(post("/v1/auth/2fa/enviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challengeId,
                                "pollToken", pollToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeSent").value(true));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoVerificacion(
                eq(DemoUserInitializer.DEMO_EMAIL), anyString(), codeCaptor.capture(), eq("LOGIN_2FA"), anyLong());

        String verifyBody = mockMvc.perform(post("/v1/auth/verificar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challengeId,
                                "pollToken", pollToken,
                                "code", codeCaptor.getValue()
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode authJson = objectMapper.readTree(verifyBody);
        assertThat(authJson.get("token").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Login Web autorizado con huella desde móvil vía /v1/auth/2fa/{id}/aprobar")
    void loginWebConHuellaMovil() throws Exception {
        String loginBody = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", DemoUserInitializer.DEMO_EMAIL,
                                "password", DemoUserInitializer.DEMO_PASSWORD,
                                "channel", "WEB"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode challenge = objectMapper.readTree(loginBody);
        String challengeId = challenge.get("challengeId").asText();
        String pollToken = challenge.get("pollToken").asText();

        mockMvc.perform(post("/v1/auth/2fa/solicitar-huella")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challengeId,
                                "pollToken", pollToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricRequested").value(true));

        Long idClient = authRepository.findByEmail(DemoUserInitializer.DEMO_EMAIL).orElseThrow().getIdClient();
        String bearer = "Bearer " + jwtTokenService.generateAccessToken(idClient, 1L, DemoUserInitializer.DEMO_EMAIL, AuthMethod.BIOMETRIC_HUELLA, Role.CLIENT);
        String confirmToken = jwtTokenService.generateConfirmationToken(idClient, 1L, DemoUserInitializer.DEMO_EMAIL, AuthMethod.BIOMETRIC_HUELLA);

        mockMvc.perform(get("/v1/auth/2fa/pendientes").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].challengeId").value(challengeId));

        mockMvc.perform(post("/v1/auth/2fa/" + challengeId + "/aprobar")
                        .header("Authorization", bearer)
                        .header("X-Confirmation-Token", confirmToken))
                .andExpect(status().isOk());

        String statusBody = mockMvc.perform(post("/v1/auth/2fa/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challengeId,
                                "pollToken", pollToken
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode statusJson = objectMapper.readTree(statusBody);
        assertThat(statusJson.get("authResponse").get("token").asText()).isNotBlank();
    }
}
