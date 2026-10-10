package com.intrumentoev.demo.service.service.contactDetail;

import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;


public interface ContactDetailService {

    ContactDetailResponse obtenerContactDetailPorIdentificador(String identificador);

    ContactDetailResponse reemplazarContactDetailPorIdentificador(String identificador, com.intrumentoev.demo.model.contactDetail.ContactDetailUpdateRequest request);

    ContactDetailResponse actualizarParcialContactDetailPorIdentificador(String identificador, ContactDetailPatchRequest request);

    void eliminarContactDetailPorIdentificador(String identificador);

    // 1. Buscar detalle de contacto por ID (GET /v1/contact-details/{id})
    ContactDetailResponse obtenerContactDetailPorId(Long id);

    // 4. Buscar detalle de contacto por ID de Cliente (GET /v1/contact-details/client/{idClient})
    ContactDetailResponse obtenerContactDetailPorIdClient(Long idClient);

    // 5. Reemplazo completo con DTO dedicado (PUT /v1/contact-details/{id})
    ContactDetailResponse reemplazarContactDetail(Long id, com.intrumentoev.demo.model.contactDetail.ContactDetailUpdateRequest request);

    // 5.b Reemplazo completo (PUT /v1/contact-details/{id})
    ContactDetailResponse reemplazarContactDetail(Long id, ContactDetailRequest request);

    // 6. Actualización parcial (PATCH /v1/contact-details/{id})
    ContactDetailResponse actualizarParcialContactDetail(Long id, ContactDetailPatchRequest request);

    // 7. Eliminar detalle de contacto por ID (DELETE /v1/contact-details/{id})
    void eliminarContactDetail(Long id);
}