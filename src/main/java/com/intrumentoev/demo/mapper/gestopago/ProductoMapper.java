package com.intrumentoev.demo.mapper.gestopago;

import com.intrumentoev.demo.entity.gestopago.Producto;
import com.intrumentoev.demo.model.gestopago.GestoPagoProductXmlResponse;
import com.intrumentoev.demo.model.gestopago.ProductoPatchRequest;
import com.intrumentoev.demo.model.gestopago.ProductoRequest;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    String GESTOPAGO_LOGO_URL_TEMPLATE = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/%d.png";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "logoUrl", expression = "java(construirLogoUrl(entity.getIdServicio()))")
    ProductoResponse toResponse(Producto entity);

    List<ProductoResponse> toResponseList(List<Producto> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ProductoRequest request, @MappingTarget Producto entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromPatch(ProductoPatchRequest request, @MappingTarget Producto entity);

    // Mapeos MapStruct de items XML a ProductoResponse (DTO)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "categoria", expression = "java(traducirCategoria(xmlItem.getIdCatTipoServicio(), xmlItem.getTipoFront()))")
    @Mapping(target = "logoUrl", expression = "java(construirLogoUrl(xmlItem.getIdServicio()))")
    ProductoResponse xmlItemToResponse(GestoPagoProductXmlResponse.ProductoXmlItem xmlItem);

    List<ProductoResponse> xmlItemsToResponseList(List<GestoPagoProductXmlResponse.ProductoXmlItem> xmlItems);

    // Mapeos MapStruct de items XML a Producto (Entidad JPA)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "categoria", expression = "java(traducirCategoria(xmlItem.getIdCatTipoServicio(), xmlItem.getTipoFront()))")
    Producto xmlItemToEntity(GestoPagoProductXmlResponse.ProductoXmlItem xmlItem);

    List<Producto> xmlItemsToEntityList(List<GestoPagoProductXmlResponse.ProductoXmlItem> xmlItems);

    @AfterMapping
    default void enrichResponse(@MappingTarget ProductoResponse target, Producto source) {
        if (target.getCategoria() == null || target.getCategoria().isBlank()) {
            target.setCategoria(traducirCategoria(source.getIdCatTipoServicio(), source.getTipoFront()));
        }
        if (target.getLogoUrl() == null || target.getLogoUrl().isBlank()) {
            target.setLogoUrl(construirLogoUrl(source.getIdServicio()));
        }
    }

    default String construirLogoUrl(Integer idServicio) {
        if (idServicio == null || idServicio <= 0) {
            return null;
        }
        return String.format(GESTOPAGO_LOGO_URL_TEMPLATE, idServicio);
    }

    @AfterMapping
    default void enrichEntity(@MappingTarget Producto target) {
        if (target.getCategoria() == null || target.getCategoria().isBlank()) {
            target.setCategoria(traducirCategoria(target.getIdCatTipoServicio(), target.getTipoFront()));
        }
    }

    default String traducirCategoria(Integer idCatTipoServicio, Integer tipoFront) {
        if (idCatTipoServicio != null) {
            switch (idCatTipoServicio) {
                case 1:
                    return "Recargas";
                case 2:
                    return "Servicios";
                case 3:
                    return "Telecomunicaciones y TV";
                case 4:
                    return "Agua y Gobierno";
                case 10:
                case 11:
                    return "Pines y Entretenimiento";
                default:
                    break;
            }
        }
        if (tipoFront != null) {
            switch (tipoFront) {
                case 1:
                    return "Recargas";
                case 2:
                    return "Servicios";
                case 4:
                    return "Pines y Entretenimiento";
                default:
                    break;
            }
        }
        return "Otros servicios";
    }

    default List<ProductoResponse> filtrarYOrdenarPorTipoFront(List<ProductoResponse> jsonItems) {
        if (jsonItems == null || jsonItems.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(1);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        return jsonItems.stream()
                .filter(p -> p.getTipoFront() != null && p.getTipoFront() > 0)
                .sorted(Comparator.comparing(ProductoResponse::getTipoFront, Comparator.nullsLast(Comparator.naturalOrder())))
                .peek(p -> {
                    if (p.getId() == null) {
                        p.setId(index.getAndIncrement());
                    }
                    if (p.getCreatedAt() == null) {
                        p.setCreatedAt(now);
                    }
                    if (p.getCategoria() == null || p.getCategoria().isBlank()) {
                        p.setCategoria(traducirCategoria(p.getIdCatTipoServicio(), p.getTipoFront()));
                    }
                    if (p.getLogoUrl() == null || p.getLogoUrl().isBlank()) {
                        p.setLogoUrl(construirLogoUrl(p.getIdServicio()));
                    }
                })
                .collect(Collectors.toList());
    }
}
