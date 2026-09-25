package com.intrumentoev.demo.repository.employment;

import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmploymentInformationRepository extends JpaRepository<EmploymentInformation, Long> {

    Optional<EmploymentInformation> findByIdClient(Long idClient);

    boolean existsByIdClient(Long idClient);
}
