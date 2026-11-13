package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Valida el header Authorization: Bearer y lo convierte en un {@link AuthenticatedClient}.
 * Es completamente stateless: todo lo necesario viaja firmado en el token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BearerTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    /**
     * Operaciones de pago: solo se acepta un token emitido por login con huella.
     */
    public AuthenticatedClient validarParaPago(String authorizationHeader) {
        AuthenticatedClient cliente = validarCliente(authorizationHeader);
        if (cliente.getAuthMethod() != AuthMethod.BIOMETRIC_HUELLA) {
            log.warn("Pago rechazado para cliente {}: authMethod={}", cliente.getIdClient(), cliente.getAuthMethod());
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }
        return cliente;
    }

    /**
     * Valida firma y expiración del Bearer sin exigir un método de autenticación específico.
     */
    public AuthenticatedClient validarCliente(String authorizationHeader) {
        String token = extraerToken(authorizationHeader);
        Claims claims;
        try {
            claims = jwtTokenService.extractClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT rechazado: {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }
        return desdeClaims(claims);
    }

    private AuthenticatedClient desdeClaims(Claims claims) {
        Long idClient = leerLong(claims, JwtTokenService.CLAIM_ID_CLIENT);
        if (idClient == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }
        return AuthenticatedClient.builder()
                .idClient(idClient)
                .idLogin(leerLong(claims, JwtTokenService.CLAIM_ID_LOGIN))
                .email(claims.getSubject())
                .authMethod(leerAuthMethod(claims))
                .role(leerRole(claims))
                .build();
    }

    private AuthMethod leerAuthMethod(Claims claims) {
        String value = claims.get(JwtTokenService.CLAIM_AUTH_METHOD, String.class);
        try {
            return value == null ? AuthMethod.PASSWORD : AuthMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }
    }

    private Role leerRole(Claims claims) {
        String value = claims.get(JwtTokenService.CLAIM_ROLE, String.class);
        try {
            return value == null ? Role.CLIENT : Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        }
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
