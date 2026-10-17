package com.intrumentoev.demo.mapper.home;

import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.model.client.ClientUnifiedPatchRequest;
import com.intrumentoev.demo.model.home.HomeResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HomeMapper {

    @Mapping(target = "municipalityName", ignore = true)
    @Mapping(target = "idState", ignore = true)
    @Mapping(target = "cveEnt", ignore = true)
    @Mapping(target = "stateName", ignore = true)
    HomeResponse toResponse(Home entity);

    List<HomeResponse> toResponseList(List<Home> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idHome", ignore = true)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUnifiedPatch(ClientUnifiedPatchRequest request, @MappingTarget Home entity);
}
