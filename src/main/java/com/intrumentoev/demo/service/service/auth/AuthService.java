package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.model.auth.*;

import java.util.Optional;

public interface AuthService {

    /**
     * Autenticación tradicional mediante correo y contraseña.
     */
    AuthResponse login(AuthRequest request);

    /**
     * Autenticación biométrica (tipo Mercado Libre / Mercado Pago con Huella o Reconocimiento Facial).
     */
    AuthResponse loginBiometrico(BiometricLoginRequest request);

    /**
     * Crea automáticamente el registro de login / auth para un cliente al terminar su proceso de creación / onboarding.
     */
    Auth crearCredencialesCliente(Long idClient, String email, String password, String biometricType, String biometricData);

    /**
     * Registra o actualiza la plantilla biométrica (Huella o Facial) para un cliente existente.
     */
    AuthResponse registrarBiometria(BiometricRegisterRequest request);

    /**
     * Renueva el token de acceso JWT a partir de un refresh token vigente.
     */
    AuthResponse refrescarToken(RefreshTokenRequest request);

    /**
     * Cierra la sesión revocando el refresh token activo.
     */
    void logout(String email);

    /**
     * Consulta el registro de autenticación por ID de cliente.
     */
    Optional<Auth> obtenerPorIdCliente(Long idClient);
}
