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
public class StateResponse implements Serializable {

    private Short idState;
    private String cveEnt;
    private String name;
    private Boolean isActive;
}
