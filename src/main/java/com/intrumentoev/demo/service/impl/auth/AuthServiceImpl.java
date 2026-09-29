package com.intrumentoev.demo.service.impl.auth;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.exception.*;
import com.intrumentoev.demo.mapper.auth.AuthMapper;
import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.service.service.auth.AesEncryptionService;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import com.intrumentoev.demo.service.service.auth.PasswordEncoder;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthRepository authRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AesEncryptionService aesEncryptionService;
    private final ServerSessionManager serverSessionManager;

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Intento de login tradicional con email: {}", email);

        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales de acceso inválidas"));

        validarEstadoYBloqueo(auth);

        if (!passwordEncoder.matches(request.getPassword(), auth.getPasswordHash())) {
            manejarIntentoFallido(auth);
            throw new InvalidCredentialsException("Credenciales de acceso inválidas");
        }

        return generarSesionExitosa(auth);
    }

    @Override
    @Transactional
    public AuthResponse loginBiometrico(BiometricLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Intento de login biométrico [{}] para email: {}", request.getBiometricType(), email);

        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Cliente no encontrado para autenticación biométrica"));

        validarEstadoYBloqueo(auth);

        if (auth.getBiometricTemplate() == null || auth.getBiometricTemplate().length == 0) {
            throw new BiometricAuthenticationException("El cliente no cuenta con datos biométricos registrados. Inicie sesión con contraseña.");
        }

        if (auth.getBiometricType() == null || !auth.getBiometricType().equalsIgnoreCase(request.getBiometricType())) {
            throw new BiometricAuthenticationException("El tipo de biometría suministrado no coincide con el método registrado (" + auth.getBiometricType() + ")");
        }

        byte[] inputTemplate;
        try {
            inputTemplate = Base64.getDecoder().decode(request.getBiometricData());
        } catch (IllegalArgumentException e) {
            throw new BiometricAuthenticationException("El formato de la plantilla biométrica Base64 es inválido");
        }

        // Descifrado de la plantilla biométrica almacenada cifrada con AES-256-GCM
        byte[] decryptedStoredTemplate = aesEncryptionService.decryptBytes(auth.getBiometricTemplate());

        // Comparación segura en tiempo constante para evitar ataques de canal lateral
        if (!java.security.MessageDigest.isEqual(decryptedStoredTemplate, inputTemplate)) {
            manejarIntentoFallido(auth);
            throw new BiometricAuthenticationException("La huella o rostro proporcionado no coincide con el registro biométrico del cliente");
        }

        log.info("Autenticación biométrica exitosa para cliente ID: {}", auth.getIdClient());
        return generarSesionExitosa(auth);
    }

    @Override
    @Transactional
    public Auth crearCredencialesCliente(Long idClient, String email, String password, String biometricType, String biometricData) {
        String emailNormalizado = email.trim().toLowerCase();
        log.info("Creando credenciales de acceso automáticas para cliente ID: {} y email: {}", idClient, emailNormalizado);

        if (authRepository.existsByEmail(emailNormalizado)) {
            throw new EmailDuplicatedException(emailNormalizado);
        }

        if (authRepository.existsByIdClient(idClient)) {
            throw new BusinessValidationException("El cliente ya cuenta con credenciales de acceso activas", "idClient");
        }

        String passwordHash = passwordEncoder.encode(password);

        byte[] template = null;
        OffsetDateTime biometricRegisteredAt = null;

        if (biometricType != null && !biometricType.isBlank() && biometricData != null && !biometricData.isBlank()) {
            try {
                byte[] rawTemplate = Base64.getDecoder().decode(biometricData);
                // Cifrado simétrico AES-256-GCM de la plantilla biométrica para almacenamiento seguro en BD
                template = aesEncryptionService.encryptBytes(rawTemplate);
                biometricRegisteredAt = OffsetDateTime.now();
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("El formato Base64 de la biometría es inválido", "biometricData");
            }
        }

        Auth auth = Auth.builder()
                .idClient(idClient)
                .email(emailNormalizado)
                .passwordHash(passwordHash)
                .biometricType(biometricType)
                .biometricTemplate(template)
                .biometricRegisteredAt(biometricRegisteredAt)
                .isActive(true)
                .failedAttempts(0)
                .build();

        Auth guardado = authRepository.save(auth);
        log.info("Credenciales de acceso creadas exitosamente para cliente ID: {}, ID Login: {}", idClient, guardado.getIdLogin());
        return guardado;
    }

    @Override
    @Transactional
    public AuthResponse registrarBiometria(BiometricRegisterRequest request) {
        log.info("Registrando biometría [{}] para cliente ID: {}", request.getBiometricType(), request.getIdClient());

        Auth auth = authRepository.findByIdClient(request.getIdClient())
                .orElseThrow(() -> new ClientNotFoundException(request.getIdClient()));

        if (!Boolean.TRUE.equals(auth.getIsActive())) {
            throw new ClientInactiveException("No se puede registrar biometría en una cuenta inactiva");
        }

        byte[] rawTemplate;
        try {
            rawTemplate = Base64.getDecoder().decode(request.getBiometricData());
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("El formato Base64 del dato biométrico es inválido", "biometricData");
        }

        // Cifrado simétrico AES-256-GCM de la plantilla biométrica
        byte[] encryptedTemplate = aesEncryptionService.encryptBytes(rawTemplate);

        auth.setBiometricType(request.getBiometricType().toUpperCase());
        auth.setBiometricTemplate(encryptedTemplate);
        auth.setBiometricRegisteredAt(OffsetDateTime.now());

        Auth guardado = authRepository.save(auth);
        log.info("Biometría registrada y cifrada con éxito para cliente ID: {}", request.getIdClient());

        return authMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public AuthResponse refrescarToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.info("Solicitud de renovación de token de acceso mediante refresh token");

        Auth auth = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenExpiredOrInvalidException("Refresh token inválido o no reconocido"));

        if (!Boolean.TRUE.equals(auth.getIsActive())) {
            throw new ClientInactiveException("La cuenta del cliente se encuentra inactiva");
        }

        if (auth.getRefreshTokenExpiresAt() == null || auth.getRefreshTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenExpiredOrInvalidException("El refresh token ha expirado. Debe autenticarse nuevamente.");
        }

        // Rotación de tokens por seguridad tipo Mercado Pago
        String nuevoAccessToken = jwtTokenService.generateAccessToken(auth.getIdClient(), auth.getIdLogin(), auth.getEmail());
        String nuevoRefreshToken = jwtTokenService.generateRefreshToken();

        auth.setRefreshToken(nuevoRefreshToken);
        auth.setRefreshTokenExpiresAt(OffsetDateTime.now().plusDays(jwtTokenService.getRefreshTokenExpirationDays()));
        authRepository.save(auth);

        // Mantener sesión activa en servidor
        serverSessionManager.setLoggedIn(true, auth.getEmail(), auth.getIdClient());

        log.info("Tokens rotados y renovados exitosamente para cliente ID: {}", auth.getIdClient());
        return authMapper.toResponse(auth, nuevoAccessToken, nuevoRefreshToken, jwtTokenService.getAccessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public void logout(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        log.info("Cerrando sesión y revocando refresh token para: {}", emailNormalizado);

        authRepository.findByEmail(emailNormalizado).ifPresent(auth -> {
            auth.setRefreshToken(null);
            auth.setRefreshTokenExpiresAt(null);
            authRepository.save(auth);
        });

        // Servidor: Cambiar booleano a false
        serverSessionManager.setLoggedIn(false, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Auth> obtenerPorIdCliente(Long idClient) {
        return authRepository.findByIdClient(idClient);
    }

    private void validarEstadoYBloqueo(Auth auth) {
        if (!Boolean.TRUE.equals(auth.getIsActive())) {
            throw new ClientInactiveException("La cuenta de acceso se encuentra inactiva");
        }

        if (auth.getFailedAttempts() != null && auth.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new AccountLockedException("La cuenta se encuentra bloqueada por exceso de intentos fallidos (" + auth.getFailedAttempts() + "). Contacte a soporte del banco.");
        }
    }

    private void manejarIntentoFallido(Auth auth) {
        int intentos = (auth.getFailedAttempts() != null ? auth.getFailedAttempts() : 0) + 1;
        auth.setFailedAttempts(intentos);
        authRepository.save(auth);
        log.warn("Intento fallido #{} de autenticación para email: {}", intentos, auth.getEmail());
    }

    private AuthResponse generarSesionExitosa(Auth auth) {
        auth.setFailedAttempts(0);
        auth.setLastLoginAt(OffsetDateTime.now());

        String accessToken = jwtTokenService.generateAccessToken(auth.getIdClient(), auth.getIdLogin(), auth.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken();

        auth.setRefreshToken(refreshToken);
        auth.setRefreshTokenExpiresAt(OffsetDateTime.now().plusDays(jwtTokenService.getRefreshTokenExpirationDays()));

        authRepository.save(auth);

        // SERVIDOR: Activar booleano en el servidor (login = true) y reiniciar contador de inactividad
        serverSessionManager.setLoggedIn(true, auth.getEmail(), auth.getIdClient());

        return authMapper.toResponse(auth, accessToken, refreshToken, jwtTokenService.getAccessTokenExpirationSeconds());
    }
}
