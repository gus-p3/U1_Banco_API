package com.intrumentoev.demo.model.gestopago;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "El campo 'producto' es obligatorio")
    @Size(max = 150, message = "El campo 'producto' no puede superar 150 caracteres")
    private String producto;

    @NotBlank(message = "El campo 'servicio' es obligatorio")
    @Size(max = 150, message = "El campo 'servicio' no puede superar 150 caracteres")
    private String servicio;

    @NotNull(message = "El campo 'idServicio' es obligatorio")
    @Positive(message = "El campo 'idServicio' debe ser positivo")
    private Integer idServicio;

    @NotNull(message = "El campo 'idProducto' es obligatorio")
    @Positive(message = "El campo 'idProducto' debe ser positivo")
    private Integer idProducto;

    @NotNull(message = "El campo 'idCatTipoServicio' es obligatorio")
    @Min(value = 0, message = "El campo 'idCatTipoServicio' no puede ser negativo")
    private Integer idCatTipoServicio;

    @NotNull(message = "El campo 'tipoFront' es obligatorio")
    @Min(value = 0, message = "El campo 'tipoFront' no puede ser negativo")
    private Integer tipoFront;

    @NotNull(message = "El campo 'hasDigitoVerificador' es obligatorio")
    private Boolean hasDigitoVerificador;

    @Size(max = 120, message = "El campo 'tipoReferencia' no puede superar 120 caracteres")
    private String tipoReferencia;

    @NotBlank(message = "El campo 'precio' es obligatorio")
    @Size(max = 30, message = "El campo 'precio' no puede superar 30 caracteres")
    private String precio;

    @NotNull(message = "El campo 'showAyuda' es obligatorio")
    private Boolean showAyuda;

    @Size(max = 500, message = "El campo 'legend' no puede superar 500 caracteres")
    private String legend;

    @Size(max = 80, message = "El campo 'categoria' no puede superar 80 caracteres")
    private String categoria;
}
