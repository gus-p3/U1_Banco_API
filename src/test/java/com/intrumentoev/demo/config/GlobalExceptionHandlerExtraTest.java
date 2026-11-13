package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intrumentoev.demo.model.error.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cubre cada @ExceptionHandler de {@link GlobalExceptionHandler}. */
class GlobalExceptionHandlerExtraTest {

    /** Modelo mínimo para provocar errores reales de Jackson. */
    static class Dto {
        public Integer n;
        public Boolean b;
        public LocalDate d;
    }

    private GlobalExceptionHandler handler;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @SuppressWarnings("unused")
    void metodo(String arg) {
    }

    private HttpMessageNotReadableException notReadable(String json) {
        try {
            mapper.readValue(json, Dto.class);
            throw new AssertionError("Se esperaba una excepción de Jackson");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return new HttpMessageNotReadableException("error", e, null);
        }
    }

    private ErrorResponse.ErrorDetail body(ResponseEntity<ErrorResponse> response, HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getError();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 con el detalle de cada campo")
    void validacion() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "obj");
        result.addError(new FieldError("obj", "email", "Correo inválido"));
        result.addError(new FieldError("obj", "name", "Nombre requerido"));
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("metodo", String.class), 0);

        var detail = body(handler.handleValidationException(new MethodArgumentNotValidException(parameter, result)),
                HttpStatus.BAD_REQUEST);

        assertThat(detail.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(detail.getDetails()).hasSize(2);
        assertThat(detail.getDetails().get(0).getTarget()).isEqualTo("email");
        assertThat(detail.getDetails().get(0).getMessage()).isEqualTo("Correo inválido");
    }

    @Test
    @DisplayName("Campo desconocido en el JSON → INVALID_FIELD")
    void campoDesconocido() {
        var detail = body(handler.handleHttpMessageNotReadable(notReadable("{\"idClient\":1}")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getCode()).isEqualTo("INVALID_FIELD");
        assertThat(detail.getTarget()).isEqualTo("idClient");
    }

    @Test
    @DisplayName("Texto en campo numérico → INVALID_TYPE con tipo esperado")
    void numeroInvalido() {
        var detail = body(handler.handleHttpMessageNotReadable(notReadable("{\"n\":\"abc\"}")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getTarget()).isEqualTo("n");
        assertThat(detail.getMessage()).contains("numérico").contains("abc");
    }

    @Test
    @DisplayName("Valor no booleano → mensaje de booleano")
    void booleanoInvalido() {
        var detail = body(handler.handleHttpMessageNotReadable(notReadable("{\"b\":\"quizas\"}")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getTarget()).isEqualTo("b");
        assertThat(detail.getMessage()).contains("booleano");
    }

    @Test
    @DisplayName("Fecha inválida → mensaje de formato de fecha")
    void fechaInvalida() {
        var detail = body(handler.handleHttpMessageNotReadable(notReadable("{\"d\":\"31-12-2020\"}")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getTarget()).isEqualTo("d");
        assertThat(detail.getMessage()).contains("fecha");
    }

    @Test
    @DisplayName("Estructura distinta a la esperada → mensaje de tipo")
    void estructuraDistinta() {
        var detail = body(handler.handleHttpMessageNotReadable(notReadable("{\"n\":{\"x\":1}}")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getTarget()).isEqualTo("n");
        assertThat(detail.getMessage()).contains("no debe enviarse como texto");
    }

    @Test
    @DisplayName("JSON malformado → MALFORMED_JSON")
    void jsonMalformado() {
        var detail = body(handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("x", new RuntimeException("boom"), null)), HttpStatus.BAD_REQUEST);
        assertThat(detail.getCode()).isEqualTo("MALFORMED_JSON");
    }

    @Test
    @DisplayName("Parámetro de ruta con tipo incorrecto → INVALID_TYPE (entero, decimal, booleano, fecha, otro, sin tipo)")
    void tipoDeParametro() {
        MethodParameter parameter = mock(MethodParameter.class);
        Object[][] casos = {
                {Long.class, "entero"},
                {int.class, "entero"},
                {Double.class, "decimal"},
                {Boolean.class, "booleano"},
                {OffsetDateTime.class, "ISO-8601"},
                {Object.class, "tipo numérico"},
                {null, "tipo numérico"}
        };
        for (Object[] caso : casos) {
            var ex = new MethodArgumentTypeMismatchException("abc", (Class<?>) caso[0], "id", parameter, null);
            var detail = body(handler.handleMethodArgumentTypeMismatch(ex), HttpStatus.BAD_REQUEST);
            assertThat(detail.getCode()).isEqualTo("INVALID_TYPE");
            assertThat(detail.getTarget()).isEqualTo("id");
            assertThat(detail.getMessage()).contains((String) caso[1]).contains("abc");
        }
    }

    @Test
    @DisplayName("Ruta inexistente → 404 en lugar de 500")
    void rutaInexistente() {
        var detail = body(handler.handleNoResourceFound(
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "/v1/nada")), HttpStatus.NOT_FOUND);
        assertThat(detail.getCode()).isEqualTo("NOT_FOUND");
        assertThat(detail.getTarget()).isEqualTo("path");
    }

    @Test
    @DisplayName("Falta un parámetro obligatorio → MISSING_PARAM")
    void parametroFaltante() {
        var detail = body(handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("email", "String")), HttpStatus.BAD_REQUEST);
        assertThat(detail.getCode()).isEqualTo("MISSING_PARAM");
        assertThat(detail.getTarget()).isEqualTo("email");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 usando el último segmento del path")
    @SuppressWarnings("unchecked")
    void violacionDeRestriccion() {
        ConstraintViolation<Object> conPunto = mock(ConstraintViolation.class);
        Path pathConPunto = mock(Path.class);
        when(pathConPunto.toString()).thenReturn("logout.email");
        when(conPunto.getPropertyPath()).thenReturn(pathConPunto);
        when(conPunto.getMessage()).thenReturn("Correo inválido");

        ConstraintViolation<Object> sinPunto = mock(ConstraintViolation.class);
        Path pathSimple = mock(Path.class);
        when(pathSimple.toString()).thenReturn("email");
        when(sinPunto.getPropertyPath()).thenReturn(pathSimple);
        when(sinPunto.getMessage()).thenReturn("Requerido");

        var detail = body(handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(conPunto, sinPunto))), HttpStatus.BAD_REQUEST);

        assertThat(detail.getTarget()).isEqualTo("parameters");
        assertThat(detail.getDetails()).extracting(ErrorResponse.ErrorDetailItem::getTarget).containsOnly("email");
    }

    @Test
    @DisplayName("Método HTTP no soportado → 405")
    void metodoNoSoportado() {
        var detail = body(handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"))), HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(detail.getCode()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(detail.getMessage()).contains("DELETE");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 con su mensaje o uno por defecto")
    void argumentoIlegal() {
        assertThat(body(handler.handleIllegalArgumentException(new IllegalArgumentException("malo")), HttpStatus.BAD_REQUEST)
                .getMessage()).isEqualTo("malo");
        assertThat(body(handler.handleIllegalArgumentException(new IllegalArgumentException()), HttpStatus.BAD_REQUEST)
                .getMessage()).isEqualTo("Argumento proporcionado inválido");
    }

    @Test
    @DisplayName("SecurityException → 403")
    void seguridad() {
        assertThat(body(handler.handleSecurityException(new SecurityException("no")), HttpStatus.FORBIDDEN)
                .getMessage()).isEqualTo("no");
        assertThat(body(handler.handleSecurityException(new SecurityException()), HttpStatus.FORBIDDEN)
                .getMessage()).contains("Acceso no autorizado");
    }

    @Test
    @DisplayName("RuntimeException → 500 INTERNAL_ERROR y Exception → 500 SERVER_ERROR")
    void errores500() {
        assertThat(body(handler.handleRuntimeException(new RuntimeException("falló")), HttpStatus.INTERNAL_SERVER_ERROR)
                .getMessage()).isEqualTo("falló");
        assertThat(body(handler.handleRuntimeException(new RuntimeException()), HttpStatus.INTERNAL_SERVER_ERROR)
                .getMessage()).isEqualTo("Error interno del servidor");
        var general = body(handler.handleGeneralException(new Exception("x")), HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(general.getCode()).isEqualTo("SERVER_ERROR");
    }
}
