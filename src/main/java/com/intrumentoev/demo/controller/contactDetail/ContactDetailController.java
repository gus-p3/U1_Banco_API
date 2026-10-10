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
    @GetMapping(value = "/cliente/{identificador}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactDetailResponse> obtenerContactDetailPorIdentificador(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        ContactDetailResponse detalle = contactDetailService.obtenerContactDetailPorIdentificador(identificador);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Reemplazo completo de detalle de contacto por CURP o RFC (PUT)", description = "Actualiza el correo y teléfonos del cliente identificado por CURP o RFC. Jamás por ID.")
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
    @DeleteMapping("/cliente/{identificador}")
    public ResponseEntity<Void> eliminarContactDetail(
            @PathVariable("identificador")
            @Pattern(regexp = IDENTIFICADOR_REGEX, message = IDENTIFICADOR_MESSAGE)
            String identificador) {
        contactDetailService.eliminarContactDetailPorIdentificador(identificador);
        return ResponseEntity.noContent().build();
    }
}