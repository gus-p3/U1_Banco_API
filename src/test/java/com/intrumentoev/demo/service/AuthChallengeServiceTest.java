package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.entity.auth.AuthChallenge;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.mapper.auth.AuthMapper;
import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.repository.auth.AuthChallengeRepository;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.service.impl.auth.AuthChallengeServiceImpl;
import com.intrumentoev.demo.service.service.auth.EmailService;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.gestopago.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthChallengeServiceTest {

    @Mock private AuthChallengeRepository challengeRepository;
    @Mock private AuthRepository authRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmailService emailService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuthMapper authMapper;

    @InjectMocks
    private AuthChallengeServiceImpl service;

    private Auth auth;
    private AuthenticatedClient principal;

    @BeforeEach
    void setUp() {
        auth = Auth.builder()
                .idLogin(1L)
                .idClient(10L)
                .email("user@banco.com")
                .passwordHash("hash")
                .emailVerified(true)
                .isActive(true)
                .biometricType("HUELLA")
                .biometricTemplate("tpl".getBytes(StandardCharsets.UTF_8))
                .build();
        principal = AuthenticatedClient.builder()
                .idClient(10L)
                .idLogin(1L)
                .email("user@banco.com")
                .role(Role.CLIENT)
                .authMethod(AuthMethod.BIOMETRIC_HUELLA)
                .build();
    }

    @Test
    void crearDesafioVerificacionCuenta_enviaCodigoYVerifica() {
        when(challengeRepository.save(any(AuthChallenge.class))).thenAnswer(i -> i.getArgument(0));
        when(authRepository.save(any(Auth.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse resp = service.crearDesafioVerificacionCuenta(auth, "Brandon");

        assertThat(resp.getRequiresVerification()).isTrue();
        assertThat(resp.getVerificationPurpose()).isEqualTo("EMAIL_VERIFICATION");
        assertThat(resp.getCodeSent()).isTrue();
        assertThat(auth.getEmailVerified()).isFalse();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoVerificacion(eq("user@banco.com"), eq("Brandon"), codeCaptor.capture(), eq("EMAIL_VERIFICATION"), anyLong());

        ArgumentCaptor<AuthChallenge> chCaptor = ArgumentCaptor.forClass(AuthChallenge.class);
        verify(challengeRepository).save(chCaptor.capture());
        AuthChallenge saved = chCaptor.getValue();

        when(challengeRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
        when(authRepository.findByIdClient(10L)).thenReturn(Optional.of(auth));
        when(jwtTokenService.generateAccessToken(anyLong(), anyLong(), anyString(), eq(AuthMethod.PASSWORD), any())).thenReturn("jwt-ok");
        when(jwtTokenService.generateRefreshToken()).thenReturn("rt-ok");
        when(jwtTokenService.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(300L);
        when(authMapper.toResponse(eq(auth), eq("jwt-ok"), eq("rt-ok"), eq(300L)))
                .thenReturn(AuthResponse.builder().token("jwt-ok").refreshToken("rt-ok").build());

        // Código erróneo incrementa intentos
        assertThatThrownBy(() -> service.verificarCodigo(new VerifyCodeRequest(saved.getId(), saved.getPollToken(), "000000".equals(codeCaptor.getValue()) ? "999999" : "000000")))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.VERIFICATION_CODE_INVALID);
        assertThat(saved.getAttempts()).isEqualTo(1);

        // Código correcto verifica email y emite tokens
        AuthResponse verified = service.verificarCodigo(new VerifyCodeRequest(saved.getId(), saved.getPollToken(), codeCaptor.getValue()));
        assertThat(verified.getToken()).isEqualTo("jwt-ok");
        assertThat(auth.getEmailVerified()).isTrue();
        assertThat(saved.getEstado()).isEqualTo("CONSUMIDO");
    }

    @Test
    void loginWebConHuellaOfreceDosOpcionesYApruebaDesdeMovil() {
        when(challengeRepository.save(any(AuthChallenge.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse challengeResp = service.iniciarDesafioLogin(auth, "WEB");
        assertThat(challengeResp.getAvailableMethods()).containsExactly("EMAIL_CODE", "MOBILE_BIOMETRIC");
        assertThat(challengeResp.getCodeSent()).isFalse();
        verifyNoInteractions(emailService);

        ArgumentCaptor<AuthChallenge> chCaptor = ArgumentCaptor.forClass(AuthChallenge.class);
        verify(challengeRepository).save(chCaptor.capture());
        AuthChallenge saved = chCaptor.getValue();

        when(challengeRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
        AuthChallengeStatusResponse reqBio = service.solicitarHuellaMovil(new ChallengeActionRequest(saved.getId(), saved.getPollToken()));
        assertThat(reqBio.getBiometricRequested()).isTrue();
        verify(pushNotificationService).enviarNotificacionLogin2fa(eq(10L), eq(saved));

        when(challengeRepository.findByIdClientAndPurposeAndEstadoAndAllowBiometricTrueAndBiometricRequestedTrueOrderByFechaCreacionDesc(10L, "LOGIN_2FA", "PENDIENTE"))
                .thenReturn(List.of(saved));
        assertThat(service.listarPendientesMovil(principal)).hasSize(1);

        service.aprobarConHuellaMovil(principal, saved.getId());
        assertThat(saved.getEstado()).isEqualTo("APROBADO");

        when(authRepository.findByIdClient(10L)).thenReturn(Optional.of(auth));
        when(jwtTokenService.generateAccessToken(anyLong(), anyLong(), anyString(), eq(AuthMethod.BIOMETRIC_HUELLA), any())).thenReturn("jwt-bio");
        when(jwtTokenService.generateRefreshToken()).thenReturn("rt-bio");
        when(jwtTokenService.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(300L);
        when(authMapper.toResponse(eq(auth), eq("jwt-bio"), eq("rt-bio"), eq(300L)))
                .thenReturn(AuthResponse.builder().token("jwt-bio").refreshToken("rt-bio").build());

        AuthChallengeStatusResponse polled = service.consultarEstadoChallenge(saved.getId(), saved.getPollToken());
        assertThat(polled.getAuthResponse()).isNotNull();
        assertThat(polled.getAuthResponse().getToken()).isEqualTo("jwt-bio");
        assertThat(saved.getEstado()).isEqualTo("CONSUMIDO");
    }

    @Test
    void loginMovilEnviaCodigoAutomaticamenteYPermiteCancelarORechazar() {
        auth.setBiometricTemplate(null);
        when(challengeRepository.save(any(AuthChallenge.class))).thenAnswer(i -> i.getArgument(0));
        when(clientRepository.findById(10L)).thenReturn(Optional.empty());

        AuthResponse resp = service.iniciarDesafioLogin(auth, "MOBILE");
        assertThat(resp.getAvailableMethods()).containsExactly("EMAIL_CODE");
        assertThat(resp.getCodeSent()).isTrue();
        verify(emailService).enviarCodigoVerificacion(eq("user@banco.com"), eq("Cliente"), anyString(), eq("LOGIN_2FA"), anyLong());

        ArgumentCaptor<AuthChallenge> chCaptor = ArgumentCaptor.forClass(AuthChallenge.class);
        verify(challengeRepository).save(chCaptor.capture());
        AuthChallenge saved = chCaptor.getValue();
        saved.setCodeSentAt(OffsetDateTime.now().minusSeconds(30));

        when(challengeRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
        service.enviarCodigoPorCorreo(new ChallengeActionRequest(saved.getId(), saved.getPollToken()));
        verify(emailService, times(2)).enviarCodigoVerificacion(eq("user@banco.com"), eq("Cliente"), anyString(), eq("LOGIN_2FA"), anyLong());

        assertThatThrownBy(() -> service.solicitarHuellaMovil(new ChallengeActionRequest(saved.getId(), saved.getPollToken())))
                .isInstanceOf(BusinessException.class);

        AuthChallengeStatusResponse cancelled = service.cancelarChallenge(new ChallengeActionRequest(saved.getId(), saved.getPollToken()));
        assertThat(cancelled.getEstado()).isEqualTo("CANCELADO");
    }
}
