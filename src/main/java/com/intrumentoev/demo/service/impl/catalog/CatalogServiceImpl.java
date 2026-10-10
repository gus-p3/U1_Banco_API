package com.intrumentoev.demo.service.impl.catalog;

import com.intrumentoev.demo.client.inegi.InegiClient;
import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.mapper.catalog.CatalogMapper;
import com.intrumentoev.demo.model.catalog.CatalogSyncResponse;
import com.intrumentoev.demo.model.catalog.GenderResponse;
import com.intrumentoev.demo.model.catalog.MaritalStatusResponse;
import com.intrumentoev.demo.model.catalog.MunicipalityResponse;
import com.intrumentoev.demo.model.catalog.NationalityResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;
import com.intrumentoev.demo.repository.catalogs.GenderRepository;
import com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.catalogs.NationalityRepository;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import com.intrumentoev.demo.service.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogServiceImpl implements CatalogService {

    private static final String REDIS_KEY_STATES = "catalogo:estados";
    private static final String REDIS_KEY_MUNICIPALITIES_PREFIX = "catalogo:municipios:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final GenderRepository genderRepository;
    private final NationalityRepository nationalityRepository;
    private final MaritalStatusRepository maritalStatusRepository;
    private final InegiClient inegiClient;
    private final CatalogMapper catalogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public List<StateResponse> obtenerEstados() {
        log.info("Obteniendo catálogo de estados...");

        // 1. Intentar obtener desde Redis
        try {
            Object cached = redisTemplate.opsForValue().get(REDIS_KEY_STATES);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                log.info("Estados recuperados exitosamente desde caché Redis ({} registros)", list.size());
                return (List<StateResponse>) cached;
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar a Redis o recuperar estados de caché: {}. Continuando con PostgreSQL", e.getMessage());
        }

        // 2. Si no está en Redis, consultar PostgreSQL
        List<State> estadosDb = stateRepository.findByIsActiveTrueOrderByNameAsc();
        if (estadosDb.isEmpty()) {
            log.info("No hay estados en PostgreSQL. Iniciando sincronización automática desde INEGI...");
            sincronizarCatalogos();
            estadosDb = stateRepository.findByIsActiveTrueOrderByNameAsc();
        }

        List<StateResponse> response = catalogMapper.toStateResponseList(estadosDb);

        // 3. Guardar en Redis
        try {
            redisTemplate.opsForValue().set(REDIS_KEY_STATES, response, CACHE_TTL);
            log.info("Estados guardados en caché Redis con TTL de 24h");
        } catch (Exception e) {
            log.warn("No se pudo guardar estados en Redis: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public List<MunicipalityResponse> obtenerMunicipiosPorEstado(String cveEnt) {
        log.info("Obteniendo catálogo de municipios para cve_ent: {}", cveEnt);
        String redisKey = REDIS_KEY_MUNICIPALITIES_PREFIX + cveEnt;

        // 1. Intentar obtener desde Redis
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                log.info("Municipios de cve_ent '{}' recuperados desde Redis ({} registros)", cveEnt, list.size());
                return (List<MunicipalityResponse>) cached;
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar Redis para municipios de '{}': {}. Continuando con PostgreSQL", cveEnt, e.getMessage());
        }

        // 2. Buscar estado en PostgreSQL
        State state = stateRepository.findByCveEnt(cveEnt)
                .orElseThrow(() -> new com.intrumentoev.demo.exception.CatalogNotFoundException("Estado con cve_ent", cveEnt));

        List<Municipality> municipiosDb = municipalityRepository.findByIdStateAndIsActiveTrueOrderByNameAsc(state.getIdState());

        // Si no hay municipios en BD para este estado, sincronizarlos desde INEGI
        if (municipiosDb.isEmpty()) {
            log.info("No hay municipios en BD para el estado {}. Consultando API INEGI...", cveEnt);
            List<InegiMunicipalityDto> inegiMuns = inegiClient.obtenerMunicipios(cveEnt);
            List<Municipality> paraGuardar = new ArrayList<>();
            for (InegiMunicipalityDto dto : inegiMuns) {
                paraGuardar.add(Municipality.builder()
                        .idState(state.getIdState())
                        .cveMun(dto.getCveMun())
                        .name(dto.getNomMun())
                        .isActive(true)
                        .build());
            }
            if (!paraGuardar.isEmpty()) {
                municipiosDb = municipalityRepository.saveAll(paraGuardar);
            }
        }

        List<MunicipalityResponse> response = catalogMapper.toMunicipalityResponseList(municipiosDb);

        // 3. Guardar en Redis
        try {
            redisTemplate.opsForValue().set(redisKey, response, CACHE_TTL);
            log.info("Municipios de cve_ent '{}' guardados en Redis con TTL de 24h", cveEnt);
        } catch (Exception e) {
            log.warn("No se pudo guardar municipios en Redis: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public CatalogSyncResponse sincronizarCatalogos() {
        log.info("Iniciando sincronización completa de catálogos desde APIs de INEGI...");

        List<InegiStateDto> inegiStates = inegiClient.obtenerEstados();
        int totalEstadosSincronizados = 0;
        int totalMunicipiosSincronizados = 0;

        for (InegiStateDto stateDto : inegiStates) {
            Optional<State> optState = stateRepository.findByCveEnt(stateDto.getCveEnt());
            State state;
            if (optState.isPresent()) {
                state = optState.get();
                state.setName(stateDto.getNomEnt());
                state.setIsActive(true);
            } else {
                state = State.builder()
                        .cveEnt(stateDto.getCveEnt())
                        .name(stateDto.getNomEnt())
                        .isActive(true)
                        .build();
            }
            state = stateRepository.save(state);
            totalEstadosSincronizados++;

            // Sincronizar municipios para esta entidad
            List<InegiMunicipalityDto> inegiMuns = inegiClient.obtenerMunicipios(state.getCveEnt());
            for (InegiMunicipalityDto munDto : inegiMuns) {
                Optional<Municipality> optMun = municipalityRepository.findByIdStateAndCveMun(state.getIdState(), munDto.getCveMun());
                Municipality mun;
                if (optMun.isPresent()) {
                    mun = optMun.get();
                    mun.setName(munDto.getNomMun());
                    mun.setIsActive(true);
                } else {
                    mun = Municipality.builder()
                            .idState(state.getIdState())
                            .cveMun(munDto.getCveMun())
                            .name(munDto.getNomMun())
                            .isActive(true)
                            .build();
                }
                municipalityRepository.save(mun);
                totalMunicipiosSincronizados++;
            }
        }

        log.info("Sincronización de INEGI finalizada en PostgreSQL. Estados: {}, Municipios: {}",
                totalEstadosSincronizados, totalMunicipiosSincronizados);

        // Actualizar caché de estados en Redis
        try {
            List<State> todosEstados = stateRepository.findByIsActiveTrueOrderByNameAsc();
            List<StateResponse> stateResponses = catalogMapper.toStateResponseList(todosEstados);
            redisTemplate.opsForValue().set(REDIS_KEY_STATES, stateResponses, CACHE_TTL);
            log.info("Caché de estados en Redis refrescada exitosamente");
        } catch (Exception e) {
            log.warn("No se pudo refrescar la caché de estados en Redis: {}", e.getMessage());
        }

        return CatalogSyncResponse.builder()
                .estadosSincronizados(totalEstadosSincronizados)
                .municipiosSincronizados(totalMunicipiosSincronizados)
                .mensaje("Catálogos de estados y municipios sincronizados exitosamente desde INEGI a PostgreSQL y Redis.")
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenderResponse> obtenerGeneros() {
        log.info("Obteniendo catálogo de géneros...");
        return genderRepository.findByIsActiveTrue().stream()
                .map(g -> GenderResponse.builder()
                        .idGender(g.getIdGender())
                        .name(g.getName())
                        .description(g.getDescription())
                        .isActive(g.getIsActive())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NationalityResponse> obtenerNacionalidades() {
        log.info("Obteniendo catálogo de nacionalidades...");
        return nationalityRepository.findByIsActiveTrue().stream()
                .map(n -> NationalityResponse.builder()
                        .idNationality(n.getIdNationality())
                        .name(n.getName())
                        .description(n.getDescription())
                        .isActive(n.getIsActive())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaritalStatusResponse> obtenerEstadosCiviles() {
        log.info("Obteniendo catálogo de estados civiles...");
        return maritalStatusRepository.findByIsActiveTrue().stream()
                .map(m -> MaritalStatusResponse.builder()
                        .idMaritalStatus(m.getIdMaritalStatus())
                        .name(m.getName())
                        .description(m.getDescription())
                        .isActive(m.getIsActive())
                        .build())
                .toList();
    }
}
