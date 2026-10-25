package com.intrumentoev.demo.service.service.gestopago;

import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;

import java.util.Optional;

public interface GestoPagoTokenService {

    /** Renueva el token solo si no existe o ya superó la edad máxima (el token dura 24 h; se usa uno por día). */
    void renovarToken();

    /** Token guardado en gestopago_tokens; si no existe, lo solicita a GestoPago. */
    String obtenerTokenVigente();

    /** Solicita un token nuevo sin importar su edad (ante 403/token expirado). Regresa el token nuevo. */
    String renovarTokenForzado();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);
}
