package com.intrumentoev.demo.model.auth;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthRequest {
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser una dirección de correo electrónico válida")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico es inválido"
    )
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "alejandro.hernandez@banco-demo.com", description = "Correo institucional del cliente")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 100, message = "La contraseña debe tener entre 12 y 100 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]+$",
            message = "La contraseña debe incluir al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    @io.swagger.v3.oas.annotations.media.Schema(example = "PasswordSegura#2026", description = "Contraseña segura del cliente")
    private String password;
}
