package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtTokenService {
    public static final String CLAIM_ID_CLIENT = "idClient";
    public static final String CLAIM_AUTH_METHOD = "authMethod";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ID_LOGIN = "idLogin";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long idClient, Long idLogin, String email, AuthMethod authMethod) {
        return generateAccessToken(idClient, idLogin, email, authMethod, Role.CLIENT);
    }

    public String generateAccessToken(Long idClient, Long idLogin, String email, AuthMethod authMethod, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationSeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
                .setSubject(email)
                .claim(CLAIM_ID_CLIENT, idClient)
                .claim(CLAIM_ID_LOGIN, idLogin)
                .claim(CLAIM_ROLE, (role == null ? Role.CLIENT : role).name())
                .claim(CLAIM_AUTH_METHOD, Objects.requireNonNull(authMethod, "authMethod es obligatorio").name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
