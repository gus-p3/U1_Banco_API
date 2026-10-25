package com.intrumentoev.demo.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoPatchRequest {

    private String producto;
    private String servicio;
    private Integer idServicio;
    private Integer idProducto;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private String tipoReferencia;
    private String precio;
    private Boolean showAyuda;
}
