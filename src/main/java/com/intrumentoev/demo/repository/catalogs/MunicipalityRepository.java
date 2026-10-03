package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, Integer> {

    List<Municipality> findByIdStateAndIsActiveTrueOrderByNameAsc(Short idState);

    Optional<Municipality> findByIdStateAndCveMun(Short idState, String cveMun);

    boolean existsByIdStateAndCveMun(Short idState, String cveMun);
    Boolean existsByIdMunicipality(Number id);
}
