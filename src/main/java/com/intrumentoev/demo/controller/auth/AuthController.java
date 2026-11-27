package com.intrumentoev.demo.controller.auth;

import com.intrumentoev.demo.exception.BusinessException;
import com.intrumentoev.demo.exception.ErrorCode;
import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.service.service.auth.AuthChallengeService;
import com.intrumentoev.demo.service.service.auth.AuthService;
import com.intrumentoev.demo.service.service.auth.BearerTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Autenticación y Seguridad",
        description = "Inicio de sesión de clientes bancarios (contraseña o biometría tipo Mercado Libre/Pago), gestión de refresh tokens y enrolamiento biométrico"
)
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Autowired(required = false)
    private AuthChallengeService authChallengeService;

    @Autowired(required = false)
    private BearerTokenValidator bearerTokenValidator;

    @Operation(
            summary = "Login con correo y contraseña",
            description = "Valida credenciales de acceso del cliente y devuelve token de acceso JWT (con su rol) y Refresh Token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "400", description = "Cuenta inactiva o formato de petición inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada por exceso de intentos fallidos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales de acceso para inicio de sesión de Alejandro Hernández",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AuthRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Credenciales Demo Alejandro Hernández",
                            summary = "Usuario Demo predeterminado",
                            value = "{\n  \"email\": \"alejandro.hernandez@banco-demo.com\",\n  \"password\": \"PasswordSegura#2026\"\n}"
                    )
            )
    )
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Login biométrico (Huella / Rostro tipo Mercado Libre)",
            description = "Autentica al cliente comparando la plantilla o firma biométrica registrada previamente (Touch ID / Face ID)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación biométrica exitosa"),
            @ApiResponse(responseCode = "400", description = "Cuenta inactiva o formato Base64 inválido"),
            @ApiResponse(responseCode = "401", description = "La firma biométrica no coincide o el cliente no tiene biometría"),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada por exceso de intentos fallidos")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales biométricas de Alejandro Hernández",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BiometricLoginRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Biometría Demo Alejandro Hernández",
                            summary = "Huella dactilar Demo predeterminada",
                            value = "{\n  \"email\": \"alejandro.hernandez@banco-demo.com\",\n  \"biometricType\": \"HUELLA\",\n  \"biometricData\": \"dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ==\"\n}"
                    )
            )
    )
    @PostMapping(
            value = "/login-biometrico",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> loginBiometrico(@Valid @RequestBody BiometricLoginRequest request) {
        AuthResponse response = authService.loginBiometrico(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Enrolamiento de biometría en la app móvil",
            description = "Permite al cliente registrar o actualizar sus datos biométricos (Huella o Facial) desde los ajustes de seguridad de la app."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Biometría registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cuenta inactiva o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PostMapping(
            value = "/biometria",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> registrarBiometria(
            @AuthenticationPrincipal AuthenticatedClient usuario,
            @Valid @RequestBody BiometricRegisterRequest request) {
        // Un cliente solo puede enrolar su propia biometría; un ADMIN puede hacerlo por cualquiera
        if (!usuario.isAdmin() && !usuario.getIdClient().equals(request.getIdClient())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        AuthResponse response = authService.registrarBiometria(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Renovación de sesión con Refresh Token",
            description = "Genera un nuevo token de acceso JWT y rota el Refresh Token sin requerir ingresar credenciales nuevamente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión renovada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token expirado o inválido")
    })
    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> refrescarToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refrescarToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cierre de sesión / Logout",
            description = "Revoca el Refresh Token del usuario dueño del JWT (Authorization: Bearer). "
                    + "El access token es stateless: expira solo al cumplirse su vigencia."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedClient usuario) {
        authService.logout(usuario.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Confirmar operación de pago (exclusivo en app móvil con huella digital)",
            description = "Recibe la firma biométrica de huella digital (HUELLA) del cliente autenticado, la valida contra su plantilla cifrada y devuelve un token de confirmación de vida corta y de un solo uso para enviar en el header X-Confirmation-Token de POST /v1/pagos. La confirmación por contraseña o desde web está prohibida (403 STRONG_AUTH_REQUIRED)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación confirmada con huella digital; devuelve confirmationToken"),
            @ApiResponse(responseCode = "401", description = "Huella inválida o sesión expirada"),
            @ApiResponse(responseCode = "403", description = "Intento de confirmar sin huella digital (ej. contraseña o web)"),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada por exceso de intentos fallidos")
    })
    @PostMapping(
            value = "/confirmar-operacion",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OperationConfirmResponse> confirmarOperacion(
            @AuthenticationPrincipal AuthenticatedClient usuario,
            @Valid @RequestBody OperationConfirmRequest request) {
        OperationConfirmResponse response = authService.confirmarOperacion(usuario, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar código OTP de 6 dígitos enviado por correo (Brevo)")
    @PostMapping(
            value = "/verificar-codigo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> verificarCodigo(@Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(authChallengeService.verificarCodigo(request));
    }

    @Operation(summary = "Enviar o reenviar código 2FA por correo electrónico (Brevo)")
    @PostMapping(
            value = "/2fa/enviar-codigo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthChallengeStatusResponse> enviarCodigo2fa(@Valid @RequestBody ChallengeActionRequest request) {
        return ResponseEntity.ok(authChallengeService.enviarCodigoPorCorreo(request));
    }

    @Operation(summary = "Solicitar autorización con huella digital en el celular Android para login Web")
    @PostMapping(
            value = "/2fa/solicitar-huella",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthChallengeStatusResponse> solicitarHuella2fa(@Valid @RequestBody ChallengeActionRequest request) {
        return ResponseEntity.ok(authChallengeService.solicitarHuellaMovil(request));
    }

    @Operation(summary = "Consultar estado del desafío 2FA (polling desde Web)")
    @PostMapping(
            value = "/2fa/estado",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthChallengeStatusResponse> consultarEstado2fa(@Valid @RequestBody ChallengeActionRequest request) {
        return ResponseEntity.ok(authChallengeService.consultarEstadoChallenge(request.getChallengeId(), request.getPollToken()));
    }

    @Operation(summary = "Cancelar desafío 2FA pendiente")
    @PostMapping(
            value = "/2fa/cancelar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthChallengeStatusResponse> cancelar2fa(@Valid @RequestBody ChallengeActionRequest request) {
        return ResponseEntity.ok(authChallengeService.cancelarChallenge(request));
    }

    @Operation(summary = "Listar solicitudes de inicio de sesión Web pendientes de aprobar con huella en móvil")
    @GetMapping(value = "/2fa/pendientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PendingLogin2faResponse>> listarPendientes2fa(
            @AuthenticationPrincipal AuthenticatedClient usuario) {
        return ResponseEntity.ok(authChallengeService.listarPendientesMovil(usuario));
    }

    @Operation(summary = "Aprobar inicio de sesión Web desde Android con huella digital (X-Confirmation-Token)")
    @PostMapping(value = "/2fa/{id}/aprobar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PendingLogin2faResponse> aprobarLogin2fa(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Confirmation-Token", required = false) String confirmationToken,
            @PathVariable("id") String id) {
        AuthenticatedClient cliente = bearerTokenValidator.validarParaPago(authorization, confirmationToken);
        return ResponseEntity.ok(authChallengeService.aprobarConHuellaMovil(cliente, id));
    }

    @Operation(summary = "Rechazar solicitud de inicio de sesión Web desde Android")
    @PostMapping(value = "/2fa/{id}/rechazar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PendingLogin2faResponse> rechazarLogin2fa(
            @AuthenticationPrincipal AuthenticatedClient usuario,
            @PathVariable("id") String id) {
        return ResponseEntity.ok(authChallengeService.rechazarDesdeMovil(usuario, id));
    }
}
