package com.intrumentoev.demo.repository.gestopago;

import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;
import com.intrumentoev.demo.model.gestopago.pago.EstadoAutorizacionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoAutorizacionRepository extends JpaRepository<PagoAutorizacion, String> {

    List<PagoAutorizacion> findByIdClienteAndEstadoOrderByFechaCreacionDesc(Long idCliente, EstadoAutorizacionPago estado);
}
