package com.intrumentoev.demo.repository.client;

import com.intrumentoev.demo.entity.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByCurp(String curp);

    Optional<Client> findByRfc(String rfc);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    List<Client> findByIsActiveTrue();

    List<Client> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}