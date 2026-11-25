package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.persona.Personas;
import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonaResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.repository.persona.PersonasRepository;
import com.intrumentoev.demo.service.impl.persona.PersonasServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock private PersonasRepository repository;
    @InjectMocks private PersonasServiceImpl service;

    private PersonasRequest request() {
        PersonasRequest r = new PersonasRequest();
        r.setNombre("Ana");
        r.setApellidoP("Pérez");
        r.setApellidoMaterno("Gómez");
        return r;
    }

    @Test
    @DisplayName("creaPersona guarda y responde con los datos")
    void crear() {
        PersonaResponse response = service.creaPersona(request());

        assertThat(response.getCodigo()).isEqualTo(1);
        assertThat(response.getNombre()).isEqualTo("Ana");
        assertThat(response.getApellidoP()).isEqualTo("Pérez");
        verify(repository).save(any(Personas.class));
    }

    @Test
    @DisplayName("actualizaPersona modifica a la persona existente")
    void actualizar() {
        Personas existente = new Personas();
        existente.setNombre("Ana");
        when(repository.findByNombre("Ana")).thenReturn(Optional.of(existente));

        GenericResponse response = service.actualizaPersona(request());

        assertThat(response.getCodigo()).isZero();
        assertThat(existente.getApellidoP()).isEqualTo("Pérez");
        verify(repository).save(existente);
    }

    @Test
    @DisplayName("actualizaPersona con persona inexistente responde código 1")
    void actualizarInexistente() {
        when(repository.findByNombre("Ana")).thenReturn(Optional.empty());

        assertThat(service.actualizaPersona(request()).getCodigo()).isEqualTo(1);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("eliminaPersona borra si existe y avisa si no")
    void eliminar() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Ana");
        Personas existente = new Personas();
        when(repository.findByNombre("Ana")).thenReturn(Optional.of(existente), Optional.empty());

        assertThat(service.eliminaPersona(request).getCodigo()).isZero();
        verify(repository).delete(existente);
        assertThat(service.eliminaPersona(request).getCodigo()).isEqualTo(1);
    }
}
