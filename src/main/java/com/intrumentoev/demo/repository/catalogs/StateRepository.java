package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Short> {

    Optional<State> findByCveEnt(String cveEnt);

    boolean existsByCveEnt(String cveEnt);

    List<State> findByIsActiveTrueOrderByNameAsc();
}
