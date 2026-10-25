package com.intrumentoev.demo.pago;

import com.intrumentoev.demo.client.gestopago.GestoPagoTxClient;
import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.pago.GestoPagoHttpResponse;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.gestopago.PagoGestoPagoRepository;
import com.intrumentoev.demo.service.impl.gestopago.CatalogoRefresher;
import com.intrumentoev.demo.service.service.gestopago.GestoPagoTokenService;
import com.intrumentoev.demo.service.service.gestopago.ProductoServiceRedis;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Base de las pruebas de pago: H2 + datos demo (DemoUserInitializer), GestoPago simulado (nunca se llama al real).
 * Todas las subclases comparten el mismo conjunto de @MockitoBean para reutilizar el contexto de Spring.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PagoIntegrationTestBase {

    protected static final String CUENTA_DEMO = "9876543210";
    protected static final String TELEFONO = "5512345678";

    @Autowired protected AccountRepository accountRepository;
    @Autowired protected PagoGestoPagoRepository pagoRepository;

    @MockitoBean protected GestoPagoTxClient txClient;
    @MockitoBean protected ProductoServiceRedis productoServiceRedis;
    @MockitoBean protected GestoPagoTokenService tokenService;
    @MockitoBean protected CatalogoRefresher catalogoRefresher;

    @BeforeEach
    void baseSetUp() {
        pagoRepository.deleteAll();
        establecerCuenta(new BigDecimal("1000.00"), "ACTIVA");
        when(tokenService.obtenerTokenVigente()).thenReturn("token-1");
        when(tokenService.renovarTokenForzado()).thenReturn("token-2");
        when(productoServiceRedis.getProductsRedis()).thenReturn(catalogo());
    }

    protected Account cuentaDemo() {
        return accountRepository.findByAccountNumber(CUENTA_DEMO).orElseThrow();
    }

    protected void establecerCuenta(BigDecimal saldo, String estatus) {
        Account cuenta = cuentaDemo();
        cuenta.setBalance(saldo);
        cuenta.setStatus(estatus);
        accountRepository.save(cuenta);
    }

    protected BigDecimal saldo() {
        return cuentaDemo().getBalance();
    }

    /** Recarga (tipoFront 1, $50) y servicio (tipoFront 2, sin precio fijo). */
    protected List<ProductoResponse> catalogo() {
        return List.of(
                ProductoResponse.builder().idServicio(133).idProducto(582).producto("Recarga 50").servicio("Telcel")
                        .idCatTipoServicio(1).tipoFront(1).precio("50.00").build(),
                ProductoResponse.builder().idServicio(79).idProducto(209).producto("Luz").servicio("CFE")
                        .idCatTipoServicio(5).tipoFront(2).precio("0").build());
    }

    protected static GestoPagoHttpResponse xml(String codigo, String texto, Long idTx, String numAutorizacion, String comision) {
        StringBuilder sb = new StringBuilder("<RESPONSE>");
        if (idTx != null) {
            sb.append("<ID_TX>").append(idTx).append("</ID_TX>");
        }
        sb.append("<NUM_AUTORIZACION>").append(numAutorizacion).append("</NUM_AUTORIZACION>");
        if (comision != null) {
            sb.append("<COMISION>").append(comision).append("</COMISION>");
        }
        sb.append("<MENSAJE><CODIGO>").append(codigo).append("</CODIGO><TEXTO>").append(texto).append("</TEXTO></MENSAJE></RESPONSE>");
        return GestoPagoHttpResponse.conRespuesta(200, sb.toString());
    }
}
