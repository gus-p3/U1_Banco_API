package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaRequest;
import com.intrumentoev.demo.model.gestopago.referencia.ValidarReferenciaResponse;

import java.math.BigDecimal;

public interface ReferenciaValidacionService {

    ValidarReferenciaResponse validarReferencia(AuthenticatedClient cliente, ValidarReferenciaRequest request);

    boolean esReferenciaValidada(Long idClient, Integer idServicio, Integer idProducto, String referencia, BigDecimal monto);

    void registrarValidacion(Long idClient, Integer idServicio, Integer idProducto, String referencia, BigDecimal monto);

    void limpiarValidacionesLocales();
}

