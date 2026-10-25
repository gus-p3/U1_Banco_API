package com.intrumentoev.demo.client;

import com.intrumentoev.demo.client.gestopago.GestoPagoXmlParser;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoTxResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoXmlParserTest {

    private final GestoPagoXmlParser parser = new GestoPagoXmlParser();

    @Test
    @DisplayName("sendTx 01: lee CODIGO/TEXTO de MENSAJE y ID_TX, NUM_AUTORIZACION, COMISION de RESPONSE")
    void sendTxAplicada() {
        String xml = "<RESPONSE><ID_TX>291437188</ID_TX><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>"
                + "<SALDO>1000.00</SALDO><COMISION>2.50</COMISION><SALDO_F>1000.00</SALDO_F><COMISION_F>2.50</COMISION_F>"
                + "<FECHA>20220101</FECHA><MONTO>50.00</MONTO>"
                + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Transaccion aplicada exitosamente</TEXTO><REFERENCIA>123</REFERENCIA></MENSAJE></RESPONSE>";

        GestoPagoTxResult r = parser.parse(xml);

        assertThat(r.getCodigo()).isEqualTo("01");
        assertThat(r.getTexto()).isEqualTo("Transaccion aplicada exitosamente");
        assertThat(r.getIdTx()).isEqualTo(291437188L);
        assertThat(r.getNumAutorizacion()).isEqualTo("100024988");
        assertThat(r.getComision()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("sendTx 02 (saldo insuficiente): sin ID_TX ni comisión")
    void sendTxSaldoInsuficiente() {
        GestoPagoTxResult r = parser.parse(
                "<RESPONSE><NUM_AUTORIZACION>0</NUM_AUTORIZACION><MENSAJE><CODIGO>02</CODIGO><TEXTO>Saldo insuficiente</TEXTO></MENSAJE></RESPONSE>");

        assertThat(r.getCodigo()).isEqualTo("02");
        assertThat(r.getIdTx()).isNull();
        assertThat(r.getComision()).isNull();
    }

    @Test
    @DisplayName("sendTx 82 (timeout)")
    void sendTxTimeout() {
        GestoPagoTxResult r = parser.parse(
                "<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>82</CODIGO><TEXTO>Timeout</TEXTO></MENSAJE></RESPONSE>");

        assertThat(r.getCodigo()).isEqualTo("82");
        assertThat(r.getNumAutorizacion()).isEqualTo("-1");
    }

    @Test
    @DisplayName("confirmTx 06 aplicada y 70 no aplicada (ID_TX dentro de MENSAJE)")
    void confirmTx() {
        GestoPagoTxResult aplicada = parser.parse("<RESPONSE><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>"
                + "<MENSAJE><CODIGO>06</CODIGO><TEXTO>aplicada exitosamente</TEXTO><ID_TX>291437188</ID_TX></MENSAJE></RESPONSE>");
        GestoPagoTxResult noAplicada = parser.parse("<RESPONSE><NUM_AUTORIZACION>0</NUM_AUTORIZACION>"
                + "<MENSAJE><CODIGO>70</CODIGO><TEXTO>La transaccion no fue aplicada</TEXTO><ID_TX>-1</ID_TX></MENSAJE></RESPONSE>");

        assertThat(aplicada.getCodigo()).isEqualTo("06");
        assertThat(aplicada.getIdTx()).isEqualTo(291437188L);
        assertThat(noAplicada.getCodigo()).isEqualTo("70");
        assertThat(noAplicada.getIdTx()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("Cuerpo vacío, no XML, sin CODIGO o con DOCTYPE regresa null (se trata como sin respuesta)")
    void respuestasInvalidas() {
        assertThat(parser.parse(null)).isNull();
        assertThat(parser.parse("  ")).isNull();
        assertThat(parser.parse("<html>502 Bad Gateway</html>")).isNull();
        assertThat(parser.parse("esto no es xml")).isNull();
        assertThat(parser.parse("<RESPONSE><MENSAJE><TEXTO>x</TEXTO></MENSAJE></RESPONSE>")).isNull();
        assertThat(parser.parse("<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><RESPONSE><MENSAJE><CODIGO>&x;</CODIGO></MENSAJE></RESPONSE>")).isNull();
    }
}
