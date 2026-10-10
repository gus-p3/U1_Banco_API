package com.intrumentoev.demo.mapper.employment;

import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmploymentInformationMapper {

    @Mapping(target = "idEmployment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EmploymentInformation toEntity(EmploymentInformationRequest request);

    EmploymentInformationResponse toResponse(EmploymentInformation entity);

    List<EmploymentInformationResponse> toResponseList(List<EmploymentInformation> entities);

    @Mapping(target = "idEmployment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(EmploymentInformationRequest request, @MappingTarget EmploymentInformation entity);

    @Mapping(target = "idEmployment", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUpdateRequest(com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request, @MappingTarget EmploymentInformation entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idEmployment", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromPatch(EmploymentInformationPatchRequest request, @MappingTarget EmploymentInformation entity);
}
