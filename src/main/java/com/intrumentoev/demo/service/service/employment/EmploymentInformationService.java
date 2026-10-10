package com.intrumentoev.demo.service.service.employment;

import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;

public interface EmploymentInformationService {

    EmploymentInformationResponse obtenerEmploymentInformationPorIdentificador(String identificador);

    EmploymentInformationResponse reemplazarEmploymentInformationPorIdentificador(String identificador, com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request);

    EmploymentInformationResponse actualizarParcialEmploymentInformationPorIdentificador(String identificador, EmploymentInformationPatchRequest request);

    void eliminarEmploymentInformationPorIdentificador(String identificador);

    EmploymentInformationResponse obtenerEmploymentInformationPorId(Long id);

    EmploymentInformationResponse obtenerEmploymentInformationPorIdClient(Long idClient);

    EmploymentInformationResponse reemplazarEmploymentInformation(Long id, com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request);

    EmploymentInformationResponse reemplazarEmploymentInformation(Long id, EmploymentInformationRequest request);

    EmploymentInformationResponse actualizarParcialEmploymentInformation(Long id, EmploymentInformationPatchRequest request);

    void eliminarEmploymentInformation(Long id);
}
