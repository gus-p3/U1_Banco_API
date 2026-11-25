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
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
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

    private static final String XML_CODIGO_05 =
            "<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Origen Valido</TEXTO></MENSAJE></RESPONSE>";
    private static final String XML_CON_PRODUCTOS =
            "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE><PRODUCTOS>"
                    + "<producto servicio=\"Telcel\" producto=\"Recarga 50\" idServicio=\"133\" idProducto=\"582\" "
                    + "idCatTipoServicio=\"1\" tipoFront=\"1\" precio=\"50.00\"><legend>Recarga Telcel</legend></producto>"
                    + "</PRODUCTOS></RESPONSE>";

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
        when(tokenService.obtenerTokenVigente()).thenReturn("gp-token-1");

        var mapper = new ProductoMapperImpl();
        var transform = new TransformProductImpl(mapper);
        var objMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ProductoServiceImpl(
                productoRepository, mapper, productClient, tokenService, transform, redisTemplate, objMapper);
    }

    @Test
    @DisplayName("sincronizarCatalogo realiza la petición real a GestoPago y si retorna CODIGO=05 usa Redis y respalda PostgreSQL")
    void sincronizarConCodigo05UsaRespaldoSinBorrar() {
        when(productClient.getProductListXml("Bearer gp-token-1")).thenReturn(XML_CODIGO_05);
        when(valueOps.get("catalogo:productos:gestopago:v2")).thenReturn(List.of(
                Map.of("producto", "Telcel 100", "servicio", "Telcel", "tipoFront", 1, "idServicio", 133, "idProducto", 582)));
        when(productoRepository.count()).thenReturn(0L);
        when(productoRepository.findAll()).thenReturn(List.of());

        List<ProductoResponse> result = service.sincronizarCatalogo();

        verify(productClient).getProductListXml("Bearer gp-token-1");
        assertThat(result).extracting(ProductoResponse::getProducto).containsExactly("Telcel 100");
        assertThat(result.get(0).getLogoUrl())
                .isEqualTo("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/133.png");
        verify(productoRepository).saveAll(anyList());
        verify(productoRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("sincronizarCatalogo guarda en Redis y PostgreSQL cuando GestoPago devuelve productos y reintenta ante 403")
    void sincronizarConProductosYReintento403() {
        Request dummyReq = Request.create(Request.HttpMethod.GET, "/getProductList.do", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        FeignException forbidden = new FeignException.Forbidden("Expired", dummyReq, null, Map.of());
        when(productClient.getProductListXml("Bearer gp-token-1")).thenThrow(forbidden);
        when(tokenService.renovarTokenForzado()).thenReturn("gp-token-2");
        when(productClient.getProductListXml("Bearer gp-token-2")).thenReturn(XML_CON_PRODUCTOS);
        when(productoRepository.findAll()).thenReturn(List.of());

        List<ProductoResponse> result = service.sincronizarCatalogo();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServicio()).isEqualTo("Telcel");
        verify(tokenService).renovarTokenForzado();
        verify(valueOps).set(eq("catalogo:productos:gestopago:v2"), any());
        verify(productoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Si Redis está vacío, getProductsRedis toma los productos de PostgreSQL y llena Redis")
    void desdePostgresYPueblaRedis() {
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
        assertThat(result.get(0).getCategoria()).isEqualTo("Recargas");
        verify(valueOps).set(anyString(), any());
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("Si Redis y PostgreSQL están vacíos consulta GestoPago; ante CODIGO=05 devuelve lista vacía sin fallar")
    void ambosVaciosConsultaGestoPagoCodigo05() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());
        when(productClient.getProductListXml(anyString())).thenReturn(XML_CODIGO_05);

        assertThat(service.getProductsRedis()).isEmpty();
        assertThat(service.obtenerProductosXML()).isEmpty();
        verify(productClient, times(2)).getProductListXml("Bearer gp-token-1");
    }

    @Test
    @DisplayName("CRUD: obtenerPorId, reemplazar, actualizarParcial y eliminar")
    void crudConsultasYActualizaciones() {
        Producto entidad = new Producto();
        entidad.setId(1);
        entidad.setProducto("Telcel 100");
        entidad.setServicio("Telcel");
        entidad.setIdServicio(133);
        entidad.setIdProducto(582);
        entidad.setTipoFront(1);

        when(productoRepository.findById(1)).thenReturn(Optional.of(entidad));
        when(productoRepository.findById(9)).thenReturn(Optional.empty());
        when(productoRepository.save(entidad)).thenReturn(entidad);
        when(productoRepository.existsById(1)).thenReturn(true);
        when(productoRepository.existsById(9)).thenReturn(false);

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
