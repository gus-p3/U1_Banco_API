package com.intrumentoev.demo.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cuerpo para la actualización unificada y modular del cliente por CURP o RFC")
public class ClientUnifiedPatchRequest {

    // Identificador obligatorio para ubicar al usuario SIN usar @PathVariable
    @NotBlank(message = "El identificador (CURP o RFC) es estrictamente obligatorio para ubicar al cliente")
    @Pattern(
            regexp = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$",
            message = "El identificador debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido"
    )
    @Schema(description = "CURP o RFC para identificar al cliente", example = "ROMA900101HDFRRN01")
    private String identificador;

    // --- 1. Datos Personales Modificables ---
    @Positive(message = "El idGender debe ser un identificador positivo")
    private Short idGender;

    @Positive(message = "El idNationality debe ser un identificador positivo")
    private Short idNationality;

    @Positive(message = "El idMaritalStatus debe ser un identificador positivo")
    private Short idMaritalStatus;

    // --- 2. Datos de Contacto Modificables ---
    @Email(message = "El correo electrónico debe ser válido")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos numéricos")
    private String mobilePhone;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe tener exactamente 10 dígitos numéricos")
    private String alternativePhone;

    // --- 3. Datos de Domicilio Modificables ---
    @Size(min = 3, max = 100, message = "La calle debe tener entre 3 y 100 caracteres")
    private String street;

    @Size(min = 1, max = 10, message = "El número exterior debe tener entre 1 y 10 caracteres")
    private String exteriorNumber;

    @Size(max = 10, message = "El número interior no puede superar 10 caracteres")
    private String interiorNumber;

    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String postalCode;

    @Size(min = 2, max = 80, message = "La colonia no puede superar 80 caracteres")
    private String neighborhood;

    @Positive(message = "El municipio debe ser un ID positivo")
    private Integer idMunicipality;

    // --- 4. Datos Laborales Modificables ---
    @Size(min = 3, max = 80, message = "La ocupación debe tener entre 3 y 80 caracteres")
    private String occupation;

    @Size(min = 3, max = 100, message = "La empresa debe tener entre 3 y 100 caracteres")
    private String company;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal monthlyIncome;
}
