package com.intrumentoev.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapperImpl;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.impl.gestopago.ProductServiceRedisImpl;
import com.intrumentoev.demo.service.impl.gestopago.TransformProductImpl;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class ProductServiceRedisImplTest {

    private GestoPagoProductClient productClient;
    private GestoPagoTokenService tokenService;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private ProductoRepository productoRepository;
    private ProductServiceRedisImpl service;

    @BeforeEach
    void setUp() {
        productClient = mock(GestoPagoProductClient.class);
        tokenService = mock(GestoPagoTokenService.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        productoRepository = mock(ProductoRepository.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        var mapper = new ProductoMapperImpl();
        service = new ProductServiceRedisImpl(productClient, mock(GestoPagoAuthClient.class), tokenService,
                new TransformProductImpl(mapper), redisTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()), productoRepository, mapper);
    }

    private void apiDevuelveProductos() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml("Bearer tok")).thenReturn(TransformProductImplTest.XML);
    }

    @Test
    @DisplayName("Con caché en Redis devuelve el catálogo filtrado (solo tipoFront > 0)")
    void desdeRedis() {
        when(valueOps.get(anyString())).thenReturn(List.of(
                Map.of("producto", "Recarga", "tipoFront", 2),
                Map.of("producto", "Oculto", "tipoFront", 0)));

        List<ProductoResponse> result = service.getProductsRedis();

        assertThat(result).extracting(ProductoResponse::getProducto).containsExactly("Recarga");
        verifyNoInteractions(productoRepository, productClient);
    }

    @Test
    @DisplayName("Sin Redis cae a PostgreSQL")
    void desdePostgres() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis caído"));
        Producto p = new Producto();
        p.setProducto("Recarga");
        p.setTipoFront(1);
        when(productoRepository.findAll()).thenReturn(List.of(p));

        assertThat(service.getProductsRedis()).hasSize(1);
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Sin caché ni BD consulta la API y guarda en Redis")
    void desdeApi() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        apiDevuelveProductos();

        List<ProductoResponse> result = service.getProductsRedis();

        assertThat(result).hasSize(1); // el de tipoFront = 0 se filtra
        verify(valueOps).set(anyString(), any());
        verify(productoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Si Redis falla al guardar, el catálogo de la API se guarda en PostgreSQL")
    void apiYRedisCaido() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        doThrow(new RuntimeException("redis")).when(valueOps).set(anyString(), any());
        apiDevuelveProductos();

        assertThat(service.getProductsRedis()).hasSize(1);
        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("sincronizarCatalogo limpia Redis y BD, y repuebla ambos")
    void sincronizar() {
        apiDevuelveProductos();

        assertThat(service.sincronizarCatalogo()).isNull();

        verify(redisTemplate).delete(anyString());
        verify(productoRepository).deleteAll();
        verify(valueOps).set(anyString(), any());
        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("sincronizarCatalogo tolera fallos de Redis")
    void sincronizarRedisCaido() {
        doThrow(new RuntimeException("redis")).when(redisTemplate).delete(anyString());
        doThrow(new RuntimeException("redis")).when(valueOps).set(anyString(), any());
        apiDevuelveProductos();

        service.sincronizarCatalogo();

        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Sin token de GestoPago se lanza IllegalStateException")
    void sinToken() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        when(tokenService.obtenerTokenVigente()).thenReturn(null);

        assertThatThrownBy(() -> service.getProductsRedis()).isInstanceOf(IllegalStateException.class);
    }
}
