package com.intrumentoev.demo.client.inegi;

import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalitiesResponse;
import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class InegiClientImpl implements InegiClient {

    private final RestClient restClient;

    public InegiClientImpl(
            @Value("${inegi.api.base-url:https://gaia.inegi.org.mx/wscatgeo/v2}") String baseUrl
    ) {
        log.info("Inicializando InegiClient con Base URL: {}", baseUrl);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<InegiStateDto> obtenerEstados() {
        try {
            log.info("Consultando catálogo de estados en API INEGI: /mgee");
            InegiStatesResponse response = restClient.get()
                    .uri("/mgee")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(InegiStatesResponse.class);

            if (response != null && response.getDatos() != null) {
                log.info("Se obtuvieron {} estados desde INEGI", response.getDatos().size());
                return response.getDatos();
            }
        } catch (Exception e) {
            log.error("Error al consultar estados desde API INEGI: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<InegiMunicipalityDto> obtenerMunicipios(String cveEnt) {
        try {
            log.info("Consultando catálogo de municipios en API INEGI para cve_ent: {}", cveEnt);
            InegiMunicipalitiesResponse response = restClient.get()
                    .uri("/mgem/{cve_ent}", cveEnt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(InegiMunicipalitiesResponse.class);

            if (response != null && response.getDatos() != null) {
                log.info("Se obtuvieron {} municipios para el estado {} desde INEGI", response.getDatos().size(), cveEnt);
                return response.getDatos();
            }
        } catch (Exception e) {
            log.error("Error al consultar municipios para estado {} desde API INEGI: {}", cveEnt, e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}
