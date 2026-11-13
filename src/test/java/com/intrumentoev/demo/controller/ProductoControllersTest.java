package com.intrumentoev.demo.controller;

import com.intrumentoev.demo.controller.gestopago.ProductoController;
import com.intrumentoev.demo.controller.gestopago.ProductoRedisController;
import com.intrumentoev.demo.controller.persona.PersonaController;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.scheduler.ProductoCatalogScheduler;
import com.intrumentoev.demo.service.impl.gestopago.CatalogoRefresher;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import com.intrumentoev.demo.service.service.persona.PersonaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Controladores delgados de productos, catálogo Redis y personas, más los jobs de catálogo. */
@ExtendWith(MockitoExtension.class)
class ProductoControllersTest {

    @Mock private ProductoService productoService;
    @Mock private ProductoServiceRedis productoServiceRedis;
    @Mock private PersonaService personaService;
    @InjectMocks private ProductoController productoController;
    @InjectMocks private ProductoRedisController redisController;

    private final ProductoResponse producto = ProductoResponse.builder().id(1).build();

    @Test
    @DisplayName("ProductoController delega en el servicio y responde con el status esperado")
    void productoController() {
        ProductoRequest request = new ProductoRequest();
        ProductoPatchRequest patch = new ProductoPatchRequest();
        when(productoService.obtenerTodosLosProductos()).thenReturn(List.of(producto));
        when(productoService.obtenerProductoPorId(1)).thenReturn(producto);
        when(productoService.reemplazarProducto(1, request)).thenReturn(producto);
        when(productoService.actualizarParcialProducto(1, patch)).thenReturn(producto);
        when(productoService.obtenerProductosXML()).thenReturn(List.of(producto));

        var creado = productoController.sincronizarProductos(UriComponentsBuilder.newInstance());
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creado.getHeaders().getLocation()).hasPath("/v1/productos");
        assertThat(productoController.obtenerTodosLosProductos().getBody()).containsExactly(producto);
        assertThat(productoController.obtenerProductoPorId(1).getBody()).isSameAs(producto);
        assertThat(productoController.reemplazarProducto(1, request).getBody()).isSameAs(producto);
        assertThat(productoController.actualizarParcialProducto(1, patch).getBody()).isSameAs(producto);
        assertThat(productoController.obtenerProductosXML().getBody()).containsExactly(producto);
        assertThat(productoController.eliminarProducto(1).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(productoService).sincronizarProductosDesdeGestoPago();
        verify(productoService).eliminarProducto(1);
    }

    @Test
    @DisplayName("ProductoRedisController consulta y sincroniza el catálogo")
    void redisController() {
        when(productoServiceRedis.getProductsRedis()).thenReturn(List.of(producto));
        when(productoServiceRedis.sincronizarCatalogo()).thenReturn(List.of(producto));

        assertThat(redisController.getProductsRedis().getBody()).containsExactly(producto);
        assertThat(redisController.sincronizarManual().getBody()).containsExactly(producto);
    }

    @Test
    @DisplayName("PersonaController delega crear, actualizar y eliminar")
    void personaController() {
        PersonaController controller = new PersonaController();
        ReflectionTestUtils.setField(controller, "personaService", personaService);
        GenericResponse ok = new GenericResponse();
        PersonasRequest request = new PersonasRequest();
        EliminaPersonaRequest elimina = new EliminaPersonaRequest();
        when(personaService.creaPersona(request)).thenReturn(null);
        when(personaService.actualizaPersona(request)).thenReturn(ok);
        when(personaService.eliminaPersona(elimina)).thenReturn(ok);

        assertThat(controller.crearUser(request).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.actualizUser(request).getBody()).isSameAs(ok);
        assertThat(controller.actualizUser(elimina).getBody()).isSameAs(ok);
    }

    @Test
    @DisplayName("El scheduler y el refresher sincronizan el catálogo y no propagan errores")
    void jobs() {
        ProductoCatalogScheduler scheduler = new ProductoCatalogScheduler(productoServiceRedis);
        CatalogoRefresher refresher = new CatalogoRefresher(productoServiceRedis);

        scheduler.renovarCatalogoDiario();
        refresher.refrescarEnSegundoPlano();
        verify(productoServiceRedis, times(2)).sincronizarCatalogo();

        when(productoServiceRedis.sincronizarCatalogo()).thenThrow(new RuntimeException("API caída"));
        scheduler.renovarCatalogoDiario();
        refresher.refrescarEnSegundoPlano();
        verify(productoServiceRedis, times(4)).sincronizarCatalogo();
    }
}
