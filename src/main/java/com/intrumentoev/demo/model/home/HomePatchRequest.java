package com.intrumentoev.demo.model.home;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePatchRequest {

    @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
    private String street;

    @Size(max = 10, message = "El número exterior no puede superar los 10 caracteres")
    private String exteriorNumber;

    @Size(max = 10, message = "El número interior no puede superar los 10 caracteres")
    private String interiorNumber;

    @Size(max = 80, message = "La colonia no puede superar los 80 caracteres")
    private String neighborhood;

    @jakarta.validation.constraints.Positive(message = "El identificador del municipio debe ser positivo")
    private Integer idMunicipality;

    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String postalCode;

    @Size(max = 50, message = "El país no puede superar los 50 caracteres")
    private String country;

    public boolean isEmpty() {
        return street == null && exteriorNumber == null && interiorNumber == null
                && neighborhood == null && idMunicipality == null && postalCode == null && country == null;
    }
}
