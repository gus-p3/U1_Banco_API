package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Refresca el catálogo de GestoPago en segundo plano cuando un pago topa con un producto que no existe.
 * Es un bean aparte para que @Async funcione (las llamadas internas de la misma clase no pasan por el proxy).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogoRefresher {

    private final ProductoServiceRedis productoServiceRedis;

    @Async
    public void refrescarEnSegundoPlano() {
        try {
            log.info("Refrescando catálogo de GestoPago en segundo plano (producto no encontrado)");
            productoServiceRedis.sincronizarCatalogo();
        } catch (Exception e) {
            log.error("No fue posible refrescar el catálogo en segundo plano: {}", e.getMessage());
        }
    }
}
