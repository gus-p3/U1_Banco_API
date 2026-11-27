package com.intrumentoev.demo.service;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.service.impl.auth.BrevoEmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BrevoEmailServiceTest {

    @Test
    void omiteEnvioCuandoApiKeyEstaVacia() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                restTemplate, "", "no-reply@banco.com", "Banco Digital", "https://api.brevo.com/v3/smtp/email");

        service.enviarCodigoVerificacion("user@banco.com", "Brandon", "123456", "EMAIL_VERIFICATION", 10);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enviaCorreoTransaccionalConApiKeyBrevo() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"messageId\":\"msg-1\"}"));

        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                restTemplate, "xkeysib-test", "no-reply@banco.com", "Banco Digital", "https://api.brevo.com/v3/smtp/email");

        service.enviarCodigoVerificacion("user@banco.com", "<Brandon>", "654321", "LOGIN_2FA", 5);

        verify(restTemplate).postForEntity(
                eq("https://api.brevo.com/v3/smtp/email"),
                argThat((HttpEntity<Map<String, Object>> entity) -> {
                    String apiKey = entity.getHeaders().getFirst("api-key");
                    Map<String, Object> body = entity.getBody();
                    return "xkeysib-test".equals(apiKey)
                            && body != null
                            && String.valueOf(body.get("htmlContent")).contains("654321")
                            && String.valueOf(body.get("htmlContent")).contains("&lt;Brandon&gt;");
                }),
                eq(String.class)
        );
    }

    @Test
    void lanzaEmailDeliveryFailedCuandoBrevoFalla() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("timeout"));

        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                restTemplate, "xkeysib-test", "", "", "");

        assertThatThrownBy(() ->
                service.enviarCodigoVerificacion("user@banco.com", null, "111222", "EMAIL_VERIFICATION", 10))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.EMAIL_DELIVERY_FAILED);
        assertThat(service).isNotNull();
    }
}
