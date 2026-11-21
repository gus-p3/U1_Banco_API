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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 1. Sincroniza productos desde la API de GestoPago SIN borrar los anteriores en Redis ni en Postgres.
     * 2. Si la API no devuelve productos (ej. CODIGO=05) o falla, agarra los de Redis y si no hay, los de Postgres.
     */
    @Override
    @Transactional
    public List<ProductoResponse> sincronizarCatalogo() {
        log.info("Iniciando sincronizacion de catalogo (sin borrar productos anteriores)...");
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApi();
        List<ProductoResponse> itemJSON = transformProduct.transformProductJSON(items);
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(itemJSON);

        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
            guardarSinBorrarEnPostgres(items);
            return productos;
        }

        log.warn("La API de GestoPago no devolvió productos; tomando respaldo de Redis o PostgreSQL.");
        return obtenerDesdeRedisOPostgres();
    }

    /**
     * Obtiene el catálogo:
     * - Primero intenta desde Redis ( o Postgres si no está en Redis).
     * - Si tampoco hay en Redis ni en Postgres, intenta sincronizar desde la API de GestoPago.
     */
    @Override
    @Transactional
    public List<ProductoResponse> getProductsRedis() {
        List<ProductoResponse> existentes = obtenerDesdeRedisOPostgres();
        if (!existentes.isEmpty()) {
            return existentes;
        }

        log.info("No se encontraron productos en Redis ni en PostgreSQL. Consultando API de GestoPago...");
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApi();
        List<ProductoResponse> itemJSON = transformProduct.transformProductJSON(items);
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(itemJSON);

        if (productos != null && !productos.isEmpty()) {
            boolean enRedis = guardarEnRedis(productos);
            if (!enRedis) {
                guardarSinBorrarEnPostgres(items);
            }
            return productos;
        }

        log.warn("GestoPago devolvió 0 productos utilizables.");
        return List.of();
    }

    // 1. POST /v1/productos -> Sincroniza desde GestoPago sin borrar los anteriores
    @Override
    @Transactional
    public void sincronizarProductosDesdeGestoPago() {
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApi();
        if (items == null || items.isEmpty()) {
            log.warn("La respuesta XML de GestoPago no contiene productos; se conservan los de Redis y Postgres.");
            return;
        }

        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(
                transformProduct.transformProductJSON(items));
        if (productos != null && !productos.isEmpty()) {
            guardarEnRedis(productos);
        }

        guardarSinBorrarEnPostgres(items);
    }

    // 2. GET /v1/productos -> Si hay en Redis o Postgres los devuelve
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerTodosLosProductos() {
        log.info("Consultando productos (Postgres / Redis)");
        List<Producto> enPostgres = productoRepository.findAll();
        if (enPostgres != null && !enPostgres.isEmpty()) {
            return productoMapper.toResponseList(enPostgres);
        }
        return leerDesdeRedis();
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

    // 7. GET /v1/productos/xml -> Consulta API XML; si no trae productos, agarra los de Redis y si no los de Postgres
    @Override
    public List<ProductoResponse> obtenerProductosXML() {
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = consultarItemsDesdeApi();
        List<ProductoResponse> resultado = transformProduct.transformProductJSON(items);
        if (resultado != null && !resultado.isEmpty()) {
            return resultado;
        }
        log.warn("La API XML de GestoPago no trajo productos; tomando respaldo de Redis o PostgreSQL.");
        return obtenerDesdeRedisOPostgres();
    }

    // -------------------------------------------------------- métodos internos

    private List<ProductoResponse> obtenerDesdeRedisOPostgres() {
        List<ProductoResponse> enRedis = leerDesdeRedis();
        if (!enRedis.isEmpty()) {
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
                    log.info("Productos recuperados desde cache Redis ({} registros).", enCache.size());
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
                    log.info("Productos recuperados desde clave legacy de Redis ({} registros).", filtrados.size());
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
            log.info("Productos recuperados desde PostgreSQL ({} registros).", enPostgres.size());
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
            log.warn("No fue posible guardar en Redis: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Guarda o actualiza los productos en PostgreSQL SIN borrar los productos anteriores.
     */
    private void guardarSinBorrarEnPostgres(List<GestoPagoProductXmlResponse.ProductoXmlItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Producto> nuevos = transformProduct.transformProductEntities(items);
        if (nuevos == null || nuevos.isEmpty()) {
            return;
        }

        List<Producto> existentes = productoRepository.findAll();
        if (existentes == null || existentes.isEmpty()) {
            productoRepository.saveAll(nuevos);
            log.info("{} productos guardados en PostgreSQL.", nuevos.size());
            return;
        }

        Map<String, Producto> mapaExistentes = existentes.stream()
                .filter(p -> p.getIdServicio() > 0 && p.getIdProducto() > 0)
                .collect(Collectors.toMap(
                        p -> p.getIdServicio() + ":" + p.getIdProducto(),
                        Function.identity(),
                        (a, b) -> a));

        for (Producto nuevo : nuevos) {
            String key = nuevo.getIdServicio() + ":" + nuevo.getIdProducto();
            Producto previo = mapaExistentes.get(key);
            if (previo != null) {
                nuevo.setId(previo.getId());
                if (previo.getCreatedAt() != null) {
                    nuevo.setCreatedAt(previo.getCreatedAt());
                }
            }
        }

        productoRepository.saveAll(nuevos);
        log.info("{} productos sincronizados en PostgreSQL sin borrar existentes.", nuevos.size());
    }

    private List<GestoPagoProductXmlResponse.ProductoXmlItem> consultarItemsDesdeApi() {
        String token = tokenService.obtenerTokenVigente();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "No se encontró token activo de GestoPago. Configure GESTOPAGO_BEARER_TOKEN con el token vigente.");
        }
        String bearerHeader = "Bearer " + token;
        String xmlResponse = gestoPagoProductClient.getProductListXml(bearerHeader);
        return transformProduct.transformProductXML(xmlResponse);
    }
}
