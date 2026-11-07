package com.intrumentoev.demo.repository.client;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.model.client.ClientUnifiedQueryRequest;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientSpecificationTest {

    @Mock
    private Root<Client> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Expression<String> stringExpression;

    @Mock
    private Predicate predicate;

    @Test
    void conFiltros_NullFiltros() {
        Specification<Client> spec = ClientSpecification.conFiltros(null);
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).conjunction();
    }

    @Test
    void conFiltros_TodosLosCampos() {
        ClientUnifiedQueryRequest filtros = new ClientUnifiedQueryRequest();
        filtros.setCurp(" CURP123456 ");
        filtros.setRfc(" RFC123456 ");
        filtros.setIdNationality((short) 1);
        filtros.setActivo(true);
        OffsetDateTime now = OffsetDateTime.now();
        filtros.setDesde(now.minusDays(1));
        filtros.setHasta(now);

        doReturn(path).when(root).get(anyString());
        doReturn(stringExpression).when(cb).upper(any());
        doReturn(predicate).when(cb).equal(any(), any());
        doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(Comparable.class));
        doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(Comparable.class));
        doReturn(predicate).when(cb).and(any(Predicate[].class));

        Specification<Client> spec = ClientSpecification.conFiltros(filtros);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void conFiltros_CamposVacios() {
        ClientUnifiedQueryRequest filtros = new ClientUnifiedQueryRequest();
        filtros.setCurp("");
        filtros.setRfc("  ");

        doReturn(predicate).when(cb).and(any(Predicate[].class));

        Specification<Client> spec = ClientSpecification.conFiltros(filtros);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).and(any(Predicate[].class));
    }
}
