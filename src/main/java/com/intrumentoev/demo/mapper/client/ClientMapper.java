package com.intrumentoev.demo.mapper.client;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.model.client.ClientOnboardingRequest;
import com.intrumentoev.demo.model.client.ClientPatchRequest;
import com.intrumentoev.demo.model.client.ClientRequest;
import com.intrumentoev.demo.model.client.ClientResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    /**
     * Convierte la petición de creación a la entidad JPA Client.
     */
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "deactivatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(ClientRequest request);

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
     */
    ClientResponse toResponse(Client entity);

    /**
     * Mapea una lista de entidades Client a una lista de respuestas DTO ClientResponse.
     */
    List<ClientResponse> toResponseList(List<Client> entities);

    /**
     * Actualiza completamente una entidad Client existente con los datos de ClientRequest.
     */
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "deactivatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ClientRequest request, @MappingTarget Client entity);

    /**
     * Actualiza parcialmente una entidad Client existente (PATCH).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idClient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromPatch(ClientPatchRequest request, @MappingTarget Client entity);
}