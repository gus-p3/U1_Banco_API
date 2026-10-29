package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.gestopago.ProductoResponse;

import java.util.List;

public interface ProductoServiceRedis {
    List<ProductoResponse> getProductsRedis();
    List<ProductoResponse> sincronizarCatalogo();
}
