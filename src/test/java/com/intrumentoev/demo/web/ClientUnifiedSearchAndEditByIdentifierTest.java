package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrumentoev.demo.service.service.auth.ServerSessionManager;
import com.intrumentoev.demo.repository.catalogs.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Pruebas de Búsqueda Unificada POST y Edición Exclusiva por CURP/RFC")
public class ClientUnifiedSearchAndEditByIdentifierTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServerSessionManager serverSessionManager;

    @Autowired
    private GenderRepository genderRepository;
    @Autowired
    private NationalityRepository nationalityRepository;
    @Autowired
    private MaritalStatusRepository maritalStatusRepository;
    @Autowired
    private MunicipalityRepository municipalityRepository;

    private MockHttpSession authenticatedSession;
    private String clientCurp;
    private String clientRfc;
    private String clientEmail;
    private String clientPhone;
    private String clientAccountNumber;

    private Short genderId;
    private Short nationalityId;
    private Short maritalId;
    private Integer municipalityId;

    private static final AtomicInteger COUNTER = new AtomicInteger(100);

    @BeforeEach
    void setUp() throws Exception {
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
        authenticatedSession = new MockHttpSession();

        var g = genderRepository.findAll().stream().findFirst().orElseGet(() ->
                genderRepository.save(com.intrumentoev.demo.entity.catalogs.Gender.builder().name("MASCULINO").isActive(true).build())
        );
        var n = nationalityRepository.findAll().stream().findFirst().orElseGet(() ->
                nationalityRepository.save(com.intrumentoev.demo.entity.catalogs.Nationality.builder().name("MEXICANA").isActive(true).build())
        );
        var m = maritalStatusRepository.findAll().stream().findFirst().orElseGet(() ->
                maritalStatusRepository.save(com.intrumentoev.demo.entity.catalogs.MaritalStatus.builder().name("SOLTERO").isActive(true).build())
        );
        var mun = municipalityRepository.findAll().stream().findFirst().orElseGet(() ->
                municipalityRepository.save(com.intrumentoev.demo.entity.catalogs.Municipality.builder().name("Cuauhtémoc").cveMun("015").idState((short) 9).isActive(true).build())
        );

        genderId = g.getIdGender();
        nationalityId = n.getIdNationality();
        maritalId = m.getIdMaritalStatus();
        municipalityId = mun.getIdMunicipality();

        int seq = COUNTER.incrementAndGet();
        clientCurp = String.format("UNIF%02d0101HDFRRN01", seq % 90 + 10);
        clientRfc = String.format("UNIF%02d0101AB1", seq % 90 + 10);
        clientEmail = String.format("cliente.unif%d@banco.com", seq);
        clientPhone = String.format("55%08d", seq);

        String onboardingJson = """
            {
              "name": "Armando",
              "lastName": "Paredes",
              "birthDate": "1990-05-15",
              "curp": "%s",
              "rfc": "%s",
              "idGender": %d,
              "idNationality": %d,
              "idMaritalStatus": %d,
              "email": "%s",
              "mobilePhone": "%s",
              "street": "Avenida Insurgentes",
              "exteriorNumber": "123",
              "neighborhood": "Roma Sur",
              "idMunicipality": %d,
              "postalCode": "06760",
              "country": "México",
              "occupation": "Analista de Datos",
              "company": "Data Insights",
              "monthlyIncome": 45000.00,
              "initialBalance": 2500.00,
              "password": "Password123!"
            }
            """.formatted(clientCurp, clientRfc, genderId, nationalityId, maritalId, clientEmail, clientPhone, municipalityId);

        MvcResult result = mockMvc.perform(post("/v1/clientes/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        clientAccountNumber = root.path("primaryAccount").path("accountNumber").asText();
    }

    @Test
    @DisplayName("1. GET y búsqueda ocultan idClient en todos los JSON de respuesta")
    void testGetClientsHidesIdClient() throws Exception {
        // GET /v1/clientes
        mockMvc.perform(get("/v1/clientes")
                        .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idClient").doesNotExist())
                .andExpect(jsonPath("$[0].curp").exists());

        // GET /v1/clientes/{identificador}
        mockMvc.perform(get("/v1/clientes/" + clientCurp)
                        .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client.idClient").doesNotExist())
                .andExpect(jsonPath("$.client.curp", is(clientCurp)))
                .andExpect(jsonPath("$.contactDetail.idClient").doesNotExist())
                .andExpect(jsonPath("$.home.idClient").doesNotExist())
                .andExpect(jsonPath("$.employmentInformation.idClient").doesNotExist())
                .andExpect(jsonPath("$.primaryAccount.idClient").doesNotExist());
    }

    @Test
    @DisplayName("2. Edición de cliente por CURP exitosa")
    void testEditClientByCurp() throws Exception {
        String updateJson = """
            {
              "name": "Armando Modificado",
              "lastName": "Paredes",
              "secondLastName": "Gómez",
              "birthDate": "1990-05-15",
              "idGender": %d,
              "idNationality": %d,
              "idMaritalStatus": %d
            }
            """.formatted(genderId, nationalityId, maritalId);

        mockMvc.perform(put("/v1/clientes/" + clientCurp)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Armando Modificado")))
                .andExpect(jsonPath("$.curp", is(clientCurp)))
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("3. Edición de cliente por RFC exitosa")
    void testEditClientByRfc() throws Exception {
        String updateJson = """
            {
              "name": "Armando Por RFC",
              "lastName": "Paredes",
              "secondLastName": "Gómez",
              "birthDate": "1990-05-15",
              "idGender": %d,
              "idNationality": %d,
              "idMaritalStatus": %d
            }
            """.formatted(genderId, nationalityId, maritalId);

        mockMvc.perform(put("/v1/clientes/" + clientRfc)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Armando Por RFC")))
                .andExpect(jsonPath("$.rfc", is(clientRfc)))
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("4. Prohibición estricta de edición por ID numérico (debe retornar 400)")
    void testEditClientByNumericIdProhibited() throws Exception {
        String updateJson = """
            {
              "name": "Intento Hacker",
              "lastName": "Paredes",
              "secondLastName": "Gómez",
              "birthDate": "1990-05-15",
              "idGender": %d,
              "idNationality": %d,
              "idMaritalStatus": %d
            }
            """.formatted(genderId, nationalityId, maritalId);

        mockMvc.perform(put("/v1/clientes/1")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].message", containsString("No se permite el uso de IDs numéricos")));
    }

    @Test
    @DisplayName("5. Edición de Domicilio por CURP y RFC")
    void testEditHomeByCurpAndRfc() throws Exception {
        String homeUpdate = """
            {
              "street": "Paseo de la Reforma",
              "exteriorNumber": "222",
              "neighborhood": "Juárez",
              "idMunicipality": %d,
              "postalCode": "06600",
              "country": "México"
            }
            """.formatted(municipalityId);

        // PUT por CURP
        mockMvc.perform(put("/v1/domicilios/cliente/" + clientCurp)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(homeUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street", is("Paseo de la Reforma")))
                .andExpect(jsonPath("$.idClient").doesNotExist());

        // PATCH por RFC
        mockMvc.perform(patch("/v1/domicilios/cliente/" + clientRfc)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exteriorNumber\": \"224\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exteriorNumber", is("224")))
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("6. Edición de Contacto por CURP y RFC")
    void testEditContactByCurpAndRfc() throws Exception {
        String contactUpdate = """
            {
              "email": "nuevo.contacto@banco.com",
              "mobilePhone": "5599887766"
            }
            """;

        mockMvc.perform(put("/v1/contact-details/cliente/" + clientCurp)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("nuevo.contacto@banco.com")))
                .andExpect(jsonPath("$.idClient").doesNotExist());

        mockMvc.perform(patch("/v1/contact-details/cliente/" + clientRfc)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobilePhone\": \"5511223344\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobilePhone", is("5511223344")))
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("7. Edición de Laboral por CURP y RFC")
    void testEditEmploymentByCurpAndRfc() throws Exception {
        String laboralUpdate = """
            {
              "occupation": "Senior Architect",
              "company": "Big FinTech",
              "monthlyIncome": 85000.00
            }
            """;

        mockMvc.perform(put("/v1/laboral/cliente/" + clientCurp)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(laboralUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupation", is("Senior Architect")))
                .andExpect(jsonPath("$.idClient").doesNotExist());

        mockMvc.perform(patch("/v1/laboral/cliente/" + clientRfc)
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyIncome\": 90000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyIncome", is(90000.0)))
                .andExpect(jsonPath("$.idClient").doesNotExist());
    }

    @Test
    @DisplayName("8. Búsqueda POST /v1/clientes/buscar con filtros variados")
    void testUnifiedSearchFilters() throws Exception {
        // Búsqueda por CURP
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curp\": \"%s\"}".formatted(clientCurp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curp", is(clientCurp)))
                .andExpect(jsonPath("$[0].idClient").doesNotExist());

        // Búsqueda por RFC
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rfc\": \"%s\"}".formatted(clientRfc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rfc", is(clientRfc)));

        // Búsqueda por Email
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\"}".formatted(clientEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curp", is(clientCurp)));

        // Búsqueda por Número de Cuenta
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\": \"%s\"}".formatted(clientAccountNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].curp", is(clientCurp)));

        // Búsqueda por Activo = true
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("9. Búsqueda POST /v1/clientes/buscar sin filtros rechaza con error de negocio")
    void testUnifiedSearchWithoutFiltersReturnsBadRequest() throws Exception {
        // Cuerpo {} vacío
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("al menos un filtro")));

        // Cuerpo con todos los campos null o blank
        mockMvc.perform(post("/v1/clientes/buscar")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curp\": \"\", \"email\": \"   \"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("al menos un filtro")));
    }
}
