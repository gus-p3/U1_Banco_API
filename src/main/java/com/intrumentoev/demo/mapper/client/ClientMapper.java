package com.intrumentoev.demo.mapper.client;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.model.client.ClientOnboardingRequest;
import com.intrumentoev.demo.model.client.ClientResponse;
import com.intrumentoev.demo.model.client.ClientUnifiedPatchRequest;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    /**
     * Convierte la petición integral de Onboarding a la entidad JPA Client.
     */
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "deactivatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntityFromOnboarding(ClientOnboardingRequest request);

    /**
     * Mapea la entidad Client a la respuesta DTO ClientResponse.
     * Los nombres de catálogos son enriquecidos posteriormente por el servicio.
     */
    @Mapping(target = "genderName", ignore = true)
    @Mapping(target = "nationalityName", ignore = true)
    @Mapping(target = "maritalStatusName", ignore = true)
    ClientResponse toResponse(Client entity);

    /**
     * Mapea una lista de entidades Client a una lista de respuestas DTO ClientResponse.
     */
    List<ClientResponse> toResponseList(List<Client> entities);

    /**
     * Actualiza parcialmente una entidad Client existente desde ClientUnifiedPatchRequest ignorando nulos.
     * CURP y RFC no se modifican aquí (se usan como identificador inmutable).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "rfc", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "deactivatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUnifiedPatch(ClientUnifiedPatchRequest request, @MappingTarget Client entity);
}