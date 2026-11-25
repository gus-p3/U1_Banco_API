package com.intrumentoev.demo.service;

import com.intrumentoev.demo.client.gestopago.GestoPagoAuthClient;
import com.intrumentoev.demo.config.StartupConfigValidator;
import com.intrumentoev.demo.mapper.gestopago.GestoPagoTokenMapper;
import com.intrumentoev.demo.repository.gestopago.GestoPagoTokenRepository;
import com.intrumentoev.demo.service.impl.gestopago.GestoPagoTokenServiceImpl;
import com.intrumentoev.demo.service.service.auth.AesEncryptionService;
import com.intrumentoev.demo.service.service.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StartupConfigValidationTest {

    @Test
    @DisplayName("jwt.secret vacío o menor a 32 bytes falla al arrancar con mensaje claro")
    void jwtSecretInvalido() {
        JwtTokenService vacio = new JwtTokenService();
        ReflectionTestUtils.setField(vacio, "jwtSecret", "   ");
        assertThatThrownBy(vacio::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");

        JwtTokenService corto = new JwtTokenService();
        ReflectionTestUtils.setField(corto, "jwtSecret", "clave-demasiado-corta");
        assertThatThrownBy(corto::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");

        JwtTokenService valido = new JwtTokenService();
        ReflectionTestUtils.setField(valido, "jwtSecret", "clave-valida-de-al-menos-32-bytes-para-hs256!");
        assertThatCode(valido::validarConfiguracion).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("security.encryption.aes-key vacío falla al construir AesEncryptionService")
    void aesKeyVacia() {
        assertThatThrownBy(() -> new AesEncryptionService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.encryption.aes-key");
        assertThatThrownBy(() -> new AesEncryptionService(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.encryption.aes-key");
    }

    @Test
    @DisplayName("GestoPagoTokenServiceImpl falla al arrancar si faltan credenciales y no hay bearer-token fijo")
    void gestoPagoCredencialesFaltantes() {
        GestoPagoTokenServiceImpl service = new GestoPagoTokenServiceImpl(
                mock(GestoPagoAuthClient.class),
                mock(GestoPagoTokenRepository.class),
                mock(GestoPagoTokenMapper.class));

        ReflectionTestUtils.setField(service, "staticBearerToken", "");
        ReflectionTestUtils.setField(service, "idDistribuidor", null);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "DISP");
        ReflectionTestUtils.setField(service, "password", "secret");
        assertThatThrownBy(service::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gestopago.auth.id-distribuidor");

        ReflectionTestUtils.setField(service, "idDistribuidor", 83);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "");
        assertThatThrownBy(service::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gestopago.auth.codigo-dispositivo");

        ReflectionTestUtils.setField(service, "codigoDispositivo", "DISP");
        ReflectionTestUtils.setField(service, "password", " ");
        assertThatThrownBy(service::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gestopago.auth.password");

        ReflectionTestUtils.setField(service, "password", "secret");
        assertThatCode(service::validarConfiguracion).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("StartupConfigValidator exige contraseña cuando el driver es PostgreSQL")
    void postgresPasswordObligatorio() {
        StartupConfigValidator sinPass = new StartupConfigValidator("org.postgresql.Driver", "");
        assertThatThrownBy(sinPass::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password");

        StartupConfigValidator conPass = new StartupConfigValidator("org.postgresql.Driver", "secret");
        assertThatCode(conPass::validate).doesNotThrowAnyException();

        StartupConfigValidator h2SinPass = new StartupConfigValidator("org.h2.Driver", "");
        assertThatCode(h2SinPass::validate).doesNotThrowAnyException();
    }
}

