package com.intrumentoev.demo.controller.home;

import com.intrumentoev.demo.model.home.HomePatchRequest;
import com.intrumentoev.demo.model.home.HomeResponse;
import com.intrumentoev.demo.model.home.HomeUpdateRequest;
import com.intrumentoev.demo.service.service.home.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Domicilios", description = "Gestión de direcciones y domicilios asociados a clientes mediante CURP o RFC")
@RestController
@RequestMapping("/v1/domicilios")
@RequiredArgsConstructor
@Validated
public class HomeController {

    private final HomeService homeService;

    private static final String IDENTIFICADOR_REGEX = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$";
    private static final String IDENTIFICADOR_MESSAGE = "El identificador del cliente debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido. No se permite el uso de IDs numéricos.";

    @Operation(summary = "Obtener domicilio por CURP o RFC del cliente")
    @GetMapping(value = "/cliente/{identificador}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeResponse> obtenerHomePorIdentificador(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        HomeResponse response = homeService.obtenerHomePorIdentificador(identificador);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reemplazo completo de domicilio por CURP o RFC (PUT)", description = "Actualiza los campos de domicilio del cliente identificado por CURP o RFC. Jamás por ID.")
    @PutMapping(
            value = "/cliente/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> reemplazarHome(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody HomeUpdateRequest request) {
        HomeResponse response = homeService.reemplazarHomePorIdentificador(identificador, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualización parcial de domicilio por CURP o RFC (PATCH)", description = "Actualiza campos específicos de domicilio del cliente identificado por CURP o RFC. Jamás por ID.")
    @PatchMapping(
            value = "/cliente/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HomeResponse> actualizarParcialHome(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody HomePatchRequest request) {
        HomeResponse response = homeService.actualizarParcialHomePorIdentificador(identificador, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar domicilio por CURP o RFC")
    @DeleteMapping("/cliente/{identificador}")
    public ResponseEntity<Void> eliminarHome(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        homeService.eliminarHomePorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }
}
