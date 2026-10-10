package com.intrumentoev.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema Bancario - Onboarding de Clientes Personas Físicas API")
                        .version("1.0.0")
                        .description("""
                                API REST empresarial construida con **Spring Boot 3.5 y Java 21**, implementando:
                                * **Arquitectura Limpia y Principios SOLID**: Separación estricta de responsabilidades entre capas.
                                * **Directrices Microsoft REST API Guidelines**: Formato homogéneo de recursos, códigos HTTP estándar y estructura de error `{ error: { code, message, target, details } }`.
                                * **Onboarding Integral**: Alta atómica de datos personales, contacto, domicilio, laborales y apertura automática de cuenta bancaria activa.
                                * **Estrategia Híbrida de Caché (Redis + PostgreSQL + INEGI)**: Disponibilidad offline/alta concurrencia para Progressive Web Apps (PWA).
                                """)
                        .contact(new Contact()
                                .name("Equipo de Desarrollo Bancario")
                                .email("soporte@banco.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("https://u1bancoapi-production.up.railway.app")
                                .description("Servidor de Producción (Railway)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Local de Desarrollo")
                ));
    }
}
