package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    private ProductoController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductoController(productoService);
    }

    @Test
    void sincronizarProductos() {
        UriComponentsBuilder ucb = UriComponentsBuilder.newInstance().scheme("http").host("localhost");
        ResponseEntity<Void> response = controller.sincronizarProductos(ucb);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(productoService).sincronizarProductosDesdeGestoPago();
    }

    @Test
    void obtenerTodosLosProductos() {
        when(productoService.obtenerTodosLosProductos()).thenReturn(List.of(new ProductoResponse()));

        ResponseEntity<List<ProductoResponse>> response = controller.obtenerTodosLosProductos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerProductoPorId() {
        when(productoService.obtenerProductoPorId(1)).thenReturn(new ProductoResponse());

        ResponseEntity<ProductoResponse> response = controller.obtenerProductoPorId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void reemplazarProducto() {
        ProductoRequest request = new ProductoRequest();
        when(productoService.reemplazarProducto(1, request)).thenReturn(new ProductoResponse());

        ResponseEntity<ProductoResponse> response = controller.reemplazarProducto(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void actualizarParcialProducto() {
        ProductoPatchRequest patch = new ProductoPatchRequest();
        when(productoService.actualizarParcialProducto(1, patch)).thenReturn(new ProductoResponse());

        ResponseEntity<ProductoResponse> response = controller.actualizarParcialProducto(1, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void eliminarProducto() {
        ResponseEntity<Void> response = controller.eliminarProducto(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productoService).eliminarProducto(1);
    }

    @Test
    void obtenerProductosXML() {
        when(productoService.obtenerProductosXML()).thenReturn(List.of(new ProductoResponse()));

        ResponseEntity<List<ProductoResponse>> response = controller.obtenerProductosXML();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}

