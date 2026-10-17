package com.intrumentoev.demo.model.client;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Criterios opcionales de búsqueda y ordenamiento de clientes")
public class ClientUnifiedQueryRequest {

    @Schema(description = "CURP del cliente (18 caracteres)", example = "ROMA900101HDFRRN01")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$", message = "El formato de CURP debe tener exactamente 18 caracteres válidos")
    private String curp;

    @Schema(description = "RFC del cliente (12 o 13 caracteres)", example = "ROMA900101ABC")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "El formato de RFC debe tener 12 o 13 caracteres alfanuméricos")
    private String rfc;

    @Schema(description = "Identificador del país/nacionalidad", example = "1")
    private Short idNationality;

    @Schema(description = "Estatus de activación del cliente (true=activo, false=inactivo)", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha inicial de registro en formato ISO-8601", example = "2026-01-01T00:00:00Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime desde;

    @Schema(description = "Fecha final de registro en formato ISO-8601", example = "2026-12-31T23:59:59Z")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime hasta;

    @Schema(description = "Campo por el cual ordenar: 'name', 'lastName', 'curp', 'createdAt'", example = "name")
    @Pattern(regexp = "^(name|lastName|curp|rfc|createdAt)$", message = "El campo sortBy solo permite: 'name', 'lastName', 'curp', 'rfc' o 'createdAt'")
    private String sortBy;

    @Schema(description = "Dirección del ordenamiento: 'ASC' o 'DESC'", example = "ASC")
    @Pattern(regexp = "^(ASC|DESC|asc|desc)$", message = "La dirección de ordenamiento debe ser 'ASC' o 'DESC'")
    private String sortDirection;
}
