package com.intrumentoev.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapperImpl;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.impl.gestopago.ProductoServiceImpl;
import com.intrumentoev.demo.service.impl.gestopago.TransformProductImpl;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class ProductoServiceImplTest {

    private ProductoRepository productoRepository;
    private GestoPagoProductClient productClient;
    private GestoPagoTokenService tokenService;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        productClient = mock(GestoPagoProductClient.class);
        tokenService = mock(GestoPagoTokenService.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        var mapper = new ProductoMapperImpl();
        var transform = new TransformProductImpl(mapper);
        var objMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ProductoServiceImpl(
                productoRepository, mapper, productClient, tokenService, transform, redisTemplate, objMapper);
    }

    private void apiDevuelveProductos() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml("Bearer tok")).thenReturn(TransformProductImplTest.XML);
    }

    @Test
    @DisplayName("Con caché en Redis devuelve el catálogo filtrado (solo tipoFront > 0)")
    void desdeRedis() {
        when(valueOps.get(anyString())).thenReturn(List.of(
                Map.of("producto", "Recarga", "tipoFront", 2, "idServicio", 133, "idProducto", 582),
                Map.of("producto", "Oculto", "tipoFront", 0, "idServicio", 79, "idProducto", 209)));

        List<ProductoResponse> result = service.getProductsRedis();

        assertThat(result).extracting(ProductoResponse::getProducto).containsExactly("Recarga");
        verifyNoInteractions(productoRepository, productClient);
    }

    @Test
    @DisplayName("Si no está en Redis lo agarra de PostgreSQL y repuebla Redis")
    void desdePostgres() {
        when(valueOps.get(anyString())).thenReturn(null);
        Producto p = new Producto();
        p.setId(1);
        p.setProducto("Recarga 50");
        p.setServicio("Telcel");
        p.setIdServicio(133);
        p.setIdProducto(582);
        p.setTipoFront(1);
        when(productoRepository.findAll()).thenReturn(List.of(p));

        List<ProductoResponse> result = service.getProductsRedis();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLogoUrl()).contains("133.png");
        verify(valueOps).set(anyString(), any());
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Sin caché ni BD consulta la API y guarda en Redis")
    void desdeApi() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        apiDevuelveProductos();

        List<ProductoResponse> result = service.getProductsRedis();

        assertThat(result).hasSize(1);
        verify(valueOps).set(anyString(), any());
    }

    @Test
    @DisplayName("Si Redis falla al guardar, el catálogo de la API se guarda en PostgreSQL sin borrar existentes")
    void apiYRedisCaido() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        doThrow(new RuntimeException("redis")).when(valueOps).set(anyString(), any());
        apiDevuelveProductos();

        assertThat(service.getProductsRedis()).hasSize(1);
        verify(productoRepository, never()).deleteAll();
        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("sincronizarCatalogo actualiza Redis y Postgres SIN borrar productos anteriores")
    void sincronizarSinBorrarAnteriores() {
        apiDevuelveProductos();
        Producto existente = new Producto();
        existente.setId(99);
        existente.setIdServicio(133);
        existente.setIdProducto(582);
        when(productoRepository.findAll()).thenReturn(List.of(existente));

        List<ProductoResponse> synced = service.sincronizarCatalogo();

        assertThat(synced).hasSize(1);
        verify(redisTemplate, never()).delete(anyString());
        verify(productoRepository, never()).deleteAll();
        verify(valueOps).set(anyString(), any());
        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Si la API devuelve 0 productos en sincronizarCatalogo, no borra nada y agarra de Redis o Postgres")
    void sincronizarVacioAgarraDeRedisOPostgres() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml("Bearer tok"))
                .thenReturn("<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Origen Valido</TEXTO></MENSAJE></RESPONSE>");
        when(valueOps.get("catalogo:productos:gestopago:v2")).thenReturn(List.of(
                Map.of("producto", "Telcel 50", "servicio", "Telcel", "idServicio", 133, "idProducto", 582, "tipoFront", 1)));

        List<ProductoResponse> fallback = service.sincronizarCatalogo();

        assertThat(fallback).hasSize(1);
        assertThat(fallback.get(0).getProducto()).isEqualTo("Telcel 50");
        verify(redisTemplate, never()).delete(anyString());
        verify(productoRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("sincronizarProductosDesdeGestoPago guarda sin borrar anteriores y si viene vacío no borra nada")
    void sincronizarProductosEndpoint() {
        apiDevuelveProductos();
        when(productoRepository.findAll()).thenReturn(List.of());

        service.sincronizarProductosDesdeGestoPago();

        verify(productoRepository, never()).deleteAll();
        verify(productoRepository).saveAll(anyList());

        clearInvocations(productoRepository);
        when(productClient.getProductListXml("Bearer tok"))
                .thenReturn("<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Origen Valido</TEXTO></MENSAJE></RESPONSE>");
        service.sincronizarProductosDesdeGestoPago();
        verify(productoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("obtenerProductosXML devuelve del XML o cae a Redis/Postgres si el XML viene vacío")
    void obtenerProductosXmlConFallback() {
        apiDevuelveProductos();
        assertThat(service.obtenerProductosXML()).hasSize(2);

        when(productClient.getProductListXml("Bearer tok"))
                .thenReturn("<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Origen Valido</TEXTO></MENSAJE></RESPONSE>");
        when(valueOps.get("catalogo:productos:gestopago:v2")).thenReturn(List.of(
                Map.of("producto", "Respaldo Redis", "tipoFront", 1, "idServicio", 133, "idProducto", 582)));

        List<ProductoResponse> fallback = service.obtenerProductosXML();
        assertThat(fallback).hasSize(1);
        assertThat(fallback.get(0).getProducto()).isEqualTo("Respaldo Redis");
    }

    @Test
    @DisplayName("Sin token de GestoPago se lanza IllegalStateException")
    void sinToken() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        when(tokenService.obtenerTokenVigente()).thenReturn(" ");

        assertThatThrownBy(() -> service.getProductsRedis()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.sincronizarProductosDesdeGestoPago()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.obtenerProductosXML()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("CRUD: obtenerTodos, obtenerPorId, reemplazar, actualizarParcial y eliminar")
    void crudConsultasYActualizaciones() {
        Producto entidad = new Producto();
        entidad.setId(1);
        entidad.setProducto("Telcel 100");
        entidad.setServicio("Telcel");
        entidad.setIdServicio(133);
        entidad.setIdProducto(582);
        entidad.setTipoFront(1);

        when(productoRepository.findAll()).thenReturn(List.of(entidad));
        when(productoRepository.findById(1)).thenReturn(Optional.of(entidad));
        when(productoRepository.findById(9)).thenReturn(Optional.empty());
        when(productoRepository.save(entidad)).thenReturn(entidad);
        when(productoRepository.existsById(1)).thenReturn(true);
        when(productoRepository.existsById(9)).thenReturn(false);

        assertThat(service.obtenerTodosLosProductos()).hasSize(1);
        assertThat(service.obtenerProductoPorId(1).getProducto()).isEqualTo("Telcel 100");
        assertThatThrownBy(() -> service.obtenerProductoPorId(9)).hasMessageContaining("no encontrado");

        ProductoRequest req = new ProductoRequest();
        req.setProducto("Telcel 200");
        req.setServicio("Telcel");
        req.setIdServicio(133);
        req.setIdProducto(582);
        req.setTipoFront(1);
        assertThat(service.reemplazarProducto(1, req).getProducto()).isEqualTo("Telcel 200");
        assertThatThrownBy(() -> service.reemplazarProducto(9, req)).hasMessageContaining("no encontrado");

        ProductoPatchRequest patch = new ProductoPatchRequest();
        patch.setPrecio("200.00");
        assertThat(service.actualizarParcialProducto(1, patch).getPrecio()).isEqualTo("200.00");
        assertThatThrownBy(() -> service.actualizarParcialProducto(9, patch)).hasMessageContaining("no encontrado");

        service.eliminarProducto(1);
        verify(productoRepository).deleteById(1);
        assertThatThrownBy(() -> service.eliminarProducto(9)).hasMessageContaining("no encontrado");
    }
}
