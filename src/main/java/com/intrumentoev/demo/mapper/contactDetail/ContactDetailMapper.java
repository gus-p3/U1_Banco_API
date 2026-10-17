package com.intrumentoev.demo.mapper.contactDetail;

import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.model.client.ClientUnifiedPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContactDetailMapper {

    ContactDetailResponse toResponse(ContactDetail entity);

    List<ContactDetailResponse> toResponseList(List<ContactDetail> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idContactDetail", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUnifiedPatch(ClientUnifiedPatchRequest request, @MappingTarget ContactDetail entity);
}