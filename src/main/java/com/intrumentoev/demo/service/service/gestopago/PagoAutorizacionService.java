package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.pago.PagoAutorizacionResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PushTokenRequest;

import java.util.List;

public interface PagoAutorizacionService {

    void registrarDispositivo(AuthenticatedClient cliente, PushTokenRequest request);

    PagoAutorizacionResponse solicitarAutorizacion(AuthenticatedClient cliente, PagoRequest request);

    List<PagoAutorizacionResponse> listarPendientes(AuthenticatedClient cliente);

    PagoAutorizacionResponse consultarAutorizacion(AuthenticatedClient cliente, String idAutorizacion);

    PagoAutorizacionResponse aprobarAutorizacion(AuthenticatedClient cliente, String idAutorizacion);

    PagoAutorizacionResponse rechazarAutorizacion(AuthenticatedClient cliente, String idAutorizacion);

    PagoAutorizacionResponse cancelarAutorizacion(AuthenticatedClient cliente, String idAutorizacion);
}
