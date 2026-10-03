package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BiometricRegisterRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long idClient;

    @NotBlank(message = "El tipo biométrico es obligatorio")
    @Pattern(regexp = "^(HUELLA|FACIAL)$", message = "El tipo biométrico debe ser 'HUELLA' o 'FACIAL'")
    private String biometricType;

    @NotBlank(message = "El template o carga biométrica en Base64 es obligatorio")
    private String biometricData;
}
