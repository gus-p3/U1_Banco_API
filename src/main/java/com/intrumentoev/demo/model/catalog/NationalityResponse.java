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
public class NationalityResponse implements Serializable {

    private Short idNationality;
    private String name;
    private String description;
    private Boolean isActive;
}
