package com.intrumentoev.demo.controller.employment;

import com.intrumentoev.demo.model.employment.EmploymentInformationPatchRequest;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.model.employment.EmploymentInformationUpdateRequest;
import com.intrumentoev.demo.service.service.employment.EmploymentInformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Información Laboral", description = "Gestión de ocupación, empresa e ingresos mensuales de clientes mediante CURP o RFC")
@RestController
@RequestMapping("/v1/laboral")
@RequiredArgsConstructor
@Validated
public class EmploymentInformationController {

    private final EmploymentInformationService employmentService;

    private static final String IDENTIFICADOR_REGEX = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$";
    private static final String IDENTIFICADOR_MESSAGE = "El identificador del cliente debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido. No se permite el uso de IDs numéricos.";

    @Operation(summary = "Obtener información laboral por CURP o RFC de cliente")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Información laboral del cliente obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para consultar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado con el identificador proporcionado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/cliente/{identificador}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmploymentInformationResponse> obtenerPorIdentificador(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        EmploymentInformationResponse response = employmentService.obtenerEmploymentInformationPorIdentificador(identificador);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reemplazo completo de información laboral por CURP o RFC (PUT)", description = "Actualiza ocupación, empresa e ingreso mensual del cliente identificado por CURP o RFC. Jamás por ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Información laboral actualizada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para modificar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Reglas de negocio no cumplidas (ej. ingreso menor a cero)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping(
            value = "/cliente/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmploymentInformationResponse> reemplazar(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody EmploymentInformationUpdateRequest request) {
        EmploymentInformationResponse response = employmentService.reemplazarEmploymentInformationPorIdentificador(identificador, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualización parcial de información laboral por CURP o RFC (PATCH)", description = "Actualiza campos específicos de información laboral del cliente identificado por CURP o RFC. Jamás por ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Información laboral actualizada parcialmente con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para modificar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Cuerpo de petición sin cambios válidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PatchMapping(
            value = "/cliente/{identificador}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmploymentInformationResponse> actualizarParcial(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody EmploymentInformationPatchRequest request) {
        EmploymentInformationResponse response = employmentService.actualizarParcialEmploymentInformationPorIdentificador(identificador, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar información laboral por CURP o RFC")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Información laboral eliminada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/cliente/{identificador}")
    public ResponseEntity<Void> eliminar(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        employmentService.eliminarEmploymentInformationPorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }
}
