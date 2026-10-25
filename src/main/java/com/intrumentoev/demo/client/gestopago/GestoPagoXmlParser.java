package com.intrumentoev.demo.client.gestopago;

import com.intrumentoev.demo.model.gestopago.pago.GestoPagoTxResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * Convierte el XML de sendTx/confirmTx en un GestoPagoTxResult.
 * Si el cuerpo no es un XML válido o no trae CODIGO regresa null (se trata como "sin respuesta").
 */
@Component
public class GestoPagoXmlParser {

    public GestoPagoTxResult parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml.trim())));

            String codigo = texto(doc, "CODIGO");
            if (codigo == null) {
                return null;
            }
            return GestoPagoTxResult.builder()
                    .codigo(codigo)
                    .texto(texto(doc, "TEXTO"))
                    .idTx(aLong(texto(doc, "ID_TX")))
                    .numAutorizacion(texto(doc, "NUM_AUTORIZACION"))
                    .comision(aDecimal(texto(doc, "COMISION")))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String texto(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long aLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal aDecimal(String value) {
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
