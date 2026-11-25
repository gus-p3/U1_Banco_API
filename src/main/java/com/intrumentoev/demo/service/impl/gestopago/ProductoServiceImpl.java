package com.intrumentoev.demo.service.impl.gestopago;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio unificado de productos con integración real a GestoPago y manejo resiliente de respuestas sin catálogo
 * (ej. CODIGO=05 TEXTO=Origen Valido):
 * - Realiza las peticiones reales de autenticación y catálogo XML a GestoPago.
 * - Si GestoPago devuelve productos, los guarda en Redis y los sincroniza en PostgreSQL sin borrar existentes.
 * - Si GestoPago responde con un código sin productos (ej. CODIGO=05) o presenta error de red, maneja la respuesta
 *   sin fallar ni vaciar la caché/BD, respaldando el catálogo entre Redis y PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private static final String REDIS_CATALOGO_KEY = "catalogo:productos:gestopago:v2";
    private static final String LEGACY_REDIS_CATALOGO_KEY = "catalogo:productos:gestopago";

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService tokenService;
    private final TransformProduct transformProduct;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<ProductoResponse> sincronizarCatalogo() {
        log.info("Sincronizando catálogo contra la API de GestoPago (sin borrar productos previos)...");
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApiTolerante();
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(
                transformProduct.transformProductJSON(items));

        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
            guardarSinBorrarEnPostgres(items);
            return productos;
        }

        log.warn("La API de GestoPago no devolvió productos utilizables (ej. CODIGO=05); usando respaldo de Redis o PostgreSQL.");
        return obtenerDesdeRedisOPostgres();
    }

    @Override
    @Transactional
    public List<ProductoResponse> getProductsRedis() {
        List<ProductoResponse> existentes = obtenerDesdeRedisOPostgres();
        if (!existentes.isEmpty()) {
            return existentes;
        }

        log.info("No se encontraron productos en Redis ni en PostgreSQL. Consultando API de GestoPago...");
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApiTolerante();
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(
                transformProduct.transformProductJSON(items));

        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
            guardarSinBorrarEnPostgres(items);
            return productos;
        }

        log.warn("GestoPago devolvió 0 productos utilizables (ej. CODIGO=05) y no existe respaldo local previo.");
        return List.of();
    }

    // 1. POST /v1/productos -> Sincroniza desde GestoPago y si retorna 0 productos conserva/respalda Redis y Postgres
    @Override
    @Transactional
    public void sincronizarProductosDesdeGestoPago() {
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApiTolerante();
        if (items == null || items.isEmpty()) {
            log.warn("GestoPago no devolvió productos en sincronización manual (ej. CODIGO=05); se conserva y respalda lo existente en Redis y PostgreSQL.");
            obtenerDesdeRedisOPostgres();
            return;
        }

        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(
                transformProduct.transformProductJSON(items));
        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
        }
        guardarSinBorrarEnPostgres(items);
    }

    // 2. GET /v1/productos -> Obtiene productos de Postgres o Redis y si ambos están vacíos consulta GestoPago
    @Override
    @Transactional
    public List<ProductoResponse> obtenerTodosLosProductos() {
        List<Producto> enPostgres = productoRepository.findAll();
        if (enPostgres != null && !enPostgres.isEmpty()) {
            List<ProductoResponse> lista = productoMapper.filtrarYOrdenarPorTipoFront(
                    productoMapper.toResponseList(enPostgres));
            if (leerDesdeRedis().isEmpty() && !lista.isEmpty()) {
                guardarEnRedis(lista);
            }
            return lista;
        }

        List<ProductoResponse> enRedis = leerDesdeRedis();
        if (!enRedis.isEmpty()) {
            sincronizarRedisHaciaPostgresSinBorrar(enRedis);
            return enRedis;
        }

        return sincronizarCatalogo();
    }

    // 3. GET /v1/productos/{id}
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return productoMapper.toResponse(producto);
    }

    // 4. PUT /v1/productos/{id}
    @Override
    @Transactional
    public ProductoResponse reemplazarProducto(Integer id, ProductoRequest request) {
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        productoMapper.updateEntityFromRequest(request, existente);
        Producto actualizado = productoRepository.save(existente);
        return productoMapper.toResponse(actualizado);
    }

    // 5. PATCH /v1/productos/{id}
    @Override
    @Transactional
    public ProductoResponse actualizarParcialProducto(Integer id, ProductoPatchRequest request) {
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
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        productoRepository.deleteById(id);
    }

    // 7. GET /v1/productos/xml -> Consulta XML en vivo de GestoPago; si devuelve CODIGO=05 sin productos, usa respaldo
    @Override
    @Transactional
    public List<ProductoResponse> obtenerProductosXML() {
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApiTolerante();
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(
                transformProduct.transformProductJSON(items));
        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
            guardarSinBorrarEnPostgres(items);
            return productos;
        }
        return obtenerDesdeRedisOPostgres();
    }

    // -------------------------------------------------------- métodos internos

    private List<ProductoResponse> obtenerDesdeRedisOPostgres() {
        List<ProductoResponse> enRedis = leerDesdeRedis();
        if (!enRedis.isEmpty()) {
            asegurarRespaldoEnPostgres(enRedis);
            return enRedis;
        }
        return leerDesdePostgres();
    }

    private List<ProductoResponse> leerDesdeRedis() {
        if (redisTemplate == null || objectMapper == null) {
            return List.of();
        }
        try {
            Object cached = redisTemplate.opsForValue().get(REDIS_CATALOGO_KEY);
            if (cached != null) {
                List<ProductoResponse> enCache = objectMapper.convertValue(
                        cached, new TypeReference<List<ProductoResponse>>() {});
                if (enCache != null && !enCache.isEmpty()) {
                    return productoMapper.filtrarYOrdenarPorTipoFront(enCache);
                }
            }
            Object legacy = redisTemplate.opsForValue().get(LEGACY_REDIS_CATALOGO_KEY);
            if (legacy != null) {
                List<ProductoResponse> enLegacy = objectMapper.convertValue(
                        legacy, new TypeReference<List<ProductoResponse>>() {});
                if (enLegacy != null && !enLegacy.isEmpty()) {
                    List<ProductoResponse> filtrados = productoMapper.filtrarYOrdenarPorTipoFront(enLegacy);
                    guardarEnRedis(filtrados);
                    return filtrados;
                }
            }
        } catch (Exception ex) {
            log.warn("No fue posible consultar Redis: {}", ex.getMessage());
        }
        return List.of();
    }

    private List<ProductoResponse> leerDesdePostgres() {
        List<Producto> enPostgres = productoRepository.findAll();
        if (enPostgres != null && !enPostgres.isEmpty()) {
            List<ProductoResponse> responses = productoMapper.toResponseList(enPostgres);
            List<ProductoResponse> filtrados = productoMapper.filtrarYOrdenarPorTipoFront(responses);
            if (!filtrados.isEmpty()) {
                guardarEnRedis(filtrados);
            }
            return filtrados;
        }
        return List.of();
    }

    private boolean guardarEnRedis(List<ProductoResponse> productos) {
        if (redisTemplate == null || productos == null || productos.isEmpty()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(REDIS_CATALOGO_KEY, productos);
            return true;
        } catch (Exception ex) {
            log.warn("No fue posible guardar catálogo en Redis: {}", ex.getMessage());
            return false;
        }
    }

    private void asegurarRespaldoEnPostgres(List<ProductoResponse> enRedis) {
        try {
            if (productoRepository.count() == 0) {
                log.info("PostgreSQL vacío; respaldando {} productos desde Redis...", enRedis.size());
                sincronizarRedisHaciaPostgresSinBorrar(enRedis);
            }
        } catch (Exception ex) {
            log.warn("No fue posible respaldar productos de Redis hacia PostgreSQL: {}", ex.getMessage());
        }
    }

    private void guardarSinBorrarEnPostgres(List<GestoPagoProductXmlResponse.ProductoXmlItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Producto> nuevos = transformProduct.transformProductEntities(items);
        persistirSinBorrarEnPostgres(nuevos);
    }

    private void sincronizarRedisHaciaPostgresSinBorrar(List<ProductoResponse> desdeRedis) {
        if (desdeRedis == null || desdeRedis.isEmpty()) {
            return;
        }
        List<Producto> candidatos = productoMapper.responseListToEntityList(desdeRedis);
        persistirSinBorrarEnPostgres(candidatos);
    }

    private void persistirSinBorrarEnPostgres(List<Producto> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return;
        }
        List<Producto> existentes = productoRepository.findAll();
        if (existentes == null || existentes.isEmpty()) {
            productoRepository.saveAll(candidatos);
            log.info("{} productos guardados en PostgreSQL.", candidatos.size());
            return;
        }

        Map<String, Producto> mapaExistentes = existentes.stream()
                .filter(p -> p.getIdServicio() > 0 && p.getIdProducto() > 0)
                .collect(Collectors.toMap(
                        p -> p.getIdServicio() + ":" + p.getIdProducto(),
                        Function.identity(),
                        (a, b) -> a));

        for (Producto nuevo : candidatos) {
            String key = nuevo.getIdServicio() + ":" + nuevo.getIdProducto();
            Producto previo = mapaExistentes.get(key);
            if (previo != null) {
                nuevo.setId(previo.getId());
                if (previo.getCreatedAt() != null) {
                    nuevo.setCreatedAt(previo.getCreatedAt());
                }
            }
        }

        productoRepository.saveAll(candidatos);
        log.info("{} productos sincronizados en PostgreSQL sin borrar existentes.", candidatos.size());
    }

    private List<GestoPagoProductXmlResponse.ProductoXmlItem> consultarItemsDesdeApiTolerante() {
        try {
            return consultarItemsDesdeApi();
        } catch (Exception ex) {
            log.warn("Error técnico al consultar getProductList.do en GestoPago ({}); se usará respaldo local.", ex.getMessage());
            return List.of();
        }
    }

    private List<GestoPagoProductXmlResponse.ProductoXmlItem> consultarItemsDesdeApi() {
        String token = tokenService.obtenerTokenVigente();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "No se encontró token activo de GestoPago. Configure las credenciales de GestoPago o GESTOPAGO_BEARER_TOKEN.");
        }
        String xmlResponse;
        try {
            xmlResponse = gestoPagoProductClient.getProductListXml("Bearer " + token);
        } catch (FeignException ex) {
            if (ex.status() == 401 || ex.status() == 403) {
                log.warn("Token GestoPago rechazado en getProductList.do (HTTP {}); renovando y reintentando una vez...", ex.status());
                token = tokenService.renovarTokenForzado();
                xmlResponse = gestoPagoProductClient.getProductListXml("Bearer " + token);
            } else {
                throw ex;
            }
        }
        return transformProduct.transformProductXML(xmlResponse);
    }
}
