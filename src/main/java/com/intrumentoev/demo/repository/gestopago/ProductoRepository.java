package com.intrumentoev.demo.repository.gestopago;

import com.intrumentoev.demo.entity.gestopago.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
}
