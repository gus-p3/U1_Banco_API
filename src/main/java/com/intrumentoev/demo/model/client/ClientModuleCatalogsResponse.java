package com.intrumentoev.demo.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Catálogos disponibles requeridos para edición por módulos en frontend (selects y dropdowns).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientModuleCatalogsResponse {

    private List<Map<String, Object>> genders;
    private List<Map<String, Object>> nationalities;
    private List<Map<String, Object>> maritalStatuses;
    private Map<String, Object> municipality;
    private Map<String, Object> state;
}
