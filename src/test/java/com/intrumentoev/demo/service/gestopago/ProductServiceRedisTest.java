package com.intrumentoev.demo.service.gestopago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.impl.gestopago.ProductServiceRedisImpl;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceRedisTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private TransformProduct transformProduct;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductServiceRedisImpl productServiceRedis;

    @Test
    @DisplayName("Sincronizar catálogo en Redis exitosamente")
    void testSincronizarCatalogo_Exitoso() {
        when(tokenService.obtenerTokenVigente()).thenReturn("valid-token");
        when(gestoPagoProductClient.getProductListXml(anyString())).thenReturn("<xml/>");

        ProductoXmlItem item = new ProductoXmlItem();
        when(transformProduct.transformProductXML(anyString())).thenReturn(List.of(item));

        ProductoResponse resp = new ProductoResponse();
        when(transformProduct.transformProductJSON(any())).thenReturn(List.of(resp));
        when(productoMapper.filtrarYOrdenarPorTipoFront(any())).thenReturn(List.of(resp));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<ProductoResponse> resultado = productServiceRedis.sincronizarCatalogo();

        assertThat(resultado).isNull(); // el modo cron de sincronizarCatalogo retorna null y guarda
        verify(valueOperations).set(eq("catalogo:productos:gestopago"), any());
    }

    @Test
    @DisplayName("Obtener catálogo desde PostgreSQL cuando no está en Redis")
    void testGetProductsRedis_FallbackPostgres() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:productos:gestopago")).thenReturn(null);

        Producto p = new Producto();
        p.setId(1);
        ProductoResponse resp = new ProductoResponse();
        resp.setId(1);

        when(productoRepository.findAll()).thenReturn(List.of(p));
        when(productoMapper.toResponseList(List.of(p))).thenReturn(List.of(resp));
        when(productoMapper.filtrarYOrdenarPorTipoFront(any())).thenReturn(List.of(resp));

        List<ProductoResponse> res = productServiceRedis.getProductsRedis();
        assertThat(res).hasSize(1);
    }
}
