package com.intrumentoev.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Value("${spring.flyway.locations:classpath:db}")
    private String[] locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String historyTable;

    @Value("${spring.flyway.schemas:public}")
    private String schema;

    @Bean(name = "flyway")
    public Flyway flyway(@Qualifier("sfDatasource") DataSource dataSource) {
        if (!flywayEnabled) {
            log.info("Flyway está deshabilitado en esta configuración (spring.flyway.enabled=false). Saltando migraciones.");
            return null;
        }

        log.info("Iniciando Flyway en schema '{}'", schema);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(historyTable)
                .schemas(schema)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        log.info("Ejecutando Flyway repair para sincronizar checksums...");
        flyway.repair();

        log.info("Ejecutando Flyway migrate...");
        flyway.migrate();

        return flyway;
    }
}
