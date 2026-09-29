package com.intrumentoev.demo.controller.contactDetail;

import com.intrumentoev.demo.model.contactDetail.ContactDetailPatchRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailRequest;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import com.intrumentoev.demo.service.service.contactDetail.ContactDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Detalles de Contacto", description = "Gestión de correos electrónicos y teléfonos móviles de clientes")
@RestController
@RequestMapping("/v1/contact-details")
@RequiredArgsConstructor
public class ContactDetailController {

    private final ContactDetailService contactDetailService;

    @Operation(summary = "Obtener detalle de contacto por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactDetailResponse> obtenerContactDetailPorId(@PathVariable("id") Long id) {
        ContactDetailResponse detalle = contactDetailService.obtenerContactDetailPorId(id);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Obtener detalle de contacto por ID de cliente")
    @GetMapping(value = "/client/{idClient}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactDetailResponse> obtenerContactDetailPorIdClient(@PathVariable("idClient") Long idClient) {
        ContactDetailResponse detalle = contactDetailService.obtenerContactDetailPorIdClient(idClient);
        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Reemplazo completo de detalle de contacto (PUT)")
    @PutMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ContactDetailResponse> reemplazarContactDetail(
            @PathVariable("id") Long id,
            @Valid @RequestBody ContactDetailRequest request) {
        ContactDetailResponse actualizado = contactDetailService.reemplazarContactDetail(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Actualización parcial de detalle de contacto (PATCH)")
    @PatchMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ContactDetailResponse> actualizarParcialContactDetail(
            @PathVariable("id") Long id,
            @Valid @RequestBody ContactDetailPatchRequest request) {
        ContactDetailResponse actualizado = contactDetailService.actualizarParcialContactDetail(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar detalle de contacto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarContactDetail(@PathVariable("id") Long id) {
        contactDetailService.eliminarContactDetail(id);
        return ResponseEntity.noContent().build();
    }
}