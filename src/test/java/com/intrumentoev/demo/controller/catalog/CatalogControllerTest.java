package com.intrumentoev.demo.controller.catalog;

import com.intrumentoev.demo.model.catalog.*;
import com.intrumentoev.demo.service.service.catalog.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    private CatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogController(catalogService);
    }

    @Test
    void obtenerGeneros() {
        when(catalogService.obtenerGeneros()).thenReturn(List.of(new GenderResponse()));
        ResponseEntity<List<GenderResponse>> response = controller.obtenerGeneros();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerNacionalidades() {
        when(catalogService.obtenerNacionalidades()).thenReturn(List.of(new NationalityResponse()));
        ResponseEntity<List<NationalityResponse>> response = controller.obtenerNacionalidades();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerEstadosCiviles() {
        when(catalogService.obtenerEstadosCiviles()).thenReturn(List.of(new MaritalStatusResponse()));
        ResponseEntity<List<MaritalStatusResponse>> response = controller.obtenerEstadosCiviles();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerEstados() {
        when(catalogService.obtenerEstados()).thenReturn(List.of(new StateResponse()));
        ResponseEntity<List<StateResponse>> response = controller.obtenerEstados();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerMunicipiosPorEstado() {
        when(catalogService.obtenerMunicipiosPorEstado("01")).thenReturn(List.of(new MunicipalityResponse()));
        ResponseEntity<List<MunicipalityResponse>> response = controller.obtenerMunicipiosPorEstado("01");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void sincronizarCatalogos() {
        when(catalogService.sincronizarCatalogos()).thenReturn(new CatalogSyncResponse());
        ResponseEntity<CatalogSyncResponse> response = controller.sincronizarCatalogos();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
