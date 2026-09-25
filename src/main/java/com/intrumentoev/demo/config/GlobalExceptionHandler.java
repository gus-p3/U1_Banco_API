package com.intrumentoev.demo.config;

import com.intrumentoev.demo.exception.BaseBusinessException;
import com.intrumentoev.demo.model.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BaseBusinessException ex) {
        log.warn("Excepción de negocio capturada [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode(ex.getErrorCode());
        detail.setMessage(ex.getMessage());
        detail.setTarget(ex.getTarget());

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Error de validación en solicitud: {}", ex.getMessage());

        List<ErrorResponse.ErrorDetailItem> detailItems = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            ErrorResponse.ErrorDetailItem item = new ErrorResponse.ErrorDetailItem();
            item.setCode("INVALID_FIELD");
            item.setTarget(fieldError.getField());
            item.setMessage(fieldError.getDefaultMessage());
            detailItems.add(item);
        });

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("BAD_REQUEST");
        detail.setMessage("Error de validación en los datos de la solicitud");
        detail.setTarget("requestBody");
        detail.setDetails(detailItems);

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Error al deserializar cuerpo de la petición: {}", ex.getMessage());

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("MALFORMED_JSON");
        detail.setMessage("El cuerpo de la solicitud no tiene un formato JSON válido o contiene datos incompatibles");
        detail.setTarget("requestBody");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException no controlada: {}", ex.getMessage(), ex);

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("INTERNAL_ERROR");
        detail.setMessage(ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Excepción general no controlada: {}", ex.getMessage(), ex);

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("SERVER_ERROR");
        detail.setMessage("Error interno no controlado en el servidor");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
