package com.intrumentoev.demo.controller.persona;

import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonaResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.service.service.persona.PersonaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController controller;

    @Test
    void crearUser() {
        PersonasRequest req = new PersonasRequest();
        when(personaService.creaPersona(req)).thenReturn(new PersonaResponse());

        ResponseEntity<GenericResponse> resp = controller.crearUser(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(personaService).creaPersona(req);
    }

    @Test
    void actualizUser() {
        PersonasRequest req = new PersonasRequest();
        when(personaService.actualizaPersona(req)).thenReturn(new GenericResponse());

        ResponseEntity<GenericResponse> resp = controller.actualizUser(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(personaService).actualizaPersona(req);
    }

    @Test
    void actualizUser_Elimina() {
        EliminaPersonaRequest req = new EliminaPersonaRequest();
        when(personaService.eliminaPersona(req)).thenReturn(new GenericResponse());

        ResponseEntity<GenericResponse> resp = controller.actualizUser(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(personaService).eliminaPersona(req);
    }
}
