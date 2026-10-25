package com.intrumentoev.demo.service.impl.gestopago;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.client.gestopago.GestoPagoProductClient;
import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;
import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoAuthResponse;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.repository.gestopago.ProductoRepository;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceRedisImpl implements ProductoServiceRedis {

    // Clave unificada en Redis para almacenar y consultar el catálogo de productos
    private static final String REDIS_CATALOGO_KEY = "catalogo:productos:gestopago";

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoAuthClient gestoPagoAuth;
    private final GestoPagoTokenRepository tokenRepository;
    private final TransformProduct transformProduct;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String password;

    @Override
    @Transactional
    public List<ProductoResponse> getProductsRedis() {
        try {
            Object cached = redisTemplate.opsForValue().get(REDIS_CATALOGO_KEY);
            if (cached != null) {
                log.info("Productos recuperados desde cache Redis.");
                List<ProductoResponse> enCache = objectMapper.convertValue(cached, new TypeReference<List<ProductoResponse>>() {});
                return productoMapper.filtrarYOrdenarPorTipoFront(enCache);
            }
        } catch (Exception ex) {
            log.warn("No fue posible consultar Redis: {}", ex.getMessage());
        }

        List<Producto> enPostgres = productoRepository.findAll();
        if (enPostgres != null && !enPostgres.isEmpty()) {
            log.info("Productos recuperados desde base de datos ({} registros).", enPostgres.size());
            List<ProductoResponse> responses = productoMapper.toResponseList(enPostgres);
            return productoMapper.filtrarYOrdenarPorTipoFront(responses);
        }

        log.info("No se encontraron productos en cache ni en base de datos. Consultando API...");
        return consumirApiYGuardar(false);
    }

    private List<ProductoResponse> consumirApiYGuardar(boolean cron) {
        String xmlResponse = consultarApiGestoPagoXml();
        List<GestoPagoProductXmlResponse.ProductoXmlItem> items = transformProduct.transformProductXML(xmlResponse);
        List<ProductoResponse> itemJSON = transformProduct.transformProductJSON(items);
        List<ProductoResponse> productos = productoMapper.filtrarYOrdenarPorTipoFront(itemJSON);

        if (cron) {
            try {
                redisTemplate.opsForValue().set(REDIS_CATALOGO_KEY, productos);
            } catch (Exception ex) {
                log.warn("Error al guardar en Redis: {}", ex.getMessage());
            }
            guardarEnPostgres(items);
            return null;
        } else {
            boolean guardadoEnRedis = false;
            try {
                redisTemplate.opsForValue().set(REDIS_CATALOGO_KEY, productos);
                guardadoEnRedis = true;
            } catch (Exception ex) {
                log.warn("No fue posible guardar en Redis: {}. Guardando en PostgreSQL...", ex.getMessage());
            }

            if (!guardadoEnRedis) {
                guardarEnPostgres(items);
            }
        }
        return productos;
    }

    private void guardarEnPostgres(List<GestoPagoProductXmlResponse.ProductoXmlItem> items) {
        List<Producto> entidades = transformProduct.transformProductEntities(items);
        productoRepository.saveAll(entidades);
        log.info("{} productos guardados en base de datos.", entidades.size());
    }

    @Override
    @Transactional
    public List<ProductoResponse> sincronizarCatalogo() {
        log.info("Iniciando sincronizacion de catalogo...");

        try {
            redisTemplate.delete(REDIS_CATALOGO_KEY);
        } catch (Exception ex) {
            log.warn("No se pudo limpiar Redis: {}", ex.getMessage());
        }

        productoRepository.deleteAll();
        return consumirApiYGuardar(true);
    }

    private String consultarApiGestoPagoXml() {
        String token = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .orElseGet(() -> {
                    GestoPagoAuthResponse auth = gestoPagoAuth.authenticate(idDistribuidor, codigoDispositivo, password);
                    return auth.getToken();
                });

        String bearerHeader = "Bearer " + token;
        return gestoPagoProductClient.getProductListXml(bearerHeader);
    }
}
