package com.intrumentoev.demo.service.impl.client;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.exception.*;
import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.mapper.account.AccountMapper;
import com.intrumentoev.demo.mapper.auth.AuthMapper;
import com.intrumentoev.demo.mapper.client.ClientMapper;
import com.intrumentoev.demo.mapper.contactDetail.ContactDetailMapper;
import com.intrumentoev.demo.mapper.employment.EmploymentInformationMapper;
import com.intrumentoev.demo.mapper.home.HomeMapper;
import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.model.auth.AuthResponse;
import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.catalogs.GenderRepository;
import com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.catalogs.NationalityRepository;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.service.service.client.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

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

    // 1. Proceso Integral de Onboarding
    @Override
    @Transactional
    public ClientDetailResponse registrarOnboarding(ClientOnboardingRequest request) {
        log.info("Iniciando proceso de Onboarding para: {} {} con CURP: {} y RFC: {}",
                request.getName(), request.getLastName(), request.getCurp(), request.getRfc());

        // Regla de Negocio: Mayoría de edad (18 años o más)
        validarMayoriaDeEdad(request.getBirthDate());

        if(!genderRepository.existsByIdGender(request.getIdGender())){
            throw new CatalogNotFoundException("Gender",request.getIdGender());
        }

        if(!nationalityRepository.existsByIdNationality(request.getIdNationality())){
            throw new CatalogNotFoundException("Nacionality",request.getIdNationality());
        }

        if(!municipalityRepository.existsByIdMunicipality(request.getIdMunicipality())){
            throw new CatalogNotFoundException("Municipaly",request.getIdMunicipality());
        }

        if (request.getClaveEntidad() != null && !request.getClaveEntidad().isBlank()) {
            State state = stateRepository.findByCveEnt(request.getClaveEntidad().trim())
                    .orElseThrow(() -> new BusinessValidationException("No existe entidad federativa con la clave: " + request.getClaveEntidad(), "claveEntidad"));
            Municipality mun = municipalityRepository.findById(request.getIdMunicipality())
                    .orElseThrow(() -> new CatalogNotFoundException("idMunicipality", request.getIdMunicipality()));
            if (!mun.getIdState().equals(state.getIdState())) {
                throw new BusinessValidationException("El municipio con ID " + request.getIdMunicipality() + " (" + mun.getName() + ") no pertenece a la entidad federativa " + state.getName() + " (clave: " + request.getClaveEntidad() + ")", "claveEntidad");
            }
        }

        if(!maritalStatusRepository.existsByIdMaritalStatus(request.getIdMaritalStatus())){
            throw new CatalogNotFoundException("Marital",request.getIdMaritalStatus());
        }

        // Regla de Negocio: CURP y RFC únicos
        if (clientRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicatedException(request.getCurp());
        }
        if (clientRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicatedException(request.getRfc());
        }

        // Regla de Negocio: Email único y normalizado en el backend
        String emailNormalizado = request.getEmail().trim().toLowerCase();
        if (contactDetailRepository.existsByEmail(emailNormalizado)) {
            throw new EmailDuplicatedException(emailNormalizado);
        }

        // Regla de Negocio: Teléfono móvil único y de 10 dígitos
        if (contactDetailRepository.existsByMobilePhone(request.getMobilePhone())) {
            throw new PhoneDuplicatedException(request.getMobilePhone());
        }

        // Regla de Negocio: Ingreso mensual mayor a cero
        if (request.getMonthlyIncome() != null && request.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El ingreso mensual debe ser mayor a cero", "monthlyIncome");
        }

        // Regla de Negocio: Saldo inicial no negativo
        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("El saldo inicial no puede ser negativo", "initialBalance");
        }

        // A. Guardar Cliente
        Client clientEntity = clientMapper.toEntityFromOnboarding(request);
        Client clienteGuardado = clientRepository.save(clientEntity);
        Long idClient = clienteGuardado.getIdClient();
        log.info("Cliente registrado con ID: {}", idClient);

        // B. Guardar Datos de Contacto
        ContactDetail contactDetail = new ContactDetail();
        contactDetail.setIdClient(idClient);
        contactDetail.setEmail(emailNormalizado);
        contactDetail.setMobilePhone(request.getMobilePhone());
        contactDetail.setAlternativePhone(request.getAlternativePhone());
        ContactDetail contactoGuardado = contactDetailRepository.save(contactDetail);

        // C. Guardar Domicilio
        Home home = Home.builder()
                .idClient(idClient)
                .street(request.getStreet())
                .exteriorNumber(request.getExteriorNumber())
                .interiorNumber(request.getInteriorNumber())
                .neighborhood(request.getNeighborhood())
                .idMunicipality(request.getIdMunicipality())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "México")
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

        // E. Creación Automática de Cuenta Bancaria Asociada
        AccountResponse cuentaGuardada = accountService.crearCuenta(idClient, request.getInitialBalance());
        log.info("Cuenta bancaria asignada automáticamente: {}", cuentaGuardada.getAccountNumber());

        // F. Creación Automática de Credenciales de Acceso y Biometría al terminar Onboarding
        AuthResponse authResponse = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            Auth authEntity = authService.crearCredencialesCliente(
                    idClient,
                    emailNormalizado,
                    request.getPassword(),
                    request.getBiometricType(),
                    request.getBiometricData()
            );
            authResponse = authMapper.toResponse(authEntity);
            log.info("Credenciales de acceso creadas automáticamente para cliente ID: {}", idClient);
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

    // 2. GET /v1/clientes
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> obtenerTodosLosClientes() {
        log.info("Consultando todos los clientes de la BD");
        List<Client> clientes = clientRepository.findAll();
        List<ClientResponse> responses = clientMapper.toResponseList(clientes);
        responses.forEach(this::enrichClientWithCatalogs);
        return responses;
    }

    // Búsqueda unificada POST /v1/clientes/buscar
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> buscarClientes(ClientSearchRequest searchRequest) {
        log.info("Búsqueda unificada de clientes con filtros: {}", searchRequest);
        if (searchRequest == null || !searchRequest.hasAtLeastOneFilter()) {
            throw new BusinessValidationException(
                    "Debe proporcionar al menos un filtro de búsqueda (curp, rfc, email, numeroCuenta, activo o rango de fechas)",
                    "filtros"
            );
        }

        if (searchRequest.getDesde() != null && searchRequest.getHasta() == null) {
            throw new BusinessValidationException("Para filtrar por rango de fechas debe proporcionar ambos parámetros: 'desde' y 'hasta'", "rangoFechas");
        }
        if (searchRequest.getDesde() == null && searchRequest.getHasta() != null) {
            throw new BusinessValidationException("Para filtrar por rango de fechas debe proporcionar ambos parámetros: 'desde' y 'hasta'", "rangoFechas");
        }
        if (searchRequest.getDesde() != null && searchRequest.getHasta() != null) {
            if (searchRequest.getDesde().isAfter(searchRequest.getHasta())) {
                throw new BusinessValidationException("La fecha inicial 'desde' no puede ser posterior a la fecha final 'hasta'", "rangoFechas");
            }
            List<Client> clientesPorFecha = clientRepository.findByCreatedAtBetween(searchRequest.getDesde(), searchRequest.getHasta());
            List<ClientResponse> res = clientMapper.toResponseList(clientesPorFecha);
            res.forEach(this::enrichClientWithCatalogs);
            return res;
        }

        if (searchRequest.getCurp() != null && !searchRequest.getCurp().isBlank()) {
            return clientRepository.findByCurp(searchRequest.getCurp().trim().toUpperCase())
                    .map(c -> {
                        ClientResponse res = clientMapper.toResponse(c);
                        enrichClientWithCatalogs(res);
                        return List.of(res);
                    })
                    .orElse(Collections.emptyList());
        }

        if (searchRequest.getRfc() != null && !searchRequest.getRfc().isBlank()) {
            return clientRepository.findByRfc(searchRequest.getRfc().trim().toUpperCase())
                    .map(c -> {
                        ClientResponse res = clientMapper.toResponse(c);
                        enrichClientWithCatalogs(res);
                        return List.of(res);
                    })
                    .orElse(Collections.emptyList());
        }

        if (searchRequest.getEmail() != null && !searchRequest.getEmail().isBlank()) {
            String emailNorm = searchRequest.getEmail().trim().toLowerCase();
            return contactDetailRepository.findByEmail(emailNorm)
                    .flatMap(cd -> clientRepository.findById(cd.getIdClient()))
                    .map(c -> {
                        ClientResponse res = clientMapper.toResponse(c);
                        enrichClientWithCatalogs(res);
                        return List.of(res);
                    })
                    .orElse(Collections.emptyList());
        }

        if (searchRequest.getNumeroCuenta() != null && !searchRequest.getNumeroCuenta().isBlank()) {
            return accountRepository.findByAccountNumber(searchRequest.getNumeroCuenta().trim())
                    .flatMap(acc -> clientRepository.findById(acc.getIdClient()))
                    .map(c -> {
                        ClientResponse res = clientMapper.toResponse(c);
                        enrichClientWithCatalogs(res);
                        return List.of(res);
                    })
                    .orElse(Collections.emptyList());
        }

        if (searchRequest.getActivo() != null) {
            List<Client> clientes = clientRepository.findByIsActive(searchRequest.getActivo());
            List<ClientResponse> res = clientMapper.toResponseList(clientes);
            res.forEach(this::enrichClientWithCatalogs);
            return res;
        }

        return Collections.emptyList();
    }

    private Client resolverClientePorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new BusinessValidationException("El identificador (CURP o RFC) es obligatorio", "identificador");
        }
        String idLimpio = identificador.trim().toUpperCase();
        return clientRepository.findByCurp(idLimpio)
                .or(() -> clientRepository.findByRfc(idLimpio))
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con CURP o RFC: " + identificador));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorIdentificador(String identificador) {
        Client cliente = resolverClientePorIdentificador(identificador);
        ClientResponse response = clientMapper.toResponse(cliente);
        enrichClientWithCatalogs(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailResponse obtenerClientePorIdentificadorConIncludes(String identificador, String include) {
        Client cliente = resolverClientePorIdentificador(identificador);
        return obtenerClientePorIdConIncludes(cliente.getIdClient(), include);
    }

    @Override
    @Transactional
    public ClientResponse reemplazarClientePorIdentificador(String identificador, ClientUpdateRequest request) {
        Client cliente = resolverClientePorIdentificador(identificador);
        return reemplazarCliente(cliente.getIdClient(), request);
    }

    @Override
    @Transactional
    public ClientResponse actualizarParcialClientePorIdentificador(String identificador, ClientPatchRequest request) {
        if (request == null || request.isEmpty()) {
            throw new BusinessValidationException("Debe proporcionar al menos un campo válido para actualizar", "requestBody");
        }
        Client cliente = resolverClientePorIdentificador(identificador);
        return actualizarParcialCliente(cliente.getIdClient(), request);
    }

    @Override
    @Transactional
    public void eliminarClientePorIdentificador(String identificador) {
        Client cliente = resolverClientePorIdentificador(identificador);
        eliminarCliente(cliente.getIdClient());
    }

    // 4. GET /v1/clientes/{id} (básico)
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorId(Long id) {
        log.info("Buscando cliente por ID: {}", id);
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        ClientResponse response = clientMapper.toResponse(cliente);
        enrichClientWithCatalogs(response);
        return response;
    }

    // 4.b Obtener cliente por ID con includes modular (contact, home, employment, accounts, catalogs)
    @Override
    @Transactional(readOnly = true)
    public ClientDetailResponse obtenerClientePorIdConIncludes(Long id, String include) {
        log.info("Buscando información modular de cliente ID: {} con includes: {}", id, include);
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        ClientResponse clientDto = clientMapper.toResponse(cliente);
        enrichClientWithCatalogs(clientDto);

        boolean includeAll = (include == null || include.isBlank() || include.equalsIgnoreCase("all"));
        Set<String> incSet = includeAll
                ? Set.of("contact", "home", "employment", "accounts", "catalogs")
                : Arrays.stream(include.toLowerCase().split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet());

        ContactDetailResponse contactoDto = null;
        if (incSet.contains("contact") || incSet.contains("contacto")) {
            ContactDetail contacto = contactDetailRepository.findByIdClient(id).orElse(null);
            if (contacto != null) {
                contactoDto = contactDetailMapper.toResponse(contacto);
            }
        }

        HomeResponse homeDto = null;
        if (incSet.contains("home") || incSet.contains("domicilio")) {
            Home home = homeRepository.findByIdClient(id).orElse(null);
            if (home != null) {
                homeDto = homeMapper.toResponse(home);
                enrichHomeWithCatalogs(homeDto);
            }
        }

        EmploymentInformationResponse laboralDto = null;
        if (incSet.contains("employment") || incSet.contains("laboral")) {
            EmploymentInformation laboral = employmentRepository.findByIdClient(id).orElse(null);
            if (laboral != null) {
                laboralDto = employmentMapper.toResponse(laboral);
            }
        }

        List<AccountResponse> cuentasDto = null;
        AccountResponse cuentaPrincipal = null;
        if (incSet.contains("accounts") || incSet.contains("cuentas")) {
            List<Account> cuentas = accountRepository.findByIdClient(id);
            cuentasDto = accountMapper.toResponseList(cuentas);
            cuentaPrincipal = cuentasDto.isEmpty() ? null : cuentasDto.get(0);
        }

        ClientModuleCatalogsResponse catalogsDto = null;
        if (incSet.contains("catalogs") || incSet.contains("catalogos")) {
            catalogsDto = buildModuleCatalogs(homeDto);
        }

        AuthResponse authDto = authService.obtenerPorIdCliente(id)
                .map(authMapper::toResponse)
                .orElse(null);

        return ClientDetailResponse.builder()
                .client(clientDto)
                .contactDetail(contactoDto)
                .home(homeDto)
                .employmentInformation(laboralDto)
                .primaryAccount(cuentaPrincipal)
                .accounts(cuentasDto)
                .auth(authDto)
                .catalogs(catalogsDto)
                .build();
    }


    private void enrichClientWithCatalogs(ClientResponse response) {
        if (response == null) return;
        if (response.getIdGender() != null) {
            genderRepository.findById(response.getIdGender()).ifPresent(g -> response.setGenderName(g.getName()));
        }
        if (response.getIdNationality() != null) {
            nationalityRepository.findById(response.getIdNationality()).ifPresent(n -> response.setNationalityName(n.getName()));
        }
        if (response.getIdMaritalStatus() != null) {
            maritalStatusRepository.findById(response.getIdMaritalStatus()).ifPresent(m -> response.setMaritalStatusName(m.getName()));
        }
    }

    private void enrichHomeWithCatalogs(HomeResponse home) {
        if (home == null || home.getIdMunicipality() == null) return;
        municipalityRepository.findById(home.getIdMunicipality()).ifPresent(mun -> {
            home.setMunicipalityName(mun.getName());
            home.setIdState(mun.getIdState());
            if (mun.getIdState() != null) {
                stateRepository.findById(mun.getIdState()).ifPresent(st -> {
                    home.setStateName(st.getName());
                    home.setCveEnt(st.getCveEnt());
                });
            }
        });
    }

    private ClientModuleCatalogsResponse buildModuleCatalogs(HomeResponse home) {
        List<Map<String, Object>> genders = genderRepository.findByIsActiveTrue().stream()
                .map(g -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idGender", g.getIdGender());
                    map.put("name", g.getName());
                    return map;
                }).toList();

        List<Map<String, Object>> nationalities = nationalityRepository.findByIsActiveTrue().stream()
                .map(n -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idNationality", n.getIdNationality());
                    map.put("name", n.getName());
                    return map;
                }).toList();

        List<Map<String, Object>> maritalStatuses = maritalStatusRepository.findByIsActiveTrue().stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idMaritalStatus", m.getIdMaritalStatus());
                    map.put("name", m.getName());
                    return map;
                }).toList();

        Map<String, Object> munMap = null;
        Map<String, Object> stateMap = null;

        if (home != null && home.getIdMunicipality() != null) {
            var munOpt = municipalityRepository.findById(home.getIdMunicipality());
            if (munOpt.isPresent()) {
                var mun = munOpt.get();
                munMap = new LinkedHashMap<>();
                munMap.put("idMunicipality", mun.getIdMunicipality());
                munMap.put("name", mun.getName());
                munMap.put("cveMun", mun.getCveMun());
                munMap.put("idState", mun.getIdState());

                if (mun.getIdState() != null) {
                    var stOpt = stateRepository.findById(mun.getIdState());
                    if (stOpt.isPresent()) {
                        var st = stOpt.get();
                        stateMap = new LinkedHashMap<>();
                        stateMap.put("idState", st.getIdState());
                        stateMap.put("name", st.getName());
                        stateMap.put("cveEnt", st.getCveEnt());
                    }
                }
            }
        }

        return ClientModuleCatalogsResponse.builder()
                .genders(genders)
                .nationalities(nationalities)
                .maritalStatuses(maritalStatuses)
                .municipality(munMap)
                .state(stateMap)
                .build();
    }

    // 6. Consultar por CURP
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorCurp(String curp) {
        log.info("Buscando cliente por CURP: {}", curp);
        Client cliente = clientRepository.findByCurp(curp)
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con CURP: " + curp));
        return clientMapper.toResponse(cliente);
    }

    // 7. Consultar por RFC
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorRfc(String rfc) {
        log.info("Buscando cliente por RFC: {}", rfc);
        Client cliente = clientRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con RFC: " + rfc));
        return clientMapper.toResponse(cliente);
    }

    // 8. Consultar por Correo
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorCorreo(String email) {
        String normalizado = email.trim().toLowerCase();
        log.info("Buscando cliente por correo electrónico: {}", normalizado);
        ContactDetail contacto = contactDetailRepository.findByEmail(normalizado)
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con el correo electrónico: " + normalizado));
        return obtenerClientePorId(contacto.getIdClient());
    }

    // 9. Consultar por Número de Cuenta
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        log.info("Buscando cliente por número de cuenta: {}", numeroCuenta);
        Account cuenta = accountRepository.findByAccountNumber(numeroCuenta)
                .orElseThrow(() -> new AccountNotFoundException(numeroCuenta));
        return obtenerClientePorId(cuenta.getIdClient());
    }

    // 10. Consultar Clientes Activos
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> obtenerClientesActivos() {
        log.info("Consultando clientes activos");
        List<Client> activos = clientRepository.findByIsActiveTrue();
        return clientMapper.toResponseList(activos);
    }

    // 11. Consultar por Rango de Fechas
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> obtenerClientesPorRangoFechas(OffsetDateTime desde, OffsetDateTime hasta) {
        log.info("Consultando clientes registrados entre {} y {}", desde, hasta);
        List<Client> clientes = clientRepository.findByCreatedAtBetween(desde, hasta);
        return clientMapper.toResponseList(clientes);
    }

    // 12. PUT /v1/clientes/{id} (Reemplazo completo con DTO dedicado)
    @Override
    @Transactional
    public ClientResponse reemplazarCliente(Long id, ClientUpdateRequest request) {
        log.info("Reemplazando información personal de cliente con ID: {}", id);
        Client existente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        // Regla de Negocio: No se permite modificar CURP ni RFC si vienen en la petición
        if (request.getCurp() != null && !request.getCurp().equals(existente.getCurp())) {
            throw new BusinessValidationException("No está permitido modificar la CURP del cliente", "curp");
        }
        if (request.getRfc() != null && !request.getRfc().equalsIgnoreCase(existente.getRfc())) {
            throw new BusinessValidationException("No está permitido modificar el RFC del cliente", "rfc");
        }

        validarMayoriaDeEdad(request.getBirthDate());

        // Regla de Negocio: IDs de catálogos deben existir en la BD
        if (!genderRepository.existsById(request.getIdGender())) {
            throw new CatalogNotFoundException("idGender", request.getIdGender());
        }
        if (!nationalityRepository.existsById(request.getIdNationality())) {
            throw new CatalogNotFoundException("idNationality", request.getIdNationality());
        }
        if (!maritalStatusRepository.existsById(request.getIdMaritalStatus())) {
            throw new CatalogNotFoundException("idMaritalStatus", request.getIdMaritalStatus());
        }

        clientMapper.updateEntityFromUpdateRequest(request, existente);
        Client actualizado = clientRepository.save(existente);
        ClientResponse response = clientMapper.toResponse(actualizado);
        enrichClientWithCatalogs(response);
        return response;
    }

    // 12.b PUT /v1/clientes/{id} (Sobrecarga de compatibilidad)
    @Override
    @Transactional
    public ClientResponse reemplazarCliente(Long id, ClientRequest request) {
        log.info("Reemplazando completo cliente con ID: {}", id);
        Client existente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        // Regla de Negocio: No se permite modificar CURP ni RFC
        if (!existente.getCurp().equals(request.getCurp())) {
            throw new BusinessValidationException("No está permitido modificar la CURP del cliente", "curp");
        }
        if (!existente.getRfc().equalsIgnoreCase(request.getRfc())) {
            throw new BusinessValidationException("No está permitido modificar el RFC del cliente", "rfc");
        }

        validarMayoriaDeEdad(request.getBirthDate());

        // Regla de Negocio: IDs de catálogos deben existir en la BD
        if (!genderRepository.existsById(request.getIdGender())) {
            throw new CatalogNotFoundException("idGender", request.getIdGender());
        }
        if (!nationalityRepository.existsById(request.getIdNationality())) {
            throw new CatalogNotFoundException("idNationality", request.getIdNationality());
        }
        if (!maritalStatusRepository.existsById(request.getIdMaritalStatus())) {
            throw new CatalogNotFoundException("idMaritalStatus", request.getIdMaritalStatus());
        }

        clientMapper.updateEntityFromRequest(request, existente);
        Client actualizado = clientRepository.save(existente);
        ClientResponse response = clientMapper.toResponse(actualizado);
        enrichClientWithCatalogs(response);
        return response;
    }

    // 13. PATCH /v1/clientes/{id} (Actualización parcial)
    @Override
    @Transactional
    public ClientResponse actualizarParcialCliente(Long id, ClientPatchRequest request) {
        log.info("Actualizando parcialmente cliente con ID: {}", id);
        if (request == null || request.isEmpty()) {
            throw new BusinessValidationException("Debe proporcionar al menos un campo válido para actualizar", "requestBody");
        }

        Client existente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        // Regla de Negocio: No se permite modificar CURP ni RFC
        if (request.getCurp() != null && !request.getCurp().equals(existente.getCurp())) {
            throw new BusinessValidationException("No está permitido modificar la CURP del cliente", "curp");
        }
        if (request.getRfc() != null && !request.getRfc().equalsIgnoreCase(existente.getRfc())) {
            throw new BusinessValidationException("No está permitido modificar el RFC del cliente", "rfc");
        }

        if (request.getBirthDate() != null) {
            validarMayoriaDeEdad(request.getBirthDate());
        }

        // Regla de Negocio: IDs de catálogos opcionales deben existir si se envían
        if (request.getIdGender() != null && !genderRepository.existsById(request.getIdGender())) {
            throw new CatalogNotFoundException("idGender", request.getIdGender());
        }
        if (request.getIdNationality() != null && !nationalityRepository.existsById(request.getIdNationality())) {
            throw new CatalogNotFoundException("idNationality", request.getIdNationality());
        }
        if (request.getIdMaritalStatus() != null && !maritalStatusRepository.existsById(request.getIdMaritalStatus())) {
            throw new CatalogNotFoundException("idMaritalStatus", request.getIdMaritalStatus());
        }

        clientMapper.updateEntityFromPatch(request, existente);

        // Control de coherencia para desactivación (regla de negocio / constraint ck_client_deactivation)
        if (Boolean.FALSE.equals(existente.getIsActive())) {
            if (existente.getDeactivatedAt() == null) {
                existente.setDeactivatedAt(OffsetDateTime.now());
            }
            // Regla de negocio: Al desactivar el cliente, sus cuentas deben quedar inactivas
            accountService.desactivarCuentasDeCliente(id);
        } else if (Boolean.TRUE.equals(existente.getIsActive())) {
            existente.setDeactivatedAt(null);
        }

        Client actualizado = clientRepository.save(existente);
        ClientResponse response = clientMapper.toResponse(actualizado);
        enrichClientWithCatalogs(response);
        return response;
    }

    // 14. DELETE /v1/clientes/{id} (Baja lógica)
    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        log.info("Ejecutando baja lógica para cliente con ID: {}", id);
        Client existente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        // Baja lógica: Desactivar cliente
        existente.setIsActive(false);
        existente.setDeactivatedAt(OffsetDateTime.now());
        clientRepository.save(existente);

        // Regla de Negocio: Solo los clientes activos podrán tener cuentas activas
        accountService.desactivarCuentasDeCliente(id);

        // Desactivación lógica de credenciales de acceso y revocación de sesión
        authService.obtenerPorIdCliente(id).ifPresent(auth -> {
            auth.setIsActive(false);
            auth.setRefreshToken(null);
            auth.setRefreshTokenExpiresAt(null);
        });

        log.info("Baja lógica completada para cliente ID: {}, cuentas y credenciales asociadas", id);
    }

    // Validar mayoría de edad (18 años o más)
    private void validarMayoriaDeEdad(LocalDate birthDate) {
        if (birthDate == null) {
            throw new BusinessValidationException("La fecha de nacimiento es obligatoria", "birthDate");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("La fecha de nacimiento no puede ser futura", "birthDate");
        }
        if (Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new BusinessValidationException("El cliente debe ser mayor de edad (18 años o más)", "birthDate");
        }
    }
}