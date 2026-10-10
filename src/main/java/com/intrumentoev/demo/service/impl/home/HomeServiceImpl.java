package com.intrumentoev.demo.service.impl.home;

import com.intrumentoev.demo.entity.home.Home;
import com.intrumentoev.demo.exception.BusinessValidationException;
import com.intrumentoev.demo.exception.CatalogNotFoundException;
import com.intrumentoev.demo.exception.ClientNotFoundException;
import com.intrumentoev.demo.mapper.home.HomeMapper;
import com.intrumentoev.demo.model.home.HomePatchRequest;
import com.intrumentoev.demo.model.home.HomeRequest;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.repository.catalogs.MunicipalityRepository;
import com.intrumentoev.demo.repository.client.ClientRepository;
import com.intrumentoev.demo.repository.home.HomeRepository;
import com.intrumentoev.demo.service.service.home.HomeService;
import com.intrumentoev.demo.entity.catalogs.Municipality;
import com.intrumentoev.demo.entity.catalogs.State;
import com.intrumentoev.demo.repository.catalogs.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

    private final HomeRepository homeRepository;
    private final ClientRepository clientRepository;
    private final MunicipalityRepository municipalityRepository;
    private final StateRepository stateRepository;
    private final HomeMapper homeMapper;

    private com.intrumentoev.demo.entity.client.Client resolverClientePorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new BusinessValidationException("El identificador (CURP o RFC) es obligatorio", "identificador");
        }
        String idLimpio = identificador.trim().toUpperCase();
        return clientRepository.findByCurp(idLimpio)
                .or(() -> clientRepository.findByRfc(idLimpio))
                .orElseThrow(() -> new ClientNotFoundException("No se encontró cliente con CURP o RFC: " + identificador));
    }

    private void validarCoherenciaEntidad(String claveEntidad, Integer idMunicipality) {
        if (idMunicipality == null) return;
        Municipality mun = municipalityRepository.findById(idMunicipality)
                .orElseThrow(() -> new CatalogNotFoundException("idMunicipality", idMunicipality));

        if (claveEntidad != null && !claveEntidad.isBlank()) {
            State state = stateRepository.findByCveEnt(claveEntidad.trim())
                    .orElseThrow(() -> new BusinessValidationException("No existe entidad federativa con la clave: " + claveEntidad, "claveEntidad"));
            if (!mun.getIdState().equals(state.getIdState())) {
                throw new BusinessValidationException("El municipio con ID " + idMunicipality + " (" + mun.getName() + ") no pertenece a la entidad federativa " + state.getName() + " (clave: " + claveEntidad + ")", "claveEntidad");
            }
        }
    }

    private HomeResponse enrichHomeResponse(Home home) {
        HomeResponse response = homeMapper.toResponse(home);
        if (response != null && response.getIdMunicipality() != null) {
            municipalityRepository.findById(response.getIdMunicipality()).ifPresent(mun -> {
                response.setMunicipalityName(mun.getName());
                response.setIdState(mun.getIdState());
                if (mun.getIdState() != null) {
                    stateRepository.findById(mun.getIdState()).ifPresent(st -> {
                        response.setStateName(st.getName());
                        response.setCveEnt(st.getCveEnt());
                    });
                }
            });
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeResponse obtenerHomePorIdentificador(String identificador) {
        var cliente = resolverClientePorIdentificador(identificador);
        return obtenerHomePorIdClient(cliente.getIdClient());
    }

    @Override
    @Transactional
    public HomeResponse reemplazarHomePorIdentificador(String identificador, com.intrumentoev.demo.model.home.HomeUpdateRequest request) {
        var cliente = resolverClientePorIdentificador(identificador);
        Home existente = homeRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe domicilio registrado para el cliente: " + identificador, "identificador"));
        return reemplazarHome(existente.getIdHome(), request);
    }

    @Override
    @Transactional
    public HomeResponse actualizarParcialHomePorIdentificador(String identificador, HomePatchRequest request) {
        var cliente = resolverClientePorIdentificador(identificador);
        Home existente = homeRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe domicilio registrado para el cliente: " + identificador, "identificador"));
        return actualizarParcialHome(existente.getIdHome(), request);
    }

    @Override
    @Transactional
    public void eliminarHomePorIdentificador(String identificador) {
        var cliente = resolverClientePorIdentificador(identificador);
        Home existente = homeRepository.findByIdClient(cliente.getIdClient())
                .orElseThrow(() -> new BusinessValidationException("No existe domicilio registrado para el cliente: " + identificador, "identificador"));
        eliminarHome(existente.getIdHome());
    }

    @Override
    @Transactional(readOnly = true)
    public HomeResponse obtenerHomePorId(Long id) {
        log.info("Buscando domicilio con ID: {}", id);
        Home home = homeRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome"));
        return enrichHomeResponse(home);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeResponse obtenerHomePorIdClient(Long idClient) {
        log.info("Buscando domicilio para cliente ID: {}", idClient);
        Home home = homeRepository.findByIdClient(idClient)
                .orElseThrow(() -> new BusinessValidationException("No existe domicilio para el cliente con ID: " + idClient, "idClient"));
        return enrichHomeResponse(home);
    }

    @Override
    @Transactional
    public HomeResponse reemplazarHome(Long id, com.intrumentoev.demo.model.home.HomeUpdateRequest request) {
        log.info("Reemplazando completo domicilio con ID: {}", id);
        Home existente = homeRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome"));

        // Si se provee idClient, validar que exista
        if (request.getIdClient() != null && !clientRepository.existsById(request.getIdClient())) {
            throw new ClientNotFoundException(request.getIdClient());
        }

        // Regla de Negocio: El municipio referenciado debe existir y coincidir con claveEntidad si se envía
        validarCoherenciaEntidad(request.getClaveEntidad(), request.getIdMunicipality());

        homeMapper.updateEntityFromUpdateRequest(request, existente);
        Home actualizado = homeRepository.save(existente);
        return enrichHomeResponse(actualizado);
    }

    @Override
    @Transactional
    public HomeResponse reemplazarHome(Long id, HomeRequest request) {
        log.info("Reemplazando completo domicilio con ID: {}", id);
        Home existente = homeRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome"));

        // Regla de Negocio: El cliente referenciado debe existir
        if (!clientRepository.existsById(request.getIdClient())) {
            throw new ClientNotFoundException(request.getIdClient());
        }

        // Regla de Negocio: El municipio referenciado debe existir y coincidir con claveEntidad si se envía
        validarCoherenciaEntidad(request.getClaveEntidad(), request.getIdMunicipality());

        homeMapper.updateEntityFromRequest(request, existente);
        Home actualizado = homeRepository.save(existente);
        return enrichHomeResponse(actualizado);
    }

    @Override
    @Transactional
    public HomeResponse actualizarParcialHome(Long id, HomePatchRequest request) {
        log.info("Actualizando parcialmente domicilio con ID: {}", id);
        if (request == null || request.isEmpty()) {
            throw new BusinessValidationException("Debe proporcionar al menos un campo válido para actualizar", "requestBody");
        }

        Home existente = homeRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome"));

        Integer munId = request.getIdMunicipality() != null ? request.getIdMunicipality() : existente.getIdMunicipality();
        validarCoherenciaEntidad(request.getClaveEntidad(), munId);

        homeMapper.updateEntityFromPatch(request, existente);
        Home actualizado = homeRepository.save(existente);
        return enrichHomeResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarHome(Long id) {
        log.info("Eliminando domicilio con ID: {}", id);
        if (!homeRepository.existsById(id)) {
            throw new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome");
        }
        homeRepository.deleteById(id);
    }
}
