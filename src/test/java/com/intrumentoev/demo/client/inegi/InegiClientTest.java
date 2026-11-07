package com.intrumentoev.demo.client.inegi;

import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InegiClientTest {

    private InegiClientImpl inegiClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://gaia.inegi.org.mx/wscatgeo/v2");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        inegiClient = new InegiClientImpl("https://gaia.inegi.org.mx/wscatgeo/v2");
        ReflectionTestUtils.setField(inegiClient, "restClient", builder.build());
    }

    @Test
    void obtenerEstados_Exito() {
        String json = "{\"datos\":[{\"cve_ent\":\"01\",\"nomgeo\":\"Aguascalientes\"}]}";
        mockServer.expect(requestTo("https://gaia.inegi.org.mx/wscatgeo/v2/mgee"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<InegiStateDto> estados = inegiClient.obtenerEstados();

        assertNotNull(estados);
        assertEquals(1, estados.size());
        assertEquals("01", estados.get(0).getCveEnt());
        assertEquals("Aguascalientes", estados.get(0).getNomEnt());
        mockServer.verify();
    }

    @Test
    void obtenerEstados_Error_RetornaListaVacia() {
        mockServer.expect(requestTo("https://gaia.inegi.org.mx/wscatgeo/v2/mgee"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        List<InegiStateDto> estados = inegiClient.obtenerEstados();

        assertNotNull(estados);
        assertTrue(estados.isEmpty());
        mockServer.verify();
    }

    @Test
    void obtenerMunicipios_Exito() {
        String json = "{\"datos\":[{\"cve_mun\":\"001\",\"nomgeo\":\"Aguascalientes\"}]}";
        mockServer.expect(requestTo("https://gaia.inegi.org.mx/wscatgeo/v2/mgem/01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<InegiMunicipalityDto> municipios = inegiClient.obtenerMunicipios("01");

        assertNotNull(municipios);
        assertEquals(1, municipios.size());
        assertEquals("001", municipios.get(0).getCveMun());
        assertEquals("Aguascalientes", municipios.get(0).getNomMun());
        mockServer.verify();
    }

    @Test
    void obtenerMunicipios_Error_RetornaListaVacia() {
        mockServer.expect(requestTo("https://gaia.inegi.org.mx/wscatgeo/v2/mgem/01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        List<InegiMunicipalityDto> municipios = inegiClient.obtenerMunicipios("01");

        assertNotNull(municipios);
        assertTrue(municipios.isEmpty());
        mockServer.verify();
    }
}
