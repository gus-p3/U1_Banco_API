package com.intrumentoev.demo.controller.catalog;

import com.intrumentoev.demo.model.catalog.CatalogSyncResponse;
import com.intrumentoev.demo.model.catalog.GenderResponse;
import com.intrumentoev.demo.model.catalog.MaritalStatusResponse;
import com.intrumentoev.demo.model.catalog.MunicipalityResponse;
import com.intrumentoev.demo.model.catalog.NationalityResponse;
import com.intrumentoev.demo.model.catalog.StateResponse;
import com.intrumentoev.demo.service.service.catalog.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.util.List;

@Tag(name = "Catálogos", description = "Consulta de catálogos: géneros, nacionalidades, estados civiles, estados federativos y municipios")
@RestController
@RequestMapping("/v1/catalogos")
@RequiredArgsConstructor
@Validated
public class CatalogController {

    private final CatalogService catalogService;

    // ──────────────────── Catálogos personales ────────────────────

    @Operation(summary = "Obtener géneros", description = "Retorna los géneros disponibles (Hombre, Mujer). Usa el idGender en el onboarding.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de géneros obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor al consultar catálogos")
    })
    @GetMapping(value = "/generos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GenderResponse>> obtenerGeneros() {
        return ResponseEntity.ok(catalogService.obtenerGeneros());
    }

    @Operation(summary = "Obtener nacionalidades", description = "Retorna las nacionalidades disponibles. Usa el idNationality en el onboarding.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de nacionalidades obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor al consultar catálogos")
    })
    @GetMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NationalityResponse>> obtenerNacionalidades() {
        return ResponseEntity.ok(catalogService.obtenerNacionalidades());
    }

    @Operation(summary = "Obtener estados civiles", description = "Retorna los estados civiles disponibles (Soltero, Casado, etc.). Usa el idMaritalStatus en el onboarding.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de estados civiles obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor al consultar catálogos")
    })
    @GetMapping(value = "/estados-civiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MaritalStatusResponse>> obtenerEstadosCiviles() {
        return ResponseEntity.ok(catalogService.obtenerEstadosCiviles());
    }

    // ──────────────────── Catálogos geográficos (INEGI) ────────────────────

    @Operation(summary = "Obtener estados federativos", description = "Retorna la lista de entidades federativas mexicanas. Consulta primero Redis y cuenta con fallback a PostgreSQL e INEGI.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de entidades federativas obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor al consultar estados")
    })
    @GetMapping(value = "/estados", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateResponse>> obtenerEstados() {
        List<StateResponse> estados = catalogService.obtenerEstados();
        return ResponseEntity.ok(estados);
    }

    @Operation(summary = "Obtener municipios por estado", description = "Retorna los municipios asociados a una entidad federativa (cve_ent). Utiliza caché en Redis y fallback a PostgreSQL e INEGI.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de municipios obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Clave de entidad inválida (debe contener exactamente 2 dígitos numéricos)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No existe la entidad federativa con la clave solicitada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor al consultar municipios")
    })
    @GetMapping(value = "/municipios/{cveEnt}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MunicipalityResponse>> obtenerMunicipiosPorEstado(
            @Parameter(description = "Clave de entidad de 2 dígitos (ej: 01, 09, 14)")
            @PathVariable("cveEnt")
            @Pattern(regexp = "^[0-9]{2}$", message = "La clave de entidad federativa debe tener exactamente 2 dígitos numéricos (ej. 01, 09, 14)")
            String cveEnt) {
        List<MunicipalityResponse> municipios = catalogService.obtenerMunicipiosPorEstado(cveEnt);
        return ResponseEntity.ok(municipios);
    }

    @Operation(summary = "Sincronizar catálogos desde INEGI", description = "Consume las APIs oficiales de INEGI (mgee y mgem) y actualiza en batch tanto PostgreSQL como la caché en Redis.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Catálogos sincronizados exitosamente con INEGI"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error al conectar o sincronizar datos desde las APIs de INEGI")
    })
    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogSyncResponse> sincronizarCatalogos() {
        CatalogSyncResponse response = catalogService.sincronizarCatalogos();
        return ResponseEntity.ok(response);
    }
}

