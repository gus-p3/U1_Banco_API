package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Valida el header Authorization: Bearer <JWT> de la petición actual para operaciones
 * sensibles (pagos). No depende solo del booleano global del ServerSessionManager.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BearerTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final ServerSessionManager serverSessionManager;

    public AuthenticatedClient validarParaPago(String authorizationHeader) {
        // a) El header debe venir en esta petición
        String token = extraerToken(authorizationHeader);

        // a) Firma y expiración
        Claims claims;
        try {
            claims = jwtTokenService.extractClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT rechazado para pago: {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }

        Long idClient = leerLong(claims, JwtTokenService.CLAIM_ID_CLIENT);
        if (idClient == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }

        // a) La sesión del servidor debe seguir activa
        if (!serverSessionManager.isUserLoggedIn()) {
            throw new BusinessException(ErrorCode.SESSION_INACTIVE);
        }

        // b) Solo se paga con un token emitido por login con huella
        String authMethod = claims.get(JwtTokenService.CLAIM_AUTH_METHOD, String.class);
        if (!AuthMethod.BIOMETRIC_HUELLA.name().equals(authMethod)) {
            log.warn("Pago rechazado para cliente {}: authMethod={}", idClient, authMethod);
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        // c) El cliente del token debe ser el mismo que tiene la sesión del servidor
        if (!idClient.equals(serverSessionManager.getActiveClientId())) {
            log.warn("Pago rechazado: idClient del token ({}) no coincide con la sesión activa", idClient);
            throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        return AuthenticatedClient.builder()
                .idClient(idClient)
                .idLogin(leerLong(claims, "idLogin"))
                .email(claims.getSubject())
                .authMethod(AuthMethod.BIOMETRIC_HUELLA)
                .build();
    }

    private String extraerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }
        return token;
    }

    // jjwt deserializa números pequeños como Integer aunque se hayan firmado como Long
    private Long leerLong(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.longValue() : null;
    }
}