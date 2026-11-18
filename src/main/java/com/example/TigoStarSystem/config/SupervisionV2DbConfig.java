package com.example.TigoStarSystem.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Conexion del formulario Supervision V2 (/supervisor/supervision_v2).
 * Reemplaza a tigohogarJdbcTemplate: apunta al servidor nuevo (172.16.0.13).
 */
@Configuration
public class SupervisionV2DbConfig {

    @Bean
    @Qualifier("supervisionV2JdbcTemplate")
    public JdbcTemplate supervisionV2JdbcTemplate(
            @Value("${app.supervisionv2.datasource.url}") String url,
            @Value("${app.supervisionv2.datasource.username}") String username,
            @Value("${app.supervisionv2.datasource.password}") String password,
            @Value("${app.supervisionv2.datasource.driver-class-name:net.sourceforge.jtds.jdbc.Driver}") String driver,
            @Value("${app.supervisionv2.datasource.hikari.pool-name:SupervisionV2HikariPool}") String poolName,
            @Value("${app.supervisionv2.datasource.hikari.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${app.supervisionv2.datasource.hikari.minimum-idle:0}") int minimumIdle,
            @Value("${app.supervisionv2.datasource.hikari.connection-timeout:60000}") long connectionTimeout,
            @Value("${app.supervisionv2.datasource.hikari.validation-timeout:5000}") long validationTimeout,
            @Value("${app.supervisionv2.datasource.hikari.idle-timeout:300000}") long idleTimeout,
            @Value("${app.supervisionv2.datasource.hikari.max-lifetime:900000}") long maxLifetime,
            @Value("${app.supervisionv2.datasource.hikari.keepalive-time:120000}") long keepaliveTime) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(url.trim());
        config.setUsername(username.trim());
        config.setPassword(password);
        config.setDriverClassName(driver.trim());
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setValidationTimeout(validationTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        config.setInitializationFailTimeout(-1);
        config.setConnectionTestQuery("SELECT 1");
        JdbcTemplate template = new JdbcTemplate(new HikariDataSource(config));
        template.setQueryTimeout(300);
        return template;
    }
}
