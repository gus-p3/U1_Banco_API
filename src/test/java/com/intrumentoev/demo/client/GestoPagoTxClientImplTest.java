package com.intrumentoev.demo.client;

import com.intrumentoev.demo.client.gestopago.GestoPagoTxClientImpl;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoTxClientImplTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void responder(int status, String cuerpo, long demoraMs) {
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            apiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException ignored) {
                // el cliente ya cerró (caso timeout)
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @Test
    @DisplayName("sendTx envía form-urlencoded random + signed (URL-encoded) con Bearer y X-API-Key")
    void sendTx() {
        responder(200, "<RESPONSE/>", 0);
        GestoPagoTxClientImpl client = new GestoPagoTxClientImpl(baseUrl, "mi-api-key", 2000, 5000);
        String cifrado = "+LWZ7icI60/DAI8g==";

        GestoPagoHttpResponse r = client.sendTx("tok-123", cifrado);

        assertThat(r.sinRespuesta()).isFalse();
        assertThat(r.httpStatus()).isEqualTo(200);
        assertThat(r.body()).isEqualTo("<RESPONSE/>");
        assertThat(path.get()).isEqualTo("/sistema/service/sendTx.do");
        assertThat(contentType.get()).startsWith("application/x-www-form-urlencoded");
        assertThat(authorization.get()).isEqualTo("Bearer tok-123");
        assertThat(apiKey.get()).isEqualTo("mi-api-key");
        assertThat(body.get()).startsWith("random=").endsWith("&signed=" + URLEncoder.encode(cifrado, StandardCharsets.UTF_8));
        assertThat(body.get()).contains("%2B").contains("%2F").contains("%3D");
    }

    @Test
    @DisplayName("confirmTx usa su propia ruta y no manda X-API-Key si no está configurada")
    void confirmTxSinApiKey() {
        responder(200, "<RESPONSE/>", 0);
        GestoPagoTxClientImpl client = new GestoPagoTxClientImpl(baseUrl, "", 2000, 5000);

        client.confirmTx("tok", "abc");

        assertThat(path.get()).isEqualTo("/sistema/service/confirmTx.do");
        assertThat(apiKey.get()).isNull();
    }

    @Test
    @DisplayName("Un 403 (token expirado) no lanza excepción: se reporta el status")
    void tokenExpirado() {
        responder(403, "EXPIRED", 0);
        GestoPagoTxClientImpl client = new GestoPagoTxClientImpl(baseUrl, "", 2000, 5000);

        GestoPagoHttpResponse r = client.sendTx("tok", "abc");

        assertThat(r.esTokenExpirado()).isTrue();
        assertThat(r.sinRespuesta()).isFalse();
    }

    @Test
    @DisplayName("Timeout de lectura se reporta como sinRespuesta (no como error)")
    void timeoutDeLectura() {
        responder(200, "<RESPONSE/>", 1500);
        GestoPagoTxClientImpl client = new GestoPagoTxClientImpl(baseUrl, "", 2000, 300);

        GestoPagoHttpResponse r = client.sendTx("tok", "abc");

        assertThat(r.sinRespuesta()).isTrue();
        assertThat(r.httpStatus()).isNull();
        assertThat(r.error()).isNotBlank();
    }

    @Test
    @DisplayName("Servidor inalcanzable se reporta como sinRespuesta")
    void servidorInalcanzable() {
        GestoPagoTxClientImpl client = new GestoPagoTxClientImpl("http://127.0.0.1:1", "", 500, 500);

        assertThat(client.sendTx("tok", "abc").sinRespuesta()).isTrue();
    }
}
