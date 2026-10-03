package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenderRepository extends JpaRepository<Gender, Short> {
    Optional<Gender> findByName(String name);
    List<Gender> findByIsActiveTrue();
    Boolean existsByIdGender(Number id);
}
