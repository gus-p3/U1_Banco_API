package com.intrumentoev.demo.controller.auth;

import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ServerSessionManager serverSessionManager;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, serverSessionManager);
    }

    @Test
    void obtenerEstadoSesionServidor() {
        ServerSessionStatusResponse status = new ServerSessionStatusResponse();
        when(serverSessionManager.getSessionStatus()).thenReturn(status);

        ResponseEntity<ServerSessionStatusResponse> response = controller.obtenerEstadoSesionServidor();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void login() {
        AuthRequest req = new AuthRequest();
        when(authService.login(req)).thenReturn(new AuthResponse());

        ResponseEntity<AuthResponse> response = controller.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void loginBiometrico() {
        BiometricLoginRequest req = new BiometricLoginRequest();
        when(authService.loginBiometrico(req)).thenReturn(new AuthResponse());

        ResponseEntity<AuthResponse> response = controller.loginBiometrico(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void registrarBiometria() {
        BiometricRegisterRequest req = new BiometricRegisterRequest();
        when(authService.registrarBiometria(req)).thenReturn(new AuthResponse());

        ResponseEntity<AuthResponse> response = controller.registrarBiometria(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void refrescarToken() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        when(authService.refrescarToken(req)).thenReturn(new AuthResponse());

        ResponseEntity<AuthResponse> response = controller.refrescarToken(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void logout() {
        ResponseEntity<Void> response = controller.logout("test@example.com");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).logout("test@example.com");
    }
}
