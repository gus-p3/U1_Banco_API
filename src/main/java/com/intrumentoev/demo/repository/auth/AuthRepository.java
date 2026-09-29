package com.intrumentoev.demo.repository.auth;

import com.intrumentoev.demo.entity.auth.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findByEmail(String email);

    Optional<Auth> findByIdClient(Long idClient);

    Optional<Auth> findByRefreshToken(String refreshToken);

    boolean existsByEmail(String email);

    boolean existsByIdClient(Long idClient);
}
