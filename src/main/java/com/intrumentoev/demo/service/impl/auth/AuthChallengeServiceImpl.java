package com.intrumentoev.demo.service.impl.auth;

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
import com.intrumentoev.demo.service.service.auth.AuthChallengeService;
import com.intrumentoev.demo.service.service.auth.EmailService;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.gestopago.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthChallengeServiceImpl implements AuthChallengeService {

    private static final String PURPOSE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String PURPOSE_LOGIN_2FA = "LOGIN_2FA";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_APROBADO = "APROBADO";
    private static final String ESTADO_RECHAZADO = "RECHAZADO";
    private static final String ESTADO_EXPIRADO = "EXPIRADO";
    private static final String ESTADO_CANCELADO = "CANCELADO";
    private static final String ESTADO_CONSUMIDO = "CONSUMIDO";
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthChallengeRepository challengeRepository;
    private final AuthRepository authRepository;
    private final ClientRepository clientRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final JwtTokenService jwtTokenService;
    private final AuthMapper authMapper;

    @Value("${auth.verification.ttl-seconds:600}")
    private long verificationTtlSeconds = 600L;

    @Value("${auth.login-2fa.ttl-seconds:300}")
    private long login2faTtlSeconds = 300L;

    @Override
    @Transactional
    public AuthResponse crearDesafioVerificacionCuenta(Auth auth, String nombreCliente) {
        auth.setEmailVerified(false);
        auth.setEmailVerifiedAt(null);
        authRepository.save(auth);

        long ttl = Math.max(60L, verificationTtlSeconds);
        String challengeId = UUID.randomUUID().toString();
        String pollToken = UUID.randomUUID().toString();
        String code = generarCodigo6();
        OffsetDateTime now = OffsetDateTime.now();

        AuthChallenge challenge = AuthChallenge.builder()
                .id(challengeId)
                .idClient(auth.getIdClient())
                .email(auth.getEmail())
                .purpose(PURPOSE_EMAIL_VERIFICATION)
                .channel("WEB")
                .pollToken(pollToken)
                .codeHash(hashCodigo(challengeId, code))
                .codeSentAt(now)
                .allowBiometric(false)
                .biometricRequested(false)
                .estado(ESTADO_PENDIENTE)
                .attempts(0)
                .expiraEn(now.plusSeconds(ttl))
                .build();

        challengeRepository.save(challenge);
        emailService.enviarCodigoVerificacion(
                auth.getEmail(),
                nombreCliente,
                code,
                PURPOSE_EMAIL_VERIFICATION,
                Math.max(1L, ttl / 60L)
        );

        return construirRespuestaDesafio(auth, challenge, List.of("EMAIL_CODE"), true, ttl);
    }

    @Override
    @Transactional
    public AuthResponse iniciarDesafioLogin(Auth auth, String channel) {
        String canal = "WEB".equalsIgnoreCase(channel) ? "WEB" : "MOBILE";
        if (!Boolean.TRUE.equals(auth.getEmailVerified())) {
            String nombre = resolverNombreCliente(auth.getIdClient());
            return crearDesafioVerificacionCuenta(auth, nombre);
        }

        boolean hasBiometric = auth.getBiometricTemplate() != null && auth.getBiometricTemplate().length > 0;
        boolean allowBiometric = "WEB".equals(canal) && hasBiometric;
        long ttl = Math.max(60L, login2faTtlSeconds);
        String challengeId = UUID.randomUUID().toString();
        String pollToken = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        // En móvil (o web sin huella registrada), enviamos el código de correo de inmediato.
        // En web con huella registrada, ofrecemos las 2 opciones (enviar código o autorizar con huella del teléfono).
        boolean autoEnviarCodigo = !"WEB".equals(canal) || !allowBiometric;
        String codeHash = null;
        OffsetDateTime codeSentAt = null;
        String codeToSend = null;
        if (autoEnviarCodigo) {
            codeToSend = generarCodigo6();
            codeHash = hashCodigo(challengeId, codeToSend);
            codeSentAt = now;
        }

        AuthChallenge challenge = AuthChallenge.builder()
                .id(challengeId)
                .idClient(auth.getIdClient())
                .email(auth.getEmail())
                .purpose(PURPOSE_LOGIN_2FA)
                .channel(canal)
                .pollToken(pollToken)
                .codeHash(codeHash)
                .codeSentAt(codeSentAt)
                .allowBiometric(allowBiometric)
                .biometricRequested(false)
                .estado(ESTADO_PENDIENTE)
                .attempts(0)
                .expiraEn(now.plusSeconds(ttl))
                .build();

        challengeRepository.save(challenge);

        if (autoEnviarCodigo) {
            String nombre = resolverNombreCliente(auth.getIdClient());
            emailService.enviarCodigoVerificacion(
                    auth.getEmail(),
                    nombre,
                    codeToSend,
                    PURPOSE_LOGIN_2FA,
                    Math.max(1L, ttl / 60L)
            );
        }

        List<String> methods = new ArrayList<>();
        methods.add("EMAIL_CODE");
        if (allowBiometric) {
            methods.add("MOBILE_BIOMETRIC");
        }

        return construirRespuestaDesafio(auth, challenge, methods, autoEnviarCodigo, ttl);
    }

    @Override
    @Transactional
    public AuthChallengeStatusResponse enviarCodigoPorCorreo(ChallengeActionRequest request) {
        AuthChallenge challenge = obtenerYValidarPollToken(request.getChallengeId(), request.getPollToken());
        validarPendienteVigente(challenge);

        OffsetDateTime now = OffsetDateTime.now();
        if (challenge.getCodeSentAt() != null && now.isBefore(challenge.getCodeSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw new BusinessException(ErrorCode.BUSINESS_VALIDATION, "Espera unos segundos antes de solicitar otro código.");
        }

        String code = generarCodigo6();
        challenge.setCodeHash(hashCodigo(challenge.getId(), code));
        challenge.setCodeSentAt(now);
        challenge.setAttempts(0);
        challengeRepository.save(challenge);

        long minutos = PURPOSE_EMAIL_VERIFICATION.equals(challenge.getPurpose())
                ? Math.max(1L, verificationTtlSeconds / 60L)
                : Math.max(1L, login2faTtlSeconds / 60L);
        String nombre = resolverNombreCliente(challenge.getIdClient());
        emailService.enviarCodigoVerificacion(challenge.getEmail(), nombre, code, challenge.getPurpose(), minutos);

        return aEstadoRespuesta(challenge, null);
    }

    @Override
    @Transactional
    public AuthChallengeStatusResponse solicitarHuellaMovil(ChallengeActionRequest request) {
        AuthChallenge challenge = obtenerYValidarPollToken(request.getChallengeId(), request.getPollToken());
        validarPendienteVigente(challenge);

        if (!Boolean.TRUE.equals(challenge.getAllowBiometric())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_VALIDATION,
                    "Tu cuenta aún no tiene huella digital registrada en Android. Usa el código enviado a tu correo."
            );
        }

        challenge.setBiometricRequested(true);
        AuthChallenge saved = challengeRepository.save(challenge);
        pushNotificationService.enviarNotificacionLogin2fa(saved.getIdClient(), saved);
        return aEstadoRespuesta(saved, null);
    }

    @Override
    @Transactional
    public AuthResponse verificarCodigo(VerifyCodeRequest request) {
        AuthChallenge challenge = obtenerYValidarPollToken(request.getChallengeId(), request.getPollToken());
        validarPendienteVigente(challenge);

        if (challenge.getCodeHash() == null || challenge.getCodeHash().isBlank()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "Primero solicita el envío del código a tu correo.");
        }

        String esperado = challenge.getCodeHash();
        String recibido = hashCodigo(challenge.getId(), request.getCode().trim());
        if (!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8))) {
            int intentos = (challenge.getAttempts() != null ? challenge.getAttempts() : 0) + 1;
            challenge.setAttempts(intentos);
            if (intentos >= MAX_ATTEMPTS) {
                challenge.setEstado(ESTADO_RECHAZADO);
                challengeRepository.save(challenge);
                throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "Demasiados intentos incorrectos. Inicia sesión nuevamente para generar otro código.");
            }
            challengeRepository.save(challenge);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "El código de verificación es incorrecto.");
        }

        Auth auth = authRepository.findByIdClient(challenge.getIdClient())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, String.valueOf(challenge.getIdClient())));

        if (PURPOSE_EMAIL_VERIFICATION.equals(challenge.getPurpose())) {
            auth.setEmailVerified(true);
            auth.setEmailVerifiedAt(OffsetDateTime.now());
        }

        challenge.setEstado(ESTADO_CONSUMIDO);
        challengeRepository.save(challenge);

        return emitirTokensFinales(auth, AuthMethod.PASSWORD);
    }

    @Override
    @Transactional
    public AuthChallengeStatusResponse consultarEstadoChallenge(String challengeId, String pollToken) {
        AuthChallenge challenge = obtenerYValidarPollToken(challengeId, pollToken);
        if (ESTADO_PENDIENTE.equals(challenge.getEstado()) && challenge.getExpiraEn().isBefore(OffsetDateTime.now())) {
            challenge.setEstado(ESTADO_EXPIRADO);
            challenge = challengeRepository.save(challenge);
        }

        AuthResponse authResponse = null;
        if (ESTADO_APROBADO.equals(challenge.getEstado())) {
            Long idClient = challenge.getIdClient();
            Auth auth = authRepository.findByIdClient(idClient)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, String.valueOf(idClient)));
            challenge.setEstado(ESTADO_CONSUMIDO);
            challenge = challengeRepository.save(challenge);
            authResponse = emitirTokensFinales(auth, AuthMethod.BIOMETRIC_HUELLA);
        }

        return aEstadoRespuesta(challenge, authResponse);
    }

    @Override
    @Transactional
    public AuthChallengeStatusResponse cancelarChallenge(ChallengeActionRequest request) {
        AuthChallenge challenge = obtenerYValidarPollToken(request.getChallengeId(), request.getPollToken());
        if (ESTADO_PENDIENTE.equals(challenge.getEstado())) {
            challenge.setEstado(ESTADO_CANCELADO);
            challenge = challengeRepository.save(challenge);
        }
        return aEstadoRespuesta(challenge, null);
    }

    @Override
    @Transactional
    public List<PendingLogin2faResponse> listarPendientesMovil(AuthenticatedClient usuario) {
        List<AuthChallenge> pendientes = challengeRepository
                .findByIdClientAndPurposeAndEstadoAndAllowBiometricTrueAndBiometricRequestedTrueOrderByFechaCreacionDesc(
                        usuario.getIdClient(),
                        PURPOSE_LOGIN_2FA,
                        ESTADO_PENDIENTE
                );
        OffsetDateTime now = OffsetDateTime.now();
        List<PendingLogin2faResponse> result = new ArrayList<>();
        for (AuthChallenge c : pendientes) {
            if (c.getExpiraEn().isBefore(now)) {
                c.setEstado(ESTADO_EXPIRADO);
                challengeRepository.save(c);
            } else {
                result.add(aPendienteMovil(c));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public PendingLogin2faResponse aprobarConHuellaMovil(AuthenticatedClient usuario, String challengeId) {
        AuthChallenge challenge = obtenerDeCliente(usuario, challengeId);
        validarPendienteVigente(challenge);
        if (!Boolean.TRUE.equals(challenge.getAllowBiometric())) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_INVALID_STATE, "Este inicio de sesión no permite aprobación biométrica.");
        }
        challenge.setEstado(ESTADO_APROBADO);
        return aPendienteMovil(challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public PendingLogin2faResponse rechazarDesdeMovil(AuthenticatedClient usuario, String challengeId) {
        AuthChallenge challenge = obtenerDeCliente(usuario, challengeId);
        validarPendienteVigente(challenge);
        challenge.setEstado(ESTADO_RECHAZADO);
        return aPendienteMovil(challengeRepository.save(challenge));
    }

    private AuthChallenge obtenerYValidarPollToken(String challengeId, String pollToken) {
        if (challengeId == null || challengeId.isBlank() || pollToken == null || pollToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND, String.valueOf(challengeId));
        }
        AuthChallenge challenge = challengeRepository.findById(challengeId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND, challengeId));
        if (!MessageDigest.isEqual(
                challenge.getPollToken().getBytes(StandardCharsets.UTF_8),
                pollToken.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return challenge;
    }

    private AuthChallenge obtenerDeCliente(AuthenticatedClient usuario, String challengeId) {
        AuthChallenge challenge = challengeRepository.findById(challengeId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND, challengeId));
        if (!usuario.getIdClient().equals(challenge.getIdClient())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return challenge;
    }

    private void validarPendienteVigente(AuthChallenge challenge) {
        if (!ESTADO_PENDIENTE.equals(challenge.getEstado())) {
            throw new BusinessException(
                    ErrorCode.VERIFICATION_CHALLENGE_EXPIRED,
                    "La solicitud de verificación ya no está activa (" + challenge.getEstado() + ")."
            );
        }
        if (challenge.getExpiraEn().isBefore(OffsetDateTime.now())) {
            challenge.setEstado(ESTADO_EXPIRADO);
            challengeRepository.save(challenge);
            throw new BusinessException(
                    ErrorCode.VERIFICATION_CHALLENGE_EXPIRED,
                    "El código o solicitud expiró. Inicia sesión nuevamente."
            );
        }
    }

    private AuthResponse construirRespuestaDesafio(
            Auth auth,
            AuthChallenge challenge,
            List<String> availableMethods,
            boolean codeSent,
            long ttl) {
        return AuthResponse.builder()
                .email(auth.getEmail())
                .role(auth.getRole())
                .isActive(auth.getIsActive())
                .emailVerified(auth.getEmailVerified())
                .hasBiometricRegistered(auth.getBiometricTemplate() != null && auth.getBiometricTemplate().length > 0)
                .biometricType(auth.getBiometricType())
                .requiresVerification(true)
                .verificationPurpose(challenge.getPurpose())
                .challengeId(challenge.getId())
                .pollToken(challenge.getPollToken())
                .availableMethods(availableMethods)
                .codeSent(codeSent)
                .challengeExpiresIn(ttl)
                .build();
    }

    private AuthResponse emitirTokensFinales(Auth auth, AuthMethod method) {
        auth.setFailedAttempts(0);
        auth.setLastLoginAt(OffsetDateTime.now());

        String accessToken = jwtTokenService.generateAccessToken(
                auth.getIdClient(), auth.getIdLogin(), auth.getEmail(), method, auth.getRole());
        String refreshToken = jwtTokenService.generateRefreshToken();

        auth.setRefreshToken(refreshToken);
        auth.setRefreshTokenExpiresAt(OffsetDateTime.now().plusDays(jwtTokenService.getRefreshTokenExpirationDays()));
        authRepository.save(auth);

        return authMapper.toResponse(auth, accessToken, refreshToken, jwtTokenService.getAccessTokenExpirationSeconds());
    }

    private AuthChallengeStatusResponse aEstadoRespuesta(AuthChallenge c, AuthResponse authResponse) {
        return AuthChallengeStatusResponse.builder()
                .challengeId(c.getId())
                .estado(c.getEstado())
                .purpose(c.getPurpose())
                .channel(c.getChannel())
                .email(c.getEmail())
                .allowBiometric(c.getAllowBiometric())
                .biometricRequested(c.getBiometricRequested())
                .codeSent(c.getCodeHash() != null)
                .expiraEn(c.getExpiraEn())
                .authResponse(authResponse)
                .build();
    }

    private PendingLogin2faResponse aPendienteMovil(AuthChallenge c) {
        return PendingLogin2faResponse.builder()
                .challengeId(c.getId())
                .email(c.getEmail())
                .channel(c.getChannel())
                .expiraEn(c.getExpiraEn())
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }

    private String resolverNombreCliente(Long idClient) {
        return clientRepository.findById(idClient)
                .map(Client::getName)
                .orElse("Cliente");
    }

    private String generarCodigo6() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String hashCodigo(String challengeId, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular hash SHA-256", e);
        }
    }
}
