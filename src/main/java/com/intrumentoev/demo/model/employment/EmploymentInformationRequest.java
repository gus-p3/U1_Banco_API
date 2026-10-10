package com.intrumentoev.demo.model.employment;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmploymentInformationRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @jakarta.validation.constraints.Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long idClient;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 80, message = "La ocupación no puede superar los 80 caracteres")
    private String occupation;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede superar los 100 caracteres")
    private String company;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal monthlyIncome;
}
