package com.intrumentoev.demo.service.impl.gestopago;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.gestopago.DispositivoPush;
import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.AuthMethod;
import com.intrumentoev.demo.model.auth.AuthenticatedClient;
import com.intrumentoev.demo.model.auth.Role;
import com.intrumentoev.demo.model.gestopago.ProductoResponse;
import com.intrumentoev.demo.model.gestopago.pago.EstadoAutorizacionPago;
import com.intrumentoev.demo.model.gestopago.pago.EstadoPago;
import com.intrumentoev.demo.model.gestopago.pago.PagoAutorizacionResponse;
import com.intrumentoev.demo.model.gestopago.pago.PagoRequest;
import com.intrumentoev.demo.model.gestopago.pago.PagoResponse;
import com.intrumentoev.demo.model.gestopago.pago.PushTokenRequest;
import com.intrumentoev.demo.model.gestopago.pago.TipoPago;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.gestopago.DispositivoPushRepository;
import com.intrumentoev.demo.repository.gestopago.PagoAutorizacionRepository;
import com.intrumentoev.demo.service.service.gestopago.PagoGestoPagoService;
import com.intrumentoev.demo.service.service.gestopago.ProductoService;
import com.intrumentoev.demo.service.service.gestopago.PushNotificationService;
import com.intrumentoev.demo.service.service.gestopago.ReferenciaValidacionService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoAutorizacionServiceImplTest {

    @Mock private DispositivoPushRepository dispositivoRepository;
    @Mock private PagoAutorizacionRepository autorizacionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProductoService productoService;
    @Mock private ReferenciaValidacionService referenciaValidacionService;
    @Mock private PagoGestoPagoService pagoGestoPagoService;
    @Mock private PushNotificationService pushNotificationService;

    private PagoAutorizacionServiceImpl service;
    private AuthenticatedClient cliente;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new PagoAutorizacionServiceImpl(
                dispositivoRepository,
                autorizacionRepository,
                accountRepository,
                clientRepository,
                productoService,
                referenciaValidacionService,
                pagoGestoPagoService,
                pushNotificationService,
                validator
        );
        cliente = AuthenticatedClient.builder()
                .idClient(1L)
                .idLogin(10L)
                .email("demo@banco.com")
                .authMethod(AuthMethod.PASSWORD)
                .role(Role.CLIENT)
                .build();
    }

    @Test
    void registrarDispositivo_creaOActualizaToken() {
        when(dispositivoRepository.findByIdClienteAndPushToken(1L, "fcm-token-1234567890"))
                .thenReturn(Optional.empty());
        service.registrarDispositivo(cliente, PushTokenRequest.builder()
                .token("fcm-token-1234567890")
                .plataforma("ANDROID")
                .deviceName("Moto G22")
                .build());
        verify(dispositivoRepository).save(any(DispositivoPush.class));
    }

    @Test
    void solicitarAutorizacion_recargaValida_creaPendienteYEnviaPush() {
        Account cuenta = Account.builder()
                .idAccount(1L).idClient(1L).accountNumber("9876543210")
                .status("ACTIVA").balance(new BigDecimal("1000.00")).build();
        Client client = new Client();
        client.setIdClient(1L);
        client.setIsActive(true);
        ProductoResponse prod = ProductoResponse.builder()
                .idServicio(133).idProducto(582).servicio("Telcel").producto("Amigo 100")
                .tipoFront(1).precio("100.00").build();

        when(accountRepository.findByAccountNumber("9876543210")).thenReturn(Optional.of(cuenta));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(productoService.getProductsRedis()).thenReturn(List.of(prod));
        when(autorizacionRepository.save(any(PagoAutorizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoRequest req = PagoRequest.builder()
                .numCuenta("9876543210").idServicio(133).idProducto(582)
                .tipoPago("RECARGA").telefono("5512345678").build();

        PagoAutorizacionResponse resp = service.solicitarAutorizacion(cliente, req);

        assertNotNull(resp.getIdAutorizacion());
        assertEquals(EstadoAutorizacionPago.PENDIENTE, resp.getEstado());
        assertEquals(new BigDecimal("100.00"), resp.getMonto());
        assertEquals("Telcel", resp.getNombreServicio());
        verify(pushNotificationService).enviarNotificacionAutorizacionPago(eq(1L), any(PagoAutorizacion.class));
    }

    @Test
    void solicitarAutorizacion_saldoInsuficiente_lanzaExcepcion() {
        Account cuenta = Account.builder()
                .idAccount(1L).idClient(1L).accountNumber("9876543210")
                .status("ACTIVA").balance(new BigDecimal("50.00")).build();
        Client client = new Client();
        client.setIdClient(1L);
        client.setIsActive(true);
        ProductoResponse prod = ProductoResponse.builder()
                .idServicio(133).idProducto(582).servicio("Telcel").producto("Amigo 100")
                .tipoFront(1).precio("100.00").build();

        when(accountRepository.findByAccountNumber("9876543210")).thenReturn(Optional.of(cuenta));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(productoService.getProductsRedis()).thenReturn(List.of(prod));

        PagoRequest req = PagoRequest.builder()
                .numCuenta("9876543210").idServicio(133).idProducto(582)
                .tipoPago("RECARGA").telefono("5512345678").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.solicitarAutorizacion(cliente, req));
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCodeEnum());
    }

    @Test
    void aprobarAutorizacion_ejecutaPagoRealYGuardaResultado() {
        PagoAutorizacion pending = PagoAutorizacion.builder()
                .id("auth-1").idCliente(1L).numCuenta("9876543210")
                .idServicio(133).idProducto(582).tipoPago(TipoPago.RECARGA)
                .telefono("5512345678").monto(new BigDecimal("100.00"))
                .estado(EstadoAutorizacionPago.PENDIENTE)
                .expiraEn(OffsetDateTime.now().plusMinutes(2))
                .build();
        when(autorizacionRepository.findById("auth-1")).thenReturn(Optional.of(pending));
        when(pagoGestoPagoService.procesarPago(eq(cliente), any(PagoRequest.class)))
                .thenReturn(PagoResponse.builder()
                        .codigo(0).estado(EstadoPago.RECHAZADO)
                        .codigoGestoPago("05")
                        .mensaje("No se puede procesar el pago por el momento, intente más tarde")
                        .upc("UPC123").build());
        when(autorizacionRepository.save(any(PagoAutorizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoAutorizacionResponse resp = service.aprobarAutorizacion(cliente, "auth-1");

        assertEquals(EstadoAutorizacionPago.APROBADO, resp.getEstado());
        assertNotNull(resp.getResultadoPago());
        assertEquals("05", resp.getResultadoPago().getCodigoGestoPago());
        assertEquals("UPC123", resp.getResultadoPago().getUpc());
    }

    @Test
    void rechazarYCancelarYExpirar_actualizanEstadosCorrectamente() {
        PagoAutorizacion pending = PagoAutorizacion.builder()
                .id("auth-2").idCliente(1L).numCuenta("9876543210")
                .idServicio(133).idProducto(582).tipoPago(TipoPago.RECARGA)
                .telefono("5512345678").monto(new BigDecimal("100.00"))
                .estado(EstadoAutorizacionPago.PENDIENTE)
                .expiraEn(OffsetDateTime.now().plusMinutes(2))
                .build();
        when(autorizacionRepository.findById("auth-2")).thenReturn(Optional.of(pending));
        when(autorizacionRepository.save(any(PagoAutorizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoAutorizacionResponse rejected = service.rechazarAutorizacion(cliente, "auth-2");
        assertEquals(EstadoAutorizacionPago.RECHAZADO, rejected.getEstado());

        PagoAutorizacion expired = PagoAutorizacion.builder()
                .id("auth-3").idCliente(1L).numCuenta("9876543210")
                .idServicio(133).idProducto(582).tipoPago(TipoPago.RECARGA)
                .telefono("5512345678").monto(new BigDecimal("100.00"))
                .estado(EstadoAutorizacionPago.PENDIENTE)
                .expiraEn(OffsetDateTime.now().minusSeconds(5))
                .build();
        when(autorizacionRepository.findById("auth-3")).thenReturn(Optional.of(expired));

        PagoAutorizacionResponse consulted = service.consultarAutorizacion(cliente, "auth-3");
        assertEquals(EstadoAutorizacionPago.EXPIRADO, consulted.getEstado());
    }

    @Test
    void listarPendientes_filtraExpiradasYDevuelveVigentes() {
        PagoAutorizacion vigente = PagoAutorizacion.builder()
                .id("v1").idCliente(1L).numCuenta("9876543210").idServicio(133).idProducto(582)
                .tipoPago(TipoPago.RECARGA).monto(new BigDecimal("100.00"))
                .estado(EstadoAutorizacionPago.PENDIENTE).expiraEn(OffsetDateTime.now().plusMinutes(1)).build();
        PagoAutorizacion vencida = PagoAutorizacion.builder()
                .id("v2").idCliente(1L).numCuenta("9876543210").idServicio(133).idProducto(582)
                .tipoPago(TipoPago.RECARGA).monto(new BigDecimal("100.00"))
                .estado(EstadoAutorizacionPago.PENDIENTE).expiraEn(OffsetDateTime.now().minusMinutes(1)).build();
        when(autorizacionRepository.findByIdClienteAndEstadoOrderByFechaCreacionDesc(1L, EstadoAutorizacionPago.PENDIENTE))
                .thenReturn(List.of(vigente, vencida));

        List<PagoAutorizacionResponse> result = service.listarPendientes(cliente);
        assertEquals(1, result.size());
        assertEquals("v1", result.get(0).getIdAutorizacion());
        assertEquals(EstadoAutorizacionPago.EXPIRADO, vencida.getEstado());
    }

    @Test
    void consultarAutorizacion_deOtroCliente_lanzaPaymentForbidden() {
        PagoAutorizacion other = PagoAutorizacion.builder()
                .id("auth-9").idCliente(999L).estado(EstadoAutorizacionPago.PENDIENTE)
                .expiraEn(OffsetDateTime.now().plusMinutes(2)).build();
        when(autorizacionRepository.findById("auth-9")).thenReturn(Optional.of(other));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consultarAutorizacion(cliente, "auth-9"));
        assertEquals(ErrorCode.PAYMENT_FORBIDDEN, ex.getErrorCodeEnum());
    }
}
