package com.intrumentoev.demo.controller.home;

import com.intrumentoev.demo.model.home.HomePatchRequest;
import com.intrumentoev.demo.model.home.HomeRequest;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.service.service.home.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Domicilios", description = "Gestión de direcciones y domicilios asociados a clientes")
@RestController
@RequestMapping("/v1/domicilios")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "Registrar domicilio", description = "Registra la dirección asociada a un cliente.")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> crearHome(@Valid @RequestBody HomeRequest request) {
        HomeResponse response = homeService.crearHome(request);
        URI location = URI.create("/v1/domicilios/" + response.getIdHome());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Obtener domicilio por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeResponse> obtenerHomePorId(@PathVariable("id") Long id) {
        HomeResponse response = homeService.obtenerHomePorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener domicilio por ID de cliente")
    @GetMapping(value = "/cliente/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeResponse> obtenerHomePorIdClient(@PathVariable("idClient") Long idClient) {
        HomeResponse response = homeService.obtenerHomePorIdClient(idClient);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reemplazo completo de domicilio (PUT)")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> reemplazarHome(
            @PathVariable("id") Long id,
            @Valid @RequestBody HomeRequest request) {
        HomeResponse response = homeService.reemplazarHome(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualización parcial de domicilio (PATCH)")
    @PatchMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> actualizarParcialHome(
            @PathVariable("id") Long id,
            @Valid @RequestBody HomePatchRequest request) {
        HomeResponse response = homeService.actualizarParcialHome(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar domicilio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarHome(@PathVariable("id") Long id) {
        homeService.eliminarHome(id);
        return ResponseEntity.noContent().build();
    }
}
