package com.intrumentoev.demo.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthenticatedClient {
    private final Long idClient;
    private final Long idLogin;
    private final String email;
    private final AuthMethod authMethod;
}