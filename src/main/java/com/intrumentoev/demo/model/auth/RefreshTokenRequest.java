package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefreshTokenRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    @Size(min = 1, max = 512, message = "El refresh token tiene una longitud inválida")
    private String refreshToken;
}
