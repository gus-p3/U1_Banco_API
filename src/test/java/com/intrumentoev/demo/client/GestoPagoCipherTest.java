package com.intrumentoev.demo.client;

import com.intrumentoev.demo.client.gestopago.GestoPagoCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoCipherTest {

    // Par de prueba publicado en la documentación de GestoPago ("Security on payload")
    private static final String KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    // "Encryption Expected Result" de la documentación y el texto plano que lo produce
    private static final String PLANO_DOC =
            "{'idProducto':582,'idServicio':133,'telefono':'5555555551','horaLocal':'20220101235900','upc':'test_UPC_83_182'}";
    private static final String CIFRADO_DOC =
            "+LWZ7icI60/DAI8ggv/SpDvJpFYeaYOZ5J7hPGK7g/87r9PRJfgFRBoBLYpBxW33ouvuwQPnhhKHXqzbiqLhMSH4gSH8SMvW6PvHfeklNfTrZnbgWFWDPSy/nqFCP0B/gKavafHG0tW5UP4KPwyhECZK2pLT+I3qYAA85uQaAPQ=";

    private GestoPagoCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = new GestoPagoCipher(KEY, IV);
    }

    @Test
    @DisplayName("Cifrar el texto de la documentación produce exactamente el resultado esperado")
    void cifraIgualQueLaDocumentacion() {
        assertThat(cipher.cifrar(PLANO_DOC)).isEqualTo(CIFRADO_DOC);
    }

    @Test
    @DisplayName("Descifrar el resultado de la documentación regresa el texto original")
    void descifraElEjemploDeLaDocumentacion() {
        assertThat(cipher.descifrar(CIFRADO_DOC)).isEqualTo(PLANO_DOC);
    }

    @Test
    @DisplayName("Ida y vuelta conserva acentos y ñ (UTF-8)")
    void idaYVueltaUtf8() {
        String json = "{\"referencia\":\"Peña Ñuñez — pago añual\",\"montoPago\":150.5}";
        assertThat(cipher.descifrar(cipher.cifrar(json))).isEqualTo(json);
    }

    @Test
    @DisplayName("Llave vacía, no Base64 o de longitud incorrecta impide crear el cifrador")
    void llaveInvalida() {
        assertThatThrownBy(() -> new GestoPagoCipher("", IV))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gestopago.crypto.secret-key");
        assertThatThrownBy(() -> new GestoPagoCipher("no-es-base64!!", IV))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> new GestoPagoCipher(IV, IV))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("IV de longitud incorrecta impide crear el cifrador")
    void ivInvalido() {
        assertThatThrownBy(() -> new GestoPagoCipher(KEY, KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gestopago.crypto.iv")
                .hasMessageContaining("16 bytes");
    }

    @Test
    @DisplayName("Un texto cifrado truncado o que no es Base64 no se puede descifrar")
    void cifradoInvalido() {
        // quitar 4 caracteres Base64 (3 bytes) deja una longitud que no es múltiplo del bloque de 16
        assertThatThrownBy(() -> cipher.descifrar(CIFRADO_DOC.substring(0, CIFRADO_DOC.length() - 8)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.descifrar("###"))
                .isInstanceOf(IllegalStateException.class);
    }
}
