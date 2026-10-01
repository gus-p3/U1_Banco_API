package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.model.auth.ServerSessionStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gestor del estado de sesión en el servidor.
 * Controla el booleano en el servidor (login = true / false),
 * con contador de inactividad en segundos (expiración a los 5 minutos / 300 segundos de inactividad).
 */
@Component
@Slf4j
public class ServerSessionManager {

    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastActivityTime = new AtomicReference<>(null);
    private final AtomicReference<String> activeUserEmail = new AtomicReference<>(null);
    private final AtomicReference<Long> activeClientId = new AtomicReference<>(null);

    @Value("${server.session.inactivity-limit-seconds:300}")
    private long inactivityLimitSeconds = 300;

    public synchronized long getInactivityLimitSeconds() {
        return inactivityLimitSeconds;
    }

    /**
     * Establece el estado del booleano en el servidor cuando el usuario hace login o logout.
     */
    public synchronized void setLoggedIn(boolean status, String email, Long idClient) {
        if (status) {
            loggedIn.set(true);
            lastActivityTime.set(Instant.now());
            activeUserEmail.set(email);
            activeClientId.set(idClient);
            log.info("SERVIDOR: Login establecido en TRUE para el usuario: {} (ID: {}). Contador de inactividad reiniciado.", email, idClient);
        } else {
            loggedIn.set(false);
            lastActivityTime.set(null);
            activeUserEmail.set(null);
            activeClientId.set(null);
            log.info("SERVIDOR: Login establecido en FALSE. Sesión cerrada.");
        }
    }

    /**
     * Evalúa si el usuario se encuentra con sesión activa en el servidor.
     * Si han transcurrido 5 o más segundos de inactividad, el booleano se pasa automáticamente a false.
     */
    public synchronized boolean isUserLoggedIn() {
        if (!loggedIn.get()) {
            return false;
        }

        Instant last = lastActivityTime.get();
        if (last == null) {
            loggedIn.set(false);
            return false;
        }

        long elapsedSeconds = Duration.between(last, Instant.now()).getSeconds();
        if (elapsedSeconds >= inactivityLimitSeconds) {
            log.warn("SERVIDOR: Han transcurrido {} segundos de inactividad (límite: {} s). Cambiando booleano a FALSE.",
                    elapsedSeconds, inactivityLimitSeconds);
            loggedIn.set(false);
            lastActivityTime.set(null);
            activeUserEmail.set(null);
            activeClientId.set(null);
            return false;
        }

        return true;
    }

    /**
     * Registra actividad del usuario en el servidor, reiniciando el contador de segundos de inactividad a 0.
     */
    public synchronized void recordActivity() {
        if (isUserLoggedIn()) {
            lastActivityTime.set(Instant.now());
        }
    }

    /**
     * Devuelve los segundos de inactividad transcurridos desde la última petición.
     */
    public synchronized long getInactivitySeconds() {
        Instant last = lastActivityTime.get();
        if (last == null || !loggedIn.get()) {
            return 0;
        }
        return Math.max(0, Duration.between(last, Instant.now()).getSeconds());
    }

    /**
     * Devuelve los segundos restantes antes de que la sesión expire por inactividad.
     */
    public synchronized long getRemainingSeconds() {
        if (!isUserLoggedIn()) {
            return 0;
        }
        long elapsed = getInactivitySeconds();
        return Math.max(0, inactivityLimitSeconds - elapsed);
    }

    /**
     * Devuelve el estado completo del booleano y los contadores en segundos para monitoreo y frontend.
     */
    public synchronized ServerSessionStatusResponse getSessionStatus() {
        boolean active = isUserLoggedIn();
        long inactivity = getInactivitySeconds();
        long remaining = getRemainingSeconds();
        Instant last = lastActivityTime.get();

        String status;
        String message;

        if (active) {
            status = "ACTIVA";
            message = "Sesión iniciada en el servidor (login = true). Inactividad: " + inactivity + "s / Límite: " + inactivityLimitSeconds + "s.";
        } else if (inactivity >= inactivityLimitSeconds) {
            status = "EXPIRADA_POR_INACTIVIDAD";
            message = "Sesión expirada tras " + inactivity + " segundos de inactividad (límite: " + inactivityLimitSeconds + "s). Inicie sesión para realizar consultas.";
        } else {
            status = "NO_INICIADA";
            message = "El usuario no ha iniciado sesión en el servidor (login = false). La creación de cuentas está permitida sin login.";
        }

        return ServerSessionStatusResponse.builder()
                .isLoggedIn(active)
                .inactivitySeconds(inactivity)
                .maxInactivitySeconds(inactivityLimitSeconds)
                .remainingSeconds(remaining)
                .userEmail(activeUserEmail.get())
                .clientId(activeClientId.get())
                .status(status)
                .lastActivityAt(last != null ? last.atOffset(ZoneOffset.UTC) : null)
                .message(message)
                .build();
    }
}
