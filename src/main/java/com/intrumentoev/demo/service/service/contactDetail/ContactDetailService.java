package com.intrumentoev.demo.service.service.contactDetail;

import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;


public interface ContactDetailService {

    // 1. Buscar detalle de contacto por ID (GET /v1/contact-details/{id})
    ContactDetailResponse obtenerContactDetailPorId(Long id);

    // 4. Buscar detalle de contacto por ID de Cliente (GET /v1/contact-details/client/{idClient})
    ContactDetailResponse obtenerContactDetailPorIdClient(Long idClient);

    // 5. Reemplazo completo (PUT /v1/contact-details/{id})
    ContactDetailResponse reemplazarContactDetail(Long id, ContactDetailRequest request);

    // 6. Actualización parcial (PATCH /v1/contact-details/{id})
    ContactDetailResponse actualizarParcialContactDetail(Long id, ContactDetailPatchRequest request);

    // 7. Eliminar detalle de contacto por ID (DELETE /v1/contact-details/{id})
    void eliminarContactDetail(Long id);
}