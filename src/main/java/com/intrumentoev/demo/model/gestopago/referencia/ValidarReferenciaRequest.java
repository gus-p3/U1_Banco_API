package com.intrumentoev.demo.model.gestopago.referencia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ValidarReferenciaRequest {

    @NotNull(message = "idServicio es obligatorio")
    @Positive(message = "idServicio debe ser positivo")
    private Integer idServicio;

    @NotNull(message = "idProducto es obligatorio")
    @Positive(message = "idProducto debe ser positivo")
    private Integer idProducto;

    @NotBlank(message = "referencia es obligatoria")
    @Size(min = 1, max = 100, message = "referencia debe tener entre 1 y 100 caracteres")
    private String referencia;

    @DecimalMin(value = "0.01", message = "monto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "monto debe tener máximo 2 decimales")
    private BigDecimal monto;
}
