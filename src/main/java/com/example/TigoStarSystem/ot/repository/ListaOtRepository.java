package com.example.TigoStarSystem.ot.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class ListaOtRepository {
    private final JdbcTemplate jdbcTemplate;
    private final OtDbSupport dbSupport;

    public ListaOtRepository(
            @Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate,
            SucursalRepository sucursalRepository,
            @Value("${spring.datasource.driver-class-name}") String dbDriver,
            @Value("${spring.datasource.url}") String mainDatasourceUrl,
            @Value("${app.sucre.datasource.url:}") String sucreDatasourceUrl,
            @Value("${auth.login.sucre.database:SucrePrueba}") String sucreDatabase,
            @Value("${app.sucre.datasource.username:${spring.datasource.username}}") String sucreUsername,
            @Value("${app.sucre.datasource.password:${spring.datasource.password}}") String sucrePassword,
            @Value("${app.datasource.params:encrypt=false;trustServerCertificate=true}") String dbParams) {
        this.jdbcTemplate = centralJdbcTemplate;
        this.dbSupport = new OtDbSupport(
                sucursalRepository,
                dbDriver,
                mainDatasourceUrl,
                sucreDatasourceUrl,
                sucreDatabase,
                sucreUsername,
                sucrePassword,
                dbParams
        );
    }

    public List<Map<String, Object>> listarPorFecha(LocalDate fecha, String tecnico, Integer idSucursal) {
        JdbcTemplate template = template(idSucursal);
        Date fechaSql = Date.valueOf(fecha);
        String tecnicoParam = isBlank(tecnico) ? null : tecnico.trim();

        try {
            return template.queryForList(
                    "EXEC dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO ?, ?",
                    fechaSql,
                    tecnicoParam
            );
        } catch (DataAccessException ex) {
            if (!shouldFallback(ex)) {
                throw ex;
            }
        }

        try {
            return template.queryForList(
                    "EXEC dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO ?",
                    fechaSql
            );
        } catch (DataAccessException ex) {
            if (!shouldFallback(ex)) {
                throw ex;
            }
        }

        return template.queryForList(
                "EXEC dbo.sp_ObtenerListaOrdenesTrabajo ?",
                fechaSql
        );
    }

    private JdbcTemplate template(Integer idSucursal) {
        return dbSupport.resolveTemplate(idSucursal, jdbcTemplate);
    }

    private boolean shouldFallback(DataAccessException ex) {
        Throwable root = ex.getMostSpecificCause();
        String message = root == null ? ex.getMessage() : root.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("esperaba el parametro")
                || normalized.contains("esperaba el parámetro")
                || normalized.contains("expects the parameter")
                || normalized.contains("expects parameter")
                || normalized.contains("too many arguments")
                || normalized.contains("demasiados argumentos")
                || normalized.contains("could not find stored procedure")
                || normalized.contains("no se encontro el procedimiento")
                || normalized.contains("no se encontró el procedimiento")
                || normalized.contains("no se encuentra el procedimiento")
                || normalized.contains("invalid object name")
                || normalized.contains("nombre de objeto no valido")
                || normalized.contains("nombre de objeto no válido");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
