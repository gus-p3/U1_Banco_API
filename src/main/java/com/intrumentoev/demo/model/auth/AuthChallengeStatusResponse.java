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
public class AuthChallengeStatusResponse {
    private String challengeId;
    private String estado;
    private String purpose;
    private String channel;
    private String email;
    private Boolean allowBiometric;
    private Boolean biometricRequested;
    private Boolean codeSent;
    private OffsetDateTime expiraEn;
    private AuthResponse authResponse;
}
