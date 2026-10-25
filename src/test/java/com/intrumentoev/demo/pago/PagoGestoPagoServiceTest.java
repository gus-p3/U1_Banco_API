package com.intrumentoev.demo.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.client.gestopago.GestoPagoCipher;
import com.intrumentoev.demo.entity.gestopago.PagoGestoPago;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;
import com.intrumentoev.demo.repository.account.AccountBalanceRepository;
import com.intrumentoev.demo.service.service.gestopago.PagoGestoPagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoGestoPagoServiceTest extends PagoIntegrationTestBase {

    @Autowired private PagoGestoPagoService pagoService;
    @Autowired private GestoPagoCipher cipher;
    @Autowired private AccountBalanceRepository balanceRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    private AuthenticatedClient cliente() {
        return AuthenticatedClient.builder().idClient(cuentaDemo().getIdClient()).authMethod(AuthMethod.BIOMETRIC_HUELLA).build();
    }

    private PagoRequest recarga() {
        return PagoRequest.builder().numCuenta(CUENTA_DEMO).idServicio(133).idProducto(582)
                .tipoPago("RECARGA").telefono(TELEFONO).build();
    }

    private PagoRequest servicio(String monto) {
        return PagoRequest.builder().numCuenta(CUENTA_DEMO).idServicio(79).idProducto(209)
                .tipoPago("SERVICIO").referencia("40425475190118187271").monto(new BigDecimal(monto)).build();
    }

    private PagoGestoPago pago(PagoResponse r) {
        return pagoRepository.findByUpc(r.getUpc()).orElseThrow();
    }

    private void envejecer(PagoGestoPago p) {
        p.setFechaEnvio(OffsetDateTime.now().minusMinutes(2));
        pagoRepository.save(p);
    }

    // ------------------------------------------------------------ sendTx

    @Test
    @DisplayName("sendTx 01: recarga APLICADA, se queda el descuento y se guarda ID_TX, autorización y comisión")
    void sendTxCodigo01() throws Exception {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "Transaccion aplicada", 291437188L, "100024988", "2.50"));

        PagoResponse r = pagoService.procesarPago(cliente(), recarga());

        assertThat(r.getCodigo()).isEqualTo(1);
        assertThat(r.getEstado()).isEqualTo(EstadoPago.APLICADO);
        assertThat(r.getCodigoGestoPago()).isEqualTo("01");
        assertThat(r.getNumAutorizacion()).isEqualTo("100024988");
        assertThat(r.getIdTx()).isEqualTo(291437188L);
        assertThat(r.getUpc()).hasSizeBetween(3, 15).matches("[A-Z0-9]+");
        assertThat(saldo()).isEqualByComparingTo("950.00"); // solo el monto; la comisión la absorbe el banco

        PagoGestoPago p = pago(r);
        assertThat(p.getEstado()).isEqualTo(EstadoPago.APLICADO);
        assertThat(p.getComision()).isEqualByComparingTo("2.50");
        assertThat(p.getResponseRaw()).contains("<CODIGO>01</CODIGO>");
        assertThat(p.getRequestCifrado()).isNotBlank();
        assertThat(balanceRepository.findByIdAccountOrderByCreatedAtDesc(cuentaDemo().getIdAccount()).get(0).getMovementType())
                .isEqualTo("PAGO_SERVICIO");

        // payload enviado: cifrado y descifrable; recarga sin referencia ni montoPago
        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(txClient).sendTx(eq("token-1"), enviado.capture());
        JsonNode json = mapper.readTree(cipher.descifrar(enviado.getValue()));
        assertThat(json.get("idProducto").asInt()).isEqualTo(582);
        assertThat(json.get("idServicio").asInt()).isEqualTo(133);
        assertThat(json.get("telefono").asText()).isEqualTo(TELEFONO);
        assertThat(json.get("unidad").asText()).isEqualTo("086");
        assertThat(json.get("upc").asText()).isEqualTo(r.getUpc());
        assertThat(json.get("horaLocal").asText()).matches("\\d{14}");
        assertThat(json.has("referencia")).isFalse();
        assertThat(json.has("montoPago")).isFalse();
        assertThat(p.getPayloadJson()).isEqualTo(cipher.descifrar(enviado.getValue()));
    }

    @Test
    @DisplayName("Servicio (tipoFront 2): telefono fijo 1111111111 más referencia y montoPago")
    void servicioPayload() throws Exception {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "ok", 1L, "10", "1.00"));

        PagoResponse r = pagoService.procesarPago(cliente(), servicio("150.00"));

        assertThat(r.getCodigo()).isEqualTo(1);
        assertThat(saldo()).isEqualByComparingTo("850.00");
        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(txClient).sendTx(eq("token-1"), enviado.capture());
        JsonNode json = mapper.readTree(cipher.descifrar(enviado.getValue()));
        assertThat(json.get("telefono").asText()).isEqualTo("1111111111");
        assertThat(json.get("referencia").asText()).isEqualTo("40425475190118187271");
        assertThat(json.get("montoPago").decimalValue()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("idCatTipoServicio 10/11: telefono fijo #1111111111")
    void telefonoCatalogo10() throws Exception {
        when(productoServiceRedis.getProductsRedis()).thenReturn(java.util.List.of(
                com.intrumentoev.demo.model.gestopago.ProductoResponse.builder().idServicio(79).idProducto(209)
                        .idCatTipoServicio(10).tipoFront(2).precio("0").build()));
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "ok", 1L, "10", "1.00"));

        pagoService.procesarPago(cliente(), servicio("20.00"));

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(txClient).sendTx(eq("token-1"), enviado.capture());
        assertThat(mapper.readTree(cipher.descifrar(enviado.getValue())).get("telefono").asText()).isEqualTo("#1111111111");
    }

    @Test
    @DisplayName("sendTx 02: RECHAZADO, se regresa el saldo y queda el movimiento REVERSO_PAGO")
    void sendTxCodigo02() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("02", "Saldo insuficiente del distribuidor", null, "0", null));

        PagoResponse r = pagoService.procesarPago(cliente(), servicio("150.00"));

        assertThat(r.getCodigo()).isZero();
        assertThat(r.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(r.getCodigoGestoPago()).isEqualTo("02");
        assertThat(r.getMensaje()).isEqualTo("Saldo insuficiente del distribuidor"); // TEXTO de GestoPago en mensaje
        assertThat(saldo()).isEqualByComparingTo("1000.00");
        assertThat(balanceRepository.findByIdAccountOrderByCreatedAtDesc(cuentaDemo().getIdAccount()).get(0).getMovementType())
                .isEqualTo("REVERSO_PAGO");
    }

    @Test
    @DisplayName("sendTx 82: POR_CONFIRMAR, NO se regresa el saldo; responde codigo 0 'en proceso' con el upc")
    void sendTxCodigo82() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));

        PagoResponse r = pagoService.procesarPago(cliente(), recarga());

        assertThat(r.getCodigo()).isZero();
        assertThat(r.getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(r.getMensaje()).isEqualTo("Pago en proceso de confirmación");
        assertThat(r.getUpc()).isNotBlank();
        assertThat(saldo()).isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("Timeout o sin respuesta: POR_CONFIRMAR y el saldo sigue descontado")
    void sinRespuesta() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.sinRespuesta("HttpTimeoutException"));

        PagoResponse r = pagoService.procesarPago(cliente(), recarga());

        assertThat(r.getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(saldo()).isEqualByComparingTo("950.00");
        assertThat(pago(r).getResponseRaw()).contains("SIN_RESPUESTA");
    }

    @Test
    @DisplayName("Se decide por el XML y no por el HTTP status: HTTP 500 con CODIGO 01 es APLICADO")
    void decideSegunXmlNoSegunHttp() {
        GestoPagoHttpResponse http500 = GestoPagoHttpResponse.conRespuesta(500,
                xml("01", "ok", 5L, "77", "1.00").body());
        when(txClient.sendTx(anyString(), anyString())).thenReturn(http500);

        assertThat(pagoService.procesarPago(cliente(), recarga()).getEstado()).isEqualTo(EstadoPago.APLICADO);
    }

    @Test
    @DisplayName("Respuesta que no es XML con CODIGO queda POR_CONFIRMAR")
    void respuestaNoXml() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.conRespuesta(502, "Bad Gateway"));

        assertThat(pagoService.procesarPago(cliente(), recarga()).getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
    }

    @Test
    @DisplayName("sendTx 06 (duplicada, ya aplicada): APLICADO")
    void sendTxCodigo06() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("06", "Duplicada", 9L, "55", "1.00"));

        PagoResponse r = pagoService.procesarPago(cliente(), recarga());

        assertThat(r.getEstado()).isEqualTo(EstadoPago.APLICADO);
        assertThat(saldo()).isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("403 token expirado: renueva el token UNA vez y reintenta")
    void renuevaTokenUnaVez() {
        when(txClient.sendTx(eq("token-1"), anyString())).thenReturn(GestoPagoHttpResponse.conRespuesta(403, "EXPIRED"));
        when(txClient.sendTx(eq("token-2"), anyString())).thenReturn(xml("01", "ok", 1L, "10", "1.00"));

        PagoResponse r = pagoService.procesarPago(cliente(), recarga());

        assertThat(r.getEstado()).isEqualTo(EstadoPago.APLICADO);
        verify(tokenService, times(1)).renovarTokenForzado();
        verify(txClient, times(2)).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Si tras renovar el token sigue el 403, no hay un tercer intento: POR_CONFIRMAR")
    void noReintentaMasDeUnaVez() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.conRespuesta(403, "EXPIRED"));

        assertThat(pagoService.procesarPago(cliente(), recarga()).getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        verify(tokenService, times(1)).renovarTokenForzado();
        verify(txClient, times(2)).sendTx(anyString(), anyString());
    }

    // ------------------------------------------------------- catálogo

    @Test
    @DisplayName("Producto fuera del catálogo: codigo 0, refresca el catálogo en segundo plano y NO descuenta ni llama a GestoPago")
    void productoNoExisteEnCatalogo() {
        PagoRequest req = recarga();
        req.setIdProducto(99999);

        PagoResponse r = pagoService.procesarPago(cliente(), req);

        assertThat(r.getCodigo()).isZero();
        assertThat(r.getMensaje()).isEqualTo("No se pudo realizar el pago: producto o servicio no disponible");
        verify(catalogoRefresher).refrescarEnSegundoPlano();
        verify(txClient, never()).sendTx(anyString(), anyString());
        assertThat(saldo()).isEqualByComparingTo("1000.00");
        assertThat(pagoRepository.count()).isZero();
    }

    @Test
    @DisplayName("GestoPago dice que el producto no existe (código configurado): mismo trato, saldo devuelto")
    void gestoPagoDiceProductoInexistente() {
        org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "codigosProductoInexistente", "35, 36");
        try {
            when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("36", "Producto no existe", null, "0", null));

            PagoResponse r = pagoService.procesarPago(cliente(), recarga());

            assertThat(r.getCodigo()).isZero();
            assertThat(r.getMensaje()).isEqualTo("No se pudo realizar el pago: producto o servicio no disponible");
            verify(catalogoRefresher).refrescarEnSegundoPlano();
            assertThat(saldo()).isEqualByComparingTo("1000.00");
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "codigosProductoInexistente", "");
        }
    }

    @Test
    @DisplayName("tipoPago debe coincidir con el producto del catálogo (422)")
    void tipoPagoNoCoincide() {
        PagoRequest recargaConServicio = PagoRequest.builder().numCuenta(CUENTA_DEMO).idServicio(79).idProducto(209)
                .tipoPago("RECARGA").telefono(TELEFONO).build();

        assertError(() -> pagoService.procesarPago(cliente(), recargaConServicio), ErrorCode.PAYMENT_PRODUCT_MISMATCH);
        assertError(() -> pagoService.procesarPago(cliente(), PagoRequest.builder().numCuenta(CUENTA_DEMO).idServicio(133)
                .idProducto(582).tipoPago("SERVICIO").referencia("123").monto(BigDecimal.TEN).build()), ErrorCode.PAYMENT_PRODUCT_MISMATCH);
    }

    @Test
    @DisplayName("En RECARGA el monto es el precio del producto; si viene otro, 422")
    void montoRecarga() {
        PagoRequest distinto = recarga();
        distinto.setMonto(new BigDecimal("99.00"));
        assertError(() -> pagoService.procesarPago(cliente(), distinto), ErrorCode.PAYMENT_PRODUCT_MISMATCH);

        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("01", "ok", 1L, "10", "1.00"));
        PagoRequest igual = recarga();
        igual.setMonto(new BigDecimal("50.00"));
        assertThat(pagoService.procesarPago(cliente(), igual).getEstado()).isEqualTo(EstadoPago.APLICADO);
    }

    // --------------------------------------------------- validaciones a–f

    @Test
    @DisplayName("Cuenta inexistente o de otro cliente: el MISMO 403 genérico (no revela si existe)")
    void propiedadDeLaCuenta() {
        AuthenticatedClient otro = AuthenticatedClient.builder().idClient(cuentaDemo().getIdClient() + 999).build();

        BusinessException ajena = capturar(() -> pagoService.procesarPago(otro, recarga()));
        PagoRequest inexistente = recarga();
        inexistente.setNumCuenta("0000000001");
        BusinessException noExiste = capturar(() -> pagoService.procesarPago(cliente(), inexistente));

        assertThat(ajena.getErrorCodeEnum()).isEqualTo(ErrorCode.PAYMENT_FORBIDDEN);
        assertThat(noExiste.getErrorCodeEnum()).isEqualTo(ErrorCode.PAYMENT_FORBIDDEN);
        assertThat(ajena.getMessage()).isEqualTo(noExiste.getMessage());
        assertThat(ajena.getHttpStatus().value()).isEqualTo(403);
        verify(txClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Cuenta INACTIVA → 422")
    void cuentaInactiva() {
        establecerCuenta(new BigDecimal("1000.00"), "INACTIVA");
        assertError(() -> pagoService.procesarPago(cliente(), recarga()), ErrorCode.PAYMENT_ACCOUNT_INACTIVE);
    }

    @Test
    @DisplayName("Saldo insuficiente → 422, sin registrar pago ni llamar a GestoPago")
    void saldoInsuficiente() {
        establecerCuenta(new BigDecimal("10.00"), "ACTIVA");

        assertError(() -> pagoService.procesarPago(cliente(), servicio("150.00")), ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(saldo()).isEqualByComparingTo("10.00");
        assertThat(pagoRepository.count()).isZero();
        verify(txClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Body inválido: telefono y referencia juntos, ninguno, o campos faltantes → 400")
    void bodyInvalido() {
        PagoRequest ambos = recarga();
        ambos.setReferencia("123");
        PagoRequest ninguno = recarga();
        ninguno.setTelefono(null);
        PagoRequest servicioSinMonto = servicio("10");
        servicioSinMonto.setMonto(null);
        PagoRequest tipoInvalido = recarga();
        tipoInvalido.setTipoPago("OTRO");
        PagoRequest telefonoCorto = recarga();
        telefonoCorto.setTelefono("123");

        for (PagoRequest malo : new PagoRequest[]{ambos, ninguno, servicioSinMonto, tipoInvalido, telefonoCorto}) {
            assertError(() -> pagoService.procesarPago(cliente(), malo), ErrorCode.INVALID_PAYMENT_REQUEST);
        }
        verify(txClient, never()).sendTx(anyString(), anyString());
    }

    // --------------------------------------------------------- confirmTx

    @Test
    @DisplayName("Job confirmTx 06: el POR_CONFIRMAR pasa a APLICADO con el MISMO payload cifrado")
    void confirmTxAplicada() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        PagoResponse r = pagoService.procesarPago(cliente(), recarga());
        envejecer(pago(r));
        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(txClient).sendTx(anyString(), enviado.capture());

        when(txClient.confirmTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.conRespuesta(200,
                "<RESPONSE><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION><MENSAJE><CODIGO>06</CODIGO>"
                        + "<TEXTO>aplicada exitosamente</TEXTO><ID_TX>291437188</ID_TX></MENSAJE></RESPONSE>"));
        pagoService.confirmarPendientes();

        PagoGestoPago p = pago(r);
        assertThat(p.getEstado()).isEqualTo(EstadoPago.APLICADO);
        assertThat(p.getIdTx()).isEqualTo(291437188L);
        assertThat(p.getNumAutorizacion()).isEqualTo("100024988");
        assertThat(p.getIntentosConfirmacion()).isEqualTo(1);
        assertThat(p.getResponseRaw()).contains("<CODIGO>82</CODIGO>").contains("<CODIGO>06</CODIGO>"); // bitácora completa
        assertThat(saldo()).isEqualByComparingTo("950.00");
        verify(txClient).confirmTx(eq("token-1"), eq(enviado.getValue())); // mismo upc y horaLocal
    }

    @Test
    @DisplayName("Job confirmTx 70 (no aplicada): RECHAZADO y se regresa el saldo")
    void confirmTxNoAplicada() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.sinRespuesta("timeout"));
        PagoResponse r = pagoService.procesarPago(cliente(), servicio("150.00"));
        envejecer(pago(r));
        assertThat(saldo()).isEqualByComparingTo("850.00");

        when(txClient.confirmTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.conRespuesta(200,
                "<RESPONSE><NUM_AUTORIZACION>0</NUM_AUTORIZACION><MENSAJE><CODIGO>70</CODIGO>"
                        + "<TEXTO>La transaccion no fue aplicada</TEXTO><ID_TX>-1</ID_TX></MENSAJE></RESPONSE>"));
        pagoService.confirmarPendientes();

        assertThat(pago(r).getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
        assertThat(balanceRepository.findByIdAccountOrderByCreatedAtDesc(cuentaDemo().getIdAccount()).get(0).getMovementType())
                .isEqualTo("REVERSO_PAGO");
    }

    @Test
    @DisplayName("Job confirmTx sin respuesta: sigue POR_CONFIRMAR y se reintenta en la siguiente vuelta")
    void confirmTxSinRespuesta() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        PagoResponse r = pagoService.procesarPago(cliente(), recarga());
        envejecer(pago(r));
        when(txClient.confirmTx(anyString(), anyString())).thenReturn(GestoPagoHttpResponse.sinRespuesta("timeout"));

        pagoService.confirmarPendientes();
        pagoService.confirmarPendientes();

        PagoGestoPago p = pago(r);
        assertThat(p.getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(p.getIntentosConfirmacion()).isEqualTo(2);
        assertThat(saldo()).isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("Job: un código de confirmTx desconocido NO devuelve el saldo (queda para reintento)")
    void confirmTxCodigoDesconocido() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        PagoResponse r = pagoService.procesarPago(cliente(), recarga());
        envejecer(pago(r));
        when(txClient.confirmTx(anyString(), anyString())).thenReturn(xml("99", "Error interno", null, "0", null));

        pagoService.confirmarPendientes();

        assertThat(pago(r).getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(saldo()).isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("Job: no confirma antes de 62 s ni después de 24 h (revisión manual: ni devuelve ni cobra)")
    void ventanaDeConfirmacion() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        PagoResponse reciente = pagoService.procesarPago(cliente(), recarga());
        PagoResponse viejo = pagoService.procesarPago(cliente(), recarga());
        PagoGestoPago pViejo = pago(viejo);
        pViejo.setFechaEnvio(OffsetDateTime.now().minusHours(25));
        pagoRepository.save(pViejo);

        pagoService.confirmarPendientes();
        pagoService.reportarVencidos(); // solo escribe en el log

        verify(txClient, never()).confirmTx(anyString(), anyString());
        assertThat(pago(reciente).getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(pago(viejo).getEstado()).isEqualTo(EstadoPago.POR_CONFIRMAR);
        assertThat(saldo()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Job: un PENDIENTE huérfano (la app cayó tras descontar) se resuelve con confirmTx")
    void pendienteHuerfano() {
        when(txClient.sendTx(anyString(), anyString())).thenReturn(xml("82", "Timeout", null, "-1", null));
        PagoResponse r = pagoService.procesarPago(cliente(), recarga());
        PagoGestoPago p = pago(r);
        p.setEstado(EstadoPago.PENDIENTE);
        p.setFechaEnvio(OffsetDateTime.now().minusMinutes(10));
        pagoRepository.save(p);
        when(txClient.confirmTx(anyString(), anyString())).thenReturn(xml("70", "No aplicada", -1L, "0", null));

        pagoService.confirmarPendientes();

        assertThat(pago(r).getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    // ------------------------------------------------------------ helpers

    private BusinessException capturar(Runnable r) {
        return (BusinessException) org.assertj.core.api.Assertions.catchThrowable(r::run);
    }

    private void assertError(Runnable r, ErrorCode esperado) {
        assertThatThrownBy(r::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCodeEnum")
                .isEqualTo(esperado);
    }
}
