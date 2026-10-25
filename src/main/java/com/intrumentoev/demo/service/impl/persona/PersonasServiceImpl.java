package com.intrumentoev.demo.service.impl.persona;

import com.intrumentoev.demo.entity.persona.Personas;
import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonaResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;
import com.intrumentoev.demo.repository.persona.PersonasRepository;
import com.intrumentoev.demo.service.service.persona.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {

    @Autowired
    private PersonasRepository personasRepository;

    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        PersonaResponse person = new PersonaResponse();
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        persona.setApellidoP(personasRequest.getApellidoP());
        personasRepository.save(persona);
        person.setCodigo(1);
        person.setMensaje("Exito");
        BeanUtils.copyProperties(persona, person);
        return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        GenericResponse genericResponse = new GenericResponse();

        Optional<Personas> existePersona = personasRepository.findByNombre(eliminaPersonaRequest.getNombre());
        if (existePersona.isPresent()) {
            Personas personaElimina = existePersona.get();
            personasRepository.delete(personaElimina);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("La persona ha sido eliminada correctamente");
        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        GenericResponse genericResponse = new GenericResponse();
        Optional<Personas> existePersona = personasRepository.findByNombre(personasRequest.getNombre());
        if (existePersona.isPresent()) {
            Personas personaActualiza = existePersona.get();
            personaActualiza.setApellidoP(personasRequest.getApellidoP());
            personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
            personasRepository.save(personaActualiza);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("la persona ha sido actualizada correctamente");
        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }
}
