package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Productos - Catálogo Redis", description = "Endpoints para consulta y sincronización de catálogo de productos en memoria caché Redis")
@RestController
@RequestMapping("/v1/productos/redis")
@RequiredArgsConstructor
public class ProductoRedisController {

    private final ProductoServiceRedis productoServiceRedis;

    @Operation(summary = "Obtener catálogo de productos desde caché Redis (con fallback a BD y API)")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getProductsRedis() {
        return ResponseEntity.ok(productoServiceRedis.getProductsRedis());
    }

    @Operation(summary = "Forzar sincronización manual del catálogo de productos hacia Redis y BD")
    @PostMapping("/sincronizar")
    public ResponseEntity<List<ProductoResponse>> sincronizarManual() {
        return ResponseEntity.ok(productoServiceRedis.sincronizarCatalogo());
    }
}
