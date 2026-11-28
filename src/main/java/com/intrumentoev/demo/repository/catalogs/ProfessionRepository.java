package com.intrumentoev.demo.repository.catalogs;

import com.intrumentoev.demo.entity.catalogs.Profession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionRepository extends JpaRepository<Profession, Short> {
    Optional<Profession> findByNameIgnoreCase(String name);
    List<Profession> findByIsActiveTrueOrderByNameAsc();
}
