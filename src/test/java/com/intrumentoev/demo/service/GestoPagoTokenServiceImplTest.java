package com.intrumentoev.demo.service;

import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;
import com.intrumentoev.demo.mapper.gestopago.GestoPagoTokenMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoAuthResponse;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.service.impl.gestopago.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GestoPagoTokenServiceImplTest {

    private GestoPagoAuthClient authClient;
    private GestoPagoTokenRepository repository;
    private GestoPagoTokenMapper mapper;
    private GestoPagoTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        authClient = mock(GestoPagoAuthClient.class);
        repository = mock(GestoPagoTokenRepository.class);
        mapper = mock(GestoPagoTokenMapper.class);
        service = new GestoPagoTokenServiceImpl(authClient, repository, mapper);
        ReflectionTestUtils.setField(service, "idDistribuidor", 83);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "DISP");
        ReflectionTestUtils.setField(service, "password", "pw");
        ReflectionTestUtils.setField(service, "tokenMaxAgeHours", 23L);
        ReflectionTestUtils.setField(service, "staticBearerToken", "");
    }

    private GestoPagoToken token(String valor, LocalDateTime actualizado) {
        GestoPagoToken t = new GestoPagoToken();
        t.setToken(valor);
        t.setFechaActualizacion(actualizado);
        return t;
    }

    private GestoPagoAuthResponse respuesta(String valor) {
        GestoPagoAuthResponse r = new GestoPagoAuthResponse();
        r.setToken(valor);
        return r;
    }

    @Test
    @DisplayName("Un token estático configurado se usa tal cual y nunca se renueva")
    void tokenEstatico() {
        ReflectionTestUtils.setField(service, "staticBearerToken", "fijo");

        assertThat(service.obtenerTokenVigente()).isEqualTo("fijo");
        service.renovarToken();

        verifyNoInteractions(authClient, repository);
    }

    @Test
    @DisplayName("Un token guardado y reciente se reutiliza")
    void reutilizaVigente() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP"))
                .thenReturn(Optional.of(token("guardado", LocalDateTime.now().minusHours(1))));

        assertThat(service.obtenerTokenVigente()).isEqualTo("guardado");
        service.renovarToken();

        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("Sin token guardado se solicita uno nuevo y se persiste")
    void solicitaNuevo() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP")).thenReturn(Optional.empty());
        when(authClient.authenticate(83, "DISP", "pw")).thenReturn(respuesta("nuevo"));
        when(mapper.toEntity(any())).thenAnswer(i -> token("nuevo", null));

        assertThat(service.obtenerTokenVigente()).isEqualTo("nuevo");

        verify(repository).save(argThat(t -> t.getIdDistribuidor() == 83 && Boolean.TRUE.equals(t.getActivo())
                && "DISP".equals(t.getCodigoDispositivo()) && t.getFechaActualizacion() != null));
    }

    @Test
    @DisplayName("Un token vencido se actualiza sobre la misma fila")
    void renuevaVencido() {
        GestoPagoToken viejo = token("viejo", LocalDateTime.now().minusHours(30));
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP")).thenReturn(Optional.of(viejo));
        when(authClient.authenticate(83, "DISP", "pw")).thenReturn(respuesta("fresco"));
        doAnswer(i -> {
            ((GestoPagoToken) i.getArgument(1)).setToken("fresco");
            return null;
        }).when(mapper).updateEntity(any(), any());

        assertThat(service.obtenerTokenVigente()).isEqualTo("fresco");
        verify(repository).save(viejo);
    }

    @Test
    @DisplayName("Un token sin texto o sin fechas se considera vencido")
    void tokenVacioVencido() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP"))
                .thenReturn(Optional.of(token(" ", LocalDateTime.now())));
        when(authClient.authenticate(any(), any(), any())).thenReturn(respuesta("x"));

        service.obtenerTokenVigente();

        verify(authClient).authenticate(83, "DISP", "pw");
    }

    @Test
    @DisplayName("renovarTokenForzado siempre pide uno nuevo; sin token en la respuesta falla")
    void forzado() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP")).thenReturn(Optional.empty());
        when(authClient.authenticate(83, "DISP", "pw")).thenReturn(respuesta("otro"), respuesta(" "), null);
        when(mapper.toEntity(any())).thenAnswer(i -> token("otro", null));

        assertThat(service.renovarTokenForzado()).isEqualTo("otro");
        assertThatThrownBy(() -> service.renovarTokenForzado()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.renovarTokenForzado()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("renovarToken programado no propaga errores de GestoPago")
    void renovarTokenTolerante() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(83, "DISP")).thenReturn(Optional.empty());
        when(authClient.authenticate(any(), any(), any())).thenThrow(new RuntimeException("caído"));

        service.renovarToken();

        verify(repository, never()).save(any());
    }
}
