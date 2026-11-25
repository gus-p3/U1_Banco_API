package com.intrumentoev.demo.client;

import com.intrumentoev.demo.client.inegi.InegiClientImpl;
import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba el cliente INEGI contra un servidor HTTP local (nunca llama al servicio real). */
class InegiClientImplTest {

    private HttpServer server;
    private InegiClientImpl client;

    private void responder(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    private void iniciar() {
        server.start();
        client = new InegiClientImpl("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("obtenerEstados devuelve los estados del JSON de INEGI")
    void estados() {
        responder("/mgee", 200, "{\"datos\":[{\"cve_ent\":\"09\",\"nomgeo\":\"Ciudad de México\",\"nom_abr\":\"CDMX\"}],\"otro\":1}");
        iniciar();

        List<InegiStateDto> estados = client.obtenerEstados();

        assertThat(estados).hasSize(1);
        assertThat(estados.get(0).getCveEnt()).isEqualTo("09");
        assertThat(estados.get(0).getNomEnt()).isEqualTo("Ciudad de México");
    }

    @Test
    @DisplayName("obtenerMunicipios consulta por cve_ent")
    void municipios() {
        responder("/mgem/09", 200, "{\"datos\":[{},{}]}");
        iniciar();

        List<InegiMunicipalityDto> municipios = client.obtenerMunicipios("09");

        assertThat(municipios).hasSize(2);
    }

    @Test
    @DisplayName("Un error HTTP devuelve lista vacía en vez de propagar la excepción")
    void errorHttp() {
        responder("/mgee", 500, "{\"error\":true}");
        responder("/mgem/09", 404, "{}");
        iniciar();

        assertThat(client.obtenerEstados()).isEmpty();
        assertThat(client.obtenerMunicipios("09")).isEmpty();
    }

    @Test
    @DisplayName("Respuesta sin 'datos' o sin cuerpo devuelve lista vacía")
    void sinDatos() {
        responder("/mgee", 200, "{}");
        responder("/mgem/01", 200, "");
        iniciar();

        assertThat(client.obtenerEstados()).isEmpty();
        assertThat(client.obtenerMunicipios("01")).isEmpty();
    }
}
