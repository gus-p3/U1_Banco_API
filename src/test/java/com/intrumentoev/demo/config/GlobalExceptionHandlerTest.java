package com.intrumentoev.demo.config;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.error.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBusinessException maneja CLIENT_NOT_FOUND (404)")
    void testHandleBusinessExceptionNotFound() {
        BusinessException ex = new BusinessException(ErrorCode.CLIENT_NOT_FOUND, "ROMA900101HDFRRN01");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CLIENT_NOT_FOUND");
        assertThat(response.getBody().getError().getTarget()).isEqualTo("identificador");
        assertThat(response.getBody().getError().getMessage()).contains("ROMA900101HDFRRN01");
    }

    @Test
    @DisplayName("handleBusinessException maneja CURP_DUPLICATED (409)")
    void testHandleBusinessExceptionConflict() {
        BusinessException ex = new BusinessException(ErrorCode.CURP_DUPLICATED, "ROMA900101HDFRRN01");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CURP_DUPLICATED");
        assertThat(response.getBody().getError().getTarget()).isEqualTo("curp");
    }

    @Test
    @DisplayName("handleBusinessException maneja INVALID_AGE (422)")
    void testHandleBusinessExceptionInvalidAge() {
        BusinessException ex = new BusinessException(ErrorCode.INVALID_AGE, 16);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_AGE");
        assertThat(response.getBody().getError().getTarget()).isEqualTo("birthDate");
        assertThat(response.getBody().getError().getMessage()).contains("16");
    }

    @Test
    @DisplayName("handleBusinessException maneja STATE_MISMATCH (422)")
    void testHandleBusinessExceptionStateMismatch() {
        BusinessException ex = new BusinessException(ErrorCode.STATE_MISMATCH, "02", "09", "Ciudad de México");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("STATE_MISMATCH");
        assertThat(response.getBody().getError().getTarget()).isEqualTo("claveEntidad");
    }
}
