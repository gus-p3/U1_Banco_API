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
public class PendingLogin2faResponse {
    private String challengeId;
    private String email;
    private String channel;
    private OffsetDateTime expiraEn;
    private OffsetDateTime fechaCreacion;
}
