package com.intrumentoev.demo.service;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.auth.Role;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerTokenValidatorTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-bytes-para-hs256!!";

    private JwtTokenService jwtTokenService;
    private BearerTokenValidator validator;

    @BeforeEach
    void setUp() {
        jwtTokenService = nuevoJwtService(SECRET, 300);
        validator = new BearerTokenValidator(jwtTokenService);
    }

    private JwtTokenService nuevoJwtService(String secret, long expSeconds) {
        JwtTokenService service = new JwtTokenService();
        ReflectionTestUtils.setField(service, "jwtSecret", secret);
        ReflectionTestUtils.setField(service, "accessTokenExpirationSeconds", expSeconds);
        return service;
    }

    private String bearer(AuthMethod method) {
        return "Bearer " + jwtTokenService.generateAccessToken(10L, 1L, "cliente@banco.com", method);
    }

    /** Token firmado con la clave correcta pero con claims arbitrarios. */
    private String tokenConClaims(java.util.Map<String, Object> claims) {
        return "Bearer " + Jwts.builder()
                .setSubject("cliente@banco.com")
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private void assertError(Runnable r, ErrorCode expected) {
        assertThatThrownBy(r::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCodeEnum")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Sin header o con esquema distinto a Bearer → TOKEN_MISSING (401)")
    void sinHeader() {
        assertError(() -> validator.validarParaPago(null), ErrorCode.TOKEN_MISSING);
        assertError(() -> validator.validarParaPago("Basic abc"), ErrorCode.TOKEN_MISSING);
        assertError(() -> validator.validarParaPago("Bearer   "), ErrorCode.TOKEN_MISSING);
    }

    @Test
    @DisplayName("Token mal formado → TOKEN_EXPIRED_OR_INVALID (401)")
    void tokenMalFormado() {
        assertError(() -> validator.validarCliente("Bearer abc.def.ghi"), ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Firma de otro secreto → TOKEN_EXPIRED_OR_INVALID (401)")
    void firmaInvalida() {
        String ajeno = nuevoJwtService("otra-clave-distinta-de-al-menos-32-bytes-xx!!", 300)
                .generateAccessToken(10L, 1L, "cliente@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        assertError(() -> validator.validarParaPago("Bearer " + ajeno), ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Token expirado → TOKEN_EXPIRED_OR_INVALID (401)")
    void tokenExpirado() {
        String expirado = nuevoJwtService(SECRET, -10)
                .generateAccessToken(10L, 1L, "cliente@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        assertError(() -> validator.validarParaPago("Bearer " + expirado), ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Token sin idClient → TOKEN_EXPIRED_OR_INVALID")
    void sinIdClient() {
        assertError(() -> validator.validarCliente(tokenConClaims(java.util.Map.of("authMethod", "PASSWORD"))),
                ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("authMethod o role desconocidos → TOKEN_EXPIRED_OR_INVALID")
    void claimsDesconocidos() {
        assertError(() -> validator.validarCliente(tokenConClaims(java.util.Map.of("idClient", 1, "authMethod", "RARO"))),
                ErrorCode.TOKEN_EXPIRED_OR_INVALID);
        assertError(() -> validator.validarCliente(tokenConClaims(java.util.Map.of("idClient", 1, "role", "SUPERUSER"))),
                ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Sin authMethod ni role en el token se asume PASSWORD / CLIENT")
    void valoresPorDefecto() {
        AuthenticatedClient cliente = validator.validarCliente(tokenConClaims(java.util.Map.of("idClient", 7)));
        assertThat(cliente.getIdClient()).isEqualTo(7L);
        assertThat(cliente.getAuthMethod()).isEqualTo(AuthMethod.PASSWORD);
        assertThat(cliente.getRole()).isEqualTo(Role.CLIENT);
        assertThat(cliente.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Login con contraseña, facial o refresh no permite pagar → STRONG_AUTH_REQUIRED (403)")
    void metodoNoPermitido() {
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD)), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_FACIAL)), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.REFRESH)), ErrorCode.STRONG_AUTH_REQUIRED);
    }

    @Test
    @DisplayName("Huella válida → devuelve los datos del token")
    void huellaValida() {
        AuthenticatedClient cliente = validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_HUELLA));

        assertThat(cliente.getIdClient()).isEqualTo(10L);
        assertThat(cliente.getIdLogin()).isEqualTo(1L);
        assertThat(cliente.getEmail()).isEqualTo("cliente@banco.com");
        assertThat(cliente.getAuthMethod()).isEqualTo(AuthMethod.BIOMETRIC_HUELLA);
    }

    @Test
    @DisplayName("validarCliente acepta cualquier método de auth y conserva el rol del token")
    void validarCliente() {
        AuthenticatedClient cliente = validator.validarCliente(bearer(AuthMethod.PASSWORD));
        assertThat(cliente.getIdClient()).isEqualTo(10L);
        assertThat(cliente.getAuthMethod()).isEqualTo(AuthMethod.PASSWORD);
        assertThat(cliente.getRole()).isEqualTo(Role.CLIENT);

        String admin = "Bearer " + jwtTokenService.generateAccessToken(1L, 2L, "admin@banco.com", AuthMethod.PASSWORD, Role.ADMIN);
        assertThat(validator.validarCliente(admin).isAdmin()).isTrue();
    }
}
