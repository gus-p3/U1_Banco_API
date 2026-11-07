package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
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
class ProductoRedisControllerTest {

    @Mock
    private ProductoServiceRedis productoServiceRedis;

    private ProductoRedisController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductoRedisController(productoServiceRedis);
    }

    @Test
    void getProductsRedis() {
        when(productoServiceRedis.getProductsRedis()).thenReturn(List.of(new ProductoResponse()));

        ResponseEntity<List<ProductoResponse>> response = controller.getProductsRedis();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(productoServiceRedis).getProductsRedis();
    }

    @Test
    void sincronizarManual() {
        when(productoServiceRedis.sincronizarCatalogo()).thenReturn(List.of(new ProductoResponse()));

        ResponseEntity<List<ProductoResponse>> response = controller.sincronizarManual();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(productoServiceRedis).sincronizarCatalogo();
    }
}
