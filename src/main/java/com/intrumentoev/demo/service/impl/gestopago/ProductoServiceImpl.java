package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;

import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService tokenService;
    private final TransformProduct transformProduct;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    // 1. POST /v1/productos -> Consume la API XML de GestoPago, mapea los primeros 20 productos y los guarda en BD
    @Override
    @Transactional
    public void sincronizarProductosDesdeGestoPago() {
        log.info("Obteniendo token activo para consumir getProductList.do");
        String token = tokenService.obtenerTokenVigente();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No se encontró token activo de GestoPago. Configure GESTOPAGO_BEARER_TOKEN con el token vigente.");
        }
        String bearerHeader = "Bearer " + token;
        log.info("Llamando a la API XML getProductList.do de GestoPago...");
        String xmlResponse = gestoPagoProductClient.getProductListXml(bearerHeader);

        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = transformProduct.transformProductXML(xmlResponse);
        if (items.isEmpty()) {
            log.warn("La respuesta XML de GestoPago no contiene productos.");
            return;
        }

        // Mapear los primeros 20 productos para pruebas
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items20 = items.stream()
                .limit(20)
                .collect(Collectors.toList());

        log.info("Mapeando y guardando los primeros {} productos en la BD...", items20.size());
        List<Producto> productosAGuardar = transformProduct.transformProductEntities(items20);

        List<Producto> guardados = productoRepository.saveAll(productosAGuardar);
        log.info("Se guardaron exitosamente {} productos en la BD", guardados.size());
    }

    // 2. GET /v1/productos
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerTodosLosProductos() {
        log.info("Consultando todos los productos de la BD");
        List<Producto> productos = productoRepository.findAll();
        return productoMapper.toResponseList(productos);
    }

    // 3. GET /v1/productos/{id}
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Integer id) {
        log.info("Buscando producto por ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return productoMapper.toResponse(producto);
    }

    // 4. PUT /v1/productos/{id} (Reemplazo completo)
    @Override
    @Transactional
    public ProductoResponse reemplazarProducto(Integer id, ProductoRequest request) {
        log.info("Reemplazando completo producto con ID: {}", id);
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        productoMapper.updateEntityFromRequest(request, existente);
        Producto actualizado = productoRepository.save(existente);
        return productoMapper.toResponse(actualizado);
    }

    // 5. PATCH /v1/productos/{id} (Actualización parcial)
    @Override
    @Transactional
    public ProductoResponse actualizarParcialProducto(Integer id, ProductoPatchRequest request) {
        log.info("Actualizando parcialmente producto con ID: {}", id);
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        productoMapper.updateEntityFromPatch(request, existente);
        Producto actualizado = productoRepository.save(existente);
        return productoMapper.toResponse(actualizado);
    }

    // 6. DELETE /v1/productos/{id}
    @Override
    @Transactional
    public void eliminarProducto(Integer id) {
        log.info("Eliminando producto con ID: {}", id);
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productoRepository.deleteById(id);
    }

    // 7. GET /v1/productos/xml
    @Override
    public List<ProductoResponse> obtenerProductosXML() {
        log.info("Obteniendo token activo para consumir getProductList.do");
        String token = tokenService.obtenerTokenVigente();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No se encontró token activo de GestoPago. Configure GESTOPAGO_BEARER_TOKEN con el token vigente.");
        }
        String bearerHeader = "Bearer " + token;
        log.info("Llamando a la API XML getProductList.do de GestoPago...");
        String xmlResponse = gestoPagoProductClient.getProductListXml(bearerHeader);

        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = transformProduct.transformProductXML(xmlResponse);
        return transformProduct.transformProductJSON(items);
    }
}
