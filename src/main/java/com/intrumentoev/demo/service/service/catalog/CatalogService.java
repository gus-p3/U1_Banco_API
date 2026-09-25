package com.intrumentoev.demo.service.service.catalog;

import com.intrumentoev.demo.model.catalog.CatalogSyncResponse;
import com.intrumentoev.demo.model.catalog.MunicipalityResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;

import java.util.List;

public interface CatalogService {

    /**
     * Obtiene todos los estados activos.
     * Estrategia de caché: Primero consulta Redis. Si no está disponible o falla,
     * consulta PostgreSQL. Si la BD está vacía, sincroniza desde la API INEGI.
     */
    List<StateResponse> obtenerEstados();

    /**
     * Obtiene los municipios correspondientes a una entidad federativa (cve_ent).
     * Estrategia de caché: Primero consulta Redis. Si no está en caché, consulta PostgreSQL.
     */
    List<MunicipalityResponse> obtenerMunicipiosPorEstado(String cveEnt);

    /**
     * Sincroniza los estados y municipios desde las APIs de INEGI,
     * persistiéndolos en PostgreSQL y actualizando la caché en Redis.
     */
    CatalogSyncResponse sincronizarCatalogos();
}
