package com.intrumentoev.demo.model.gestopago.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * codigo sigue la convención de GestoPago: 1 = pago aplicado (CODIGO "01"), 0 = no se pudo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoResponse {

    @Schema(example = "1", description = "1 = aplicado, 0 = no se pudo / en proceso")
    private Integer codigo;

    @Schema(example = "Transacción aplicada exitosamente")
    private String mensaje;

    @Schema(example = "APLICADO", allowableValues = {"APLICADO", "RECHAZADO", "POR_CONFIRMAR"})
    private EstadoPago estado;

    @Schema(example = "01", description = "CODIGO devuelto por GestoPago")
    private String codigoGestoPago;

    private String numAutorizacion;
    private Long idTx;

    @Schema(example = "BXMGH3K9P2Q7R4T", description = "Identificador único del pago; úsalo para consultar un pago POR_CONFIRMAR")
    private String upc;
}
