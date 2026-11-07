package com.intrumentoev.demo.service.gestopago;

import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private TransformProduct transformProduct;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    @DisplayName("Sincronización exitosa desde GestoPago hacia BD")
    void testSincronizarProductosDesdeGestoPago_Exitoso() {
        when(tokenService.obtenerTokenVigente()).thenReturn("valid-token");
        when(gestoPagoProductClient.getProductListXml(anyString())).thenReturn("<xml/>");

        ProductoXmlItem item = new ProductoXmlItem();
        when(transformProduct.transformProductXML(anyString())).thenReturn(List.of(item));

        Producto p = new Producto();
        when(transformProduct.transformProductEntities(any())).thenReturn(List.of(p));

        productoService.sincronizarProductosDesdeGestoPago();

        verify(productoRepository).saveAll(any());
    }

    @Test
    @DisplayName("Sincronización sin token lanza IllegalStateException")
    void testSincronizarProductos_SinTokenLanzaExcepcion() {
        when(tokenService.obtenerTokenVigente()).thenReturn("");
        assertThatThrownBy(() -> productoService.sincronizarProductosDesdeGestoPago())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Listar y obtener producto por ID")
    void testListarYObtenerProducto() {
        Producto p = new Producto();
        p.setId(1);
        ProductoResponse resp = new ProductoResponse();
        resp.setId(1);

        when(productoRepository.findAll()).thenReturn(List.of(p));
        when(productoMapper.toResponseList(List.of(p))).thenReturn(List.of(resp));

        List<ProductoResponse> lista = productoService.obtenerTodosLosProductos();
        assertThat(lista).hasSize(1);

        when(productoRepository.findById(1)).thenReturn(Optional.of(p));
        when(productoMapper.toResponse(p)).thenReturn(resp);

        ProductoResponse single = productoService.obtenerProductoPorId(1);
        assertThat(single.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Actualizar parcialmente producto existente")
    void testActualizarParcialProducto() {
        Producto p = new Producto();
        p.setId(1);
        p.setPrecio("100.00");

        when(productoRepository.findById(1)).thenReturn(Optional.of(p));
        when(productoRepository.save(p)).thenReturn(p);
        when(productoMapper.toResponse(p)).thenReturn(new ProductoResponse());

        ProductoPatchRequest patch = new ProductoPatchRequest();
        patch.setPrecio("150.00");

        ProductoResponse updated = productoService.actualizarParcialProducto(1, patch);
        assertThat(updated).isNotNull();
    }

    @Test
    @DisplayName("Eliminar producto existente")
    void testEliminarProducto() {
        when(productoRepository.existsById(1)).thenReturn(true);
        productoService.eliminarProducto(1);
        verify(productoRepository).deleteById(1);
    }
}
