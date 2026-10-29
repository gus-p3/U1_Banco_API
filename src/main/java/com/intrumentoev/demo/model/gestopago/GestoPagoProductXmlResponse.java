package com.intrumentoev.demo.model.gestopago;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductXmlResponse {

    @XmlElement(name = "MENSAJE")
    private MensajeXml mensaje;

    @XmlElementWrapper(name = "PRODUCTOS")
    @XmlElement(name = "producto")
    private List<ProductoXmlItem> productos;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MensajeXml {
        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductoXmlItem {
        @XmlAttribute(name = "servicio")
        private String servicio;

        @XmlAttribute(name = "producto")
        private String producto;

        @XmlAttribute(name = "idServicio")
        private Integer idServicio;

        @XmlAttribute(name = "idProducto")
        private Integer idProducto;

        @XmlAttribute(name = "idCatTipoServicio")
        private Integer idCatTipoServicio;

        @XmlAttribute(name = "tipoFront")
        private Integer tipoFront;

        @XmlAttribute(name = "hasDigitoVerificador")
        private Boolean hasDigitoVerificador;

        @XmlAttribute(name = "precio")
        private String precio;

        @XmlAttribute(name = "showAyuda")
        private Boolean showAyuda;

        @XmlAttribute(name = "tipoReferencia")
        private String tipoReferencia;

        @XmlElement(name = "legend")
        private String legend;
    }
}
