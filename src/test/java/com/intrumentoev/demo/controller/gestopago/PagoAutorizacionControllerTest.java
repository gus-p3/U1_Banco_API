package com.intrumentoev.demo.controller.gestopago;

import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.auth.Role;
import com.intrumentoev.demo.model.gestopago.pago.EstadoAutorizacionPago;
import com.intrumentoev.demo.model.gestopago.pago.PagoAutorizacionResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PushTokenRequest;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.gestopago.PagoAutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoAutorizacionControllerTest {

    @Mock private BearerTokenValidator bearerTokenValidator;
    @Mock private PagoAutorizacionService autorizacionService;

    private PagoAutorizacionController controller;
    private AuthenticatedClient cliente;

    @BeforeEach
    void setUp() {
        controller = new PagoAutorizacionController(bearerTokenValidator, autorizacionService);
        cliente = AuthenticatedClient.builder()
                .idClient(1L).email("demo@banco.com")
                .authMethod(AuthMethod.BIOMETRIC_HUELLA).role(Role.CLIENT).build();
    }

    @Test
    void endpointsDeAutorizacion_delegaConValidacionCorrecta() {
        when(bearerTokenValidator.validarCliente("Bearer jwt")).thenReturn(cliente);
        when(bearerTokenValidator.validarParaPago("Bearer jwt", "confirm-huella")).thenReturn(cliente);

        PushTokenRequest pushReq = PushTokenRequest.builder().token("token-1234567890").plataforma("ANDROID").build();
        assertEquals(HttpStatus.NO_CONTENT, controller.registrarPushToken("Bearer jwt", pushReq).getStatusCode());
        verify(autorizacionService).registrarDispositivo(cliente, pushReq);

        PagoAutorizacionResponse resp = PagoAutorizacionResponse.builder()
                .idAutorizacion("id-1").estado(EstadoAutorizacionPago.PENDIENTE).build();
        PagoRequest pagoReq = PagoRequest.builder().tipoPago("RECARGA").build();
        when(autorizacionService.solicitarAutorizacion(cliente, pagoReq)).thenReturn(resp);
        assertEquals(resp, controller.solicitarAutorizacion("Bearer jwt", pagoReq).getBody());

        when(autorizacionService.listarPendientes(cliente)).thenReturn(List.of(resp));
        assertEquals(1, controller.listarPendientes("Bearer jwt").getBody().size());

        when(autorizacionService.consultarAutorizacion(cliente, "id-1")).thenReturn(resp);
        assertEquals(resp, controller.consultarAutorizacion("Bearer jwt", "id-1").getBody());

        when(autorizacionService.aprobarAutorizacion(cliente, "id-1")).thenReturn(resp);
        ResponseEntity<PagoAutorizacionResponse> approved = controller.aprobarAutorizacion("Bearer jwt", "confirm-huella", "id-1");
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        verify(bearerTokenValidator).validarParaPago("Bearer jwt", "confirm-huella");

        when(autorizacionService.rechazarAutorizacion(cliente, "id-1")).thenReturn(resp);
        assertEquals(HttpStatus.OK, controller.rechazarAutorizacion("Bearer jwt", "id-1").getStatusCode());

        when(autorizacionService.cancelarAutorizacion(cliente, "id-1")).thenReturn(resp);
        assertEquals(HttpStatus.OK, controller.cancelarAutorizacion("Bearer jwt", "id-1").getStatusCode());
    }
}
