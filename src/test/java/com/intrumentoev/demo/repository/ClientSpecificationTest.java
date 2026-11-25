package com.intrumentoev.demo.repository;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.model.client.ClientUnifiedQueryRequest;
import com.intrumentoev.demo.repository.client.ClientSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class ClientSpecificationTest {

    private final Root<Client> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @Test
    @DisplayName("Sin filtros devuelve una conjunción (todos los clientes)")
    void sinFiltros() {
        ClientSpecification.conFiltros(null).toPredicate(root, query, cb);
        ClientSpecification.conFiltros(ClientUnifiedQueryRequest.builder().curp(" ").rfc("").build())
                .toPredicate(root, query, cb);

        verify(cb, times(1)).conjunction();
        verify(cb, never()).equal(any(), (Object) any());
    }

    @Test
    @DisplayName("Cada filtro agrega su predicado (CURP/RFC en mayúsculas)")
    void todosLosFiltros() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(1);
        OffsetDateTime hasta = OffsetDateTime.now();
        ClientUnifiedQueryRequest filtros = ClientUnifiedQueryRequest.builder()
                .curp(" roma900101hdfrrn01 ").rfc("roma900101ab1")
                .idNationality((short) 1).activo(true).desde(desde).hasta(hasta).build();

        Specification<Client> spec = ClientSpecification.conFiltros(filtros);
        spec.toPredicate(root, query, cb);

        verify(cb).equal(any(), eq("ROMA900101HDFRRN01"));
        verify(cb).equal(any(), eq("ROMA900101AB1"));
        verify(cb).equal(any(), eq((short) 1));
        verify(cb).equal(any(), eq(true));
        verify(cb).greaterThanOrEqualTo(any(), eq(desde));
        verify(cb).lessThanOrEqualTo(any(), eq(hasta));
    }
}
