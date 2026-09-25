package com.intrumentoev.demo.model.account;

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
public class AccountRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long idClient;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    private BigDecimal initialBalance;
}
