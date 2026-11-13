package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Escribe errores de seguridad (401/403) con el mismo formato JSON que el resto de la API. */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(@Lazy ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode(errorCode.getCode());
        detail.setMessage(errorCode.format());
        detail.setTarget(errorCode.getDefaultTarget());
        ErrorResponse body = new ErrorResponse();
        body.setError(detail);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
