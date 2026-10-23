package com.intrumentoev.demo.config;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.entity.catalogs.*;
import com.intrumentoev.demo.entity.client.Client;
import com.intrumentoev.demo.entity.contactDetail.ContactDetail;
import com.intrumentoev.demo.entity.employment.EmploymentInformation;
import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.repository.account.AccountRepository;
import com.intrumentoev.demo.repository.auth.AuthRepository;
import com.intrumentoev.demo.repository.catalogs.*;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.contactDetail.ContactDetailRepository;
import com.intrumentoev.demo.repository.employment.EmploymentInformationRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.service.auth.AesEncryptionService;
import com.intrumentoev.demo.service.service.auth.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoUserInitializer implements CommandLineRunner {

    public static final String DEMO_EMAIL = "alejandro.hernandez@banco-demo.com";
    public static final String DEMO_PASSWORD = "PasswordSegura#2026";
    public static final String DEMO_BIOMETRIC_TYPE = "HUELLA";
    public static final String DEMO_BIOMETRIC_DATA = "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ==";
    public static final String DEMO_CURP = "HERA900515HDFRND01";
    public static final String DEMO_RFC = "HERA900515AB1";

    private final AuthRepository authRepository;
    private final ClientRepository clientRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final HomeRepository homeRepository;
    private final EmploymentInformationRepository employmentRepository;
    private final AccountRepository accountRepository;
    private final GenderRepository genderRepository;
    private final NationalityRepository nationalityRepository;
    private final MaritalStatusRepository maritalStatusRepository;
    private final StateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            if (authRepository.existsByEmail(DEMO_EMAIL)) {
                log.info("Usuario demo [{}] ya existe en la base de datos.", DEMO_EMAIL);
                return;
            }

            log.info("Inicializando usuario de demostración Alejandro Hernández...");

            // Verificar o crear catálogos mínimos si la BD está vacía (ej. H2 en tests)
            Short idGender = genderRepository.findAll().stream().findFirst().map(g -> g.getIdGender()).orElseGet(() -> {
                Gender g = Gender.builder().name("Hombre").description("CURP: H").isActive(true).build();
                return genderRepository.save(g).getIdGender();
            });

            Short idNationality = nationalityRepository.findAll().stream().findFirst().map(n -> n.getIdNationality()).orElseGet(() -> {
                Nationality n = Nationality.builder().name("Mexicana").description("Nacionalidad Mexicana").isActive(true).build();
                return nationalityRepository.save(n).getIdNationality();
            });

            Short idMaritalStatus = maritalStatusRepository.findAll().stream().findFirst().map(m -> m.getIdMaritalStatus()).orElseGet(() -> {
                MaritalStatus ms = MaritalStatus.builder().name("Soltero(a)").isActive(true).build();
                return maritalStatusRepository.save(ms).getIdMaritalStatus();
            });

            Short idState = stateRepository.findByCveEnt("09").map(s -> s.getIdState()).orElseGet(() -> {
                return stateRepository.findAll().stream().findFirst().map(s -> s.getIdState()).orElseGet(() -> {
                    State st = State.builder().cveEnt("09").name("Ciudad de México").isActive(true).build();
                    return stateRepository.save(st).getIdState();
                });
            });

            Integer idMunicipality = municipalityRepository.findAll().stream().findFirst().map(m -> m.getIdMunicipality()).orElseGet(() -> {
                Municipality m = Municipality.builder().idState(idState).cveMun("001").name("Cuauhtémoc").isActive(true).build();
                return municipalityRepository.save(m).getIdMunicipality();
            });

            // 1. Cliente
            Client client = clientRepository.findByCurp(DEMO_CURP)
                    .or(() -> clientRepository.findByRfc(DEMO_RFC))
                    .orElseGet(Client::new);
            client.setName("Alejandro");
            client.setLastName("Hernández");
            client.setSecondLastName("Gómez");
            client.setBirthDate(LocalDate.of(1990, 5, 15));
            client.setCurp(DEMO_CURP);
            client.setRfc(DEMO_RFC);
            client.setIdGender(idGender);
            client.setIdNationality(idNationality);
            client.setIdMaritalStatus(idMaritalStatus);
            client.setIsActive(true);
            client = clientRepository.save(client);

            Long clientId = client.getIdClient();

            // 2. Contacto (Reutilizar si ya existe por ID de cliente, email o teléfono para evitar errores de unicidad)
            ContactDetail cd = contactDetailRepository.findByIdClient(clientId)
                    .or(() -> contactDetailRepository.findByEmail(DEMO_EMAIL))
                    .or(() -> contactDetailRepository.findByMobilePhone("5512345678"))
                    .orElseGet(ContactDetail::new);
            cd.setIdClient(clientId);
            cd.setEmail(DEMO_EMAIL);
            cd.setMobilePhone("5512345678");
            contactDetailRepository.save(cd);

            // 3. Domicilio
            Home home = homeRepository.findByIdClient(clientId)
                    .orElseGet(() -> Home.builder().idClient(clientId).build());
            home.setIdClient(clientId);
            home.setStreet("Av. Paseo de la Reforma");
            home.setExteriorNumber("222");
            home.setInteriorNumber("Piso 8");
            home.setNeighborhood("Juárez");
            home.setIdMunicipality(idMunicipality);
            home.setPostalCode("06600");
            home.setCountry("México");
            homeRepository.save(home);

            // 4. Laboral
            EmploymentInformation emp = employmentRepository.findByIdClient(clientId)
                    .orElseGet(() -> EmploymentInformation.builder().idClient(clientId).build());
            emp.setIdClient(clientId);
            emp.setOccupation("Ingeniero de Software");
            emp.setCompany("Banco Demo S.A.");
            emp.setMonthlyIncome(BigDecimal.valueOf(45000.00));
            employmentRepository.save(emp);

            // 5. Cuenta Bancaria
            Account acc = accountRepository.findByIdClient(clientId).stream().findFirst()
                    .or(() -> accountRepository.findByAccountNumber("9876543210"))
                    .orElseGet(() -> Account.builder().idClient(clientId).build());
            acc.setIdClient(clientId);
            acc.setAccountNumber("9876543210");
            acc.setBalance(BigDecimal.valueOf(25000.00));
            acc.setStatus("ACTIVA");
            accountRepository.save(acc);

            // 6. Credenciales Auth
            Auth auth = authRepository.findByEmail(DEMO_EMAIL)
                    .or(() -> authRepository.findByIdClient(clientId))
                    .orElseGet(() -> Auth.builder().idClient(clientId).email(DEMO_EMAIL).build());
            byte[] rawBiometric = Base64.getDecoder().decode(DEMO_BIOMETRIC_DATA);
            byte[] encryptedBiometric = aesEncryptionService.encryptBytes(rawBiometric);
            auth.setIdClient(clientId);
            auth.setEmail(DEMO_EMAIL);
            auth.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            auth.setBiometricType(DEMO_BIOMETRIC_TYPE);
            auth.setBiometricTemplate(encryptedBiometric);
            auth.setBiometricRegisteredAt(OffsetDateTime.now());
            auth.setIsActive(true);
            auth.setFailedAttempts(0);
            authRepository.save(auth);

            log.info("Usuario demo Alejandro Hernández inicializado con éxito.");
        } catch (Exception e) {
            log.error("Error al inicializar usuario demo: {}", e.getMessage(), e);
        }
    }
}
