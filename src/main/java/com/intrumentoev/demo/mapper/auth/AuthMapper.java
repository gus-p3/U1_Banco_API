package com.intrumentoev.demo.mapper.auth;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.model.auth.AuthRegisterRequest;
import com.intrumentoev.demo.model.auth.AuthResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    /**
     * Convierte la petición de registro de credenciales a la entidad JPA Auth.
     */
    @Mapping(target = "idLogin", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "biometricTemplate", ignore = true)
    @Mapping(target = "biometricRegisteredAt", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "refreshTokenExpiresAt", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Auth toEntity(AuthRegisterRequest request);

    /**
     * Mapea la entidad Auth a la respuesta DTO AuthResponse básica.
     */
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", ignore = true)
    @Mapping(target = "hasBiometricRegistered", expression = "java(entity.getBiometricTemplate() != null && entity.getBiometricTemplate().length > 0)")
    AuthResponse toResponse(Auth entity);

    /**
     * Genera la respuesta DTO AuthResponse con tokens de acceso y refresh incluidos.
     */
    default AuthResponse toResponse(Auth entity, String accessToken, String refreshToken, Long expiresIn) {
        AuthResponse response = toResponse(entity);
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        return response;
    }
}
