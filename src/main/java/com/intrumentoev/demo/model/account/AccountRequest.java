package com.intrumentoev.demo.model.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class AccountRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @jakarta.validation.constraints.Positive(message = "El ID del cliente debe ser un número entero positivo mayor a 0")
    private Long idClient;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    private BigDecimal initialBalance;
}
