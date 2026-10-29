package com.intrumentoev.demo.repository.gestopago;

import com.intrumentoev.demo.entity.gestopago.PagoGestoPago;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoGestoPagoRepository extends JpaRepository<PagoGestoPago, Long> {

    Optional<PagoGestoPago> findByUpc(String upc);

    boolean existsByUpc(String upc);

    // Bloquea la fila del pago para que el flujo principal y el job no lo resuelvan dos veces
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagoGestoPago p where p.id = :id")
    Optional<PagoGestoPago> findByIdForUpdate(@Param("id") Long id);

    // Pagos que el job de confirmTx debe revisar (los más antiguos primero, máximo 50 por vuelta)
    List<PagoGestoPago> findTop50ByEstadoAndFechaEnvioBeforeOrderByFechaEnvioAsc(EstadoPago estado, OffsetDateTime limite);

    // Ventana de confirmTx: enviados hace más de N segundos pero dentro de las 24 h de vigencia
    List<PagoGestoPago> findTop50ByEstadoAndFechaEnvioBetweenOrderByFechaEnvioAsc(
            EstadoPago estado, OffsetDateTime desde, OffsetDateTime hasta);
}