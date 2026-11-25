package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class BiometricLoginRequest {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser una dirección de correo válida")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico es inválido"
    )
    @io.swagger.v3.oas.annotations.media.Schema(example = "alejandro.hernandez@banco-demo.com", description = "Correo electrónico del cliente")
    private String email;

    @NotBlank(message = "El tipo biométrico es obligatorio")
    @Pattern(regexp = "^(HUELLA|FACIAL)$", message = "El tipo biométrico debe ser 'HUELLA' o 'FACIAL'")
    @io.swagger.v3.oas.annotations.media.Schema(example = "HUELLA", description = "Tipo de sensor biométrico utilizado (HUELLA o FACIAL)")
    private String biometricType;

    @NotBlank(message = "Los datos biométricos (token/template Base64) son obligatorios")
    @Size(max = 4096, message = "Los datos biométricos exceden el tamaño máximo permitido")
    @io.swagger.v3.oas.annotations.media.Schema(example = "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ==", description = "Plantilla biométrica o firma criptográfica codificada en Base64")
    private String biometricData;

    @Size(max = 120, message = "El identificador del dispositivo no puede superar 120 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "pixel-7-pro-security-key", description = "Identificador del dispositivo seguro (opcional)")
    private String deviceId;
}
