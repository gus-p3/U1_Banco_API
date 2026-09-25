package com.intrumentoev.demo.controller.catalog;

import com.intrumentoev.demo.model.catalog.CatalogSyncResponse;
import com.intrumentoev.demo.model.catalog.MunicipalityResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;
import com.intrumentoev.demo.service.service.catalog.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogos Geográficos (INEGI)", description = "Consulta y sincronización de estados y municipios con caché en Redis y persistencia en PostgreSQL")
@RestController
@RequestMapping("/v1/catalogos")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Obtener estados federativos", description = "Retorna la lista de entidades federativas mexicanas. Consulta primero Redis y cuenta con fallback a PostgreSQL e INEGI.")
    @GetMapping(value = "/estados", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateResponse>> obtenerEstados() {
        List<StateResponse> estados = catalogService.obtenerEstados();
        return ResponseEntity.ok(estados);
    }

    @Operation(summary = "Obtener municipios por estado", description = "Retorna los municipios asociados a una entidad federativa (cve_ent). Utiliza caché en Redis y fallback a PostgreSQL e INEGI.")
    @GetMapping(value = "/municipios/{cveEnt}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MunicipalityResponse>> obtenerMunicipiosPorEstado(
            @Parameter(description = "Clave de entidad de 2 dígitos (ej: 01, 09, 14)") @PathVariable("cveEnt") String cveEnt) {
        List<MunicipalityResponse> municipios = catalogService.obtenerMunicipiosPorEstado(cveEnt);
        return ResponseEntity.ok(municipios);
    }

    @Operation(summary = "Sincronizar catálogos desde INEGI", description = "Consume las APIs oficiales de INEGI (mgee y mgem) y actualiza en batch tanto PostgreSQL como la caché en Redis.")
    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogSyncResponse> sincronizarCatalogos() {
        CatalogSyncResponse response = catalogService.sincronizarCatalogos();
        return ResponseEntity.ok(response);
    }
}
