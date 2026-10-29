package com.intrumentoev.demo.model.gestopago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String producto;

    @NotBlank(message = "El campo 'servicio' es obligatorio")
    private String servicio;

    @NotNull(message = "El campo 'idServicio' es obligatorio")
    private Integer idServicio;

    @NotNull(message = "El campo 'idProducto' es obligatorio")
    private Integer idProducto;

    @NotNull(message = "El campo 'idCatTipoServicio' es obligatorio")
    private Integer idCatTipoServicio;

    @NotNull(message = "El campo 'tipoFront' es obligatorio")
    private Integer tipoFront;

    @NotNull(message = "El campo 'hasDigitoVerificador' es obligatorio")
    private Boolean hasDigitoVerificador;

    private String tipoReferencia;

    @NotBlank(message = "El campo 'precio' es obligatorio")
    private String precio;

    @NotNull(message = "El campo 'showAyuda' es obligatorio")
    private Boolean showAyuda;
}
