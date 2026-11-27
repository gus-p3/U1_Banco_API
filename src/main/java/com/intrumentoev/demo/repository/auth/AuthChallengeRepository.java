package com.intrumentoev.demo.repository.auth;

import com.intrumentoev.demo.entity.auth.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, String> {

    List<AuthChallenge> findByIdClientAndPurposeAndEstadoAndAllowBiometricTrueAndBiometricRequestedTrueOrderByFechaCreacionDesc(
            Long idClient,
            String purpose,
            String estado
    );
}
