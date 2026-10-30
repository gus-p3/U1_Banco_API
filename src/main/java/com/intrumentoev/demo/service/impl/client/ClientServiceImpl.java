package com.intrumentoev.demo.service.impl.client;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.exception.*;
import com.intrumentoev.demo.mapper.account.AccountMapper;
import com.intrumentoev.demo.mapper.auth.AuthMapper;
import com.intrumentoev.demo.mapper.client.ClientMapper;
import com.intrumentoev.demo.mapper.contactDetail.ContactDetailMapper;
import com.intrumentoev.demo.mapper.employment.EmploymentInformationMapper;
import com.intrumentoev.demo.mapper.home.HomeMapper;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.model.auth.AuthResponse;
import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.catalogs.GenderRepository;
import com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.catalogs.NationalityRepository;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.client.ClientSpecification;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.service.service.client.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final HomeRepository homeRepository;
    private final EmploymentInformationRepository employmentRepository;
    private final AccountRepository accountRepository;

    private final GenderRepository genderRepository;
    private final NationalityRepository nationalityRepository;
    private final MaritalStatusRepository maritalStatusRepository;
    private final MunicipalityRepository municipalityRepository;
    private final StateRepository stateRepository;

    private final ClientMapper clientMapper;
    private final ContactDetailMapper contactDetailMapper;
    private final HomeMapper homeMapper;
    private final EmploymentInformationMapper employmentMapper;
    private final AccountMapper accountMapper;
    private final AuthMapper authMapper;

    private final AccountService accountService;
    private final AuthService authService;

    // =========================================================================
    // 1. GET: Consulta y Búsqueda Dinámica con Ordenamiento
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> consultarClientes(ClientUnifiedQueryRequest request, List<String> includes) {
        log.info("Consultando clientes con filtros dinámicos: {}, includes: {}", request, includes);

        Specification<Client> spec = ClientSpecification.conFiltros(request);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (request != null && request.getSortBy() != null && !request.getSortBy().isBlank()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("DESC")) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, request.getSortBy());
        }

        List<Client> clientes = clientRepository.findAll(spec, sort);
        List<ClientResponse> responses = clientMapper.toResponseList(clientes);
        responses.forEach(clientDto -> {
            enrichClientWithCatalogs(clientDto);
            if (includes != null && !includes.isEmpty()) {
                boolean incAccounts = includes.stream().anyMatch(i -> i.equalsIgnoreCase("accounts") || i.equalsIgnoreCase("cuentas"));
                boolean incHome = includes.stream().anyMatch(i -> i.equalsIgnoreCase("home") || i.equalsIgnoreCase("domicilio"));
                boolean incContact = includes.stream().anyMatch(i -> i.equalsIgnoreCase("contact") || i.equalsIgnoreCase("contacto"));
                boolean incEmployment = includes.stream().anyMatch(i -> i.equalsIgnoreCase("employment") || i.equalsIgnoreCase("laboral"));

                if (incAccounts) {
                    var cuentasDto = accountMapper.toResponseList(accountRepository.findByIdClient(clientDto.getIdClient()));
                    clientDto.setAccounts(cuentasDto);
                    clientDto.setPrimaryAccount(cuentasDto.isEmpty() ? null : cuentasDto.get(0));
                }
                if (incHome) {
                    homeRepository.findByIdClient(clientDto.getIdClient()).ifPresent(home -> {
                        var homeDto = homeMapper.toResponse(home);
                        enrichHomeWithCatalogs(homeDto);
                        clientDto.setHome(homeDto);
                    });
                }
                if (incContact) {
                    contactDetailRepository.findByIdClient(clientDto.getIdClient()).ifPresent(contact -> {
                        clientDto.setContactDetail(contactDetailMapper.toResponse(contact));
                    });
                }
                if (incEmployment) {
                    employmentRepository.findByIdClient(clientDto.getIdClient()).ifPresent(emp -> {
                        clientDto.setEmploymentInformation(employmentMapper.toResponse(emp));
                    });
                }
            }
        });
        return responses;
    }

    // =========================================================================
    // 2. PATCH: Actualización Unificada y Modular (Súper PATCH)
    // =========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientDetailResponse actualizarParcialUnificado(ClientUnifiedPatchRequest request) {
        String id = request.getIdentificador().trim().toUpperCase();
        log.info("Iniciando actualización unificada para cliente con identificador: {}", id);

        Client client = buscarEntidadPorIdentificador(id);
        Long idClient = client.getIdClient();

        // 1. Actualizar datos personales (MapStruct ignora nulos automáticamente)
        clientMapper.updateEntityFromUnifiedPatch(request, client);
        clientRepository.save(client);

        // 2. Actualizar contacto (validación de unicidad y mapeo automático)
        ContactDetail contact = contactDetailRepository.findByIdClient(idClient).orElseGet(() -> {
            ContactDetail c = new ContactDetail();
            c.setIdClient(idClient);
            return c;
        });
        validarDuplicadosContactoPatch(request, idClient);
        contactDetailMapper.updateEntityFromUnifiedPatch(request, contact);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            contact.setEmail(request.getEmail().trim().toLowerCase());
        }
        contactDetailRepository.save(contact);

        // 3. Actualizar domicilio (validación de catálogo y mapeo automático)
        Home home = homeRepository.findByIdClient(idClient)
                .orElseGet(() -> Home.builder().idClient(idClient).build());
        if (request.getIdMunicipality() != null) {
            municipalityRepository.findById(request.getIdMunicipality())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND, "municipio", request.getIdMunicipality().longValue()));
        }
        homeMapper.updateEntityFromUnifiedPatch(request, home);
        homeRepository.save(home);

        // 4. Actualizar información laboral (mapeo automático sin ifs)
        EmploymentInformation employment = employmentRepository.findByIdClient(idClient)
                .orElseGet(() -> EmploymentInformation.builder().idClient(idClient).build());
        employmentMapper.updateEntityFromUnifiedPatch(request, employment);
        employmentRepository.save(employment);

        return construirDetalleCliente(client, contact, home, employment);
    }

    private void validarDuplicadosContactoPatch(ClientUnifiedPatchRequest request, Long idClient) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String emailNorm = request.getEmail().trim().toLowerCase();
            contactDetailRepository.findByEmail(emailNorm).ifPresent(otro -> {
                if (!otro.getIdClient().equals(idClient)) {
                    throw new BusinessException(ErrorCode.EMAIL_DUPLICATED, emailNorm);
                }
            });
        }
        if (request.getMobilePhone() != null && !request.getMobilePhone().isBlank()) {
            String phoneNorm = request.getMobilePhone().trim();
            contactDetailRepository.findByMobilePhone(phoneNorm).ifPresent(otro -> {
                if (!otro.getIdClient().equals(idClient)) {
                    throw new BusinessException(ErrorCode.PHONE_DUPLICATED, phoneNorm);
                }
            });
        }
    }

    // =========================================================================
    // 3. POST: Creación Integral (Onboarding Bancario)
    // =========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientDetailResponse registrarOnboarding(ClientOnboardingRequest request) {
        log.info("Iniciando proceso de Onboarding bancario para CURP: {}", request.getCurp());

        // 1. Validación de mayoría de edad (mínimo 18 años)
        int edad = Period.between(request.getBirthDate(), LocalDate.now()).getYears();
        if (edad < 18) {
            throw new BusinessException(ErrorCode.INVALID_AGE, edad);
        }

        // 2. Validación de no duplicidad de CURP y RFC
        String curpNorm = request.getCurp().trim().toUpperCase();
        String rfcNorm = request.getRfc().trim().toUpperCase();

        if (clientRepository.existsByCurp(curpNorm)) {
            throw new BusinessException(ErrorCode.CURP_DUPLICATED, curpNorm);
        }
        if (clientRepository.existsByRfc(rfcNorm)) {
            throw new BusinessException(ErrorCode.RFC_DUPLICATED, rfcNorm);
        }

        // 3. Validación de no duplicidad de Email y Teléfono
        String emailNorm = request.getEmail().trim().toLowerCase();
        if (contactDetailRepository.existsByEmail(emailNorm)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED, emailNorm);
        }
        if (contactDetailRepository.existsByMobilePhone(request.getMobilePhone())) {
            throw new BusinessException(ErrorCode.PHONE_DUPLICATED, request.getMobilePhone());
        }

        // 4. Validación de existencia de Municipio y coherencia con claveEntidad
        Municipality mun = municipalityRepository.findById(request.getIdMunicipality())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND, "municipio", request.getIdMunicipality().longValue()));

        if (request.getClaveEntidad() != null && !request.getClaveEntidad().isBlank()) {
            State state = stateRepository.findById(mun.getIdState()).orElse(null);
            if (state != null && !state.getCveEnt().equals(request.getClaveEntidad().trim())) {
                throw new BusinessException(
                        ErrorCode.STATE_MISMATCH,
                        request.getClaveEntidad(),
                        state.getCveEnt(),
                        state.getName()
                );
            }
        }

        // A. Guardar Cliente
        Client client = clientMapper.toEntityFromOnboarding(request);
        client.setCurp(curpNorm);
        client.setRfc(rfcNorm);
        Client clienteGuardado = clientRepository.save(client);
        Long idClient = clienteGuardado.getIdClient();

        // B. Guardar Contacto
        ContactDetail contact = new ContactDetail();
        contact.setIdClient(idClient);
        contact.setEmail(emailNorm);
        contact.setMobilePhone(request.getMobilePhone());
        contact.setAlternativePhone(request.getAlternativePhone());
        ContactDetail contactoGuardado = contactDetailRepository.save(contact);

        // C. Guardar Domicilio
        Home home = Home.builder()
                .idClient(idClient)
                .street(request.getStreet())
                .exteriorNumber(request.getExteriorNumber())
                .interiorNumber(request.getInteriorNumber())
                .neighborhood(request.getNeighborhood())
                .idMunicipality(request.getIdMunicipality())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry()
                        : "México")
                .build();
        Home domicilioGuardado = homeRepository.save(home);

        // D. Guardar Información Laboral
        EmploymentInformation employment = EmploymentInformation.builder()
                .idClient(idClient)
                .occupation(request.getOccupation())
                .company(request.getCompany())
                .monthlyIncome(request.getMonthlyIncome())
                .build();
        EmploymentInformation laboralGuardado = employmentRepository.save(employment);

        // E. Creación de Cuenta Bancaria
        AccountResponse cuentaGuardada = accountService.crearCuenta(idClient, request.getInitialBalance());

        // F. Creación de Credenciales de Acceso
        AuthResponse authResponse = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            Auth auth = authService.crearCredencialesCliente(
                    idClient,
                    emailNorm,
                    request.getPassword(),
                    request.getBiometricType(),
                    request.getBiometricData());
            authResponse = authMapper.toResponse(auth);
        }

        ClientResponse clienteDto = clientMapper.toResponse(clienteGuardado);
        enrichClientWithCatalogs(clienteDto);

        HomeResponse homeDto = homeMapper.toResponse(domicilioGuardado);
        enrichHomeWithCatalogs(homeDto);

        return ClientDetailResponse.builder()
                .client(clienteDto)
                .contactDetail(contactDetailMapper.toResponse(contactoGuardado))
                .home(homeDto)
                .employmentInformation(employmentMapper.toResponse(laboralGuardado))
                .primaryAccount(cuentaGuardada)
                .accounts(List.of(cuentaGuardada))
                .auth(authResponse)
                .build();
    }

    // =========================================================================
    // 4. DELETE: Baja Lógica Unificada
    // =========================================================================
    @Override
    @Transactional
    public void desactivarClienteLogicamente(ClientDeactivateRequest request) {
        String id = request.getIdentificador().trim().toUpperCase();
        log.info("Ejecutando baja lógica para cliente con identificador: {}", id);

        Client client = buscarEntidadPorIdentificador(id);

        if (!Boolean.TRUE.equals(client.getIsActive())) {
            throw new BusinessException(ErrorCode.CLIENT_INACTIVE, id);
        }

        client.setIsActive(false);
        client.setDeactivatedAt(OffsetDateTime.now());
        clientRepository.save(client);

        List<Account> cuentas = accountRepository.findByIdClient(client.getIdClient());
        cuentas.forEach(c -> c.setStatus("INACTIVA"));
        accountRepository.saveAll(cuentas);
        log.info("Baja lógica completada para cliente ID: {}. Cuentas desactivadas: {}", client.getIdClient(),
                cuentas.size());
    }

    // =========================================================================
    // Métodos Auxiliares Internos
    // =========================================================================
    private Client buscarEntidadPorIdentificador(String identificador) {
        return clientRepository.findByCurp(identificador)
                .or(() -> clientRepository.findByRfc(identificador))
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, identificador));
    }

    private ClientDetailResponse construirDetalleCliente(Client client, ContactDetail contact, Home home,
            EmploymentInformation employment) {
        ClientResponse clienteDto = clientMapper.toResponse(client);
        enrichClientWithCatalogs(clienteDto);

        HomeResponse homeDto = homeMapper.toResponse(home);
        enrichHomeWithCatalogs(homeDto);

        List<AccountResponse> cuentasDto = accountMapper
                .toResponseList(accountRepository.findByIdClient(client.getIdClient()));
        AccountResponse primaryAccount = cuentasDto.isEmpty() ? null : cuentasDto.get(0);

        return ClientDetailResponse.builder()
                .client(clienteDto)
                .contactDetail(contactDetailMapper.toResponse(contact))
                .home(homeDto)
                .employmentInformation(employmentMapper.toResponse(employment))
                .primaryAccount(primaryAccount)
                .accounts(cuentasDto)
                .build();
    }

    private void enrichClientWithCatalogs(ClientResponse response) {
        if (response == null)
            return;
        if (response.getIdGender() != null) {
            genderRepository.findById(response.getIdGender())
                    .ifPresent(g -> response.setGenderName(g.getName()));
        }
        if (response.getIdNationality() != null) {
            nationalityRepository.findById(response.getIdNationality())
                    .ifPresent(n -> response.setNationalityName(n.getName()));
        }
        if (response.getIdMaritalStatus() != null) {
            maritalStatusRepository.findById(response.getIdMaritalStatus())
                    .ifPresent(m -> response.setMaritalStatusName(m.getName()));
        }
    }

    private void enrichHomeWithCatalogs(HomeResponse homeDto) {
        if (homeDto == null || homeDto.getIdMunicipality() == null)
            return;
        Municipality mun = municipalityRepository.findById(homeDto.getIdMunicipality()).orElse(null);
        if (mun != null) {
            homeDto.setMunicipalityName(mun.getName());
            State state = stateRepository.findById(mun.getIdState()).orElse(null);
            if (state != null) {
                homeDto.setIdState(state.getIdState());
                homeDto.setStateName(state.getName());
                homeDto.setCveEnt(state.getCveEnt());
            }
        }
    }
}