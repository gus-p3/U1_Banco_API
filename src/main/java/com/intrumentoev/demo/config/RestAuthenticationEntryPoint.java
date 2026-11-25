package com.intrumentoev.demo.config;

import com.intrumentoev.demo.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401: sin Bearer → TOKEN_MISSING; Bearer presente pero inválido o expirado → TOKEN_EXPIRED_OR_INVALID. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean hasBearer = header != null
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && !header.substring(BEARER_PREFIX.length()).isBlank();
        errorWriter.write(response, hasBearer ? ErrorCode.TOKEN_EXPIRED_OR_INVALID : ErrorCode.TOKEN_MISSING);
    }
}
