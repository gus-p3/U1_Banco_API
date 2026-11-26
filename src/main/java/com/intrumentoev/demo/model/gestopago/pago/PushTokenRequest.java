package com.intrumentoev.demo.model.gestopago.pago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenRequest {

    @NotBlank(message = "El token push es obligatorio")
    @Size(min = 10, max = 512, message = "El token push debe tener entre 10 y 512 caracteres")
    private String token;

    @NotBlank(message = "La plataforma es obligatoria")
    @Pattern(regexp = "^(ANDROID|IOS)$", message = "La plataforma debe ser ANDROID o IOS")
    private String plataforma;

    @Size(max = 120, message = "El nombre del dispositivo no puede exceder 120 caracteres")
    private String deviceName;
}
