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
            Client client = clientRepository.findByCurp(DEMO_CURP).orElseGet(() -> {
                Client c = new Client();
                c.setName("Alejandro");
                c.setLastName("Hernández");
                c.setSecondLastName("Gómez");
                c.setBirthDate(LocalDate.of(1990, 5, 15));
                c.setCurp(DEMO_CURP);
                c.setRfc(DEMO_RFC);
                c.setIdGender(idGender);
                c.setIdNationality(idNationality);
                c.setIdMaritalStatus(idMaritalStatus);
                c.setIsActive(true);
                return clientRepository.save(c);
            });

            Long clientId = client.getIdClient();

            // 2. Contacto
            if (contactDetailRepository.findByIdClient(clientId).isEmpty()) {
                ContactDetail cd = new ContactDetail();
                cd.setIdClient(clientId);
                cd.setEmail(DEMO_EMAIL);
                cd.setMobilePhone("5512345678");
                contactDetailRepository.save(cd);
            }

            // 3. Domicilio
            if (homeRepository.findByIdClient(clientId).isEmpty()) {
                Home home = Home.builder()
                        .idClient(clientId)
                        .street("Av. Paseo de la Reforma")
                        .exteriorNumber("222")
                        .interiorNumber("Piso 8")
                        .neighborhood("Juárez")
                        .idMunicipality(idMunicipality)
                        .postalCode("06600")
                        .country("México")
                        .build();
                homeRepository.save(home);
            }

            // 4. Laboral
            if (employmentRepository.findByIdClient(clientId).isEmpty()) {
                EmploymentInformation emp = EmploymentInformation.builder()
                        .idClient(clientId)
                        .occupation("Ingeniero de Software")
                        .company("Banco Demo S.A.")
                        .monthlyIncome(BigDecimal.valueOf(45000.00))
                        .build();
                employmentRepository.save(emp);
            }

            // 5. Cuenta Bancaria
            if (accountRepository.findByIdClient(clientId).isEmpty()) {
                Account acc = Account.builder()
                        .idClient(clientId)
                        .accountNumber("9876543210")
                        .balance(BigDecimal.valueOf(25000.00))
                        .status("ACTIVA")
                        .build();
                accountRepository.save(acc);
            }

            // 6. Credenciales Auth
            byte[] rawBiometric = Base64.getDecoder().decode(DEMO_BIOMETRIC_DATA);
            byte[] encryptedBiometric = aesEncryptionService.encryptBytes(rawBiometric);

            Auth auth = Auth.builder()
                    .idClient(clientId)
                    .email(DEMO_EMAIL)
                    .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                    .biometricType(DEMO_BIOMETRIC_TYPE)
                    .biometricTemplate(encryptedBiometric)
                    .biometricRegisteredAt(OffsetDateTime.now())
                    .isActive(true)
                    .failedAttempts(0)
                    .build();
            authRepository.save(auth);

            log.info("Usuario demo Alejandro Hernández inicializado con éxito.");
        } catch (Exception e) {
            log.error("Error al inicializar usuario demo: {}", e.getMessage(), e);
        }
    }
}
