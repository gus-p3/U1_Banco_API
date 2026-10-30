package com.intrumentoev.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intrumentoev.demo.config.GlobalExceptionHandler;
import com.intrumentoev.demo.controller.client.ClientController;
import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.client.*;
import com.intrumentoev.demo.service.service.client.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /v1/clientes/buscar - Consulta unificada responde 200 OK")
    void testConsultarClientesExitoso() throws Exception {
        ClientUnifiedQueryRequest query = ClientUnifiedQueryRequest.builder()
                .curp("ROMA900101HDFRRN01")
                .build();

        ClientResponse clientResp = ClientResponse.builder()
                .curp("ROMA900101HDFRRN01")
                .name("Juan")
                .build();

        when(clientService.consultarClientes(any(), any())).thenReturn(List.of(clientResp));

        mockMvc.perform(post("/v1/clientes/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].curp").value("ROMA900101HDFRRN01"))
                .andExpect(jsonPath("$[0].name").value("Juan"));
    }

    @Test
    @DisplayName("POST /v1/clientes - Onboarding integral responde 201 CREATED")
    void testRegistrarClienteExitoso() throws Exception {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .name("Carlos")
                .lastName("Martínez")
                .birthDate(LocalDate.of(1994, 6, 15))
                .curp("MALC940615HDFRPR03")
                .rfc("MALC9406154A8")
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .email("carlos@banco.com")
                .mobilePhone("5512345678")
                .street("Insurgentes Sur")
                .exteriorNumber("1602")
                .neighborhood("Crédito Constructor")
                .idMunicipality(14)
                .postalCode("03940")
                .occupation("Ingeniero")
                .company("Tech SA")
                .monthlyIncome(new BigDecimal("40000.00"))
                .initialBalance(new BigDecimal("1000.00"))
                .password("PasswordSegura#2026")
                .build();

        ClientDetailResponse response = ClientDetailResponse.builder()
                .client(ClientResponse.builder().curp("MALC940615HDFRPR03").name("Carlos").build())
                .build();

        when(clientService.registrarOnboarding(any())).thenReturn(response);

        mockMvc.perform(post("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client.curp").value("MALC940615HDFRPR03"));
    }

    @Test
    @DisplayName("POST /v1/clientes - Rechaza con 409 cuando CURP está duplicada")
    void testRegistrarClienteCurpDuplicada() throws Exception {
        ClientOnboardingRequest request = ClientOnboardingRequest.builder()
                .name("Carlos")
                .lastName("Martínez")
                .birthDate(LocalDate.of(1994, 6, 15))
                .curp("MALC940615HDFRPR03")
                .rfc("MALC9406154A8")
                .idGender((short) 1)
                .idNationality((short) 1)
                .idMaritalStatus((short) 1)
                .email("carlos@banco.com")
                .mobilePhone("5512345678")
                .street("Insurgentes")
                .exteriorNumber("1602")
                .neighborhood("Centro")
                .idMunicipality(14)
                .postalCode("03940")
                .occupation("Ingeniero")
                .company("Tech SA")
                .monthlyIncome(new BigDecimal("40000.00"))
                .initialBalance(new BigDecimal("1000.00"))
                .password("PasswordSegura#2026")
                .build();

        when(clientService.registrarOnboarding(any()))
                .thenThrow(new BusinessException(ErrorCode.CURP_DUPLICATED, "MALC940615HDFRPR03"));

        mockMvc.perform(post("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CURP_DUPLICATED"))
                .andExpect(jsonPath("$.error.target").value("curp"));
    }

    @Test
    @DisplayName("PATCH /v1/clientes - Súper PATCH responde 200 OK")
    void testActualizarParcialClienteExitoso() throws Exception {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("MALC940615HDFRPR03")
                .mobilePhone("5599887766")
                .build();

        ClientDetailResponse response = ClientDetailResponse.builder()
                .client(ClientResponse.builder().curp("MALC940615HDFRPR03").name("Carlos").build())
                .build();

        when(clientService.actualizarParcialUnificado(any())).thenReturn(response);

        mockMvc.perform(patch("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client.curp").value("MALC940615HDFRPR03"));
    }

    @Test
    @DisplayName("PATCH /v1/clientes - Rechaza con 404 si el cliente no existe")
    void testActualizarParcialClienteNoExiste() throws Exception {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("ABCD900101HDFRRN09")
                .build();

        when(clientService.actualizarParcialUnificado(any()))
                .thenThrow(new BusinessException(ErrorCode.CLIENT_NOT_FOUND, "ABCD900101HDFRRN09"));

        mockMvc.perform(patch("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /v1/clientes - Rechaza con 400 Bad Request si el identificador no tiene formato CURP o RFC")
    void testActualizarParcialFormatoInvalido() throws Exception {
        ClientUnifiedPatchRequest request = ClientUnifiedPatchRequest.builder()
                .identificador("FORMATO_INVALIDO_123")
                .build();

        mockMvc.perform(patch("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("DELETE /v1/clientes - Baja lógica responde 204 No Content")
    void testDesactivarClienteExitoso() throws Exception {
        ClientDeactivateRequest request = ClientDeactivateRequest.builder()
                .identificador("MALC940615HDFRPR03")
                .reason("Cancelación voluntaria")
                .build();

        doNothing().when(clientService).desactivarClienteLogicamente(any());

        mockMvc.perform(delete("/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(clientService, times(1)).desactivarClienteLogicamente(any());
    }
}
