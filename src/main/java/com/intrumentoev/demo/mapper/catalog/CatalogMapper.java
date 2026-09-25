package com.intrumentoev.demo.mapper.catalog;

import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.model.catalog.MunicipalityResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    StateResponse toStateResponse(State state);

    List<StateResponse> toStateResponseList(List<State> states);

    MunicipalityResponse toMunicipalityResponse(Municipality municipality);

    List<MunicipalityResponse> toMunicipalityResponseList(List<Municipality> municipalities);
}
