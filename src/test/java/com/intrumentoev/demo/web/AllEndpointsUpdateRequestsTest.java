package com.intrumentoev.demo.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Pruebas Integrales de DTOs Dedicados de Edición/Actualización")
public class AllEndpointsUpdateRequestsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.intrumentoev.demo.service.service.auth.ServerSessionManager serverSessionManager;

    @Autowired
    private com.intrumentoev.demo.repository.catalogs.GenderRepository genderRepository;
    @Autowired
    private com.intrumentoev.demo.repository.catalogs.NationalityRepository nationalityRepository;
    @Autowired
    private com.intrumentoev.demo.repository.catalogs.MaritalStatusRepository maritalStatusRepository;
    @Autowired
    private com.intrumentoev.demo.repository.catalogs.MunicipalityRepository municipalityRepository;

    private MockHttpSession authenticatedSession;
    private String createdClientCurp;
    private String createdClientRfc;

    private Short gender1Id;
    private Short gender2Id;
    private Short nationality1Id;
    private Short marital1Id;
    private Short marital2Id;
    private Integer municipalityId;

    private static final java.util.concurrent.atomic.AtomicInteger COUNTER = new java.util.concurrent.atomic.AtomicInteger(10);

    @BeforeEach
    void setUp() throws Exception {
        serverSessionManager.setLoggedIn(true, "admin@banco.com", 1L);
        authenticatedSession = new MockHttpSession();

        var g1 = genderRepository.findAll().stream().findFirst().orElseGet(() ->
                genderRepository.save(com.intrumentoev.demo.entity.catalogs.Gender.builder().name("MASCULINO").isActive(true).build())
        );
        var g2 = genderRepository.findAll().stream().filter(g -> !g.getIdGender().equals(g1.getIdGender())).findFirst().orElseGet(() ->
                genderRepository.save(com.intrumentoev.demo.entity.catalogs.Gender.builder().name("FEMENINO").isActive(true).build())
        );
        var n1 = nationalityRepository.findAll().stream().findFirst().orElseGet(() ->
                nationalityRepository.save(com.intrumentoev.demo.entity.catalogs.Nationality.builder().name("MEXICANA").isActive(true).build())
        );
        var m1 = maritalStatusRepository.findAll().stream().findFirst().orElseGet(() ->
                maritalStatusRepository.save(com.intrumentoev.demo.entity.catalogs.MaritalStatus.builder().name("SOLTERO").isActive(true).build())
        );
        var m2 = maritalStatusRepository.findAll().stream().filter(m -> !m.getIdMaritalStatus().equals(m1.getIdMaritalStatus())).findFirst().orElseGet(() ->
                maritalStatusRepository.save(com.intrumentoev.demo.entity.catalogs.MaritalStatus.builder().name("CASADO").isActive(true).build())
        );
        var mun1 = municipalityRepository.findAll().stream().findFirst().orElseGet(() ->
                municipalityRepository.save(com.intrumentoev.demo.entity.catalogs.Municipality.builder().idState((short) 9).cveMun("015").name("CUAUHTEMOC").isActive(true).build())
        );

        gender1Id = g1.getIdGender();
        gender2Id = g2.getIdGender();
        nationality1Id = n1.getIdNationality();
        marital1Id = m1.getIdMaritalStatus();
        marital2Id = m2.getIdMaritalStatus();
        municipalityId = mun1.getIdMunicipality();

        int count = COUNTER.incrementAndGet();
        String curp = String.format("TEST850101HDFRRN%02d", count % 90 + 10);
        String rfc = String.format("TEST850101%03d", count % 900 + 100);
        String email = "update.test." + count + "@banco.com";
        String phone = String.format("55%08d", count);

        String onboardingJson = """
            {
              "name": "Pedro",
              "secondName": "Antonio",
              "lastName": "Ramirez",
              "secondLastName": "Sanchez",
              "birthDate": "1985-01-01",
              "curp": "%s",
              "rfc": "%s",
              "idGender": %d,
              "idNationality": %d,
              "idMaritalStatus": %d,
              "email": "%s",
              "mobilePhone": "%s",
              "street": "Avenida Siempre Viva",
              "exteriorNumber": "742",
              "neighborhood": "Springfield",
              "idMunicipality": %d,
              "postalCode": "06600",
              "country": "Mexico",
              "occupation": "Ingeniero",
              "company": "Planta Nuclear",
              "monthlyIncome": 35000.00,
              "initialBalance": 1000.00,
              "password": "Password123!"
            }
            """.formatted(curp, rfc, gender1Id, nationality1Id, marital1Id, email, phone, municipalityId);

        MvcResult result = mockMvc.perform(post("/v1/clientes/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        createdClientCurp = root.path("client").path("curp").asText();
        createdClientRfc = root.path("client").path("rfc").asText();
    }

    @Nested
    @DisplayName("1. PUT /v1/clientes/{identificador} con ClientUpdateRequest")
    class ClientUpdateTests {

        @Test
        @DisplayName("Actualiza datos personales exitosamente vía CURP sin enviar CURP ni RFC")
        void testUpdateClientSuccessWithoutCurpAndRfc() throws Exception {
            String updateJson = """
                {
                  "name": "Pedro Modificado",
                  "secondName": "Antonio",
                  "lastName": "Ramirez",
                  "secondLastName": "Sanchez",
                  "birthDate": "1985-01-01",
                  "idGender": %d,
                  "idNationality": %d,
                  "idMaritalStatus": %d
                }
                """.formatted(gender2Id, nationality1Id, marital2Id);

            mockMvc.perform(put("/v1/clientes/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Pedro Modificado")))
                    .andExpect(jsonPath("$.idGender", is(gender2Id.intValue())))
                    .andExpect(jsonPath("$.idMaritalStatus", is(marital2Id.intValue())))
                    .andExpect(jsonPath("$.curp", notNullValue()))
                    .andExpect(jsonPath("$.rfc", notNullValue()));
        }

        @Test
        @DisplayName("Actualiza datos personales exitosamente vía RFC")
        void testUpdateClientSuccessViaRfc() throws Exception {
            String updateJson = """
                {
                  "name": "Pedro Via RFC",
                  "secondName": "Antonio",
                  "lastName": "Ramirez",
                  "secondLastName": "Sanchez",
                  "birthDate": "1985-01-01",
                  "idGender": %d,
                  "idNationality": %d,
                  "idMaritalStatus": %d
                }
                """.formatted(gender1Id, nationality1Id, marital1Id);

            mockMvc.perform(put("/v1/clientes/" + createdClientRfc)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Pedro Via RFC")));
        }

        @Test
        @DisplayName("Rechaza actualización si se intenta modificar la CURP en ClientUpdateRequest")
        void testUpdateClientRejectsCurpModification() throws Exception {
            String updateJson = """
                {
                  "name": "Pedro",
                  "lastName": "Ramirez",
                  "secondLastName": "Sanchez",
                  "birthDate": "1985-01-01",
                  "idGender": %d,
                  "idNationality": %d,
                  "idMaritalStatus": %d,
                  "curp": "DIFE850101HDFRRN01"
                }
                """.formatted(gender1Id, nationality1Id, marital1Id);

            mockMvc.perform(put("/v1/clientes/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.message", containsString("No está permitido modificar la CURP")));
        }
    }

    @Nested
    @DisplayName("2. PUT /v1/contact-details/cliente/{identificador} con ContactDetailUpdateRequest")
    class ContactDetailUpdateTests {

        @Test
        @DisplayName("Actualiza datos de contacto exitosamente por CURP")
        void testUpdateContactDetailSuccessWithoutIdClient() throws Exception {
            String updateJson = """
                {
                  "email": "nuevo.email.contacto@banco.com",
                  "mobilePhone": "5512349988",
                  "alternativePhone": "5587654321"
                }
                """;

            mockMvc.perform(put("/v1/contact-details/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("nuevo.email.contacto@banco.com")))
                    .andExpect(jsonPath("$.mobilePhone", is("5512349988")))
                    .andExpect(jsonPath("$.alternativePhone", is("5587654321")));
        }

        @Test
        @DisplayName("Rechaza actualización de contacto con formato de teléfono inválido")
        void testUpdateContactDetailInvalidPhone() throws Exception {
            String updateJson = """
                {
                  "email": "nuevo.email@banco.com",
                  "mobilePhone": "12345"
                }
                """;

            mockMvc.perform(put("/v1/contact-details/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("mobilePhone")));
        }
    }

    @Nested
    @DisplayName("3. PUT /v1/domicilios/cliente/{identificador} con HomeUpdateRequest")
    class HomeUpdateTests {

        @Test
        @DisplayName("Actualiza domicilio exitosamente por CURP")
        void testUpdateHomeSuccessWithoutIdClient() throws Exception {
            String updateJson = """
                {
                  "street": "Calle Hamburgo",
                  "exteriorNumber": "100",
                  "interiorNumber": "4B",
                  "neighborhood": "Juárez",
                  "idMunicipality": %d,
                  "postalCode": "06600",
                  "country": "México"
                }
                """.formatted(municipalityId);

            mockMvc.perform(put("/v1/domicilios/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.street", is("Calle Hamburgo")))
                    .andExpect(jsonPath("$.exteriorNumber", is("100")))
                    .andExpect(jsonPath("$.interiorNumber", is("4B")));
        }

        @Test
        @DisplayName("Rechaza actualización de domicilio con código postal inválido")
        void testUpdateHomeInvalidPostalCode() throws Exception {
            String updateJson = """
                {
                  "street": "Calle Hamburgo",
                  "exteriorNumber": "100",
                  "neighborhood": "Juárez",
                  "idMunicipality": %d,
                  "postalCode": "123"
                }
                """.formatted(municipalityId);

            mockMvc.perform(put("/v1/domicilios/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("postalCode")));
        }
    }

    @Nested
    @DisplayName("4. PUT /v1/laboral/cliente/{identificador} con EmploymentInformationUpdateRequest")
    class EmploymentUpdateTests {

        @Test
        @DisplayName("Actualiza información laboral exitosamente por CURP")
        void testUpdateEmploymentSuccessWithoutIdClient() throws Exception {
            String updateJson = """
                {
                  "occupation": "Tech Lead",
                  "company": "Banco Tech Corp",
                  "monthlyIncome": 75000.00
                }
                """;

            mockMvc.perform(put("/v1/laboral/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.occupation", is("Tech Lead")))
                    .andExpect(jsonPath("$.company", is("Banco Tech Corp")))
                    .andExpect(jsonPath("$.monthlyIncome", is(75000.0)));
        }

        @Test
        @DisplayName("Rechaza actualización laboral si monthlyIncome es String (protección estricta)")
        void testUpdateEmploymentRejectsStringIncome() throws Exception {
            String updateJson = """
                {
                  "occupation": "Tech Lead",
                  "company": "Banco Tech Corp",
                  "monthlyIncome": "75000.00"
                }
                """;

            mockMvc.perform(put("/v1/laboral/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code", is("INVALID_TYPE")))
                    .andExpect(jsonPath("$.error.target", is("monthlyIncome")));
        }

        @Test
        @DisplayName("Rechaza actualización laboral si monthlyIncome es cero o negativo")
        void testUpdateEmploymentRejectsZeroIncome() throws Exception {
            String updateJson = """
                {
                  "occupation": "Tech Lead",
                  "company": "Banco Tech Corp",
                  "monthlyIncome": 0.00
                }
                """;

            mockMvc.perform(put("/v1/laboral/cliente/" + createdClientCurp)
                            .session(authenticatedSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.details[*].target", hasItem("monthlyIncome")));
        }
    }
}
