package com.intrumentoev.demo.model.auth;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de reautenticación biométrica para confirmar una operación de pago en la app móvil.
 * Solo se autoriza con huella digital registrada (HUELLA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationConfirmRequest {

    @Size(min = 1, max = 100, message = "La contraseña debe tener entre 1 y 100 caracteres")
    private String password;

    @Size(max = 20, message = "El tipo biométrico no puede exceder 20 caracteres")
    private String biometricType;

    @Size(max = 4096, message = "Los datos biométricos exceden el tamaño máximo permitido")
    private String biometricData;
}
