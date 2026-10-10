package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * o si han transcurrido más de 5 minutos (300 segundos) de inactividad.
 */
@Component
@Slf4j
public class ServerSessionInterceptor implements HandlerInterceptor {

    private final ServerSessionManager serverSessionManager;
    private final ObjectMapper objectMapper;

    public ServerSessionInterceptor(
            ServerSessionManager serverSessionManager,
            @org.springframework.context.annotation.Lazy ObjectMapper objectMapper) {
        this.serverSessionManager = serverSessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String rawPath = request.getRequestURI();
        String path = rawPath != null ? rawPath.replaceAll("/+", "/") : "";
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
            long limit = serverSessionManager.getInactivityLimitSeconds();
            log.warn("Petición bloqueada hacia {} {}: el servidor tiene login = FALSE o pasaron > {}s de inactividad", method, path, limit);

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", OffsetDateTime.now().toString());
            errorBody.put("status", HttpStatus.UNAUTHORIZED.value());
            errorBody.put("error", "Acceso No Autorizado (Sesión Inactiva)");
            errorBody.put("message", "Operación restringida: Para realizar consultas en el servidor, debe iniciar sesión (login = true). " +
                    "Si ya había iniciado sesión, esta expiró automáticamente tras " + limit + " segundos de inactividad.");
            errorBody.put("path", rawPath);
            errorBody.put("inactivityLimitSeconds", limit);

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return false;
        }

        // 4. Si la sesión está activa, registrar actividad y reiniciar contador de inactividad
        serverSessionManager.recordActivity();
        return true;
    }

    private boolean isPublicEndpoint(String path, String method) {
        String cleanPath = path == null ? "" : path.replaceAll("/+", "/");
        if (cleanPath.length() > 1 && cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        // Creación y registro de cliente / cuenta (Onboarding)
        if ("POST".equalsIgnoreCase(method) && (
                cleanPath.equals("/v1/clientes/onboarding") ||
                cleanPath.endsWith("/v1/clientes/onboarding") ||
                cleanPath.equals("/v1/cuentas") ||
                cleanPath.endsWith("/v1/cuentas") ||
                cleanPath.equals("/v1/catalogos/sincronizar") ||
                cleanPath.endsWith("/v1/catalogos/sincronizar")
        )) {
            return true;
        }

        // Autenticación (Login, Biometría, Refresh, Estado del Servidor)
        if (cleanPath.contains("/v1/auth/login") ||
            cleanPath.contains("/v1/auth/login-biometrico") ||
            cleanPath.contains("/v1/auth/refresh") ||
            cleanPath.contains("/v1/auth/session-status") ||
            cleanPath.contains("/v1/auth/estado-servidor")) {
            return true;
        }

        // Catálogos auxiliares (requeridos para llenar los formularios de onboarding)
        if ("GET".equalsIgnoreCase(method) && cleanPath.contains("/v1/catalogos")) {
            return true;
        }

        // Documentación Swagger / OpenAPI
        if (cleanPath.contains("/swagger-ui") ||
            cleanPath.contains("/v3/api-docs") ||
            cleanPath.contains("/swagger-ui.html") ||
            cleanPath.contains("/favicon.ico") ||
            cleanPath.startsWith("/error")) {
            return true;
        }

        return false;
    }
}
