package com.intrumentoev.demo.client.inegi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InegiMunicipalityDto implements Serializable {

    @JsonProperty("cve_ent")
    private String cveEnt;

    @JsonProperty("cve_mun")
    private String cveMun;

    @JsonProperty("nomgeo")
    private String nomMun;
}
