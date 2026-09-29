package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.exception.*;
import com.intrumentoev.demo.mapper.auth.AuthMapper;
import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.service.impl.auth.AuthServiceImpl;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.auth.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private com.intrumentoev.demo.service.service.auth.AesEncryptionService aesEncryptionService;

    @Mock
    private com.intrumentoev.demo.service.service.auth.ServerSessionManager serverSessionManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private Auth mockAuth;
    private final String rawPassword = "Password123#_";
    private final String encodedPassword = "10000:salt:hash";
    private final byte[] mockBiometricBytes = "huella-digital-vector-123".getBytes(StandardCharsets.UTF_8);
    private String base64Biometric;

    @BeforeEach
    void setUp() {
        base64Biometric = Base64.getEncoder().encodeToString(mockBiometricBytes);

        mockAuth = Auth.builder()
                .idLogin(1L)
                .idClient(10L)
                .email("cliente@banco.com")
                .passwordHash(encodedPassword)
                .biometricType("HUELLA")
                .biometricTemplate(mockBiometricBytes)
                .biometricRegisteredAt(OffsetDateTime.now())
                .refreshToken("old-refresh-token")
                .refreshTokenExpiresAt(OffsetDateTime.now().plusDays(7))
                .isActive(true)
                .failedAttempts(0)
                .build();
    }

    @Test
    @DisplayName("Login tradicional exitoso retorna AuthResponse con access y refresh tokens")
    void testLoginTradicionalExitoso() {
        AuthRequest request = new AuthRequest("cliente@banco.com", rawPassword);

        when(authRepository.findByEmail("cliente@banco.com")).thenReturn(Optional.of(mockAuth));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenService.generateAccessToken(anyLong(), anyLong(), anyString())).thenReturn("mock-access-jwt");
        when(jwtTokenService.generateRefreshToken()).thenReturn("mock-new-refresh-token");
        when(jwtTokenService.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse expectedResponse = AuthResponse.builder()
                .token("mock-access-jwt")
                .refreshToken("mock-new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .idClient(10L)
                .email("cliente@banco.com")
                .isActive(true)
                .hasBiometricRegistered(true)
                .biometricType("HUELLA")
                .build();

        when(authMapper.toResponse(eq(mockAuth), eq("mock-access-jwt"), eq("mock-new-refresh-token"), eq(3600L)))
                .thenReturn(expectedResponse);

        AuthResponse actual = authService.login(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getToken()).isEqualTo("mock-access-jwt");
        assertThat(actual.getRefreshToken()).isEqualTo("mock-new-refresh-token");
        assertThat(mockAuth.getFailedAttempts()).isEqualTo(0);
        verify(authRepository).save(mockAuth);
    }

    @Test
    @DisplayName("Login con contraseña incorrecta incrementa failedAttempts y lanza InvalidCredentialsException")
    void testLoginPasswordIncorrecto() {
        AuthRequest request = new AuthRequest("cliente@banco.com", "WrongPassword123!");

        when(authRepository.findByEmail("cliente@banco.com")).thenReturn(Optional.of(mockAuth));
        when(passwordEncoder.matches("WrongPassword123!", encodedPassword)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(mockAuth.getFailedAttempts()).isEqualTo(1);
        verify(authRepository).save(mockAuth);
    }

    @Test
    @DisplayName("Login falla con AccountLockedException cuando failedAttempts >= 5")
    void testLoginCuentaBloqueada() {
        mockAuth.setFailedAttempts(5);
        AuthRequest request = new AuthRequest("cliente@banco.com", rawPassword);

        when(authRepository.findByEmail("cliente@banco.com")).thenReturn(Optional.of(mockAuth));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("Login biométrico exitoso valida template y genera tokens tipo Mercado Libre")
    void testLoginBiometricoExitoso() {
        BiometricLoginRequest request = new BiometricLoginRequest(
                "cliente@banco.com",
                "HUELLA",
                base64Biometric,
                "device-pixel-8"
        );

        when(authRepository.findByEmail("cliente@banco.com")).thenReturn(Optional.of(mockAuth));
        when(aesEncryptionService.decryptBytes(mockBiometricBytes)).thenReturn(mockBiometricBytes);
        when(jwtTokenService.generateAccessToken(anyLong(), anyLong(), anyString())).thenReturn("mock-bio-jwt");
        when(jwtTokenService.generateRefreshToken()).thenReturn("mock-bio-refresh-token");
        when(jwtTokenService.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse expected = AuthResponse.builder()
                .token("mock-bio-jwt")
                .refreshToken("mock-bio-refresh-token")
                .hasBiometricRegistered(true)
                .build();

        when(authMapper.toResponse(eq(mockAuth), anyString(), anyString(), anyLong())).thenReturn(expected);

        AuthResponse actual = authService.loginBiometrico(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getToken()).isEqualTo("mock-bio-jwt");
        verify(authRepository).save(mockAuth);
    }

    @Test
    @DisplayName("Login biométrico con template no coincidente lanza BiometricAuthenticationException")
    void testLoginBiometricoNoCoincide() {
        String invalidBiometric = Base64.getEncoder().encodeToString("otra-huella".getBytes(StandardCharsets.UTF_8));
        BiometricLoginRequest request = new BiometricLoginRequest("cliente@banco.com", "HUELLA", invalidBiometric, "device-1");

        when(authRepository.findByEmail("cliente@banco.com")).thenReturn(Optional.of(mockAuth));
        when(aesEncryptionService.decryptBytes(mockBiometricBytes)).thenReturn(mockBiometricBytes);

        assertThatThrownBy(() -> authService.loginBiometrico(request))
                .isInstanceOf(BiometricAuthenticationException.class);

        assertThat(mockAuth.getFailedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Crear credenciales automáticamente al terminar creación de usuario")
    void testCrearCredencialesCliente() {
        when(authRepository.existsByEmail("nuevo@banco.com")).thenReturn(false);
        when(authRepository.existsByIdClient(20L)).thenReturn(false);
        when(passwordEncoder.encode("SecurePass1234#")).thenReturn("hash-1234");
        when(aesEncryptionService.encryptBytes(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(Auth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Auth creado = authService.crearCredencialesCliente(20L, "nuevo@banco.com", "SecurePass1234#", "FACIAL", base64Biometric);

        assertThat(creado).isNotNull();
        assertThat(creado.getIdClient()).isEqualTo(20L);
        assertThat(creado.getEmail()).isEqualTo("nuevo@banco.com");
        assertThat(creado.getBiometricType()).isEqualTo("FACIAL");
        assertThat(creado.getBiometricTemplate()).isEqualTo(mockBiometricBytes);
        assertThat(creado.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Refrescar token exitosamente renueva Access Token y rota Refresh Token")
    void testRefrescarTokenExitoso() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(authRepository.findByRefreshToken("valid-refresh-token")).thenReturn(Optional.of(mockAuth));
        when(jwtTokenService.generateAccessToken(anyLong(), anyLong(), anyString())).thenReturn("new-jwt");
        when(jwtTokenService.generateRefreshToken()).thenReturn("rotated-refresh-token");
        when(jwtTokenService.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse expected = AuthResponse.builder().token("new-jwt").refreshToken("rotated-refresh-token").build();
        when(authMapper.toResponse(eq(mockAuth), eq("new-jwt"), eq("rotated-refresh-token"), eq(3600L))).thenReturn(expected);

        AuthResponse response = authService.refrescarToken(request);

        assertThat(response.getToken()).isEqualTo("new-jwt");
        assertThat(response.getRefreshToken()).isEqualTo("rotated-refresh-token");
        verify(authRepository).save(mockAuth);
    }

    @Test
    @DisplayName("Refrescar token expirado lanza TokenExpiredOrInvalidException")
    void testRefrescarTokenExpirado() {
        mockAuth.setRefreshTokenExpiresAt(OffsetDateTime.now().minusDays(1));
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

        when(authRepository.findByRefreshToken("expired-token")).thenReturn(Optional.of(mockAuth));

        assertThatThrownBy(() -> authService.refrescarToken(request))
                .isInstanceOf(TokenExpiredOrInvalidException.class);
    }
}
