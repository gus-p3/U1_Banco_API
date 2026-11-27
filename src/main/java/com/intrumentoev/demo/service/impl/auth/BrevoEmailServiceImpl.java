package com.intrumentoev.demo.service.impl.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.service.service.auth.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de correos transaccionales vía Brevo (API v3 /smtp/email).
 */
@Service
@Slf4j
public class BrevoEmailServiceImpl implements EmailService {

    private static final String DEFAULT_BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String apiUrl;

    @Autowired
    public BrevoEmailServiceImpl(
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender-email:no-reply@bancodigital.com}") String senderEmail,
            @Value("${brevo.sender-name:Banco Digital}") String senderName,
            @Value("${brevo.api-url:" + DEFAULT_BREVO_URL + "}") String apiUrl) {
        this(new RestTemplate(), apiKey, senderEmail, senderName, apiUrl);
    }

    public BrevoEmailServiceImpl(
            RestTemplate restTemplate,
            String apiKey,
            String senderEmail,
            String senderName,
            String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail != null && !senderEmail.isBlank() ? senderEmail.trim() : "no-reply@bancodigital.com";
        this.senderName = senderName != null && !senderName.isBlank() ? senderName.trim() : "Banco Digital";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : DEFAULT_BREVO_URL;
    }

    @Override
    public void enviarCodigoVerificacion(String destinatarioEmail, String nombre, String codigo, String proposito, long minutosVigencia) {
        if (apiKey.isBlank()) {
            log.warn("BREVO_API_KEY no configurada; omitiendo envío real de correo de {} a {}", proposito, destinatarioEmail);
            return;
        }

        boolean esCuentaNueva = "EMAIL_VERIFICATION".equalsIgnoreCase(proposito);
        String asunto = esCuentaNueva
                ? "Verifica tu cuenta de Banco Digital"
                : "Código de verificación para iniciar sesión — Banco Digital";
        String titulo = esCuentaNueva
                ? "Verifica tu nueva cuenta"
                : "Confirma tu inicio de sesión";
        String descripcion = esCuentaNueva
                ? "Para verificar tu cuenta creada, introduce el siguiente código de 6 dígitos en la aplicación:"
                : "Detectamos un inicio de sesión en tu cuenta. Introduce el siguiente código de 6 dígitos para continuar:";
        String nombreSeguro = escaparHtml(nombre != null && !nombre.isBlank() ? nombre : "Cliente");

        String html = "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:24px;border:1px solid #E2E8F0;border-radius:12px;background:#FFFFFF;\">"
                + "<div style=\"color:#2563EB;font-weight:800;font-size:20px;margin-bottom:12px;\">Banco Digital</div>"
                + "<h2 style=\"color:#0F172A;margin:0 0 8px 0;\">" + titulo + "</h2>"
                + "<p style=\"color:#475569;font-size:15px;line-height:1.5;\">Hola <strong>" + nombreSeguro + "</strong>,</p>"
                + "<p style=\"color:#475569;font-size:15px;line-height:1.5;\">" + descripcion + "</p>"
                + "<div style=\"margin:24px 0;padding:18px;text-align:center;background:#EFF6FF;border:1px solid #BFDBFE;border-radius:10px;\">"
                + "<span style=\"font-size:32px;font-weight:800;letter-spacing:8px;color:#1D4ED8;\">" + escaparHtml(codigo) + "</span>"
                + "</div>"
                + "<p style=\"color:#64748B;font-size:13px;line-height:1.4;\">Este código vence en <strong>" + minutosVigencia + " minutos</strong>. Si no solicitaste esta operación, ignora este mensaje.</p>"
                + "</div>";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of("name", senderName, "email", senderEmail));
        payload.put("to", List.of(Map.of("email", destinatarioEmail, "name", nombre != null && !nombre.isBlank() ? nombre : destinatarioEmail)));
        payload.put("subject", asunto);
        payload.put("htmlContent", html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-key", apiKey);

        try {
            restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), String.class);
            log.info("Correo de {} enviado por Brevo a {}", proposito, destinatarioEmail);
        } catch (Exception e) {
            log.error("Error al enviar correo vía Brevo a {}: {}", destinatarioEmail, e.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    private String escaparHtml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
