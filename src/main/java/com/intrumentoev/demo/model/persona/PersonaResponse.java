package com.intrumentoev.demo.model.persona;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonaResponse extends GenericResponse {
    private String nombre;
    private String apellidoP;
    private String apellidoMaterno;
}
