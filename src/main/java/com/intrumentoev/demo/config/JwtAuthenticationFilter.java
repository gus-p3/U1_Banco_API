package com.intrumentoev.demo.config;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el JWT del header Authorization y, si es válido, deja al usuario autenticado en el
 * SecurityContext con su rol. Si falta o es inválido NO responde: la petición sigue anónima y
 * Spring Security decide (401 por {@link RestAuthenticationEntryPoint} en rutas protegidas).
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                AuthenticatedClient cliente = tokenValidator.validarCliente(header);
                var authentication = new UsernamePasswordAuthenticationToken(
                        cliente, null, List.of(new SimpleGrantedAuthority(cliente.getRole().authority())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException e) {
                log.debug("Bearer ignorado en {}: {}", request.getRequestURI(), e.getErrorCodeEnum());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
