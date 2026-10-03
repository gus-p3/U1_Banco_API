package com.intrumentoev.demo.service;

import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.exception.*;
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
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.impl.client.ClientServiceImpl;
import com.intrumentoev.demo.service.service.account.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AccountService accountService;
    @Mock
    private com.intrumentoev.demo.service.service.auth.AuthService authService;
    @Mock
    private com.intrumentoev.demo.mapper.auth.AuthMapper authMapper;

    @Mock
    private com.intrumentoev.demo.repository.catalogs.GenderRepository genderRepository;
    @Mock
    private com.intrumentoev.demo.repository.catalogs.NationalityRepository nationalityRepository;
    @Mock
    private com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository maritalStatusRepository;
    @Mock
    private com.intrumentoev.demo.repository.catalogs.MunicipalityRepository municipalityRepository;
    @Mock
    private com.intrumentoev.demo.repository.catalogs.StateRepository stateRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private ClientOnboardingRequest validOnboardingRequest;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        validOnboardingRequest = ClientOnboardingRequest.builder()
                .name("Juan")
                .secondName("Carlos")
                .lastName("Pérez")
                .secondLastName("Gómez")
                .birthDate(LocalDate.now().minusYears(25))
                .curp("PERJ950101HDFRMN01")
                .rfc("PERJ9501011A2")
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .email("juan.perez@example.com")
                .mobilePhone("5512345678")
                .street("Av. Insurgentes Sur")
                .exteriorNumber("123")
                .neighborhood("Del Valle")
                .idMunicipality(1)
                .postalCode("03100")
                .country("México")
                .occupation("Desarrollador de Software")
                .company("Tech Solutions")
                .monthlyIncome(new BigDecimal("35000.00"))
                .initialBalance(new BigDecimal("1500.00"))
                .build();

        mockClient = new Client();
        mockClient.setIdClient(1L);
        mockClient.setName("Juan");
        mockClient.setLastName("Pérez");
        mockClient.setSecondLastName("Gómez");
        mockClient.setCurp("PERJ950101HDFRMN01");
        mockClient.setRfc("PERJ9501011A2");
        mockClient.setBirthDate(LocalDate.now().minusYears(25));
        mockClient.setIsActive(true);

        lenient().when(genderRepository.existsByIdGender(any())).thenReturn(true);
        lenient().when(genderRepository.existsById(any())).thenReturn(true);
        lenient().when(nationalityRepository.existsByIdNationality(any())).thenReturn(true);
        lenient().when(nationalityRepository.existsById(any())).thenReturn(true);
        lenient().when(maritalStatusRepository.existsByIdMaritalStatus(any())).thenReturn(true);
        lenient().when(maritalStatusRepository.existsById(any())).thenReturn(true);
        lenient().when(municipalityRepository.existsByIdMunicipality(any())).thenReturn(true);
        lenient().when(municipalityRepository.existsById(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Onboarding exitoso crea cliente, contacto, domicilio, laboral y cuenta bancaria")
    void testOnboardingExitoso() {
        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByMobilePhone(anyString())).thenReturn(false);

        when(clientMapper.toEntityFromOnboarding(any())).thenReturn(mockClient);
        when(clientRepository.save(any())).thenReturn(mockClient);

        ContactDetail mockContact = new ContactDetail();
        mockContact.setIdContactDetail(10L);
        mockContact.setIdClient(1L);
        when(contactDetailRepository.save(any())).thenReturn(mockContact);

        Home mockHome = Home.builder().idHome(20L).idClient(1L).build();
        when(homeRepository.save(any())).thenReturn(mockHome);

        EmploymentInformation mockEmployment = EmploymentInformation.builder().idEmployment(30L).idClient(1L).build();
        when(employmentRepository.save(any())).thenReturn(mockEmployment);

        AccountResponse mockAccount = AccountResponse.builder()
                .idAccount(100L)
                .accountNumber("1002345678")
                .idClient(1L)
                .balance(new BigDecimal("1500.00"))
                .status("ACTIVA")
                .build();
        when(accountService.crearCuenta(eq(1L), any())).thenReturn(mockAccount);

        when(clientMapper.toResponse(any())).thenReturn(ClientResponse.builder().idClient(1L).curp("PERJ950101HDFRMN01").build());
        when(contactDetailMapper.toResponse(any())).thenReturn(ContactDetailResponse.builder().email("juan.perez@example.com").build());
        when(homeMapper.toResponse(any())).thenReturn(HomeResponse.builder().street("Av. Insurgentes Sur").build());
        when(employmentMapper.toResponse(any())).thenReturn(EmploymentInformationResponse.builder().company("Tech Solutions").build());

        ClientDetailResponse response = clientService.registrarOnboarding(validOnboardingRequest);

        assertThat(response).isNotNull();
        assertThat(response.getClient().getIdClient()).isEqualTo(1L);
        assertThat(response.getPrimaryAccount()).isNotNull();
        assertThat(response.getPrimaryAccount().getAccountNumber()).isEqualTo("1002345678");
        verify(accountService, times(1)).crearCuenta(eq(1L), eq(new BigDecimal("1500.00")));
    }

    @Test
    @DisplayName("Onboarding rechaza cliente menor de 18 años")
    void testOnboardingMenorDeEdad() {
        validOnboardingRequest.setBirthDate(LocalDate.now().minusYears(17));

        assertThatThrownBy(() -> clientService.registrarOnboarding(validOnboardingRequest))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("El cliente debe ser mayor de edad");

        verifyNoInteractions(clientRepository);
    }

    @Test
    @DisplayName("Onboarding rechaza CURP duplicada")
    void testOnboardingCurpDuplicada() {
        when(clientRepository.existsByCurp("PERJ950101HDFRMN01")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(validOnboardingRequest))
                .isInstanceOf(CurpDuplicatedException.class);
    }

    @Test
    @DisplayName("Onboarding rechaza RFC duplicado")
    void testOnboardingRfcDuplicado() {
        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc("PERJ9501011A2")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(validOnboardingRequest))
                .isInstanceOf(RfcDuplicatedException.class);
    }

    @Test
    @DisplayName("Onboarding rechaza email duplicado y normaliza minúsculas")
    void testOnboardingEmailDuplicado() {
        validOnboardingRequest.setEmail("JUAN.PEREZ@EXAMPLE.COM");
        when(clientRepository.existsByCurp(anyString())).thenReturn(false);
        when(clientRepository.existsByRfc(anyString())).thenReturn(false);
        when(contactDetailRepository.existsByEmail("juan.perez@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registrarOnboarding(validOnboardingRequest))
                .isInstanceOf(EmailDuplicatedException.class);
    }

    @Test
    @DisplayName("Actualización rechaza modificación de CURP (campo inmutable)")
    void testActualizacionRechazaModificarCurp() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));

        ClientRequest updateRequest = ClientRequest.builder()
                .name("Juan")
                .lastName("Pérez")
                .secondLastName("Gómez")
                .curp("NUEV950101HDFRMN01") // Diferente CURP
                .rfc("PERJ9501011A2")
                .birthDate(LocalDate.now().minusYears(25))
                .build();

        assertThatThrownBy(() -> clientService.reemplazarCliente(1L, updateRequest))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("No está permitido modificar la CURP");
    }

    @Test
    @DisplayName("Actualización con ClientUpdateRequest exitosa sin necesidad de enviar CURP ni RFC")
    void testActualizacionConClientUpdateRequest() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));
        when(clientRepository.save(any())).thenReturn(mockClient);
        when(clientMapper.toResponse(any())).thenReturn(ClientResponse.builder().idClient(1L).name("Juan Modificado").build());

        ClientUpdateRequest updateRequest = ClientUpdateRequest.builder()
                .name("Juan Modificado")
                .lastName("Pérez")
                .secondLastName("Gómez")
                .birthDate(LocalDate.now().minusYears(25))
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .build();

        ClientResponse response = clientService.reemplazarCliente(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Juan Modificado");
        verify(clientMapper, times(1)).updateEntityFromUpdateRequest(eq(updateRequest), eq(mockClient));
        verify(clientRepository, times(1)).save(mockClient);
    }

    @Test
    @DisplayName("ClientUpdateRequest rechaza si envía una CURP distinta a la registrada")
    void testClientUpdateRequestRechazaModificarCurp() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));

        ClientUpdateRequest updateRequest = ClientUpdateRequest.builder()
                .name("Juan")
                .lastName("Pérez")
                .secondLastName("Gómez")
                .curp("OTRA950101HDFRMN01")
                .birthDate(LocalDate.now().minusYears(25))
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .build();

        assertThatThrownBy(() -> clientService.reemplazarCliente(1L, updateRequest))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("No está permitido modificar la CURP");
    }

    @Test
    @DisplayName("Baja lógica desactiva cliente y desactiva sus cuentas bancarias")
    void testBajaLogicaCliente() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));

        clientService.eliminarCliente(1L);

        assertThat(mockClient.getIsActive()).isFalse();
        assertThat(mockClient.getDeactivatedAt()).isNotNull();
        verify(clientRepository, times(1)).save(mockClient);
        verify(accountService, times(1)).desactivarCuentasDeCliente(1L);
    }

    @Test
    @DisplayName("Obtener cliente por ID con includes modular devuelve entidades con sus IDs y catálogos")
    void testObtenerClientePorIdConIncludes() {
        ClientResponse clientResponse = ClientResponse.builder().idClient(1L).name("Juan").lastName("Pérez").build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));
        when(clientMapper.toResponse(mockClient)).thenReturn(clientResponse);
        when(contactDetailRepository.findByIdClient(1L)).thenReturn(Optional.of(new ContactDetail()));
        when(contactDetailMapper.toResponse(any())).thenReturn(ContactDetailResponse.builder().idContactDetail(99L).idClient(1L).email("test@banco.com").build());
        when(homeRepository.findByIdClient(1L)).thenReturn(Optional.of(new Home()));
        when(homeMapper.toResponse(any())).thenReturn(HomeResponse.builder().idHome(88L).idClient(1L).idMunicipality(5).build());
        when(employmentRepository.findByIdClient(1L)).thenReturn(Optional.of(new EmploymentInformation()));
        when(employmentMapper.toResponse(any())).thenReturn(EmploymentInformationResponse.builder().idEmployment(77L).idClient(1L).occupation("Dev").build());
        when(accountRepository.findByIdClient(1L)).thenReturn(List.of());
        when(genderRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(nationalityRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(maritalStatusRepository.findByIsActiveTrue()).thenReturn(List.of());

        ClientDetailResponse response = clientService.obtenerClientePorIdConIncludes(1L, "contact,home,employment,accounts,catalogs");

        assertThat(response).isNotNull();
        assertThat(response.getClient()).isNotNull();
        assertThat(response.getContactDetail()).isNotNull();
        assertThat(response.getContactDetail().getIdContactDetail()).isEqualTo(99L);
        assertThat(response.getHome()).isNotNull();
        assertThat(response.getHome().getIdHome()).isEqualTo(88L);
        assertThat(response.getEmploymentInformation()).isNotNull();
        assertThat(response.getEmploymentInformation().getIdEmployment()).isEqualTo(77L);
        assertThat(response.getCatalogs()).isNotNull();
    }
}
