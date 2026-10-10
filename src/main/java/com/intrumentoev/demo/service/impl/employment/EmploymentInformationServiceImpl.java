package com.intrumentoev.demo.service.impl.employment;

import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.exception.ClientNotFoundException;
import com.intrumentoev.demo.mapper.employment.EmploymentInformationMapper;
import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.service.service.employment.EmploymentInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmploymentInformationServiceImpl implements EmploymentInformationService {

    private final EmploymentInformationRepository employmentRepository;
    private final ClientRepository clientRepository;
    private final EmploymentInformationMapper employmentMapper;

    private com.intrumentoev.demo.entity.client.Client resolverClientePorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new BusinessValidationException("El identificador (CURP o RFC) es obligatorio", "identificador");
        }
        String idLimpio = identificador.trim().toUpperCase();
        return clientRepository.findByCurp(idLimpio)
                .or(() -> clientRepository.findByRfc(idLimpio))
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con CURP o RFC: " + identificador));
    }

    @Override
    @Transactional(readOnly = true)
    public EmploymentInformationResponse obtenerEmploymentInformationPorIdentificador(String identificador) {
        var cliente = resolverClientePorIdentificador(identificador);
        return obtenerEmploymentInformationPorIdClient(cliente.getIdClient());
    }

    @Override
    @Transactional
    public EmploymentInformationResponse reemplazarEmploymentInformationPorIdentificador(String identificador, com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request) {
        var cliente = resolverClientePorIdentificador(identificador);
        EmploymentInformation existente = employmentRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe información laboral registrada para el cliente: " + identificador, "identificador"));
        return reemplazarEmploymentInformation(existente.getIdEmployment(), request);
    }

    @Override
    @Transactional
    public EmploymentInformationResponse actualizarParcialEmploymentInformationPorIdentificador(String identificador, EmploymentInformationPatchRequest request) {
        var cliente = resolverClientePorIdentificador(identificador);
        EmploymentInformation existente = employmentRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe información laboral registrada para el cliente: " + identificador, "identificador"));
        return actualizarParcialEmploymentInformation(existente.getIdEmployment(), request);
    }

    @Override
    @Transactional
    public void eliminarEmploymentInformationPorIdentificador(String identificador) {
        var cliente = resolverClientePorIdentificador(identificador);
        EmploymentInformation existente = employmentRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe información laboral registrada para el cliente: " + identificador, "identificador"));
        eliminarEmploymentInformation(existente.getIdEmployment());
    }

    @Override
    @Transactional(readOnly = true)
    public EmploymentInformationResponse obtenerEmploymentInformationPorId(Long id) {
        log.info("Buscando información laboral con ID: {}", id);
        EmploymentInformation info = employmentRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Información laboral no encontrada con ID: " + id, "idEmployment"));
        return employmentMapper.toResponse(info);
    }

    @Override
    @Transactional(readOnly = true)
    public EmploymentInformationResponse obtenerEmploymentInformationPorIdClient(Long idClient) {
        log.info("Buscando información laboral para cliente ID: {}", idClient);
        EmploymentInformation info = employmentRepository.findByIdClient(idClient)
                .orElseThrow(() -> new BusinessValidationException("No existe información laboral para el cliente con ID: " + idClient, "idClient"));
        return employmentMapper.toResponse(info);
    }

    @Override
    @Transactional
    public EmploymentInformationResponse reemplazarEmploymentInformation(Long id, com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request) {
        log.info("Reemplazando información laboral con ID: {}", id);
        EmploymentInformation existente = employmentRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Información laboral no encontrada con ID: " + id, "idEmployment"));

        // Si se envía idClient, verificar que exista
        if (request.getIdClient() != null && !clientRepository.existsById(request.getIdClient())) {
            throw new ClientNotFoundException(request.getIdClient());
        }

        if (request.getMonthlyIncome() != null && request.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El ingreso mensual debe ser mayor a cero", "monthlyIncome");
        }

        employmentMapper.updateEntityFromUpdateRequest(request, existente);
        EmploymentInformation actualizado = employmentRepository.save(existente);
        return employmentMapper.toResponse(actualizado);
    }

    @Override
    @Transactional
    public EmploymentInformationResponse reemplazarEmploymentInformation(Long id, EmploymentInformationRequest request) {
        log.info("Reemplazando información laboral con ID: {}", id);
        EmploymentInformation existente = employmentRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Información laboral no encontrada con ID: " + id, "idEmployment"));

        // Regla de Negocio: El cliente referenciado debe existir
        if (!clientRepository.existsById(request.getIdClient())) {
            throw new ClientNotFoundException(request.getIdClient());
        }

        if (request.getMonthlyIncome() != null && request.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El ingreso mensual debe ser mayor a cero", "monthlyIncome");
        }

        employmentMapper.updateEntityFromRequest(request, existente);
        EmploymentInformation actualizado = employmentRepository.save(existente);
        return employmentMapper.toResponse(actualizado);
    }

    @Override
    @Transactional
    public EmploymentInformationResponse actualizarParcialEmploymentInformation(Long id, EmploymentInformationPatchRequest request) {
        log.info("Actualizando parcialmente información laboral con ID: {}", id);
        if (request == null || request.isEmpty()) {
            throw new BusinessValidationException("Debe proporcionar al menos un campo válido para actualizar", "requestBody");
        }

        EmploymentInformation existente = employmentRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Información laboral no encontrada con ID: " + id, "idEmployment"));

        if (request.getMonthlyIncome() != null && request.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El ingreso mensual debe ser mayor a cero", "monthlyIncome");
        }

        employmentMapper.updateEntityFromPatch(request, existente);
        EmploymentInformation actualizado = employmentRepository.save(existente);
        return employmentMapper.toResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarEmploymentInformation(Long id) {
        log.info("Eliminando información laboral con ID: {}", id);
        if (!employmentRepository.existsById(id)) {
            throw new BusinessValidationException("Información laboral no encontrada con ID: " + id, "idEmployment");
        }
        employmentRepository.deleteById(id);
    }
}
