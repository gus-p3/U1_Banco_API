package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapperImpl;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.impl.gestopago.TransformProductImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformProductImplTest {

    static final String XML = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE><PRODUCTOS>"
            + "<producto servicio=\"Telcel\" producto=\"Recarga 50\" idServicio=\"133\" idProducto=\"582\" "
            + "idCatTipoServicio=\"1\" tipoFront=\"1\" precio=\"50.00\"><legend>Recarga de tiempo aire Telcel</legend></producto>"
            + "<producto servicio=\"CFE\" producto=\"Luz\" idServicio=\"79\" idProducto=\"209\" "
            + "idCatTipoServicio=\"2\" tipoFront=\"0\"><legend>Pago de recibo CFE</legend></producto>"
            + "</PRODUCTOS></RESPONSE>";

    private final TransformProductImpl transform = new TransformProductImpl(new ProductoMapperImpl());

    @Test
    @DisplayName("transformProductXML parsea los productos del XML incluyendo legend")
    void parseaXml() {
        List<ProductoXmlItem> items = transform.transformProductXML(XML);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getServicio()).isEqualTo("Telcel");
        assertThat(items.get(0).getIdProducto()).isEqualTo(582);
        assertThat(items.get(0).getLegend()).isEqualTo("Recarga de tiempo aire Telcel");
    }

    @Test
    @DisplayName("XML nulo, vacío o mal formado lanza RuntimeException")
    void xmlInvalido() {
        assertThatThrownBy(() -> transform.transformProductXML(null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> transform.transformProductXML("  ")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> transform.transformProductXML("<no-es-xml"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("parseando");
    }

    @Test
    @DisplayName("XML sin productos devuelve lista vacía")
    void sinProductos() {
        assertThat(transform.transformProductXML("<RESPONSE><MENSAJE><CODIGO>01</CODIGO></MENSAJE></RESPONSE>")).isEmpty();
    }

    @Test
    @DisplayName("transformProductEntities asigna createdAt y tolera listas vacías")
    void entidades() {
        List<Producto> entidades = transform.transformProductEntities(transform.transformProductXML(XML));

        assertThat(entidades).hasSize(2).allSatisfy(p -> assertThat(p.getCreatedAt()).isNotNull());
        assertThat(transform.transformProductEntities(null)).isEmpty();
        assertThat(transform.transformProductEntities(List.of())).isEmpty();
    }

    @Test
    @DisplayName("transformProductJSON numera los productos, asigna createdAt, legend y categoria")
    void json() {
        List<ProductoResponse> respuestas = transform.transformProductJSON(transform.transformProductXML(XML));

        assertThat(respuestas).extracting(ProductoResponse::getId).containsExactly(1, 2);
        assertThat(respuestas).allSatisfy(p -> assertThat(p.getCreatedAt()).isNotNull());
        assertThat(respuestas.get(0).getLegend()).isEqualTo("Recarga de tiempo aire Telcel");
        assertThat(respuestas.get(0).getCategoria()).isEqualTo("Recargas");
        assertThat(respuestas.get(0).getLogoUrl()).isEqualTo("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/133.png");
        assertThat(respuestas.get(1).getCategoria()).isEqualTo("Servicios");
        assertThat(respuestas.get(1).getLogoUrl()).isEqualTo("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/79.png");
        assertThat(transform.transformProductJSON(null)).isEmpty();
        assertThat(transform.transformProductJSON(List.of())).isEmpty();
    }

    @Test
    @DisplayName("traducirCategoria mapea idCatTipoServicio y tipoFront a categorías claras en español")
    void categorias() {
        ProductoMapperImpl mapper = new ProductoMapperImpl();
        assertThat(mapper.traducirCategoria(1, 1)).isEqualTo("Recargas");
        assertThat(mapper.traducirCategoria(2, 2)).isEqualTo("Servicios");
        assertThat(mapper.traducirCategoria(3, 2)).isEqualTo("Telecomunicaciones y TV");
        assertThat(mapper.traducirCategoria(4, 2)).isEqualTo("Agua y Gobierno");
        assertThat(mapper.traducirCategoria(10, 4)).isEqualTo("Pines y Entretenimiento");
        assertThat(mapper.traducirCategoria(11, 4)).isEqualTo("Pines y Entretenimiento");
        assertThat(mapper.traducirCategoria(99, 1)).isEqualTo("Recargas");
        assertThat(mapper.traducirCategoria(99, 2)).isEqualTo("Servicios");
        assertThat(mapper.traducirCategoria(99, 4)).isEqualTo("Pines y Entretenimiento");
        assertThat(mapper.traducirCategoria(null, null)).isEqualTo("Otros servicios");
    }
}
