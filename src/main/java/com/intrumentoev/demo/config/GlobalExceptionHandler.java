package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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
        detail.setCode("INVALID_TYPE");
        detail.setTarget("requestBody");

        Throwable cause = ex.getCause();

        // Si la causa es un campo con formato inválido o valor de texto no permitido para tipos numéricos
        if (cause instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String fieldName = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String expectedType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "numérico";
            detail.setTarget(fieldName);
            detail.setMessage("El campo '" + fieldName + "' debe ser de tipo numérico (" + expectedType + "). Valor recibido: '" + ife.getValue() + "'");
        } else if (cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
            String fieldName = mie.getPath().get(mie.getPath().size() - 1).getFieldName();
            Class<?> targetType = mie.getTargetType();
            String expectedType = targetType != null ? targetType.getSimpleName() : "numérico";
            detail.setTarget(fieldName);
            detail.setMessage("El campo '" + fieldName + "' debe ser de tipo numérico (" + expectedType + ") y no debe enviarse como texto");
        } else {
            detail.setCode("MALFORMED_JSON");
            detail.setMessage("El cuerpo de la solicitud no tiene un formato JSON válido o contiene datos incompatibles");
        }

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Error de tipo en parámetro de petición: {}={}", ex.getName(), ex.getValue());

        String paramName = ex.getName();
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "numérico";

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("INVALID_TYPE");
        detail.setTarget(paramName);
        detail.setMessage("El parámetro '" + paramName + "' debe ser de tipo numérico (" + typeName + "). Valor recibido: '" + ex.getValue() + "'");

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
