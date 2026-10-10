package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.MaritalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaritalStatusRepository extends JpaRepository<MaritalStatus, Short> {
    Optional<MaritalStatus> findByName(String name);
    List<MaritalStatus> findByIsActiveTrue();
    Boolean existsByIdMaritalStatus(Number id);
}
