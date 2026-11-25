package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida el header Authorization: Bearer y lo convierte en un {@link AuthenticatedClient}.
 * Para operaciones de pago exige además un token de confirmación reciente, de un solo uso y ligado al cliente.
 */
@Component
@Slf4j
public class BearerTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDIS_CONSUMED_CONFIRM_PREFIX = "confirmacion:usado:";

    private final JwtTokenService jwtTokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Instant> consumedConfirmationTokens = new ConcurrentHashMap<>();

    public BearerTokenValidator(JwtTokenService jwtTokenService) {
        this(jwtTokenService, null);
    }

    @Autowired
    public BearerTokenValidator(JwtTokenService jwtTokenService,
                                @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.jwtTokenService = jwtTokenService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Compatibilidad con firma anterior: sin token de confirmación se rechaza con STRONG_AUTH_REQUIRED.
     */
    public AuthenticatedClient validarParaPago(String authorizationHeader) {
        return validarParaPago(authorizationHeader, null);
    }

    /**
     * Operaciones de pago: exige un JWT válido del cliente y un token de confirmación reciente
     * (X-Confirmation-Token) de un solo uso emitido exclusivamente mediante HUELLA digital (BIOMETRIC_HUELLA).
     * Rechaza cualquier confirmación por contraseña o desde web con 403 STRONG_AUTH_REQUIRED.
     */
    public AuthenticatedClient validarParaPago(String authorizationHeader, String confirmationTokenHeader) {
        AuthenticatedClient cliente = validarCliente(authorizationHeader);
        if (confirmationTokenHeader == null || confirmationTokenHeader.isBlank()) {
            log.warn("Pago rechazado para cliente {}: falta X-Confirmation-Token", cliente.getIdClient());
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        Claims confirmClaims;
        try {
            confirmClaims = jwtTokenService.extractClaims(confirmationTokenHeader.trim());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token de confirmación rechazado para cliente {}: {}", cliente.getIdClient(), e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        String purpose = confirmClaims.get(JwtTokenService.CLAIM_TOKEN_PURPOSE, String.class);
        if (!JwtTokenService.PURPOSE_OPERATION_CONFIRM.equals(purpose)) {
            log.warn("Token de confirmación con propósito inválido para cliente {}", cliente.getIdClient());
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        AuthMethod confirmMethod;
        try {
            String rawMethod = confirmClaims.get(JwtTokenService.CLAIM_AUTH_METHOD, String.class);
            confirmMethod = rawMethod != null ? AuthMethod.valueOf(rawMethod) : null;
        } catch (IllegalArgumentException e) {
            confirmMethod = null;
        }
        if (confirmMethod != AuthMethod.BIOMETRIC_HUELLA) {
            log.warn("Pago rechazado para cliente {}: confirmación no realizada con HUELLA (authMethod={})",
                    cliente.getIdClient(), confirmMethod);
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        Long confirmClientId = leerLong(confirmClaims, JwtTokenService.CLAIM_ID_CLIENT);
        if (confirmClientId == null || !confirmClientId.equals(cliente.getIdClient())) {
            log.warn("Token de confirmación de otro cliente: esperado={}, recibido={}", cliente.getIdClient(), confirmClientId);
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        String jti = confirmClaims.getId();
        if (jti == null || jti.isBlank() || !consumirTokenUnico(jti, confirmClaims.getExpiration())) {
            log.warn("Token de confirmación reutilizado o sin jti para cliente {}", cliente.getIdClient());
            throw new BusinessException(ErrorCode.STRONG_AUTH_REQUIRED);
        }

        return cliente;
    }

    private boolean consumirTokenUnico(String jti, Date expiration) {
        Instant now = Instant.now();
        consumedConfirmationTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        Instant expInstant = expiration != null ? expiration.toInstant() : now.plusSeconds(120);
        if (consumedConfirmationTokens.putIfAbsent(jti, expInstant) != null) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                long ttlSeconds = Math.max(1L, Duration.between(now, expInstant).getSeconds());
                Boolean set = redisTemplate.opsForValue()
                        .setIfAbsent(REDIS_CONSUMED_CONFIRM_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
                if (Boolean.FALSE.equals(set)) {
                    return false;
                }
            } catch (Exception e) {
                log.debug("Redis no disponible al registrar jti de confirmación, se usa control en memoria: {}", e.getMessage());
            }
        }
        return true;
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
