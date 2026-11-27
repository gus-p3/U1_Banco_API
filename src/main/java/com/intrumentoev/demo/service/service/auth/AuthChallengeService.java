package com.intrumentoev.demo.service.service.auth;

import com.intrumentoev.demo.entity.auth.Auth;
import com.intrumentoev.demo.model.auth.*;

import java.util.List;

public interface AuthChallengeService {

    AuthResponse crearDesafioVerificacionCuenta(Auth auth, String nombreCliente);

    AuthResponse iniciarDesafioLogin(Auth auth, String channel);

    AuthChallengeStatusResponse enviarCodigoPorCorreo(ChallengeActionRequest request);

    AuthChallengeStatusResponse solicitarHuellaMovil(ChallengeActionRequest request);

    AuthResponse verificarCodigo(VerifyCodeRequest request);

    AuthChallengeStatusResponse consultarEstadoChallenge(String challengeId, String pollToken);

    AuthChallengeStatusResponse cancelarChallenge(ChallengeActionRequest request);

    List<PendingLogin2faResponse> listarPendientesMovil(AuthenticatedClient usuario);

    PendingLogin2faResponse aprobarConHuellaMovil(AuthenticatedClient usuario, String challengeId);

    PendingLogin2faResponse rechazarDesdeMovil(AuthenticatedClient usuario, String challengeId);
}
