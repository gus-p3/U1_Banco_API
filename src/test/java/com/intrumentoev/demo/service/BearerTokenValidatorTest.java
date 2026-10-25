package com.intrumentoev.demo.service;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerTokenValidatorTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-bytes-para-hs256!!";

    private JwtTokenService jwtTokenService;
    private ServerSessionManager sessionManager;
    private BearerTokenValidator validator;

    @BeforeEach
    void setUp() {
        jwtTokenService = nuevoJwtService(SECRET, 300);
        sessionManager = new ServerSessionManager();
        ReflectionTestUtils.setField(sessionManager, "inactivityLimitSeconds", 300L);
        validator = new BearerTokenValidator(jwtTokenService, sessionManager);
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
    @DisplayName("Firma de otro secreto → TOKEN_EXPIRED_OR_INVALID (401)")
    void firmaInvalida() {
        String ajeno = nuevoJwtService("otra-clave-distinta-de-al-menos-32-bytes-xx!!", 300)
                .generateAccessToken(10L, 1L, "cliente@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        sessionManager.setLoggedIn(true, "cliente@banco.com", 10L);
        assertError(() -> validator.validarParaPago("Bearer " + ajeno), ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Token expirado → TOKEN_EXPIRED_OR_INVALID (401)")
    void tokenExpirado() {
        String expirado = nuevoJwtService(SECRET, -10)
                .generateAccessToken(10L, 1L, "cliente@banco.com", AuthMethod.BIOMETRIC_HUELLA);
        sessionManager.setLoggedIn(true, "cliente@banco.com", 10L);
        assertError(() -> validator.validarParaPago("Bearer " + expirado), ErrorCode.TOKEN_EXPIRED_OR_INVALID);
    }

    @Test
    @DisplayName("Token válido pero sesión del servidor cerrada → SESSION_INACTIVE (401)")
    void sesionInactiva() {
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_HUELLA)), ErrorCode.SESSION_INACTIVE);
    }

    @Test
    @DisplayName("Login con contraseña, facial o refresh no permite pagar → STRONG_AUTH_REQUIRED (403)")
    void metodoNoPermitido() {
        sessionManager.setLoggedIn(true, "cliente@banco.com", 10L);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.PASSWORD)), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_FACIAL)), ErrorCode.STRONG_AUTH_REQUIRED);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.REFRESH)), ErrorCode.STRONG_AUTH_REQUIRED);
    }

    @Test
    @DisplayName("La sesión del servidor pertenece a otro cliente → PAYMENT_FORBIDDEN (403)")
    void sesionDeOtroCliente() {
        sessionManager.setLoggedIn(true, "otro@banco.com", 99L);
        assertError(() -> validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_HUELLA)), ErrorCode.PAYMENT_FORBIDDEN);
    }

    @Test
    @DisplayName("Huella + sesión del mismo cliente → devuelve el idClient del token")
    void huellaValida() {
        sessionManager.setLoggedIn(true, "cliente@banco.com", 10L);

        AuthenticatedClient cliente = validator.validarParaPago(bearer(AuthMethod.BIOMETRIC_HUELLA));

        assertThat(cliente.getIdClient()).isEqualTo(10L);
        assertThat(cliente.getIdLogin()).isEqualTo(1L);
        assertThat(cliente.getEmail()).isEqualTo("cliente@banco.com");
        assertThat(cliente.getAuthMethod()).isEqualTo(AuthMethod.BIOMETRIC_HUELLA);
    }
}
