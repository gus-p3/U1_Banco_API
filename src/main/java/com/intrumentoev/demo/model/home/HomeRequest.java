package com.intrumentoev.demo.model.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class HomeRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long idClient;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
    private String street;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no puede superar los 10 caracteres")
    private String exteriorNumber;

    @Size(max = 10, message = "El número interior no puede superar los 10 caracteres")
    private String interiorNumber;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 80, message = "La colonia no puede superar los 80 caracteres")
    private String neighborhood;

    @NotNull(message = "El ID del municipio es obligatorio")
    private Integer idMunicipality;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String postalCode;

    @Size(max = 50, message = "El país no puede superar los 50 caracteres")
    private String country;
}
