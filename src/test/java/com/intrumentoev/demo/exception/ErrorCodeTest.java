package com.intrumentoev.demo.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("Todos los códigos de error tienen código, estatus y target definidos")
    void testEnumIntegrity() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.getCode()).isNotBlank();
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getMessageTemplate()).isNotBlank();
            assertThat(code.getDefaultTarget()).isNotBlank();
        }
    }

    @Test
    @DisplayName("format() formatea adecuadamente con argumentos y maneja argumentos nulos")
    void testFormatWithArguments() {
        String formatted = ErrorCode.CURP_DUPLICATED.format("ROMA900101HDFRRN01");
        assertThat(formatted).isEqualTo("La CURP 'ROMA900101HDFRRN01' ya se encuentra registrada");

        String formattedState = ErrorCode.STATE_MISMATCH.format("02", "09", "Ciudad de México");
        assertThat(formattedState).contains("02").contains("09").contains("Ciudad de México");

        String noArgs = ErrorCode.INVALID_CREDENTIALS.format();
        assertThat(noArgs).isEqualTo("Credenciales de acceso inválidas");
    }

    @Test
    @DisplayName("Verificación de mapeo de HttpStatus específicos")
    void testHttpStatusMapping() {
        assertThat(ErrorCode.CLIENT_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.CURP_DUPLICATED.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.INVALID_AGE.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ErrorCode.INVALID_CREDENTIALS.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.ACCOUNT_LOCKED.getHttpStatus()).isEqualTo(HttpStatus.LOCKED);
        assertThat(ErrorCode.ACCESS_DENIED.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
