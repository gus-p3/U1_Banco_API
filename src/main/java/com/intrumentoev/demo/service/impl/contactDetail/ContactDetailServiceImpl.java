package com.intrumentoev.demo.service.impl.contactDetail;

import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.exception.BusinessValidationException;

import com.intrumentoev.demo.exception.EmailDuplicatedException;
import com.intrumentoev.demo.exception.PhoneDuplicatedException;
import com.intrumentoev.demo.mapper.contactDetail.ContactDetailMapper;
import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;

import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.service.service.contactDetail.ContactDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class ContactDetailServiceImpl implements ContactDetailService {

    private final ContactDetailRepository contactDetailRepository;
    private final ContactDetailMapper contactDetailMapper;

    // 1. GET /v1/contact-details/{id}
    @Override
    @Transactional(readOnly = true)
    public ContactDetailResponse obtenerContactDetailPorId(Long id) {
        log.info("Buscando detalle de contacto por ID: {}", id);
        ContactDetail detalle = contactDetailRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Detalle de contacto no encontrado con ID: " + id, "idContactDetail"));
        return contactDetailMapper.toResponse(detalle);
    }

    // 4. GET /v1/contact-details/client/{idClient}
    @Override
    @Transactional(readOnly = true)
    public ContactDetailResponse obtenerContactDetailPorIdClient(Long idClient) {
        log.info("Buscando detalle de contacto para el cliente ID: {}", idClient);
        ContactDetail detalle = contactDetailRepository.findByIdClient(idClient)
                .orElseThrow(() -> new BusinessValidationException("Detalle de contacto no encontrado para el cliente con ID: " + idClient, "idClient"));
        return contactDetailMapper.toResponse(detalle);
    }

    // 5. PUT /v1/contact-details/{id} (Reemplazo completo con DTO dedicado)
    @Override
    @Transactional
    public ContactDetailResponse reemplazarContactDetail(Long id, com.intrumentoev.demo.model.contactDetail.ContactDetailUpdateRequest request) {
        log.info("Reemplazando completamente el detalle de contacto con ID: {}", id);
        ContactDetail existente = contactDetailRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Detalle de contacto no encontrado con ID: " + id, "idContactDetail"));

        String emailNormalizado = request.getEmail().trim().toLowerCase();

        // Validar que el nuevo email no pertenezca a otro registro
        if (!existente.getEmail().equalsIgnoreCase(emailNormalizado) &&
                contactDetailRepository.existsByEmail(emailNormalizado)) {
            throw new EmailDuplicatedException(emailNormalizado);
        }

        // Validar que el nuevo teléfono no pertenezca a otro registro
        if (!existente.getMobilePhone().equals(request.getMobilePhone()) &&
                contactDetailRepository.existsByMobilePhone(request.getMobilePhone())) {
            throw new PhoneDuplicatedException(request.getMobilePhone());
        }

        contactDetailMapper.updateEntityFromUpdateRequest(request, existente);
        existente.setEmail(emailNormalizado);

        ContactDetail actualizado = contactDetailRepository.save(existente);
        return contactDetailMapper.toResponse(actualizado);
    }

    // 5.b PUT /v1/contact-details/{id} (Sobrecarga de compatibilidad)
    @Override
    @Transactional
    public ContactDetailResponse reemplazarContactDetail(Long id, ContactDetailRequest request) {
        log.info("Reemplazando completamente el detalle de contacto con ID: {}", id);
        ContactDetail existente = contactDetailRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Detalle de contacto no encontrado con ID: " + id, "idContactDetail"));

        String emailNormalizado = request.getEmail().trim().toLowerCase();

        // Validar que el nuevo email no pertenezca a otro registro
        if (!existente.getEmail().equalsIgnoreCase(emailNormalizado) &&
                contactDetailRepository.existsByEmail(emailNormalizado)) {
            throw new EmailDuplicatedException(emailNormalizado);
        }

        // Validar que el nuevo teléfono no pertenezca a otro registro
        if (!existente.getMobilePhone().equals(request.getMobilePhone()) &&
                contactDetailRepository.existsByMobilePhone(request.getMobilePhone())) {
            throw new PhoneDuplicatedException(request.getMobilePhone());
        }

        contactDetailMapper.updateEntityFromRequest(request, existente);
        existente.setEmail(emailNormalizado);

        ContactDetail actualizado = contactDetailRepository.save(existente);
        return contactDetailMapper.toResponse(actualizado);
    }

    // 6. PATCH /v1/contact-details/{id} (Actualización parcial)
    @Override
    @Transactional
    public ContactDetailResponse actualizarParcialContactDetail(Long id, ContactDetailPatchRequest request) {
        log.info("Actualizando parcialmente el detalle de contacto con ID: {}", id);
        ContactDetail existente = contactDetailRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Detalle de contacto no encontrado con ID: " + id, "idContactDetail"));

        if (request.getEmail() != null) {
            String emailNormalizado = request.getEmail().trim().toLowerCase();
            if (!existente.getEmail().equalsIgnoreCase(emailNormalizado) &&
                    contactDetailRepository.existsByEmail(emailNormalizado)) {
                throw new EmailDuplicatedException(emailNormalizado);
            }
            request.setEmail(emailNormalizado);
        }

        // Validar unicidad del teléfono solo si se envía y cambia
        if (request.getMobilePhone() != null &&
                !existente.getMobilePhone().equals(request.getMobilePhone()) &&
                contactDetailRepository.existsByMobilePhone(request.getMobilePhone())) {
            throw new PhoneDuplicatedException(request.getMobilePhone());
        }

        contactDetailMapper.updateEntityFromPatch(request, existente);
        ContactDetail actualizado = contactDetailRepository.save(existente);
        return contactDetailMapper.toResponse(actualizado);
    }

    // 7. DELETE /v1/contact-details/{id}
    @Override
    @Transactional
    public void eliminarContactDetail(Long id) {
        log.info("Eliminando detalle de contacto con ID: {}", id);
        if (!contactDetailRepository.existsById(id)) {
            throw new BusinessValidationException("Detalle de contacto no encontrado con ID: " + id, "idContactDetail");
        }
        contactDetailRepository.deleteById(id);
    }
}