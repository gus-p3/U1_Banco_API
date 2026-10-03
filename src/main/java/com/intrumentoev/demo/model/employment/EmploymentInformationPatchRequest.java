package com.intrumentoev.demo.model.employment;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
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
public class EmploymentInformationPatchRequest {

    @Size(max = 80, message = "La ocupación no puede superar los 80 caracteres")
    private String occupation;

    @Size(max = 100, message = "La empresa no puede superar los 100 caracteres")
    private String company;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal monthlyIncome;
}
