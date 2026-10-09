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
    private final HomeMapper homeMapper;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse obtenerHomePorId(Long id) {
        log.info("Buscando domicilio con ID: {}", id);
        Home home = homeRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Domicilio no encontrado con ID: " + id, "idHome"));
        return homeMapper.toResponse(home);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeResponse obtenerHomePorIdClient(Long idClient) {
        log.info("Buscando domicilio para cliente ID: {}", idClient);
        Home home = homeRepository.findByIdClient(idClient)
                .orElseThrow(() -> new BusinessValidationException("No existe domicilio para el cliente con ID: " + idClient, "idClient"));
        return homeMapper.toResponse(home);
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

        // Regla de Negocio: El municipio referenciado debe existir
        if (!municipalityRepository.existsById(request.getIdMunicipality())) {
            throw new CatalogNotFoundException("idMunicipality", request.getIdMunicipality());
        }

        homeMapper.updateEntityFromUpdateRequest(request, existente);
        Home actualizado = homeRepository.save(existente);
        return homeMapper.toResponse(actualizado);
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

        // Regla de Negocio: El municipio referenciado debe existir
        if (!municipalityRepository.existsById(request.getIdMunicipality())) {
            throw new CatalogNotFoundException("idMunicipality", request.getIdMunicipality());
        }

        homeMapper.updateEntityFromRequest(request, existente);
        Home actualizado = homeRepository.save(existente);
        return homeMapper.toResponse(actualizado);
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

        // Regla de Negocio: El municipio referenciado debe existir si se envía
        if (request.getIdMunicipality() != null && !municipalityRepository.existsById(request.getIdMunicipality())) {
            throw new CatalogNotFoundException("idMunicipality", request.getIdMunicipality());
        }

        homeMapper.updateEntityFromPatch(request, existente);
        Home actualizado = homeRepository.save(existente);
        return homeMapper.toResponse(actualizado);
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
