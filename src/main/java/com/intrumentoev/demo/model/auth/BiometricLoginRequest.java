package com.intrumentoev.demo.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricLoginRequest {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser una dirección de correo válida")
    private String email;

    @NotBlank(message = "El tipo biométrico es obligatorio")
    @Pattern(regexp = "^(HUELLA|FACIAL)$", message = "El tipo biométrico debe ser 'HUELLA' o 'FACIAL'")
    private String biometricType;

    @NotBlank(message = "Los datos biométricos (token/template Base64) son obligatorios")
    private String biometricData;

    private String deviceId; // Identificador del dispositivo seguro (opcional)
}
