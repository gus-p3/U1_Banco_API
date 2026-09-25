package com.intrumentoev.demo.model.contactDetail;

import jakarta.validation.constraints.Email;
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
public class ContactDetailPatchRequest {

    @Email(message = "Debe ser una dirección de correo electrónico válida")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico no es válido"
    )
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "El teléfono móvil debe tener exactamente 10 dígitos numéricos"
    )
    private String mobilePhone;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "El teléfono alternativo debe tener exactamente 10 dígitos numéricos"
    )
    private String alternativePhone;
}