package com.intrumentoev.demo.service.gestopago;

import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.mapper.gestopago.ProductoMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse.ProductoXmlItem;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.service.impl.gestopago.TransformProductImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TransformProductTest {

    @Spy
    private ProductoMapper productoMapper = Mappers.getMapper(ProductoMapper.class);

    @InjectMocks
    private TransformProductImpl transformProduct;

    private final String validXml = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<RESPONSE>"
            + "  <MENSAJE><CODIGO>01</CODIGO><TEXTO>Exito</TEXTO></MENSAJE>"
            + "  <PRODUCTOS>"
            + "    <producto servicio='TELCEL' producto='Paquete 100' idServicio='133' idProducto='582' idCatTipoServicio='1' tipoFront='1' hasDigitoVerificador='false' precio='100.00'/>"
            + "    <producto servicio='CFE' producto='Luz CFE' idServicio='79' idProducto='209' idCatTipoServicio='2' tipoFront='2' hasDigitoVerificador='true' precio='0.00'/>"
            + "  </PRODUCTOS>"
            + "</RESPONSE>";

    @Test
    @DisplayName("Parseo de XML válido extrae la lista de productos")
    void testTransformProductXML_Exitoso() {
        List<ProductoXmlItem> items = transformProduct.transformProductXML(validXml);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getServicio()).isEqualTo("TELCEL");
        assertThat(items.get(0).getIdProducto()).isEqualTo(582);
    }

    @Test
    @DisplayName("XML vacío o nulo lanza RuntimeException")
    void testTransformProductXML_VacioOLanzaExcepcion() {
        assertThatThrownBy(() -> transformProduct.transformProductXML(null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> transformProduct.transformProductXML("  "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("XML con formato roto lanza RuntimeException de parseo")
    void testTransformProductXML_InvalidoLanzaExcepcion() {
        assertThatThrownBy(() -> transformProduct.transformProductXML("<MAL_XML>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error parseando");
    }

    @Test
    @DisplayName("Transformación a entidades maneja listas vacías y nulas")
    void testTransformProductEntities_Vacio() {
        assertThat(transformProduct.transformProductEntities(null)).isEmpty();
        assertThat(transformProduct.transformProductEntities(Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("Transformación a entidades asigna fecha de creación")
    void testTransformProductEntities_Exitoso() {
        ProductoXmlItem item = new ProductoXmlItem();
        item.setServicio("TELCEL");
        item.setProducto("Paquete 100");
        item.setIdServicio(133);
        item.setIdProducto(582);
        item.setPrecio("100.00");

        List<Producto> entities = transformProduct.transformProductEntities(List.of(item));
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Transformación a JSON maneja listas vacías y asigna índices")
    void testTransformProductJSON() {
        assertThat(transformProduct.transformProductJSON(null)).isEmpty();

        ProductoXmlItem item = new ProductoXmlItem();
        item.setServicio("CFE");
        item.setIdProducto(209);

        List<ProductoResponse> responses = transformProduct.transformProductJSON(List.of(item));
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1);
        assertThat(responses.get(0).getCreatedAt()).isNotNull();
    }
}
