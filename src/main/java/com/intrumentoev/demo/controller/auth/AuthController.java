package com.intrumentoev.demo.controller.auth;

import com.intrumentoev.demo.model.auth.*;
import com.intrumentoev.demo.service.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private final com.intrumentoev.demo.service.service.auth.ServerSessionManager serverSessionManager;

    @Operation(
            summary = "Consultar estado del booleano del servidor y contador de inactividad",
            description = "Devuelve si el usuario tiene sesión activa en el servidor (login = true/false), contador de segundos de inactividad y segundos restantes antes del timeout de 5 minutos (300 segundos)."
    )
    @GetMapping(
            value = {"/session-status", "/estado-servidor"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServerSessionStatusResponse> obtenerEstadoSesionServidor() {
        ServerSessionStatusResponse status = serverSessionManager.getSessionStatus();
        return ResponseEntity.ok(status);
    }

    @Operation(
            summary = "Login con correo y contraseña",
            description = "Valida credenciales de acceso del cliente y devuelve token de acceso JWT y Refresh Token."
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
    public ResponseEntity<AuthResponse> registrarBiometria(@Valid @RequestBody BiometricRegisterRequest request) {
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
            description = "Invalida y revoca el Refresh Token activo de la cuenta del cliente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente")
    })
    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logout(
            @RequestParam("email")
            @NotBlank(message = "El correo electrónico es requerido para cerrar sesión")
            @Email(message = "El formato del correo electrónico proporcionado para logout no es válido")
            String email) {
        authService.logout(email);
        return ResponseEntity.noContent().build();
    }
}
