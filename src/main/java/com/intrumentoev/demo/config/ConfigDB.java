package com.intrumentoev.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.intrumentoev.demo.repository",
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {

    @Autowired
    private Environment env;

    @Bean(name = "sfDatasource")
    public DataSource sfDatasource() {
        HikariConfig config = new HikariConfig();
        try {
            String jdbcUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            // Soporte automático para DATABASE_URL de Railway / Heroku (postgresql:// o postgres://)
            String rawDatabaseUrl = env.getProperty("DATABASE_URL");
            if ((jdbcUrl == null || jdbcUrl.contains("localhost:5432")) && rawDatabaseUrl != null && !rawDatabaseUrl.isBlank()) {
                try {
                    if (rawDatabaseUrl.startsWith("postgres://") || rawDatabaseUrl.startsWith("postgresql://")) {
                        java.net.URI dbUri = new java.net.URI(rawDatabaseUrl);
                        String userInfo = dbUri.getUserInfo();
                        if (userInfo != null && userInfo.contains(":")) {
                            String[] parts = userInfo.split(":", 2);
                            username = parts[0];
                            password = parts[1];
                        }
                        int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;
                        String path = dbUri.getPath();
                        jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + path;
                        log.info("ConfigDB: Detectada DATABASE_URL de Railway. URL JDBC configurada: jdbc:postgresql://{}:{}{}", dbUri.getHost(), port, path);
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo parsear DATABASE_URL ({}), manteniendo configuración: {}", rawDatabaseUrl, ex.getMessage());
                }
            }

            config.setJdbcUrl(jdbcUrl);
            config.setPassword(password);
            config.setUsername(username);
            config.setMaximumPoolSize(10);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(30000);
            config.setValidationTimeout(5000);
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");

        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexión a la base de datos, a causa de: ", e);
            throw new IllegalStateException("Fallo al inicializar sfDatasource: " + e.getMessage(), e);
        }
        return new HikariDataSource(config);
    }

    @Bean(name = "sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        try {
            em.setDataSource(sfDatasource());
            em.setPackagesToScan("com.intrumentoev.demo.entity");
            em.setPersistenceUnitName("sfDatasource");

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties = new HashMap<>();
            String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "none");
            String dialect = env.getProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("hibernate.hbm2ddl.auto", ddlAuto);
            properties.put("hibernate.show-sql", false);
            properties.put("hibernate.dialect", dialect);
            properties.put("jakarta.persistence.query.timeout", 600000);

            em.setJpaPropertyMap(properties);

        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexión a la base de datos, a causa de: ", e);
            return null;
        }
        return em;
    }

    @Bean(name = "sfTransactionManager")
    public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory) {
        return new JpaTransactionManager(sfEntityManagerFactory);
    }
}