package com.intrumentoev.demo.controller.employment;

import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.service.service.employment.EmploymentInformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Información Laboral", description = "Gestión de ocupación, empresa e ingresos mensuales de clientes")
@RestController
@RequestMapping("/v1/laboral")
@RequiredArgsConstructor
@Validated
public class EmploymentInformationController {

    private final EmploymentInformationService employmentService;

    @Operation(summary = "Obtener información laboral por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmploymentInformationResponse> obtenerPorId(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id) {
        EmploymentInformationResponse response = employmentService.obtenerEmploymentInformationPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener información laboral por ID de cliente")
    @GetMapping(value = "/cliente/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmploymentInformationResponse> obtenerPorIdClient(
            @PathVariable("idClient") @Positive(message = "El identificador del cliente debe ser un número entero positivo mayor a 0") Long idClient) {
        EmploymentInformationResponse response = employmentService.obtenerEmploymentInformationPorIdClient(idClient);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reemplazo completo de información laboral (PUT)", description = "Actualiza ocupación, empresa e ingreso mensual sin requerir idClient.")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmploymentInformationResponse> reemplazar(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id,
            @Valid @RequestBody com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest request) {
        EmploymentInformationResponse response = employmentService.reemplazarEmploymentInformation(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualización parcial de información laboral (PATCH)")
    @PatchMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmploymentInformationResponse> actualizarParcial(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id,
            @Valid @RequestBody EmploymentInformationPatchRequest request) {
        EmploymentInformationResponse response = employmentService.actualizarParcialEmploymentInformation(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar información laboral")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable("id") @Positive(message = "El identificador debe ser un número entero positivo mayor a 0") Long id) {
        employmentService.eliminarEmploymentInformation(id);
        return ResponseEntity.noContent().build();
    }
}
