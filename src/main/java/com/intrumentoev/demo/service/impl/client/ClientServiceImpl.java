package com.intrumentoev.demo.service.impl.client;

import com.intrumentoev.demo.entity.account.Account;
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
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.service.account.AccountService;
import com.intrumentoev.demo.service.service.client.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final ClientMapper clientMapper;
    private final ContactDetailMapper contactDetailMapper;
    private final HomeMapper homeMapper;
    private final EmploymentInformationMapper employmentMapper;
    private final AccountMapper accountMapper;

    private final AccountService accountService;

    // 1. Proceso Integral de Onboarding
    @Override
    @Transactional
    public ClientDetailResponse registrarOnboarding(ClientOnboardingRequest request) {
        log.info("Iniciando proceso de Onboarding para: {} {} con CURP: {} y RFC: {}",
                request.getName(), request.getLastName(), request.getCurp(), request.getRfc());

        // Regla de Negocio: Mayoría de edad (18 años o más)
        validarMayoriaDeEdad(request.getBirthDate());

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

        return ClientDetailResponse.builder()
                .client(clientMapper.toResponse(clienteGuardado))
                .contactDetail(contactDetailMapper.toResponse(contactoGuardado))
                .home(homeMapper.toResponse(domicilioGuardado))
                .employmentInformation(employmentMapper.toResponse(laboralGuardado))
                .primaryAccount(cuentaGuardada)
                .accounts(List.of(cuentaGuardada))
                .build();
    }

    // 2. POST /v1/clientes
    @Override
    @Transactional
    public ClientResponse crearCliente(ClientRequest request) {
        log.info("Creando nuevo cliente con CURP: {} y RFC: {}", request.getCurp(), request.getRfc());

        validarMayoriaDeEdad(request.getBirthDate());

        if (clientRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicatedException(request.getCurp());
        }
        if (clientRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicatedException(request.getRfc());
        }

        Client entity = clientMapper.toEntity(request);
        Client guardado = clientRepository.save(entity);
        log.info("Cliente creado exitosamente con ID: {}", guardado.getIdClient());

        return clientMapper.toResponse(guardado);
    }

    // 3. GET /v1/clientes
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> obtenerTodosLosClientes() {
        log.info("Consultando todos los clientes de la BD");
        List<Client> clientes = clientRepository.findAll();
        return clientMapper.toResponseList(clientes);
    }

    // 4. GET /v1/clientes/{id}
    @Override
    @Transactional(readOnly = true)
    public ClientResponse obtenerClientePorId(Long id) {
        log.info("Buscando cliente por ID: {}", id);
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        return clientMapper.toResponse(cliente);
    }

    // 5. GET /v1/clientes/{id}/detalle
    @Override
    @Transactional(readOnly = true)
    public ClientDetailResponse obtenerDetalleCompletoClientePorId(Long id) {
        log.info("Buscando información completa de cliente ID: {}", id);
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        ContactDetail contacto = contactDetailRepository.findByIdClient(id).orElse(null);
        Home domicilio = homeRepository.findByIdClient(id).orElse(null);
        EmploymentInformation laboral = employmentRepository.findByIdClient(id).orElse(null);
        List<Account> cuentas = accountRepository.findByIdClient(id);

        List<AccountResponse> cuentasDto = accountMapper.toResponseList(cuentas);
        AccountResponse cuentaPrincipal = cuentasDto.isEmpty() ? null : cuentasDto.get(0);

        return ClientDetailResponse.builder()
                .client(clientMapper.toResponse(cliente))
                .contactDetail(contacto != null ? contactDetailMapper.toResponse(contacto) : null)
                .home(domicilio != null ? homeMapper.toResponse(domicilio) : null)
                .employmentInformation(laboral != null ? employmentMapper.toResponse(laboral) : null)
                .primaryAccount(cuentaPrincipal)
                .accounts(cuentasDto)
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

    // 12. PUT /v1/clientes/{id} (Reemplazo completo)
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

        clientMapper.updateEntityFromRequest(request, existente);
        Client actualizado = clientRepository.save(existente);
        return clientMapper.toResponse(actualizado);
    }

    // 13. PATCH /v1/clientes/{id} (Actualización parcial)
    @Override
    @Transactional
    public ClientResponse actualizarParcialCliente(Long id, ClientPatchRequest request) {
        log.info("Actualizando parcialmente cliente con ID: {}", id);
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
        return clientMapper.toResponse(actualizado);
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
        log.info("Baja lógica completada para cliente ID: {} y sus cuentas asociadas", id);
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