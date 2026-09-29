package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interceptor de seguridad que verifica el booleano en el servidor (login = true).
 * Permite la creación de clientes y cuentas sin haber iniciado sesión, pero restringe
 * las consultas de clientes, cuentas y operaciones administrativas si login es false
 * o si han transcurrido más de 5 segundos de inactividad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServerSessionInterceptor implements HandlerInterceptor {

    private final ServerSessionManager serverSessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Métodos pre-flight CORS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. Rutas públicas: Onboarding y creación de cuenta (permitidas sin haber iniciado sesión)
        if (isPublicEndpoint(path, method)) {
            return true;
        }

        // 3. Consultas y operaciones protegidas: Requieren login = true en el servidor
        if (!serverSessionManager.isUserLoggedIn()) {
            log.warn("Petición bloqueada hacia {} {}: el servidor tiene login = FALSE o pasaron > 5s de inactividad", method, path);

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", OffsetDateTime.now().toString());
            errorBody.put("status", HttpStatus.UNAUTHORIZED.value());
            errorBody.put("error", "Acceso No Autorizado (Sesión Inactiva)");
            errorBody.put("message", "Operación restringida: Para realizar consultas en el servidor, debe iniciar sesión (login = true). " +
                    "Si ya había iniciado sesión, esta expiró automáticamente tras 5 segundos de inactividad.");
            errorBody.put("path", path);
            errorBody.put("inactivityLimitSeconds", 5);

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return false;
        }

        // 4. Si la sesión está activa, registrar actividad y reiniciar contador de inactividad
        serverSessionManager.recordActivity();
        return true;
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Creación y registro de cliente / cuenta (Onboarding)
        if ("POST".equalsIgnoreCase(method) && (
                path.equals("/v1/clientes/onboarding") ||
                path.equals("/v1/cuentas") ||
                        path.equals("/v1/catalogos/sincronizar")
        )) {
            return true;
        }

        // Autenticación (Login, Biometría, Refresh, Estado del Servidor)
        if (path.startsWith("/v1/auth/login") ||
            path.startsWith("/v1/auth/login-biometrico") ||
            path.startsWith("/v1/auth/refresh") ||
            path.startsWith("/v1/auth/session-status") ||
            path.startsWith("/v1/auth/estado-servidor")) {
            return true;
        }

        // Catálogos auxiliares (requeridos para llenar los formularios de onboarding)
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/v1/catalogos")) {
            return true;
        }

        // Documentación Swagger / OpenAPI
        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.equals("/swagger-ui.html") ||
            path.equals("/favicon.ico") ||
            path.startsWith("/error")) {
            return true;
        }

        return false;
    }
}
