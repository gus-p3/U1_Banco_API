package com.intrumentoev.demo.controller.home;

import com.intrumentoev.demo.model.home.HomePatchRequest;
import com.intrumentoev.demo.model.home.HomeRequest;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.service.service.home.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Domicilios", description = "Gestión de direcciones y domicilios asociados a clientes")
@RestController
@RequestMapping("/v1/domicilios")
@RequiredArgsConstructor
@Validated
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "Obtener domicilio por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeResponse> obtenerHomePorId(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id) {
        HomeResponse response = homeService.obtenerHomePorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener domicilio por ID de cliente")
    @GetMapping(value = "/cliente/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeResponse> obtenerHomePorIdClient(
            @PathVariable("idClient") @Positive(message = "El identificador del cliente debe ser un número entero positivo mayor a 0") Long idClient) {
        HomeResponse response = homeService.obtenerHomePorIdClient(idClient);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reemplazo completo de domicilio (PUT)", description = "Actualiza los campos de domicilio sin requerir el idClient.")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> reemplazarHome(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id,
            @Valid @RequestBody com.intrumentoev.demo.model.home.HomeUpdateRequest request) {
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
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id,
            @Valid @RequestBody HomePatchRequest request) {
        HomeResponse response = homeService.actualizarParcialHome(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar domicilio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarHome(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id) {
        homeService.eliminarHome(id);
        return ResponseEntity.noContent().build();
    }
}
