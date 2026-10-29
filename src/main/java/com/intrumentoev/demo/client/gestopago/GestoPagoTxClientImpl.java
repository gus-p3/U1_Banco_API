package com.intrumentoev.demo.client.gestopago;

import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * sendTx / confirmTx con timeout de lectura propio (65 s), distinto al resto de clientes (5 s).
 * Cualquier timeout o corte de conexión se reporta como sinRespuesta; el status HTTP nunca lanza excepción.
 */
@Component
@Slf4j
public class GestoPagoTxClientImpl implements GestoPagoTxClient {

    private static final String SEND_TX_PATH = "/sistema/service/sendTx.do";
    private static final String CONFIRM_TX_PATH = "/sistema/service/confirmTx.do";

    private final RestClient restClient;
    private final String apiKey;

    public GestoPagoTxClientImpl(@Value("${gestopago.auth.url}") String baseUrl,
                                 @Value("${gestopago.tx.api-key:}") String apiKey,
                                 @Value("${gestopago.tx.connect-timeout-ms:5000}") long connectTimeoutMs,
                                 @Value("${gestopago.tx.read-timeout-ms:65000}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
    }

    @Override
    public GestoPagoHttpResponse sendTx(String bearerToken, String payloadCifrado) {
        return post(SEND_TX_PATH, bearerToken, payloadCifrado);
    }

    @Override
    public GestoPagoHttpResponse confirmTx(String bearerToken, String payloadCifrado) {
        return post(CONFIRM_TX_PATH, bearerToken, payloadCifrado);
    }

    private GestoPagoHttpResponse post(String path, String bearerToken, String payloadCifrado) {
        String form = "random=" + System.currentTimeMillis()
                + "&signed=" + URLEncoder.encode(payloadCifrado, StandardCharsets.UTF_8);
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(h -> {
                        h.setBearerAuth(bearerToken);
                        if (apiKey != null && !apiKey.isBlank()) {
                            h.set("X-API-Key", apiKey);
                        }
                    })
                    .body(form)
                    .exchange((request, response) -> GestoPagoHttpResponse.conRespuesta(
                            response.getStatusCode().value(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.warn("Sin respuesta de GestoPago en {}: {}", path, e.getClass().getSimpleName());
            return GestoPagoHttpResponse.sinRespuesta(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
