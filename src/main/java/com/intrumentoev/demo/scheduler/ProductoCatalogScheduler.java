package com.intrumentoev.demo.scheduler;

import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductoCatalogScheduler {

    private final ProductoServiceRedis productoServiceRedis;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Mexico_City")
    public void renovarCatalogoDiario() {
        log.info("Iniciando actualizacion diaria de catalogo de productos");
        try {
            productoServiceRedis.sincronizarCatalogo();
            log.info("Catalogo de productos actualizado exitosamente");
        } catch (Exception ex) {
            log.error("Error al sincronizar catalogo: {}", ex.getMessage(), ex);
        }
    }
}
