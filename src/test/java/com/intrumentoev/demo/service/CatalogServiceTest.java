package com.intrumentoev.demo.service;

import com.intrumentoev.demo.client.inegi.InegiClient;
import com.intrumentoev.demo.client.inegi.dto.InegiMunicipalityDto;
import com.intrumentoev.demo.client.inegi.dto.InegiStateDto;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.mapper.catalog.CatalogMapper;
import com.intrumentoev.demo.model.catalog.CatalogSyncResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;
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
    @DisplayName("Obtener municipios por estado lanza CatalogNotFoundException cuando no existe el estado")
    void testObtenerMunicipiosPorEstadoNoExiste() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:municipios:99")).thenReturn(null);
        when(stateRepository.findByCveEnt("99")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> catalogService.obtenerMunicipiosPorEstado("99"))
                .isInstanceOf(com.intrumentoev.demo.exception.CatalogNotFoundException.class)
                .hasMessageContaining("99");
    }
}
