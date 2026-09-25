package com.intrumentoev.demo.model.client;

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
public class ClientRequest {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El nombre contiene caracteres no válidos")
    private String name;

    @Size(min = 2, max = 50, message = "El segundo nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El segundo nombre contiene caracteres no válidos")
    private String secondName;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El primer apellido debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El primer apellido contiene caracteres no válidos")
    private String lastName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El segundo apellido debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El segundo apellido contiene caracteres no válidos")
    private String secondLastName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    @NotBlank(message = "El CURP es obligatorio")
    @Size(min = 18, max = 18, message = "El CURP debe tener exactamente 18 caracteres")
    @Pattern(
            regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$",
            message = "El formato del CURP es inválido"
    )
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 13, max = 13, message = "El RFC de persona física debe tener exactamente 13 caracteres")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$",
            message = "El formato del RFC es inválido"
    )
    private String rfc;

    @NotNull(message = "El género es obligatorio")
    private Short idGender;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Short idNationality;

    @NotNull(message = "El estado civil es obligatorio")
    private Short idMaritalStatus;
}