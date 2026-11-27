package com.intrumentoev.demo.service.impl.gestopago;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.intrumentoev.demo.entity.auth.AuthChallenge;
import com.intrumentoev.demo.entity.gestopago.DispositivoPush;
import com.intrumentoev.demo.entity.gestopago.PagoAutorizacion;
import com.intrumentoev.demo.repository.gestopago.DispositivoPushRepository;
import com.intrumentoev.demo.service.service.gestopago.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de notificaciones push para autorización remota de pagos (Web -> Android con huella).
 * Soporta tokens nativos FCM (vía Firebase Admin SDK) y tokens de Expo Push Service.
 * Si no hay credenciales de Firebase configuradas, degrada limpiamente sin interrumpir el flujo
 * (el dispositivo móvil igualmente detectará la autorización pendiente al consultar el API).
 */
@Service
@Slf4j
public class FirebasePushNotificationServiceImpl implements PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final DispositivoPushRepository dispositivoRepository;
    private final RestTemplate restTemplate;
    private final String serviceAccountJson;
    private final String credentialsPath;

    private volatile boolean firebaseInitialized;

    public FirebasePushNotificationServiceImpl(
            DispositivoPushRepository dispositivoRepository,
            @Value("${firebase.service-account-json:}") String serviceAccountJson,
            @Value("${firebase.credentials-path:}") String credentialsPath) {
        this.dispositivoRepository = dispositivoRepository;
        this.restTemplate = new RestTemplate();
        this.serviceAccountJson = serviceAccountJson;
        this.credentialsPath = credentialsPath;
        inicializarFirebaseSiAplica();
    }

    private void inicializarFirebaseSiAplica() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                firebaseInitialized = true;
                return;
            }
            InputStream stream = resolverCredenciales();
            if (stream == null) {
                log.info("Firebase Admin no configurado (defina FIREBASE_SERVICE_ACCOUNT_JSON o FIREBASE_CREDENTIALS_PATH para push nativo FCM).");
                return;
            }
            try (InputStream is = stream) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(is))
                        .build();
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                log.info("Firebase Admin SDK inicializado correctamente para notificaciones FCM.");
            }
        } catch (Exception e) {
            log.warn("No fue posible inicializar Firebase Admin SDK: {}", e.getMessage());
        }
    }

    private InputStream resolverCredenciales() throws Exception {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.trim().getBytes(StandardCharsets.UTF_8));
        }
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            return new FileInputStream(credentialsPath.trim());
        }
        return null;
    }

    @Override
    public void enviarNotificacionAutorizacionPago(Long idCliente, PagoAutorizacion autorizacion) {
        List<DispositivoPush> dispositivos = dispositivoRepository.findByIdClienteAndActivoTrue(idCliente);
        if (dispositivos.isEmpty()) {
            log.debug("Cliente {} sin dispositivos push registrados; la app móvil recibirá la solicitud en vivo.", idCliente);
            return;
        }
        String titulo = "Autoriza tu pago";
        String cuerpo = String.format("Confirma con tu huella el pago de $%s en %s",
                autorizacion.getMonto().toPlainString(),
                autorizacion.getNombreServicio() != null ? autorizacion.getNombreServicio() : "Banco Digital");

        for (DispositivoPush dispositivo : dispositivos) {
            String token = dispositivo.getPushToken();
            if (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[")) {
                enviarExpoPush(token, titulo, cuerpo, autorizacion);
            } else {
                enviarFcm(dispositivo, titulo, cuerpo, autorizacion);
            }
        }
    }

    @Override
    public void enviarNotificacionLogin2fa(Long idCliente, AuthChallenge challenge) {
        List<DispositivoPush> dispositivos = dispositivoRepository.findByIdClienteAndActivoTrue(idCliente);
        if (dispositivos.isEmpty()) {
            log.debug("Cliente {} sin dispositivos push registrados para login 2FA.", idCliente);
            return;
        }
        String titulo = "Autoriza tu inicio de sesión en Web";
        String cuerpo = "Confirma con tu huella digital el acceso a Banco Digital Web (" + challenge.getEmail() + ").";

        for (DispositivoPush dispositivo : dispositivos) {
            String token = dispositivo.getPushToken();
            if (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[")) {
                enviarExpoPushLogin(token, titulo, cuerpo, challenge);
            } else {
                enviarFcmLogin(dispositivo, titulo, cuerpo, challenge);
            }
        }
    }

    private void enviarFcmLogin(DispositivoPush dispositivo, String titulo, String cuerpo, AuthChallenge challenge) {
        if (!firebaseInitialized) {
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(dispositivo.getPushToken())
                    .setNotification(Notification.builder().setTitle(titulo).setBody(cuerpo).build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder().setChannelId("payment-auth").build())
                            .build())
                    .putData("type", "LOGIN_2FA")
                    .putData("challengeId", challenge.getId())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception ex) {
            log.warn("Error enviando FCM para login 2FA {}: {}", challenge.getId(), ex.getMessage());
        }
    }

    private void enviarExpoPushLogin(String token, String titulo, String cuerpo, AuthChallenge challenge) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("to", token);
            body.put("title", titulo);
            body.put("body", cuerpo);
            body.put("sound", "default");
            body.put("priority", "high");
            body.put("channelId", "payment-auth");
            body.put("data", Map.of("type", "LOGIN_2FA", "challengeId", challenge.getId()));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(EXPO_PUSH_URL, new HttpEntity<>(body, headers), String.class);
        } catch (Exception ex) {
            log.warn("Error enviando Expo Push para login 2FA {}: {}", challenge.getId(), ex.getMessage());
        }
    }

    private void enviarFcm(DispositivoPush dispositivo, String titulo, String cuerpo, PagoAutorizacion auth) {
        if (!firebaseInitialized) {
            log.debug("FCM omitido (Firebase Admin no inicializado) para autorización {}", auth.getId());
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(dispositivo.getPushToken())
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("payment-auth")
                                    .setClickAction("OPEN_PAYMENT_AUTH")
                                    .build())
                            .build())
                    .putData("type", "PAYMENT_AUTHORIZATION")
                    .putData("autorizacionId", auth.getId())
                    .putData("monto", auth.getMonto().toPlainString())
                    .putData("servicio", auth.getNombreServicio() != null ? auth.getNombreServicio() : "")
                    .build();
            String msgId = FirebaseMessaging.getInstance().send(message);
            log.info("Notificación FCM enviada (messageId={}) para autorización {}", msgId, auth.getId());
        } catch (FirebaseMessagingException ex) {
            log.warn("Error FCM al enviar notificación para autorización {}: {}", auth.getId(), ex.getMessage());
            if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                dispositivo.setActivo(Boolean.FALSE);
                dispositivoRepository.save(dispositivo);
            }
        } catch (Exception ex) {
            log.warn("Error inesperado enviando FCM para autorización {}: {}", auth.getId(), ex.getMessage());
        }
    }

    private void enviarExpoPush(String token, String titulo, String cuerpo, PagoAutorizacion auth) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("to", token);
            body.put("title", titulo);
            body.put("body", cuerpo);
            body.put("sound", "default");
            body.put("priority", "high");
            body.put("channelId", "payment-auth");
            body.put("data", Map.of(
                    "type", "PAYMENT_AUTHORIZATION",
                    "autorizacionId", auth.getId(),
                    "monto", auth.getMonto().toPlainString()
            ));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(EXPO_PUSH_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Notificación Expo Push enviada para autorización {}", auth.getId());
        } catch (Exception ex) {
            log.warn("Error enviando Expo Push para autorización {}: {}", auth.getId(), ex.getMessage());
        }
    }
}
