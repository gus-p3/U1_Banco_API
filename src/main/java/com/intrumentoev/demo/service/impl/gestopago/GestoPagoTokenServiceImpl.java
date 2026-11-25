package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;
import com.intrumentoev.demo.mapper.gestopago.GestoPagoTokenMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoAuthResponse;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor:#{null}}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:}")
    private String password;

    @Value("${gestopago.auth.token-max-age-hours:23}")
    private long tokenMaxAgeHours;

    @Value("${gestopago.auth.bearer-token:}")
    private String staticBearerToken;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @PostConstruct
    public void validarConfiguracion() {
        if (staticBearerToken != null && !staticBearerToken.isBlank()) {
            return;
        }
        if (idDistribuidor == null) {
            throw new IllegalStateException(
                    "Falta configurar la propiedad obligatoria gestopago.auth.id-distribuidor (variable de entorno GESTOPAGO_ID_DISTRIBUIDOR)");
        }
        if (codigoDispositivo == null || codigoDispositivo.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la propiedad obligatoria gestopago.auth.codigo-dispositivo (variable de entorno GESTOPAGO_CODIGO_DISPOSITIVO)");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la propiedad obligatoria gestopago.auth.password (variable de entorno GESTOPAGO_PASSWORD)");
        }
    }

    @Override
    public synchronized void renovarToken() {
        if (staticBearerToken != null && !staticBearerToken.isBlank()) {
            return;
        }
        Optional<GestoPagoToken> actual = obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (actual.isPresent() && !estaVencido(actual.get())) {
            log.debug("Token GestoPago vigente, no se renueva");
            return;
        }
        try {
            solicitarYGuardarToken();
        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized String obtenerTokenVigente() {
        if (staticBearerToken != null && !staticBearerToken.isBlank()) {
            return staticBearerToken;
        }
        Optional<GestoPagoToken> actual = obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (actual.isPresent() && !estaVencido(actual.get())) {
            return actual.get().getToken();
        }
        return solicitarYGuardarToken();
    }

    @Override
    public synchronized String renovarTokenForzado() {
        if (staticBearerToken != null && !staticBearerToken.isBlank()) {
            return staticBearerToken;
        }
        log.warn("Renovación forzada del token GestoPago (token expirado o rechazado con 403)");
        return solicitarYGuardarToken();
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    private boolean estaVencido(GestoPagoToken token) {
        LocalDateTime ultima = token.getFechaActualizacion() != null ? token.getFechaActualizacion() : token.getFechaCreacion();
        return token.getToken() == null || token.getToken().isBlank() || ultima == null
                || Duration.between(ultima, LocalDateTime.now()).toHours() >= tokenMaxAgeHours;
    }

    private String solicitarYGuardarToken() {
        log.info("Solicitando token GestoPago para distribuidor={}", idDistribuidor);
        GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            throw new IllegalStateException("La respuesta de GestoPago no contiene token");
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(existing -> {
                    tokenMapper.updateEntity(response, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    nuevo.setActivo(true);
                    return nuevo;
                });

        tokenEntity.setFechaActualizacion(LocalDateTime.now());
        tokenRepository.save(tokenEntity);
        log.info("Token GestoPago guardado correctamente");
        return tokenEntity.getToken();
    }
}
