package com.intrumentoev.demo.service.service.client;

import com.intrumentoev.demo.model.client.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface ClientService {

    /**
     * Proceso integral de Onboarding:
     * Registra datos personales, contacto, domicilio, información laboral
     * y crea automáticamente una cuenta bancaria con saldo inicial.
     */
    ClientDetailResponse registrarOnboarding(ClientOnboardingRequest request);

    /**
     * Listar todos los clientes (GET /v1/clientes)
     */
    List<ClientResponse> obtenerTodosLosClientes();

    /**
     * Buscar cliente por ID (GET /v1/clientes/{id})
     */
    ClientResponse obtenerClientePorId(Long id);

    /**
     * Obtener detalle completo o modular del cliente con soporte para ?include=contact,home,employment,accounts,catalogs
     * Devuelve los IDs y los catálogos necesarios para edición modular en frontend.
     */
    ClientDetailResponse obtenerClientePorIdConIncludes(Long id, String include);


    /**
     * Búsqueda unificada de clientes vía POST con filtros en cuerpo (CURP, RFC, Email, No. Cuenta, Activo, Fechas).
     * Si no se envía ningún filtro, arroja BusinessValidationException (HTTP 400).
     */
    List<ClientResponse> buscarClientes(ClientSearchRequest searchRequest);

    /**
     * Buscar cliente por CURP o RFC (identificador alfanumérico)
     */
    ClientResponse obtenerClientePorIdentificador(String identificador);

    /**
     * Obtener detalle completo o modular del cliente por CURP o RFC con soporte para ?include=...
     */
    ClientDetailResponse obtenerClientePorIdentificadorConIncludes(String identificador, String include);

    /**
     * Reemplazo completo de información personal del cliente por CURP o RFC (PUT /v1/clientes/{identificador})
     */
    ClientResponse reemplazarClientePorIdentificador(String identificador, ClientUpdateRequest request);

    /**
     * Actualización parcial del cliente por CURP o RFC (PATCH /v1/clientes/{identificador})
     */
    ClientResponse actualizarParcialClientePorIdentificador(String identificador, ClientPatchRequest request);

    /**
     * Baja lógica del cliente por CURP o RFC (DELETE /v1/clientes/{identificador})
     */
    void eliminarClientePorIdentificador(String identificador);

    /**
     * Buscar cliente por CURP
     */
    ClientResponse obtenerClientePorCurp(String curp);

    /**
     * Buscar cliente por RFC
     */
    ClientResponse obtenerClientePorRfc(String rfc);

    /**
     * Buscar cliente por correo electrónico
     */
    ClientResponse obtenerClientePorCorreo(String email);

    /**
     * Buscar cliente por número de cuenta bancaria
     */
    ClientResponse obtenerClientePorNumeroCuenta(String numeroCuenta);

    /**
     * Consultar clientes activos
     */
    List<ClientResponse> obtenerClientesActivos();

    /**
     * Obtener clientes registrados en un rango de fechas
     */
    List<ClientResponse> obtenerClientesPorRangoFechas(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Reemplazo completo de información personal del cliente con DTO dedicado (PUT /v1/clientes/{id})
     * No se permite modificar CURP ni RFC.
     */
    ClientResponse reemplazarCliente(Long id, ClientUpdateRequest request);

    /**
     * Reemplazo completo de información personal del cliente (PUT /v1/clientes/{id})
     * No se permite modificar CURP ni RFC.
     */
    ClientResponse reemplazarCliente(Long id, ClientRequest request);

    /**
     * Actualización parcial del cliente (PATCH /v1/clientes/{id})
     * No se permite modificar CURP ni RFC.
     */
    ClientResponse actualizarParcialCliente(Long id, ClientPatchRequest request);

    /**
     * Baja lógica del cliente (DELETE /v1/clientes/{id}):
     * Desactiva el cliente y desactiva todas sus cuentas bancarias asociadas.
     */
    void eliminarCliente(Long id);
}