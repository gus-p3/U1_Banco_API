package com.intrumentoev.demo.model.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponse {

    private Long idHome;
    private Long idClient;
    private String street;
    private String exteriorNumber;
    private String interiorNumber;
    private String neighborhood;
    private Integer idMunicipality;
    private String municipalityName;
    private Short idState;
    private String stateName;
    private String postalCode;
    private String country;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
