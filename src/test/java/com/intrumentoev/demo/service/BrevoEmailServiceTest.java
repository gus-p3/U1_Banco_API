package com.intrumentoev.demo.service;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.gestopago.pago.PaymentTicketEmailRequest;
import com.intrumentoev.demo.service.impl.auth.BrevoEmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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

    @Test
    @SuppressWarnings("unchecked")
    void enviaTicketRechazadoSinAutorizacionNegativaYConLeyendaReal() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"messageId\":\"msg-2\"}"));

        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                restTemplate, "xkeysib-test", "no-reply@banco.com", "Banco Digital", "https://api.brevo.com/v3/smtp/email");

        service.enviarTicketPago(PaymentTicketEmailRequest.builder()
                .destinatarioEmail("user@banco.com")
                .nombreCliente("Brandon")
                .estado("RECHAZADO")
                .tipoOperacion("Recarga móvil")
                .nombreServicio("ABIB")
                .nombreProducto("ABIB 100")
                .destino("4281076133")
                .cuentaEnmascarada("****3410")
                .monto(new BigDecimal("100.00"))
                .numAutorizacion("-1")
                .upc("BXMWZOXNZ2EVIYM")
                .fechaHora("27/11/2026 18:47:00 CST")
                .leyendaProducto("Recibe soporte las 24h marcando al *787.")
                .build());

        verify(restTemplate).postForEntity(
                eq("https://api.brevo.com/v3/smtp/email"),
                argThat((HttpEntity<Map<String, Object>> entity) -> {
                    Map<String, Object> body = entity.getBody();
                    if (body == null) return false;
                    String subject = String.valueOf(body.get("subject"));
                    String html = String.valueOf(body.get("htmlContent"));
                    return subject.contains("Pago rechazado")
                            && html.contains("RECHAZADO")
                            && html.contains("Importe intentado")
                            && html.contains("Soporte y aclaraciones")
                            && html.contains("marcando al *787")
                            && !html.contains("Folio de autorización");
                }),
                eq(String.class)
        );
    }
}
