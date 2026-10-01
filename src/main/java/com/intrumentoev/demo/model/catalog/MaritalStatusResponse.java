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
public class MaritalStatusResponse implements Serializable {

    private Short idMaritalStatus;
    private String name;
    private String description;
    private Boolean isActive;
}
