package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.mapper.account.AccountMapper;
import com.intrumentoev.demo.mapper.client.ClientMapper;
import com.intrumentoev.demo.mapper.contactDetail.ContactDetailMapper;
import com.intrumentoev.demo.mapper.employment.EmploymentInformationMapper;
import com.intrumentoev.demo.mapper.home.HomeMapper;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.impl.client.ClientServiceImpl;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ContactDetailRepository contactDetailRepository;
    @Mock
    private HomeRepository homeRepository;
    @Mock
    private EmploymentInformationRepository employmentRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MunicipalityRepository municipalityRepository;
    @Mock
    private StateRepository stateRepository;

    @Mock
    private ClientMapper clientMapper;
    @Mock
    private ContactDetailMapper contactDetailMapper;
    @Mock
    private HomeMapper homeMapper;
    @Mock
    private EmploymentInformationMapper employmentMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private com.intrumentoev.demo.mapper.auth.AuthMapper authMapper;

    @Mock
    private AccountService accountService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client mockClient;

    @BeforeEach
    void setUp() {
        mockClient = new Client();
        mockClient.setIdClient(1L);
        mockClient.setName("Juan");
        mockClient.setLastName("Pérez");
        mockClient.setCurp("ROMA900101HDFRRN01");
        mockClient.setRfc("ROMA900101ABC");
        mockClient.setBirthDate(LocalDate.of(1990, 1, 1));
        mockClient.setIdGender((short) 1);
        mockClient.setIdNationality((short) 1);
        mockClient.setIdMaritalStatus((short) 1);
        mockClient.setIsActive(true);
        mockClient.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("API 1 - Consultar clientes con filtros y ordenamiento dinámico")
    @SuppressWarnings("unchecked")
    void testConsultarClientes() {
        ClientUnifiedQueryRequest request = ClientUnifiedQueryRequest.builder()
                .curp("ROMA900101HDFRRN01")
                .sortBy("name")
                .sortDirection("ASC")
                .build();

        when(clientRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockClient));
        when(clientMapper.toResponseList(anyList())).thenReturn(List.of(ClientResponse.builder().curp("ROMA900101HDFRRN01").name("Juan").build()));

        List<ClientResponse> resultados = clientService.consultarClientes(request);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getCurp()).isEqualTo("ROMA900101HDFRRN01");
        verify(clientRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("API 2 - Actualizar parcialmente datos del cliente (Súper PATCH)")
    void testActualizarParcialUnificado() {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .idMaritalStatus((short) 2)
                .mobilePhone("5599887766")
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));
        when(contactDetailRepository.findByIdClient(1L)).thenReturn(Optional.of(new ContactDetail()));
        when(homeRepository.findByIdClient(1L)).thenReturn(Optional.of(new Home()));
        when(employmentRepository.findByIdClient(1L)).thenReturn(Optional.of(new EmploymentInformation()));
        when(accountRepository.findByIdClient(1L)).thenReturn(List.of());
        when(clientMapper.toResponse(any())).thenReturn(ClientResponse.builder().name("Carlos").curp("ROMA900101HDFRRN01").build());
        when(homeMapper.toResponse(any())).thenReturn(new HomeResponse());
        when(contactDetailMapper.toResponse(any())).thenReturn(new ContactDetailResponse());
        when(employmentMapper.toResponse(any())).thenReturn(new EmploymentInformationResponse());

        ClientDetailResponse response = clientService.actualizarParcialUnificado(request);

        assertThat(response).isNotNull();
        assertThat(response.getClient().getName()).isEqualTo("Carlos");
        verify(clientRepository, times(1)).save(mockClient);
    }

    @Test
    @DisplayName("API 2 - PATCH lanza EMAIL_DUPLICATED si el email ya pertenece a otro cliente")
    void testActualizarParcialEmailDuplicado() {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .email("otro@banco.com")
                .build();

        ContactDetail otroContacto = new ContactDetail();
        otroContacto.setIdClient(99L);
        otroContacto.setEmail("otro@banco.com");

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));
        when(contactDetailRepository.findByIdClient(1L)).thenReturn(Optional.of(new ContactDetail()));
        when(contactDetailRepository.findByEmail("otro@banco.com")).thenReturn(Optional.of(otroContacto));

        assertThatThrownBy(() -> clientService.actualizarParcialUnificado(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.EMAIL_DUPLICATED);
    }

    @Test
    @DisplayName("API 2 - PATCH lanza PHONE_DUPLICATED si el teléfono ya pertenece a otro cliente")
    void testActualizarParcialTelefonoDuplicado() {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .mobilePhone("5599999999")
                .build();

        ContactDetail otroContacto = new ContactDetail();
        otroContacto.setIdClient(99L);
        otroContacto.setMobilePhone("5599999999");

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));
        when(contactDetailRepository.findByIdClient(1L)).thenReturn(Optional.of(new ContactDetail()));
        when(contactDetailRepository.findByMobilePhone("5599999999")).thenReturn(Optional.of(otroContacto));

        assertThatThrownBy(() -> clientService.actualizarParcialUnificado(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.PHONE_DUPLICATED);
    }

    @Test
    @DisplayName("API 3 - Onboarding integral crea cliente, contactos, domicilio, empleo y cuenta bancaria")
    void testRegistrarOnboardingExitoso() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .name("Juan")
                .lastName("Pérez")
                .birthDate(LocalDate.of(1990, 1, 1))
                .curp("ROMA900101HDFRRN01")
                .rfc("ROMA900101ABC")
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .email("juan@banco.com")
                .mobilePhone("5512345678")
                .street("Av. Insurgentes")
                .exteriorNumber("100")
                .postalCode("01000")
                .neighborhood("Centro")
                .idMunicipality(1)
                .occupation("Ingeniero")
                .company("Tech Corp")
                .monthlyIncome(new BigDecimal("30000.00"))
                .initialBalance(new BigDecimal("1000.00"))
                .password("Password123!#")
                .build();

        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByMobilePhone(anyString())).thenReturn(false);
        when(municipalityRepository.findById(1)).thenReturn(Optional.of(new com.intrumentoev.demo.entity.catalogs.Municipality()));

        when(clientMapper.toEntityFromOnboarding(request)).thenReturn(mockClient);
        when(clientRepository.save(any(Client.class))).thenReturn(mockClient);
        when(contactDetailRepository.save(any(ContactDetail.class))).thenReturn(new ContactDetail());
        when(homeRepository.save(any(Home.class))).thenReturn(new Home());
        when(employmentRepository.save(any(EmploymentInformation.class))).thenReturn(new EmploymentInformation());
        when(accountService.crearCuenta(eq(1L), any())).thenReturn(AccountResponse.builder().accountNumber("1234567890").build());

        when(clientMapper.toResponse(any())).thenReturn(ClientResponse.builder().curp("ROMA900101HDFRRN01").build());
        when(homeMapper.toResponse(any())).thenReturn(new HomeResponse());
        when(contactDetailMapper.toResponse(any())).thenReturn(new ContactDetailResponse());
        when(employmentMapper.toResponse(any())).thenReturn(new EmploymentInformationResponse());
        when(authService.crearCredencialesCliente(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(new com.intrumentoev.demo.entity.auth.Auth());
        when(authMapper.toResponse(any())).thenReturn(new com.intrumentoev.demo.model.auth.AuthResponse());

        ClientDetailResponse response = clientService.registrarOnboarding(request);

        assertThat(response).isNotNull();
        assertThat(response.getClient().getCurp()).isEqualTo("ROMA900101HDFRRN01");
        verify(accountService, times(1)).crearCuenta(eq(1L), any());
    }

    @Test
    @DisplayName("API 4 - Baja lógica desactiva cliente y pasa sus cuentas a INACTIVA")
    void testDesactivarClienteLogicamente() {
        ClientDeactivateRequest request = ClientDeactivateRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .reason("Cancelación voluntaria")
                .build();

        Account mockAccount = Account.builder().idAccount(10L).accountNumber("1234567890").status("ACTIVA").build();

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));
        when(accountRepository.findByIdClient(1L)).thenReturn(List.of(mockAccount));

        clientService.desactivarClienteLogicamente(request);

        assertThat(mockClient.getIsActive()).isFalse();
        assertThat(mockClient.getDeactivatedAt()).isNotNull();
        assertThat(mockAccount.getStatus()).isEqualTo("INACTIVA");
        verify(clientRepository, times(1)).save(mockClient);
        verify(accountRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza registro de menores de edad (INVALID_AGE)")
    void testRegistrarOnboardingMenorDeEdad() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .name("Pedro")
                .birthDate(LocalDate.now().minusYears(17))
                .curp("ROMA090101HDFRRN01")
                .rfc("ROMA090101ABC")
                .build();

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.INVALID_AGE);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza CURP ya registrada (CURP_DUPLICATED)")
    void testRegistrarOnboardingCurpDuplicada() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .build();

        when(clientRepository.existsByCurp("ROMA950520HDFRRN01")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CURP_DUPLICATED);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza RFC ya registrado (RFC_DUPLICATED)")
    void testRegistrarOnboardingRfcDuplicado() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .build();

        when(clientRepository.existsByCurp("ROMA950520HDFRRN01")).thenReturn(false);
        when(clientRepository.existsByRfc("ROMA950520ABC")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.RFC_DUPLICATED);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza Email ya registrado (EMAIL_DUPLICATED)")
    void testRegistrarOnboardingEmailDuplicado() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .email("duplicado@correo.com")
                .build();

        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail("duplicado@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.EMAIL_DUPLICATED);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza Teléfono ya registrado (PHONE_DUPLICATED)")
    void testRegistrarOnboardingTelefonoDuplicado() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .email("nuevo@correo.com")
                .mobilePhone("5511223344")
                .build();

        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail("nuevo@correo.com")).thenReturn(false);
        when(contactDetailRepository.existsByMobilePhone("5511223344")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.PHONE_DUPLICATED);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza Municipio no encontrado en catálogo (CATALOG_NOT_FOUND)")
    void testRegistrarOnboardingMunicipioNoEncontrado() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .email("nuevo@correo.com")
                .mobilePhone("5511223344")
                .idMunicipality(9999)
                .build();

        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByMobilePhone(anyString())).thenReturn(false);
        when(municipalityRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CATALOG_NOT_FOUND);
    }

    @Test
    @DisplayName("API 3 - Onboarding rechaza inconsistencia entre claveEntidad y Municipio (STATE_MISMATCH)")
    void testRegistrarOnboardingEstadoIncoherente() {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .birthDate(LocalDate.of(1995, 5, 20))
                .curp("ROMA950520HDFRRN01")
                .rfc("ROMA950520ABC")
                .email("nuevo@correo.com")
                .mobilePhone("5511223344")
                .idMunicipality(14)
                .claveEntidad("02") // Clave enviada: 02 (Baja California)
                .build();

        Municipality mun = new Municipality();
        mun.setIdMunicipality(14);
        mun.setIdState((short) 9); // Pertenece a CDMX

        State cdmx = new State();
        cdmx.setIdState((short) 9);
        cdmx.setCveEnt("09");
        cdmx.setName("Ciudad de México");

        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByMobilePhone(anyString())).thenReturn(false);
        when(municipalityRepository.findById(14)).thenReturn(Optional.of(mun));
        when(stateRepository.findById((short) 9)).thenReturn(Optional.of(cdmx));

        assertThatThrownBy(() -> clientService.registrarOnboarding(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.STATE_MISMATCH);
    }

    @Test
    @DisplayName("API 2 - PATCH lanza CLIENT_NOT_FOUND si el cliente no existe")
    void testActualizarParcialClienteNoExiste() {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("INEXISTENTE123456")
                .build();

        when(clientRepository.findByCurp("INEXISTENTE123456")).thenReturn(Optional.empty());
        when(clientRepository.findByRfc("INEXISTENTE123456")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.actualizarParcialUnificado(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CLIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("API 2 - PATCH lanza CATALOG_NOT_FOUND si el nuevo idMunicipality no existe")
    void testActualizarParcialMunicipioNoExiste() {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .idMunicipality(99999)
                .build();

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));
        when(contactDetailRepository.findByIdClient(1L)).thenReturn(Optional.of(new ContactDetail()));
        when(homeRepository.findByIdClient(1L)).thenReturn(Optional.of(new Home()));
        when(municipalityRepository.findById(99999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.actualizarParcialUnificado(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CATALOG_NOT_FOUND);
    }

    @Test
    @DisplayName("API 4 - DELETE lanza CLIENT_NOT_FOUND si el identificador no existe")
    void testDesactivarClienteNoExiste() {
        ClientDeactivateRequest request = ClientDeactivateRequest.builder()
                .identificador("NOEXISTE")
                .build();

        when(clientRepository.findByCurp("NOEXISTE")).thenReturn(Optional.empty());
        when(clientRepository.findByRfc("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.desactivarClienteLogicamente(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CLIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("API 4 - DELETE lanza CLIENT_INACTIVE si el cliente ya está inactivo")
    void testDesactivarClienteYaInactivo() {
        mockClient.setIsActive(false);

        ClientDeactivateRequest request = ClientDeactivateRequest.builder()
                .identificador("ROMA900101HDFRRN01")
                .build();

        when(clientRepository.findByCurp("ROMA900101HDFRRN01")).thenReturn(Optional.of(mockClient));

        assertThatThrownBy(() -> clientService.desactivarClienteLogicamente(request))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCodeEnum() == ErrorCode.CLIENT_INACTIVE);
    }
}

