package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long idClient;
    private String email;
    private Boolean isActive;
    private Boolean hasBiometricRegistered;
    private String biometricType;
    private OffsetDateTime lastLoginAt;

    public static AuthResponse fromEntity(com.intrumentoev.demo.entity.auth.Auth auth, String token, String refreshToken, Long expiresIn) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .idClient(auth.getIdClient())
                .email(auth.getEmail())
                .isActive(auth.getIsActive())
                .hasBiometricRegistered(auth.getBiometricTemplate() != null && auth.getBiometricTemplate().length > 0)
                .biometricType(auth.getBiometricType())
                .lastLoginAt(auth.getLastLoginAt())
                .build();
    }

    public static AuthResponse fromEntity(com.intrumentoev.demo.entity.auth.Auth auth, String token) {
        return fromEntity(auth, token, auth.getRefreshToken(), 3600L);
    }
}