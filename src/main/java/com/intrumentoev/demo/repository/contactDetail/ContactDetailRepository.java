package com.intrumentoev.demo.repository.contactDetail;

import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactDetailRepository extends JpaRepository<ContactDetail, Long> {

    // Buscar el detalle de contacto por el ID del cliente (Relación 1 a 1)
    Optional<ContactDetail> findByIdClient(Long idClient);

    // Buscar por correo electrónico
    Optional<ContactDetail> findByEmail(String email);

    // Buscar por teléfono móvil
    Optional<ContactDetail> findByMobilePhone(String mobilePhone);

    // Comprobar si ya existe un detalle de contacto para un cliente
    boolean existsByIdClient(Long idClient);

    // Comprobar si ya existe un correo electrónico registrado
    boolean existsByEmail(String email);

    // Comprobar si ya existe un teléfono móvil registrado
    boolean existsByMobilePhone(String mobilePhone);
}