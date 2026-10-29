package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.service.gestopago.TransformProduct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransformProductImpl implements TransformProduct {

    private final ProductoMapper productoMapper;

    @Override
    public List<ProductoXmlItem> transformProductXML(String xmlRequest) {
        if (xmlRequest == null || xmlRequest.isBlank()) {
            throw new RuntimeException(
                    "La respuesta XML recibida de GestoPago es nula o vacía. " +
                    "Verifique el token de autenticación o la API Key.");
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GestoPagoProductXmlResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            GestoPagoProductXmlResponse parsedXml =
                    (GestoPagoProductXmlResponse) unmarshaller.unmarshal(new StringReader(xmlRequest));

            if (parsedXml.getProductos() == null || parsedXml.getProductos().isEmpty()) {
                log.warn("La respuesta XML de GestoPago no contiene productos.");
                return Collections.emptyList();
            }

            log.info("XML parseado correctamente: {} productos encontrados.",
                    parsedXml.getProductos().size());

            return parsedXml.getProductos();

        } catch (JAXBException e) {
            log.error("Error al parsear el XML de GestoPago: {}", e.getMessage(), e);
            throw new RuntimeException("Error parseando la respuesta XML de GestoPago: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> transformProductEntities(List<ProductoXmlItem> itemList) {
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Producto> entities = productoMapper.xmlItemsToEntityList(itemList);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.setCreatedAt(now);
            }
        });
        return entities;
    }

    @Override
    public List<ProductoResponse> transformProductJSON(List<ProductoXmlItem> itemList) {
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductoResponse> responses = productoMapper.xmlItemsToResponseList(itemList);

        AtomicInteger index = new AtomicInteger(1);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        responses.forEach(p -> {
            p.setId(index.getAndIncrement());
            if (p.getCreatedAt() == null) {
                p.setCreatedAt(now);
            }
        });
        return responses;
    }
}
