package com.intrumentoev.demo.repository.client;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.model.client.ClientUnifiedQueryRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClientSpecification {

    public static Specification<Client> conFiltros(ClientUnifiedQueryRequest filtros) {
        return (root, query, cb) -> {
            if (filtros == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtros.getCurp() != null && !filtros.getCurp().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("curp")), filtros.getCurp().trim().toUpperCase()));
            }

            if (filtros.getRfc() != null && !filtros.getRfc().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("rfc")), filtros.getRfc().trim().toUpperCase()));
            }

            if (filtros.getIdNationality() != null) {
                predicates.add(cb.equal(root.get("idNationality"), filtros.getIdNationality()));
            }

            if (filtros.getActivo() != null) {
                predicates.add(cb.equal(root.get("isActive"), filtros.getActivo()));
            }

            if (filtros.getDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filtros.getDesde()));
            }

            if (filtros.getHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filtros.getHasta()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
