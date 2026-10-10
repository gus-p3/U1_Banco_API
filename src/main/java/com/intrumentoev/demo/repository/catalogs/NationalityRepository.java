package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.Nationality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NationalityRepository extends JpaRepository<Nationality, Short> {
    Optional<Nationality> findByName(String name);
    List<Nationality> findByIsActiveTrue();
    Boolean existsByIdNationality(Number id);
}
