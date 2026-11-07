package com.intrumentoev.demo.service.persona;

import com.intrumentoev.demo.entity.persona.Personas;
import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonaResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.repository.persona.PersonasRepository;
import com.intrumentoev.demo.service.impl.persona.PersonasServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonasServiceTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    private PersonasRequest request;

    @BeforeEach
    void setUp() {
        request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("Perez");
        request.setApellidoMaterno("Lopez");
    }

    @Test
    void creaPersona_Exito() {
        when(personasRepository.save(any(Personas.class))).thenAnswer(i -> i.getArgument(0));

        PersonaResponse response = personasService.creaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("Exito", response.getMensaje());
        assertEquals("Juan", response.getNombre());
        verify(personasRepository).save(any(Personas.class));
    }

    @Test
    void eliminaPersona_Existe() {
        EliminaPersonaRequest req = new EliminaPersonaRequest();
        req.setNombre("Juan");

        Personas p = new Personas();
        p.setNombre("Juan");

        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(p));

        GenericResponse resp = personasService.eliminaPersona(req);

        assertEquals(0, resp.getCodigo());
        assertEquals("La persona ha sido eliminada correctamente", resp.getMensaje());
        verify(personasRepository).delete(p);
    }

    @Test
    void eliminaPersona_NoExiste() {
        EliminaPersonaRequest req = new EliminaPersonaRequest();
        req.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse resp = personasService.eliminaPersona(req);

        assertEquals(1, resp.getCodigo());
        assertEquals("La persona no existe ", resp.getMensaje());
        verify(personasRepository, never()).delete(any());
    }

    @Test
    void actualizaPersona_Existe() {
        Personas p = new Personas();
        p.setNombre("Juan");
        p.setApellidoP("Antiguo");

        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(p));
        when(personasRepository.save(any(Personas.class))).thenAnswer(i -> i.getArgument(0));

        GenericResponse resp = personasService.actualizaPersona(request);

        assertEquals(0, resp.getCodigo());
        assertEquals("la persona ha sido actualizada correctamente", resp.getMensaje());
        assertEquals("Perez", p.getApellidoP());
        assertEquals("Lopez", p.getApellidoMaterno());
        verify(personasRepository).save(p);
    }

    @Test
    void actualizaPersona_NoExiste() {
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.empty());

        GenericResponse resp = personasService.actualizaPersona(request);

        assertEquals(1, resp.getCodigo());
        assertEquals("La persona no existe ", resp.getMensaje());
        verify(personasRepository, never()).save(any());
    }
}

