package com.intrumentoev.demo.model.auth;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de reautenticación para confirmar una operación sensible (ej. pago en web o móvil).
 * Acepta contraseña o huella digital registrada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationConfirmRequest {

    @Size(min = 1, max = 100, message = "La contraseña debe tener entre 1 y 100 caracteres")
    private String password;

    private String biometricType;

    private String biometricData;
}

