package com.intrumentoev.demo.service.gestopago;

import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.entity.gestopago.GestoPagoToken;
import com.intrumentoev.demo.mapper.gestopago.GestoPagoTokenMapper;
import com.intrumentoev.demo.model.gestopago.GestoPagoAuthResponse;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.service.impl.gestopago.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestoPagoTokenServiceTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 123);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "DEV-01");
        ReflectionTestUtils.setField(tokenService, "password", "pass123");
        ReflectionTestUtils.setField(tokenService, "tokenMaxAgeHours", 23L);
        ReflectionTestUtils.setField(tokenService, "staticBearerToken", "");
    }

    @Test
    void obtenerTokenVigente_TokenExistenteValido() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid-token");
        token.setFechaActualizacion(LocalDateTime.now().minusHours(2));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.of(token));

        String result = tokenService.obtenerTokenVigente();

        assertEquals("valid-token", result);
        verify(gestoPagoAuthClient, never()).authenticate(any(), any(), any());
    }

    @Test
    void obtenerTokenVigente_StaticTokenConfigurado() {
        ReflectionTestUtils.setField(tokenService, "staticBearerToken", "static-token-abc");

        String result = tokenService.obtenerTokenVigente();

        assertEquals("static-token-abc", result);
        verify(tokenRepository, never()).findByIdDistribuidorAndCodigoDispositivo(any(), any());
    }

    @Test
    void obtenerTokenVigente_TokenExpirado_PideNuevoYActualizaExistente() {
        GestoPagoToken existingToken = new GestoPagoToken();
        existingToken.setToken("old-token");
        existingToken.setFechaActualizacion(LocalDateTime.now().minusHours(25));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.of(existingToken));

        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("new-token-123");
        when(gestoPagoAuthClient.authenticate(123, "DEV-01", "pass123")).thenReturn(authResponse);

        when(tokenRepository.save(any(GestoPagoToken.class))).thenAnswer(i -> i.getArgument(0));

        String result = tokenService.obtenerTokenVigente();

        assertEquals("old-token", result); // updateEntity modifies existingToken mock or returned
        verify(tokenMapper).updateEntity(authResponse, existingToken);
        verify(tokenRepository).save(existingToken);
    }

    @Test
    void renovarTokenForzado_CreaNuevoTokenSiNoExiste() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.empty());

        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("created-token");
        when(gestoPagoAuthClient.authenticate(123, "DEV-01", "pass123")).thenReturn(authResponse);

        GestoPagoToken nuevo = new GestoPagoToken();
        nuevo.setToken("created-token");
        when(tokenMapper.toEntity(authResponse)).thenReturn(nuevo);
        when(tokenRepository.save(any(GestoPagoToken.class))).thenAnswer(i -> i.getArgument(0));

        String result = tokenService.renovarTokenForzado();

        assertEquals("created-token", result);
        verify(tokenRepository).save(nuevo);
    }

    @Test
    void renovarToken_Expirado_LlamaSolicitarYGuardar() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.empty());

        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("renewed-token");
        when(gestoPagoAuthClient.authenticate(123, "DEV-01", "pass123")).thenReturn(authResponse);

        GestoPagoToken nuevo = new GestoPagoToken();
        nuevo.setToken("renewed-token");
        when(tokenMapper.toEntity(authResponse)).thenReturn(nuevo);

        tokenService.renovarToken();

        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    void renovarToken_Vigente_NoRenueva() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid-token");
        token.setFechaActualizacion(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.of(token));

        tokenService.renovarToken();

        verify(gestoPagoAuthClient, never()).authenticate(any(), any(), any());
    }

    @Test
    void solicitarToken_RespuestaVacia_LanzaExcepcion() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(123, "DEV-01"))
                .thenReturn(Optional.empty());
        when(gestoPagoAuthClient.authenticate(123, "DEV-01", "pass123")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> tokenService.renovarTokenForzado());
    }
}
