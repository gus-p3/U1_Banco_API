package com.intrumentoev.demo.service.service.persona;

import com.intrumentoev.demo.model.persona.EliminaPersonaRequest;
import com.intrumentoev.demo.model.persona.GenericResponse;
import com.intrumentoev.demo.model.persona.PersonaResponse;
import com.intrumentoev.demo.model.persona.PersonasRequest;

public interface PersonaService {
    PersonaResponse creaPersona(PersonasRequest personasRequest);
    GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest);
    GenericResponse actualizaPersona(PersonasRequest personasRequest);
}
