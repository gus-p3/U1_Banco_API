package com.intrumentoev.demo.controller.contactDetail;

import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import com.intrumentoev.demo.model.contactDetail.ContactDetailUpdateRequest;
import com.intrumentoev.demo.service.service.contactDetail.ContactDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Detalles de Contacto", description = "Gestión de correos electrónicos y teléfonos móviles de clientes mediante CURP o RFC")
@RestController
@RequestMapping("/v1/contact-details")
@RequiredArgsConstructor
@Validated
public class ContactDetailController {

    private final ContactDetailService contactDetailService;

    private static final String IDENTIFICADOR_REGEX = "^([A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3})$";
    private static final String IDENTIFICADOR_MESSAGE = "El identificador del cliente debe ser una CURP (18 caracteres) o RFC (12 o 13 caracteres) válido. No se permite el uso de IDs numéricos.";

    @Operation(summary = "Obtener detalle de contacto por CURP o RFC de cliente")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de contacto obtenido exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para consultar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado con el identificador proporcionado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = "/cliente/{identificador}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactDetailResponse> obtenerContactDetailPorIdentificador(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        ContactDetailResponse detalle = contactDetailService.obtenerContactDetailPorIdentificador(identificador);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Reemplazo completo de detalle de contacto por CURP o RFC (PUT)", description = "Actualiza el correo y teléfonos del cliente identificado por CURP o RFC. Jamás por ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de contacto actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para modificar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Correo electrónico o teléfono ya registrados en otra cuenta"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping(
            value = "/cliente/{identificador}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ContactDetailResponse> reemplazarContactDetail(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody ContactDetailUpdateRequest request) {
        ContactDetailResponse actualizado = contactDetailService.reemplazarContactDetailPorIdentificador(identificador, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Actualización parcial de detalle de contacto por CURP o RFC (PATCH)", description = "Actualiza campos específicos de contacto del cliente identificado por CURP o RFC. Jamás por ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de contacto actualizado parcialmente con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado para modificar información de otro cliente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Correo electrónico o teléfono ya registrados en otra cuenta"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Cuerpo de petición sin cambios válidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PatchMapping(
            value = "/cliente/{identificador}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ContactDetailResponse> actualizarParcialContactDetail(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador,
            @Valid @RequestBody ContactDetailPatchRequest request) {
        ContactDetailResponse actualizado = contactDetailService.actualizarParcialContactDetailPorIdentificador(identificador, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar detalle de contacto por CURP o RFC")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Detalle de contacto eliminado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Formato de CURP o RFC inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sesión no iniciada o inactiva en el servidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/cliente/{identificador}")
    public ResponseEntity<Void> eliminarContactDetail(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        contactDetailService.eliminarContactDetailPorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }
}