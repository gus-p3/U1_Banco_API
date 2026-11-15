package com.intrumentoev.demo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida al arrancar que las variables de entorno obligatorias de base de datos
 * estén configuradas cuando se utiliza PostgreSQL, evitando arrancar con valores vacíos o débiles.
 */
@Component
public class StartupConfigValidator {

    private final String driverClassName;
    private final String datasourcePassword;

    public StartupConfigValidator(
            @Value("${spring.datasource.driver-class-name:}") String driverClassName,
            @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.driverClassName = driverClassName;
        this.datasourcePassword = datasourcePassword;
    }

    @PostConstruct
    public void validate() {
        if (driverClassName != null
                && driverClassName.toLowerCase().contains("postgresql")
                && (datasourcePassword == null || datasourcePassword.isBlank())) {
            throw new IllegalStateException(
                    "Falta configurar la propiedad obligatoria spring.datasource.password (variables de entorno SPRING_DATASOURCE_PASSWORD o PGPASSWORD)");
        }
    }
}

