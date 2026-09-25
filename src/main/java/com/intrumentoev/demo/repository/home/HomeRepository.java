package com.intrumentoev.demo.repository.home;

import com.intrumentoev.demo.entity.home.Home;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HomeRepository extends JpaRepository<Home, Long> {

    Optional<Home> findByIdClient(Long idClient);

    boolean existsByIdClient(Long idClient);
}
