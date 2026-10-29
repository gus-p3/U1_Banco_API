package com.intrumentoev.demo.controller.persona;

import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.service.service.persona.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Personas", description = "Endpoints de administración de personas")
@RestController
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @Operation(summary = "Crear nueva persona")
    @PostMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crearUser(@Valid @RequestBody PersonasRequest personasRequest) {
        return new ResponseEntity<>(personaService.creaPersona(personasRequest), HttpStatus.OK);
    }

    @Operation(summary = "Actualizar persona existente")
    @PutMapping(value = "/personasActualiza", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody PersonasRequest personasRequest) {
        return new ResponseEntity<>(personaService.actualizaPersona(personasRequest), HttpStatus.OK);
    }

    @Operation(summary = "Eliminar persona por nombre")
    @PutMapping(value = "/personasElimina", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody EliminaPersonaRequest personasRequest) {
        return new ResponseEntity<>(personaService.eliminaPersona(personasRequest), HttpStatus.OK);
    }
}
