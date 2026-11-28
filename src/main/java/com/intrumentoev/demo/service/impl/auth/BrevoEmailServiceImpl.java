package com.intrumentoev.demo.service.impl.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.gestopago.pago.PaymentTicketEmailRequest;
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

    @Override
    public void enviarTicketPago(PaymentTicketEmailRequest ticket) {
        if (ticket == null || ticket.getDestinatarioEmail() == null || ticket.getDestinatarioEmail().isBlank()) {
            return;
        }
        if (apiKey.isBlank()) {
            log.warn("BREVO_API_KEY no configurada; omitiendo envío real de ticket de pago UPC {} a {}",
                    ticket.getUpc(), ticket.getDestinatarioEmail());
            return;
        }

        String nombreSeguro = escaparHtml(ticket.getNombreCliente() != null && !ticket.getNombreCliente().isBlank()
                ? ticket.getNombreCliente() : "Cliente");
        String servicio = escaparHtml(ticket.getNombreServicio() != null ? ticket.getNombreServicio() : "Servicio");
        String producto = escaparHtml(ticket.getNombreProducto() != null ? ticket.getNombreProducto() : "");
        String tipo = escaparHtml(ticket.getTipoOperacion() != null ? ticket.getTipoOperacion() : "Pago");
        String destino = escaparHtml(ticket.getDestino() != null ? ticket.getDestino() : "N/D");
        String cuenta = escaparHtml(ticket.getCuentaEnmascarada() != null ? ticket.getCuentaEnmascarada() : "N/D");
        String montoTxt = ticket.getMonto() != null ? "$" + ticket.getMonto().setScale(2, java.math.RoundingMode.HALF_UP) + " MXN" : "$0.00 MXN";
        String estadoRaw = ticket.getEstado() != null ? ticket.getEstado().trim().toUpperCase() : "APLICADO";
        boolean esAplicado = "APLICADO".equals(estadoRaw);
        boolean esRechazado = "RECHAZADO".equals(estadoRaw);

        String badgeBg = esAplicado ? "#ECFDF5" : (esRechazado ? "#FEF2F2" : "#FFFBEB");
        String badgeFg = esAplicado ? "#059669" : (esRechazado ? "#DC2626" : "#D97706");
        String estadoLabel = esAplicado ? "APLICADO" : (esRechazado ? "RECHAZADO" : "POR CONFIRMAR");
        String titulo = esAplicado
                ? "Comprobante de " + tipo
                : (esRechazado ? "Aviso de " + tipo + " rechazado" : "Operación en proceso de confirmación");
        String intro = esAplicado
                ? "Hola <strong>" + nombreSeguro + "</strong>, tu operación fue aplicada exitosamente. Aquí tienes tu comprobante:"
                : (esRechazado
                        ? "Hola <strong>" + nombreSeguro + "</strong>, tu intento de operación no pudo procesarse y no se aplicó el cargo a tu cuenta. Aquí tienes el detalle:"
                        : "Hola <strong>" + nombreSeguro + "</strong>, tu operación está en proceso de confirmación con el proveedor. Aquí tienes el resumen:");
        String etiquetaMonto = esAplicado
                ? "Importe pagado"
                : (esRechazado ? "Importe intentado" : "Importe en proceso");
        String asunto = esAplicado
                ? "Comprobante de pago " + servicio + " (" + montoTxt + ") — Banco Digital"
                : (esRechazado
                        ? "Pago rechazado " + servicio + " (" + montoTxt + ") — Banco Digital"
                        : "Pago en proceso " + servicio + " (" + montoTxt + ") — Banco Digital");

        String rawAuth = ticket.getNumAutorizacion() != null ? ticket.getNumAutorizacion().trim() : "";
        String filaAutorizacion = (!rawAuth.isEmpty() && !"-1".equals(rawAuth))
                ? "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">Folio de autorización</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">" + escaparHtml(rawAuth) + "</td></tr>"
                : "";
        String filaIdTx = ticket.getIdTx() != null
                ? "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">ID Transacción (idTx)</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">" + ticket.getIdTx() + "</td></tr>"
                : "";
        String upc = escaparHtml(ticket.getUpc() != null ? ticket.getUpc() : "N/D");
        String fecha = escaparHtml(ticket.getFechaHora() != null ? ticket.getFechaHora() : "");
        String leyenda = ticket.getLeyendaProducto() != null && !ticket.getLeyendaProducto().isBlank()
                ? escaparHtml(ticket.getLeyendaProducto().trim())
                : "";
        String bloqueSoporte = leyenda.isEmpty()
                ? ""
                : "<div style=\"background:#F8FAFC;border-left:4px solid #1C69F7;border-radius:6px;padding:12px;\">"
                        + "<div style=\"font-size:12px;font-weight:700;color:#0F172A;margin-bottom:4px;\">Soporte y aclaraciones</div>"
                        + "<div style=\"font-size:12px;color:#475569;line-height:1.45;\">" + leyenda + "</div>"
                        + "</div>";

        String html = "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E2E8F0;border-radius:14px;background:#FFFFFF;\">"
                + "<div style=\"display:flex;justify-content:space-between;align-items:center;border-bottom:2px solid #1C69F7;padding-bottom:12px;margin-bottom:16px;\">"
                + "<div style=\"color:#1C69F7;font-weight:800;font-size:20px;\">Banco Digital</div>"
                + "<span style=\"background:" + badgeBg + ";color:" + badgeFg + ";padding:4px 10px;border-radius:999px;font-size:12px;font-weight:700;\">" + estadoLabel + "</span>"
                + "</div>"
                + "<h2 style=\"color:#0F172A;margin:0 0 6px 0;font-size:18px;\">" + titulo + "</h2>"
                + "<p style=\"color:#475569;font-size:14px;margin:0 0 16px 0;\">" + intro + "</p>"
                + "<div style=\"background:#EFF6FF;border:1px solid #BFDBFE;border-radius:10px;padding:14px;text-align:center;margin-bottom:16px;\">"
                + "<div style=\"font-size:12px;color:#1D4ED8;font-weight:700;text-transform:uppercase;\">" + etiquetaMonto + "</div>"
                + "<div style=\"font-size:28px;font-weight:800;color:#0F172A;margin-top:4px;\">" + montoTxt + "</div>"
                + "<div style=\"font-size:13px;color:#334155;margin-top:4px;\">" + servicio + (producto.isEmpty() ? "" : " · " + producto) + "</div>"
                + "</div>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:13px;color:#334155;margin-bottom:16px;\">"
                + "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">Destino / Referencia</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">" + destino + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">Cuenta de cargo</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">" + cuenta + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">Comisión cliente</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">$0.00 MXN</td></tr>"
                + filaAutorizacion
                + filaIdTx
                + "<tr><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;color:#64748B;\">Folio UPC</td><td style=\"padding:6px 0;border-bottom:1px solid #F1F5F9;text-align:right;font-weight:700;\">" + upc + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;color:#64748B;\">Fecha y hora</td><td style=\"padding:6px 0;text-align:right;font-weight:700;\">" + fecha + "</td></tr>"
                + "</table>"
                + bloqueSoporte
                + "</div>";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of("name", senderName, "email", senderEmail));
        payload.put("to", List.of(Map.of(
                "email", ticket.getDestinatarioEmail(),
                "name", ticket.getNombreCliente() != null && !ticket.getNombreCliente().isBlank()
                        ? ticket.getNombreCliente() : ticket.getDestinatarioEmail()
        )));
        payload.put("subject", asunto);
        payload.put("htmlContent", html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-key", apiKey);

        try {
            restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), String.class);
            log.info("Ticket de pago UPC {} enviado por Brevo a {}", ticket.getUpc(), ticket.getDestinatarioEmail());
        } catch (Exception e) {
            log.error("Error al enviar ticket de pago UPC {} vía Brevo a {}: {}",
                    ticket.getUpc(), ticket.getDestinatarioEmail(), e.getMessage());
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
