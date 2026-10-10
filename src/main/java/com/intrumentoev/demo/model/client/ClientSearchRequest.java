package com.intrumentoev.demo.model.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filtros de búsqueda para clientes bancarios. Debe proporcionarse al menos un criterio.")
public class ClientSearchRequest {

    @Schema(description = "CURP del cliente (18 caracteres alfanuméricos)", example = "ROMA900101HDFRRN01")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$", message = "El formato del CURP es inválido (debe tener 18 caracteres alfanuméricos)")
    private String curp;

    @Schema(description = "RFC del cliente (12 o 13 caracteres)", example = "ROMA900101ABC")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "El formato del RFC es inválido (debe tener 12 o 13 caracteres)")
    private String rfc;

    @Schema(description = "Correo electrónico asociado", example = "cliente@ejemplo.com")
    @Email(message = "El formato del correo electrónico es inválido")
    private String email;

    @Schema(description = "Número de cuenta bancaria de 10 dígitos", example = "1234567890")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos numéricos")
    private String numeroCuenta;

    @Schema(description = "Estatus activo del cliente", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha inicial de registro (ISO-8601)", example = "2026-01-01T00:00:00Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime desde;

    @Schema(description = "Fecha final de registro (ISO-8601)", example = "2026-12-31T23:59:59Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime hasta;

    public boolean hasAtLeastOneFilter() {
        return (curp != null && !curp.isBlank()) ||
               (rfc != null && !rfc.isBlank()) ||
               (email != null && !email.isBlank()) ||
               (numeroCuenta != null && !numeroCuenta.isBlank()) ||
               (activo != null) ||
               (desde != null) ||
               (hasta != null);
    }
}
