package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;

import java.util.List;

public interface ProductoService {

    // 1. Sincronizar/Crear productos desde API XML de GestoPago (POST /v1/productos)
    void sincronizarProductosDesdeGestoPago();

    // 2. Listar todos los productos guardados en la BD (GET /v1/productos)
    List<ProductoResponse> obtenerTodosLosProductos();

    // 3. Buscar por ID (GET /v1/productos/{id})
    ProductoResponse obtenerProductoPorId(Integer id);

    // 4. Reemplazo completo (PUT /v1/productos/{id})
    ProductoResponse reemplazarProducto(Integer id, ProductoRequest request);

    // 5. Actualización parcial (PATCH /v1/productos/{id})
    ProductoResponse actualizarParcialProducto(Integer id, ProductoPatchRequest request);

    // 6. Eliminar por ID (DELETE /v1/productos/{id})
    void eliminarProducto(Integer id);

    // 7. Obtener productos desde el XML
    List<ProductoResponse> obtenerProductosXML();
}
