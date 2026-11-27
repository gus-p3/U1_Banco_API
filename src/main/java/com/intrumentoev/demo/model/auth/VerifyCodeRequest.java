package com.intrumentoev.demo.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyCodeRequest {

    @NotBlank(message = "El identificador del desafío es obligatorio")
    private String challengeId;

    @NotBlank(message = "El token de verificación es obligatorio")
    private String pollToken;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código debe contener exactamente 6 dígitos numéricos")
    private String code;
}
