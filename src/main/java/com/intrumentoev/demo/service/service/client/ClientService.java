package com.intrumentoev.demo.service.service.client;

import com.intrumentoev.demo.model.client.*;

import java.util.List;

/**
 * Servicio unificado de administración de clientes y usuarios.
 * Alberga exclusivamente la lógica de las 4 APIs obligatorias:
 * 1. Consulta dinámica y ordenamiento (GET)
 * 2. Actualización modular y granular (PATCH)
 * 3. Creación integral / Onboarding (POST)
 * 4. Baja lógica (DELETE)
 */
public interface ClientService {

    /**
     * API 1: Consulta dinámica combinando filtros y ordenamiento dinámico vía Request DTO.
     */
    List<ClientResponse> consultarClientes(ClientUnifiedQueryRequest request);

    /**
     * API 2: Súper PATCH para actualización parcial y modular de datos personales, contacto, domicilio y laboral.
     */
    ClientDetailResponse actualizarParcialUnificado(ClientUnifiedPatchRequest request);

    /**
     * API 3: Creación integral de cliente (Onboarding bancario) con validaciones y apertura de cuenta inicial.
     */
    ClientDetailResponse registrarOnboarding(ClientOnboardingRequest request);

    /**
     * API 4: Baja lógica de cliente y cuentas asociadas recibiendo identificador en el cuerpo.
     */
    void desactivarClienteLogicamente(ClientDeactivateRequest request);
}