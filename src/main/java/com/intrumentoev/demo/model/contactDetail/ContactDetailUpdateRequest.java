package com.intrumentoev.demo.model.contactDetail;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO exclusivo para actualización y edición de los datos de contacto de un cliente.
 * El idClient no es obligatorio para editar el detalle de contacto por su identificador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactDetailUpdateRequest {

    /**
     * Opcional: el cliente ya está vinculado por la URL o la entidad existente.
     */
    @jakarta.validation.constraints.Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
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
