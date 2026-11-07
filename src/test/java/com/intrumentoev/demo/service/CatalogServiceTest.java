package com.intrumentoev.demo.service;

import com.intrumentoev.demo.client.inegi.InegiClient;
import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import com.intrumentoev.demo.entity.catalogs.Gender;
import com.intrumentoev.demo.entity.catalogs.MaritalStatus;
import com.intrumentoev.demo.entity.catalogs.Nationality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.mapper.catalog.CatalogMapper;
import com.intrumentoev.demo.model.catalog.*;
import com.intrumentoev.demo.repository.catalogs.GenderRepository;
import com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository;
import com.intrumentoev.demo.repository.catalogs.NationalityRepository;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import com.intrumentoev.demo.service.impl.catalog.CatalogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private GenderRepository genderRepository;

    @Mock
    private NationalityRepository nationalityRepository;

    @Mock
    private MaritalStatusRepository maritalStatusRepository;

    @Mock
    private InegiClient inegiClient;

    @Mock
    private CatalogMapper catalogMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    @Test
    @DisplayName("Obtener estados desde caché Redis cuando está disponible")
    void testObtenerEstadosDesdeRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<StateResponse> cached = List.of(
                StateResponse.builder().idState((short) 1).cveEnt("01").name("Aguascalientes").isActive(true).build()
        );
        when(valueOperations.get("catalogo:estados")).thenReturn(cached);

        List<StateResponse> result = catalogService.obtenerEstados();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Aguascalientes");
        verifyNoInteractions(stateRepository);
    }

    @Test
    @DisplayName("Obtener estados desde PostgreSQL cuando Redis no tiene datos")
    void testObtenerEstadosFallbackPostgres() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:estados")).thenReturn(null);

        State state = State.builder().idState((short) 1).cveEnt("01").name("Aguascalientes").isActive(true).build();
        when(stateRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(state));

        StateResponse stateResponse = StateResponse.builder().idState((short) 1).cveEnt("01").name("Aguascalientes").isActive(true).build();
        when(catalogMapper.toStateResponseList(List.of(state))).thenReturn(List.of(stateResponse));

        List<StateResponse> result = catalogService.obtenerEstados();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCveEnt()).isEqualTo("01");
        verify(valueOperations, times(1)).set(eq("catalogo:estados"), any(), any());
    }

    @Test
    @DisplayName("Sincronizar catálogos desde INEGI y almacenar en PostgreSQL y Redis")
    void testSincronizarCatalogos() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        InegiStateDto stateDto = InegiStateDto.builder().cveEnt("01").nomEnt("Aguascalientes").build();
        when(inegiClient.obtenerEstados()).thenReturn(List.of(stateDto));

        State state = State.builder().idState((short) 1).cveEnt("01").name("Aguascalientes").isActive(true).build();
        when(stateRepository.findByCveEnt("01")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenReturn(state);

        InegiMunicipalityDto munDto = InegiMunicipalityDto.builder().cveEnt("01").cveMun("001").nomMun("Aguascalientes").build();
        when(inegiClient.obtenerMunicipios("01")).thenReturn(List.of(munDto));
        when(municipalityRepository.findByIdStateAndCveMun((short) 1, "001")).thenReturn(Optional.empty());

        CatalogSyncResponse response = catalogService.sincronizarCatalogos();

        assertThat(response).isNotNull();
        assertThat(response.getEstadosSincronizados()).isEqualTo(1);
        assertThat(response.getMunicipiosSincronizados()).isEqualTo(1);
        verify(stateRepository, times(1)).save(any(State.class));
        verify(municipalityRepository, times(1)).save(any(Municipality.class));
        verify(valueOperations, times(1)).set(eq("catalogo:estados"), any(), any());
    }

    @Test
    @DisplayName("Obtener municipios por estado lanza BusinessException cuando no existe el estado")
    void testObtenerMunicipiosPorEstadoNoExiste() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:municipios:99")).thenReturn(null);
        when(stateRepository.findByCveEnt("99")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> catalogService.obtenerMunicipiosPorEstado("99"))
                .isInstanceOf(com.intrumentoev.demo.exception.BusinessException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Obtener municipios desde caché Redis")
    void testObtenerMunicipiosDesdeRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<MunicipalityResponse> cached = List.of(
                MunicipalityResponse.builder().idMunicipality(1).cveMun("001").name("Aguascalientes").build()
        );
        when(valueOperations.get("catalogo:municipios:01")).thenReturn(cached);

        List<MunicipalityResponse> result = catalogService.obtenerMunicipiosPorEstado("01");

        assertThat(result).hasSize(1);
        verifyNoInteractions(stateRepository);
    }

    @Test
    @DisplayName("Obtener municipios desde BD cuando Redis no tiene datos")
    void testObtenerMunicipiosDesdeDb() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:municipios:01")).thenReturn(null);

        State state = State.builder().idState((short) 1).cveEnt("01").name("Aguascalientes").build();
        when(stateRepository.findByCveEnt("01")).thenReturn(Optional.of(state));

        Municipality mun = Municipality.builder().idMunicipality(1).idState((short) 1).cveMun("001").name("Aguascalientes").build();
        when(municipalityRepository.findByIdStateAndIsActiveTrueOrderByNameAsc((short) 1)).thenReturn(List.of(mun));

        MunicipalityResponse munResp = MunicipalityResponse.builder().idMunicipality(1).cveMun("001").name("Aguascalientes").build();
        when(catalogMapper.toMunicipalityResponseList(List.of(mun))).thenReturn(List.of(munResp));

        List<MunicipalityResponse> result = catalogService.obtenerMunicipiosPorEstado("01");

        assertThat(result).hasSize(1);
        verify(valueOperations).set(eq("catalogo:municipios:01"), any(), any());
    }

    @Test
    @DisplayName("Obtener géneros activos")
    void testObtenerGeneros() {
        Gender gender = Gender.builder().idGender((short) 1).name("Hombre").description("Masculino").isActive(true).build();
        when(genderRepository.findByIsActiveTrue()).thenReturn(List.of(gender));

        List<GenderResponse> result = catalogService.obtenerGeneros();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Hombre");
    }

    @Test
    @DisplayName("Obtener nacionalidades activas")
    void testObtenerNacionalidades() {
        Nationality nat = Nationality.builder().idNationality((short) 1).name("Mexicana").description("México").isActive(true).build();
        when(nationalityRepository.findByIsActiveTrue()).thenReturn(List.of(nat));

        List<NationalityResponse> result = catalogService.obtenerNacionalidades();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mexicana");
    }

    @Test
    @DisplayName("Obtener estados civiles activos")
    void testObtenerEstadosCiviles() {
        MaritalStatus status = MaritalStatus.builder().idMaritalStatus((short) 1).name("Soltero").description("Soltero").isActive(true).build();
        when(maritalStatusRepository.findByIsActiveTrue()).thenReturn(List.of(status));

        List<MaritalStatusResponse> result = catalogService.obtenerEstadosCiviles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Soltero");
    }
}
