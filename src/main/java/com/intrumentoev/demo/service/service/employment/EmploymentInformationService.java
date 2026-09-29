package com.intrumentoev.demo.service.service.employment;

import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;

public interface EmploymentInformationService {

    EmploymentInformationResponse obtenerEmploymentInformationPorId(Long id);

    EmploymentInformationResponse obtenerEmploymentInformationPorIdClient(Long idClient);

    EmploymentInformationResponse reemplazarEmploymentInformation(Long id, EmploymentInformationRequest request);

    EmploymentInformationResponse actualizarParcialEmploymentInformation(Long id, EmploymentInformationPatchRequest request);

    void eliminarEmploymentInformation(Long id);
}
