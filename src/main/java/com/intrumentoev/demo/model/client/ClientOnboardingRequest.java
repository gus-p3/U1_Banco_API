package com.intrumentoev.demo.model.client;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOnboardingRequest {

    // --- 1. Datos Personales ---
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El nombre contiene caracteres no válidos")
    private String name;

    @Size(min = 2, max = 50, message = "El segundo nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El segundo nombre contiene caracteres no válidos")
    private String secondName;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El apellido paterno contiene caracteres no válidos")
    private String lastName;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$", message = "El apellido materno contiene caracteres no válidos")
    private String secondLastName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    @NotBlank(message = "La CURP es obligatoria")
    @Size(min = 18, max = 18, message = "La CURP debe tener exactamente 18 caracteres")
    @Pattern(
            regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$",
            message = "El formato de la CURP es inválido"
    )
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 12, max = 13, message = "El RFC debe tener 12 o 13 caracteres")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$",
            message = "El formato del RFC es inválido"
    )
    private String rfc;

    @NotNull(message = "El sexo es obligatorio")
    private Short idGender;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Short idNationality;

    @NotNull(message = "El estado civil es obligatorio")
    private Short idMaritalStatus;

    // --- 2. Datos de Contacto ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe ser una dirección de correo electrónico válida")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "El formato del correo electrónico es inválido"
    )
    private String email;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String mobilePhone;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String alternativePhone;

    // --- 3. Domicilio ---
    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
    private String street;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no puede superar los 10 caracteres")
    private String exteriorNumber;

    @Size(max = 10, message = "El número interior no puede superar los 10 caracteres")
    private String interiorNumber;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 80, message = "La colonia no puede superar los 80 caracteres")
    private String neighborhood;

    @NotNull(message = "El municipio es obligatorio")
    private Integer idMunicipality;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String postalCode;

    @Size(max = 50, message = "El país no puede superar los 50 caracteres")
    @Builder.Default
    private String country = "México";

    // --- 4. Información Laboral ---
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 80, message = "La ocupación no puede superar los 80 caracteres")
    private String occupation;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede superar los 100 caracteres")
    private String company;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal monthlyIncome;

    // --- 5. Cuenta Bancaria Inicial (Opcional, si no se envía se toma el default del sistema) ---
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    private BigDecimal initialBalance;
}
