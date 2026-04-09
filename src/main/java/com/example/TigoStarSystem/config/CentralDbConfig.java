package com.example.TigoStarSystem.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
public class CentralDbConfig {
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Qualifier("centralJdbcTemplate")
    public JdbcTemplate centralJdbcTemplate(
            @Value("${app.central.datasource.url}") String centralUrl,
            @Value("${app.central.datasource.username}") String centralUsername,
            @Value("${app.central.datasource.password}") String centralPassword,
            @Value("${app.central.datasource.driver-class-name}") String centralDriver,
            @Value("${app.central.datasource.hikari.pool-name:CentralHikariPool}") String poolName,
            @Value("${app.central.datasource.hikari.maximum-pool-size:${spring.datasource.hikari.maximum-pool-size:10}}") int maximumPoolSize,
            @Value("${app.central.datasource.hikari.minimum-idle:${spring.datasource.hikari.minimum-idle:2}}") int minimumIdle,
            @Value("${app.central.datasource.hikari.connection-timeout:${spring.datasource.hikari.connection-timeout:15000}}") long connectionTimeout,
            @Value("${app.central.datasource.hikari.validation-timeout:${spring.datasource.hikari.validation-timeout:5000}}") long validationTimeout,
            @Value("${app.central.datasource.hikari.idle-timeout:${spring.datasource.hikari.idle-timeout:300000}}") long idleTimeout,
            @Value("${app.central.datasource.hikari.max-lifetime:${spring.datasource.hikari.max-lifetime:900000}}") long maxLifetime,
            @Value("${app.central.datasource.hikari.keepalive-time:${spring.datasource.hikari.keepalive-time:120000}}") long keepaliveTime,
            @Value("${app.central.datasource.hikari.initialization-fail-timeout:${spring.datasource.hikari.initialization-fail-timeout:-1}}") long initializationFailTimeout,
            @Value("${app.central.datasource.hikari.connection-test-query:${spring.datasource.hikari.connection-test-query:SELECT 1}}") String connectionTestQuery) {
        String url = requireValue("app.central.datasource.url", centralUrl);
        String username = requireValue("app.central.datasource.username", centralUsername);
        String password = requireValue("app.central.datasource.password", centralPassword);
        String driver = requireValue("app.central.datasource.driver-class-name", centralDriver);

        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setValidationTimeout(validationTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        config.setInitializationFailTimeout(initializationFailTimeout);
        config.setConnectionTestQuery(connectionTestQuery);
        return new JdbcTemplate(new HikariDataSource(config));
    }

    private String requireValue(String propertyName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Propiedad requerida no configurada: " + propertyName);
        }
        return value.trim();
    }
}
