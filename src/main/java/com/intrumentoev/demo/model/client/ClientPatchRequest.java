package com.intrumentoev.demo.model.client;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientPatchRequest {

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El nombre contiene caracteres no válidos")
    private String name;

    @Size(min = 2, max = 50, message = "El segundo nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El segundo nombre contiene caracteres no válidos")
    private String secondName;

    @Size(min = 2, max = 50, message = "El primer apellido debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El primer apellido contiene caracteres no válidos")
    private String lastName;

    @Size(min = 2, max = 50, message = "El segundo apellido debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El segundo apellido contiene caracteres no válidos")
    private String secondLastName;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    @Size(min = 18, max = 18, message = "El CURP debe tener exactamente 18 caracteres")
    @Pattern(
            regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$",
            message = "El formato del CURP es inválido"
    )
    private String curp;

    @Size(min = 13, max = 13, message = "El RFC de persona física debe tener exactamente 13 caracteres")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$",
            message = "El formato del RFC es inválido"
    )
    private String rfc;

    private Short idGender;
    private Short idNationality;
    private Short idMaritalStatus;
    private Boolean isActive;
    private OffsetDateTime deactivatedAt;
}