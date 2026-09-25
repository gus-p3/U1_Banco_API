package com.intrumentoev.demo.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MunicipalityResponse implements Serializable {

    private Integer idMunicipality;
    private Short idState;
    private String cveMun;
    private String name;
    private Boolean isActive;
}
