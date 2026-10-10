package com.intrumentoev.demo.model.contactDetail;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactDetailRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long idClient;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser una dirección de correo electrónico válida")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico es inválido"
    )
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos"
    )
    private String mobilePhone;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos"
    )
    private String alternativePhone;
}