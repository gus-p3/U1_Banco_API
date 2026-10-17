package com.intrumentoev.demo.model.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para baja lógica de cliente mediante CURP o RFC en cuerpo")
public class ClientDeactivateRequest {

    @NotBlank(message = "El identificador (CURP o RFC) es estrictamente obligatorio")
    @Pattern(
            regexp = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$",
            message = "El identificador debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido"
    )
    @Schema(description = "CURP o RFC del cliente a desactivar", example = "ROMA900101HDFRRN01")
    private String identificador;

    @Size(max = 255, message = "El motivo no puede exceder los 255 caracteres")
    @Schema(description = "Motivo opcional de la baja lógica", example = "Solicitud voluntaria del cuentahabiente")
    private String reason;
}
