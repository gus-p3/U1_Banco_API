package com.intrumentoev.demo.mapper.employment;

import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.model.client.ClientUnifiedPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmploymentInformationMapper {

    EmploymentInformationResponse toResponse(EmploymentInformation entity);

    List<EmploymentInformationResponse> toResponseList(List<EmploymentInformation> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idEmployment", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUnifiedPatch(ClientUnifiedPatchRequest request, @MappingTarget EmploymentInformation entity);
}
