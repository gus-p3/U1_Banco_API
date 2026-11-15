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
        ReflectionTestUtils.setField(service, "confirmationTokenExpirationSeconds", expSeconds);
        return service;
    }

    private String bearer(AuthMethod method) {
        return "Bearer " + jwtTokenService.generateAccessToken(10L, 1L, "cliente@banco.com", method);
    }

    private String confirmToken(Long idClient, AuthMethod method) {
        return jwtTokenService.generateConfirmationToken(idClient, 1L, "cliente@banco.com", method);
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
    @DisplayName("Sin X-Confirmation-Token no permite pagar con ningún método → STRONG_AUTH_REQUIRED (403)")
    void sinConfirmacionNoPermitePagar() {
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), null), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), "  "), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_HUELLA), null), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.REFRESH)), ErrorCode.STRONG_AUTH_REQUIRED);
    }

    @Test
    @DisplayName("Confirmación válida permite pagar tanto con token PASSWORD como BIOMETRIC_HUELLA")
    void confirmacionValidaPermitePagar() {
        AuthenticatedClient conPass = validator.validarParaPago(
                bearer(AuthMethod.PASSWORD),
                confirmToken(10L, AuthMethod.PASSWORD));
        assertThat(conPass.getIdClient()).isEqualTo(10L);
        assertThat(conPass.getAuthMethod()).isEqualTo(AuthMethod.PASSWORD);

        AuthenticatedClient conHuella = validator.validarParaPago(
                bearer(AuthMethod.BIOMETRIC_HUELLA),
                confirmToken(10L, AuthMethod.BIOMETRIC_HUELLA));
        assertThat(conHuella.getIdClient()).isEqualTo(10L);
        assertThat(conHuella.getAuthMethod()).isEqualTo(AuthMethod.BIOMETRIC_HUELLA);
    }

    @Test
    @DisplayName("Token de confirmación expirado, mal formado, reutilizado o de otro cliente → STRONG_AUTH_REQUIRED (403)")
    void confirmacionInvalidaExpiradaReutilizadaOAjenas() {
        // Expirado
        String confirmExpirado = nuevoJwtService(SECRET, -10)
                .generateConfirmationToken(10L, 1L, "cliente@banco.com", AuthMethod.PASSWORD);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), confirmExpirado),
                ErrorCode.STRONG_AUTH_REQUIRED);

        // Mal formado o un access token común usado como confirmación
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), "no-es-jwt"),
                ErrorCode.STRONG_AUTH_REQUIRED);
        String accessNormal = jwtTokenService.generateAccessToken(10L, 1L, "cliente@banco.com", AuthMethod.PASSWORD);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), accessNormal),
                ErrorCode.STRONG_AUTH_REQUIRED);

        // De otro cliente
        String confirmOtroCliente = confirmToken(999L, AuthMethod.PASSWORD);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), confirmOtroCliente),
                ErrorCode.STRONG_AUTH_REQUIRED);

        // Reutilizado (de un solo uso)
        String confirmUnico = confirmToken(10L, AuthMethod.PASSWORD);
        assertThat(validator.validarParaPago(bearer(AuthMethod.PASSWORD), confirmUnico).getIdClient()).isEqualTo(10L);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD), confirmUnico),
                ErrorCode.STRONG_AUTH_REQUIRED);
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
