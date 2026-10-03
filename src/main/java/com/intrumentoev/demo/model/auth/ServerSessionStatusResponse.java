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
public class ServerSessionStatusResponse {

    private Boolean isLoggedIn;
    private Long inactivitySeconds;
    private Long maxInactivitySeconds;
    private Long remainingSeconds;
    private String userEmail;
    private Long clientId;
    private String status;
    private OffsetDateTime lastActivityAt;
    private String message;
}
