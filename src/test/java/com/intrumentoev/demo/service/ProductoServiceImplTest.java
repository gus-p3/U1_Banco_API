package com.intrumentoev.demo.service;

import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.impl.gestopago.ProductoServiceImpl;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoMapper productoMapper;
    @Mock private GestoPagoProductClient productClient;
    @Mock private GestoPagoTokenService tokenService;
    @Mock private TransformProduct transformProduct;
    @InjectMocks private ProductoServiceImpl service;

    private final Producto producto = new Producto();
    private final ProductoResponse response = ProductoResponse.builder().id(1).build();

    @Test
    @DisplayName("sincronizar guarda como máximo los primeros 20 productos del XML")
    void sincronizar() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml("Bearer tok")).thenReturn("<xml/>");
        List<ProductoXmlItem> items = java.util.stream.IntStream.range(0, 30).mapToObj(i -> new ProductoXmlItem()).toList();
        when(transformProduct.transformProductXML("<xml/>")).thenReturn(items);
        when(transformProduct.transformProductEntities(anyList())).thenReturn(List.of(producto));
        when(productoRepository.saveAll(anyList())).thenReturn(List.of(producto));

        service.sincronizarProductosDesdeGestoPago();

        verify(transformProduct).transformProductEntities(argThat(l -> l.size() == 20));
        verify(productoRepository).saveAll(List.of(producto));
    }

    @Test
    @DisplayName("sincronizar sin productos no guarda nada")
    void sincronizarVacio() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml(anyString())).thenReturn("<xml/>");
        when(transformProduct.transformProductXML(anyString())).thenReturn(List.of());

        service.sincronizarProductosDesdeGestoPago();

        verify(productoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Sin token activo se lanza IllegalStateException")
    void sinToken() {
        when(tokenService.obtenerTokenVigente()).thenReturn(" ");
        assertThatThrownBy(() -> service.sincronizarProductosDesdeGestoPago()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.obtenerProductosXML()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("obtenerProductosXML transforma el XML a JSON")
    void productosXml() {
        when(tokenService.obtenerTokenVigente()).thenReturn("tok");
        when(productClient.getProductListXml("Bearer tok")).thenReturn("<xml/>");
        when(transformProduct.transformProductXML("<xml/>")).thenReturn(List.of());
        when(transformProduct.transformProductJSON(List.of())).thenReturn(List.of(response));

        assertThat(service.obtenerProductosXML()).containsExactly(response);
    }

    @Test
    @DisplayName("obtenerTodos y obtenerPorId")
    void consultas() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(productoMapper.toResponseList(List.of(producto))).thenReturn(List.of(response));
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));
        when(productoRepository.findById(9)).thenReturn(Optional.empty());
        when(productoMapper.toResponse(producto)).thenReturn(response);

        assertThat(service.obtenerTodosLosProductos()).containsExactly(response);
        assertThat(service.obtenerProductoPorId(1)).isSameAs(response);
        assertThatThrownBy(() -> service.obtenerProductoPorId(9)).hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("reemplazar y actualizar parcial persisten el producto; inexistente lanza excepción")
    void actualizaciones() {
        ProductoRequest request = new ProductoRequest();
        ProductoPatchRequest patch = new ProductoPatchRequest();
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));
        when(productoRepository.findById(9)).thenReturn(Optional.empty());
        when(productoRepository.save(producto)).thenReturn(producto);
        when(productoMapper.toResponse(producto)).thenReturn(response);

        assertThat(service.reemplazarProducto(1, request)).isSameAs(response);
        assertThat(service.actualizarParcialProducto(1, patch)).isSameAs(response);
        verify(productoMapper).updateEntityFromRequest(request, producto);
        verify(productoMapper).updateEntityFromPatch(patch, producto);

        assertThatThrownBy(() -> service.reemplazarProducto(9, request)).hasMessageContaining("no encontrado");
        assertThatThrownBy(() -> service.actualizarParcialProducto(9, patch)).hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("eliminar borra si existe y lanza excepción si no")
    void eliminar() {
        when(productoRepository.existsById(1)).thenReturn(true);
        when(productoRepository.existsById(9)).thenReturn(false);

        service.eliminarProducto(1);

        verify(productoRepository).deleteById(1);
        assertThatThrownBy(() -> service.eliminarProducto(9)).hasMessageContaining("no encontrado");
    }
}
