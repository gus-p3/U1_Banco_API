package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BiometricRegisterRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long idClient;

    @NotBlank(message = "El tipo biométrico es obligatorio")
    @Pattern(regexp = "^(HUELLA|FACIAL)$", message = "El tipo biométrico debe ser 'HUELLA' o 'FACIAL'")
    private String biometricType;

    @NotBlank(message = "El template o carga biométrica en Base64 es obligatorio")
    @Size(max = 4096, message = "Los datos biométricos exceden el tamaño máximo permitido")
    private String biometricData;
}
