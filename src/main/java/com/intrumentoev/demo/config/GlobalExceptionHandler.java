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

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

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

        // 1. Detección de propiedades desconocidas / no permitidas en el JSON (Mass Assignment)
        if (cause instanceof UnrecognizedPropertyException upe) {
            String propertyName = upe.getPropertyName();
            detail.setCode("INVALID_FIELD");
            detail.setTarget(propertyName);
            detail.setMessage("El campo '" + propertyName + "' no está permitido o no existe en este modelo de datos");
        }
        // 2. Coerción o formato inválido de tipos (números, fechas, booleanos)
        else if (cause instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String fieldName = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            Class<?> targetType = ife.getTargetType();
            String expectedType = targetType != null ? targetType.getSimpleName() : "numérico";
            detail.setTarget(fieldName);

            if (targetType != null && (Boolean.class.isAssignableFrom(targetType) || targetType == boolean.class)) {
                detail.setMessage("El campo '" + fieldName + "' debe ser booleano (true o false). Valor recibido: '" + ife.getValue() + "'");
            } else if (targetType != null && java.time.temporal.Temporal.class.isAssignableFrom(targetType)) {
                detail.setMessage("El campo '" + fieldName + "' debe tener formato de fecha válido (YYYY-MM-DD). Valor recibido: '" + ife.getValue() + "'");
            } else {
                detail.setMessage("El campo '" + fieldName + "' debe ser de tipo numérico (" + expectedType + "). Valor recibido: '" + ife.getValue() + "'");
            }
        }
        // 3. Desajuste de entrada general de Jackson
        else if (cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
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

        String expectedDesc;
        if (requiredType != null) {
            if (Long.class.isAssignableFrom(requiredType) || requiredType == long.class
                    || Integer.class.isAssignableFrom(requiredType) || requiredType == int.class
                    || Short.class.isAssignableFrom(requiredType) || requiredType == short.class) {
                expectedDesc = "número entero (" + typeName + ")";
            } else if (Number.class.isAssignableFrom(requiredType) || requiredType == double.class || requiredType == float.class) {
                expectedDesc = "número decimal (" + typeName + ")";
            } else if (Boolean.class.isAssignableFrom(requiredType) || requiredType == boolean.class) {
                expectedDesc = "booleano ('true' o 'false')";
            } else if (java.time.temporal.Temporal.class.isAssignableFrom(requiredType)) {
                expectedDesc = "fecha/hora en formato ISO-8601 (YYYY-MM-DDTHH:mm:ssZ)";
            } else {
                expectedDesc = "tipo numérico (" + typeName + ")";
            }
        } else {
            expectedDesc = "tipo numérico (" + typeName + ")";
        }

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("INVALID_TYPE");
        detail.setTarget(paramName);
        detail.setMessage("El parámetro '" + paramName + "' debe ser de " + expectedDesc + ". Valor recibido: '" + ex.getValue() + "'");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Parámetro obligatorio faltante en query: {}", ex.getParameterName());

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("MISSING_PARAM");
        detail.setTarget(ex.getParameterName());
        detail.setMessage("El parámetro de consulta '" + ex.getParameterName() + "' es obligatorio y no fue proporcionado");

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Violación de restricción en parámetros: {}", ex.getMessage());

        List<ErrorResponse.ErrorDetailItem> detailItems = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String target = propertyPath.contains(".") ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
            ErrorResponse.ErrorDetailItem item = new ErrorResponse.ErrorDetailItem();
            item.setCode("INVALID_PARAM");
            item.setTarget(target);
            item.setMessage(violation.getMessage());
            detailItems.add(item);
        });

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("BAD_REQUEST");
        detail.setMessage("Parámetro(s) de ruta o consulta inválidos");
        detail.setTarget("parameters");
        detail.setDetails(detailItems);

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método HTTP no soportado: {}", ex.getMethod());

        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        detail.setCode("METHOD_NOT_ALLOWED");
        detail.setTarget("httpMethod");
        detail.setMessage("El método HTTP '" + ex.getMethod() + "' no está permitido para esta ruta. Métodos soportados: " + ex.getSupportedHttpMethods());

        ErrorResponse response = new ErrorResponse();
        response.setError(detail);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
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
