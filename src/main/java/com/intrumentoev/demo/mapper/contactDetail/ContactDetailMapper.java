package com.intrumentoev.demo.mapper.contactDetail;

import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContactDetailMapper {

    /**
     * Convierte la petición de creación (POST) a la entidad JPA ContactDetail.
     * Se ignoran la clave primaria y las fechas de auditoría.
     */
    @Mapping(target = "idContactDetail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ContactDetail toEntity(ContactDetailRequest request);

    /**
     * Mapea la entidad ContactDetail a la respuesta DTO ContactDetailResponse.
     */
    ContactDetailResponse toResponse(ContactDetail entity);

    /**
     * Mapea una lista de entidades a una lista de DTOs de respuesta.
     */
    List<ContactDetailResponse> toResponseList(List<ContactDetail> entities);

    /**
     * Actualiza completamente una entidad existente (PUT).
     */
    @Mapping(target = "idContactDetail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ContactDetailRequest request, @MappingTarget ContactDetail entity);

    /**
     * Actualiza parcialmente una entidad existente (PATCH).
     * Modifica únicamente los campos no nulos del DTO de entrada.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idContactDetail", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromPatch(ContactDetailPatchRequest request, @MappingTarget ContactDetail entity);
}