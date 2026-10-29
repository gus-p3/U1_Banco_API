package com.intrumentoev.demo.model.gestopago.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /v1/pagos. El cliente NUNCA viaja en el body: se toma del claim idClient del JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe tener exactamente 10 dígitos numéricos")
    @Schema(example = "9876543210", description = "Cuenta de la que se descuenta el pago")
    private String numCuenta;

    @NotNull(message = "idServicio es obligatorio")
    @Positive(message = "idServicio debe ser positivo")
    @Schema(example = "133", description = "ID del servicio en el catálogo de GestoPago")
    private Integer idServicio;

    @NotNull(message = "idProducto es obligatorio")
    @Positive(message = "idProducto debe ser positivo")
    @Schema(example = "582", description = "ID del producto en el catálogo de GestoPago")
    private Integer idProducto;

    @NotBlank(message = "tipoPago es obligatorio")
    @Pattern(regexp = "^(RECARGA|SERVICIO)$", message = "tipoPago debe ser 'RECARGA' o 'SERVICIO'")
    @Schema(example = "RECARGA", allowableValues = {"RECARGA", "SERVICIO"})
    private String tipoPago;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
    @Schema(example = "5512345678", description = "Obligatorio solo si tipoPago = RECARGA")
    private String telefono;

    @Size(min = 1, max = 100, message = "La referencia debe tener entre 1 y 100 caracteres")
    @Schema(example = "40425475190118187271", description = "Obligatorio solo si tipoPago = SERVICIO")
    private String referencia;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 2 decimales")
    @Schema(example = "150.00", description = "Obligatorio en SERVICIO. En RECARGA se toma el precio del producto (si viene, debe coincidir)")
    private BigDecimal monto;
}
