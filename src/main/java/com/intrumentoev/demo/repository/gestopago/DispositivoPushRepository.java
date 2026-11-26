package com.intrumentoev.demo.repository.gestopago;

import com.intrumentoev.demo.entity.gestopago.DispositivoPush;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    Optional<DispositivoPush> findByIdClienteAndPushToken(Long idCliente, String pushToken);

    List<DispositivoPush> findByIdClienteAndActivoTrue(Long idCliente);
}
