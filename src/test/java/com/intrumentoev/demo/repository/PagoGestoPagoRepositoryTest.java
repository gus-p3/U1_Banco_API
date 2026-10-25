package com.intrumentoev.demo.repository;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.gestopago.PagoGestoPago;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.TipoPago;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.gestopago.PagoGestoPagoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PagoGestoPagoRepositoryTest {

    @Autowired
    private PagoGestoPagoRepository pagoRepository;

    @Autowired
    private AccountRepository accountRepository;

    private PagoGestoPago nuevoPago(String upc) {
        return PagoGestoPago.builder()
                .idCliente(1L)
                .numCuenta("1234567890")
                .idServicio(79)
                .idProducto(209)
                .tipoPago(TipoPago.SERVICIO)
                .referencia("40425475190118187271")
                .monto(new BigDecimal("150.00"))
                .upc(upc)
                .payloadJson("{\"upc\":\"" + upc + "\"}")
                .build();
    }

    @Test
    @DisplayName("Guarda un pago con estado PENDIENTE, 0 intentos y fecha de creación")
    void guardaConValoresPorDefecto() {
        PagoGestoPago guardado = pagoRepository.saveAndFlush(nuevoPago("BXTEST000000001"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(guardado.getIntentosConfirmacion()).isZero();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(pagoRepository.findByUpc("BXTEST000000001")).isPresent();
        assertThat(pagoRepository.existsByUpc("BXTEST000000001")).isTrue();
    }

    @Test
    @DisplayName("El UPC es único: un segundo pago con el mismo UPC se rechaza")
    void upcDuplicadoFalla() {
        pagoRepository.saveAndFlush(nuevoPago("BXTEST000000002"));

        assertThatThrownBy(() -> pagoRepository.saveAndFlush(nuevoPago("BXTEST000000002")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("El job solo encuentra POR_CONFIRMAR enviados antes del límite")
    void buscaPendientesDeConfirmar() {
        PagoGestoPago viejo = nuevoPago("BXTEST000000003");
        viejo.setEstado(EstadoPago.POR_CONFIRMAR);
        viejo.setFechaEnvio(OffsetDateTime.now().minusMinutes(2));
        pagoRepository.save(viejo);

        PagoGestoPago reciente = nuevoPago("BXTEST000000004");
        reciente.setEstado(EstadoPago.POR_CONFIRMAR);
        reciente.setFechaEnvio(OffsetDateTime.now());
        pagoRepository.saveAndFlush(reciente);

        var pendientes = pagoRepository.findTop50ByEstadoAndFechaEnvioBeforeOrderByFechaEnvioAsc(
                EstadoPago.POR_CONFIRMAR, OffsetDateTime.now().minusSeconds(62));

        assertThat(pendientes).extracting(PagoGestoPago::getUpc)
                .contains("BXTEST000000003")
                .doesNotContain("BXTEST000000004");
    }

    @Test
    @DisplayName("findByAccountNumberForUpdate lee la cuenta con bloqueo dentro de la transacción")
    void bloqueaCuentaPorNumero() {
        accountRepository.saveAndFlush(Account.builder()
                .accountNumber("5550001112")
                .idClient(1L)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVA")
                .build());

        assertThat(accountRepository.findByAccountNumberForUpdate("5550001112"))
                .get()
                .extracting(Account::getBalance)
                .isEqualTo(new BigDecimal("500.00"));
    }
}
