package com.intrumentoev.demo.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoPatchRequest {

    @Size(min = 1, max = 150, message = "El campo 'producto' debe tener entre 1 y 150 caracteres")
    private String producto;

    @Size(min = 1, max = 150, message = "El campo 'servicio' debe tener entre 1 y 150 caracteres")
    private String servicio;

    @Positive(message = "El campo 'idServicio' debe ser positivo")
    private Integer idServicio;

    @Positive(message = "El campo 'idProducto' debe ser positivo")
    private Integer idProducto;

    @Min(value = 0, message = "El campo 'idCatTipoServicio' no puede ser negativo")
    private Integer idCatTipoServicio;

    @Min(value = 0, message = "El campo 'tipoFront' no puede ser negativo")
    private Integer tipoFront;

    private Boolean hasDigitoVerificador;

    @Size(max = 120, message = "El campo 'tipoReferencia' no puede superar 120 caracteres")
    private String tipoReferencia;

    @Size(min = 1, max = 30, message = "El campo 'precio' debe tener entre 1 y 30 caracteres")
    private String precio;

    private Boolean showAyuda;

    @Size(max = 500, message = "El campo 'legend' no puede superar 500 caracteres")
    private String legend;

    @Size(max = 80, message = "El campo 'categoria' no puede superar 80 caracteres")
    private String categoria;
}
