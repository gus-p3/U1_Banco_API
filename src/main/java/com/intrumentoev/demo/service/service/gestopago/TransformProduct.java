package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;

import java.util.List;

public interface TransformProduct {

    List<GestoPagoProductXmlResponse.ProductoXmlItem> transformProductXML(String xmlRequest);

    List<Producto> transformProductEntities(List<GestoPagoProductXmlResponse.ProductoXmlItem> itemList);

    List<ProductoResponse> transformProductJSON(List<GestoPagoProductXmlResponse.ProductoXmlItem> itemList);
}
