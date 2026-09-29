package com.intrumentoev.demo.service;

import com.intrumentoev.demo.model.auth.ServerSessionStatusResponse;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ServerSessionManagerTest {

    private ServerSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new ServerSessionManager();
        ReflectionTestUtils.setField(sessionManager, "inactivityLimitSeconds", 5L);
    }

    @Test
    @DisplayName("Estado inicial del servidor es login = false")
    void testEstadoInicial() {
        assertThat(sessionManager.isUserLoggedIn()).isFalse();
        ServerSessionStatusResponse status = sessionManager.getSessionStatus();
        assertThat(status.getIsLoggedIn()).isFalse();
        assertThat(status.getStatus()).isEqualTo("NO_INICIADA");
    }

    @Test
    @DisplayName("Login en servidor establece booleano en true y reinicia inactividad")
    void testLoginEnServidor() {
        sessionManager.setLoggedIn(true, "cliente@banco.com", 1L);

        assertThat(sessionManager.isUserLoggedIn()).isTrue();
        ServerSessionStatusResponse status = sessionManager.getSessionStatus();
        assertThat(status.getIsLoggedIn()).isTrue();
        assertThat(status.getUserEmail()).isEqualTo("cliente@banco.com");
        assertThat(status.getClientId()).isEqualTo(1L);
        assertThat(status.getStatus()).isEqualTo("ACTIVA");
        assertThat(status.getMaxInactivitySeconds()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Logout en servidor pasa booleano a false")
    void testLogoutEnServidor() {
        sessionManager.setLoggedIn(true, "cliente@banco.com", 1L);
        assertThat(sessionManager.isUserLoggedIn()).isTrue();

        sessionManager.setLoggedIn(false, null, null);
        assertThat(sessionManager.isUserLoggedIn()).isFalse();
    }

    @Test
    @DisplayName("Expiración de sesión por inactividad (> 5 segundos) cambia booleano a false")
    void testExpiracionPorInactividad() {
        // Configuramos límite pequeño de 1 segundo para la prueba
        ReflectionTestUtils.setField(sessionManager, "inactivityLimitSeconds", 1L);

        sessionManager.setLoggedIn(true, "cliente@banco.com", 1L);
        assertThat(sessionManager.isUserLoggedIn()).isTrue();

        try {
            Thread.sleep(1200); // Esperar 1.2 segundos para superar el límite
        } catch (InterruptedException ignored) {}

        // Al consultar tras el periodo de inactividad, debe pasar automáticamente a false
        assertThat(sessionManager.isUserLoggedIn()).isFalse();
    }

    @Test
    @DisplayName("Registrar actividad reinicia el contador de inactividad")
    void testRegistrarActividad() {
        sessionManager.setLoggedIn(true, "cliente@banco.com", 1L);
        sessionManager.recordActivity();
        assertThat(sessionManager.isUserLoggedIn()).isTrue();
    }
}
