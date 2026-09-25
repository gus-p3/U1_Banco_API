package com.intrumentoev.demo.client.inegi;

import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;

import java.util.List;

public interface InegiClient {

    /**
     * Consume la API de INEGI para obtener todos los estados de México.
     * GET https://gaia.inegi.org.mx/wscatgeo/v2/mgee
     *
     * @return Lista de estados (cve_ent, nom_ent, nom_abr).
     */
    List<InegiStateDto> obtenerEstados();

    /**
     * Consume la API de INEGI para obtener los municipios de un estado dado por cve_ent.
     * GET https://gaia.inegi.org.mx/wscatgeo/v2/mgem/{cve_ent}
     *
     * @param cveEnt Clave de entidad federativa de 2 dígitos (ej: "01", "09").
     * @return Lista de municipios de la entidad.
     */
    List<InegiMunicipalityDto> obtenerMunicipios(String cveEnt);
}
