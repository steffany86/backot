package com.example.TigoStarSystem.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class SucursalRepository {
    private static final Logger logger = LoggerFactory.getLogger(SucursalRepository.class);
    private static final String SP_SUCURSALES_WEB_DBO = "EXEC dbo.spx_ObtenerSucursalesConexionWeb";
    private static final String SP_SUCURSALES_WEB = "EXEC spx_ObtenerSucursalesConexionWeb";
    private final JdbcTemplate jdbcTemplate;

    public SucursalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> obtenerSucursales() {
        List<Map<String, Object>> rows = obtenerSucursalesSp();
        completarTecnologia(rows);
        return rows;
    }

    private List<Map<String, Object>> obtenerSucursalesSp() {
        try {
            return jdbcTemplate.queryForList(SP_SUCURSALES_WEB_DBO);
        } catch (DataAccessException ex) {
            if (!isMissingStoredProcedure(ex)) {
                logger.warn("Fallo al ejecutar {}.", SP_SUCURSALES_WEB_DBO, ex);
                throw ex;
            }
        }
        try {
            return jdbcTemplate.queryForList(SP_SUCURSALES_WEB);
        } catch (DataAccessException ex) {
            logger.warn("Fallo al ejecutar {}.", SP_SUCURSALES_WEB, ex);
            throw ex;
        }
    }

    /**
     * Si el SP no devuelve la columna Tecnologia (HFC/DTH), la completa desde tbl_sucursal.
     * Si tampoco se puede leer, deja las filas sin tecnologia (no rompe el login).
     */
    private void completarTecnologia(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean yaTrae = rows.get(0).keySet().stream().anyMatch(k -> "tecnologia".equalsIgnoreCase(k));
        if (yaTrae) {
            return;
        }
        try {
            Map<Integer, Object> porId = new java.util.HashMap<>();
            for (Map<String, Object> r : jdbcTemplate.queryForList("SELECT Id_Sucursal, Tecnologia FROM tbl_Sucursal")) {
                Object id = r.get("Id_Sucursal");
                if (id instanceof Number) {
                    porId.put(((Number) id).intValue(), r.get("Tecnologia"));
                }
            }
            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, Object> e : row.entrySet()) {
                    String k = e.getKey();
                    if ("id_sucursal".equalsIgnoreCase(k) || "idsucursal".equalsIgnoreCase(k)) {
                        if (e.getValue() instanceof Number) {
                            row.put("Tecnologia", porId.get(((Number) e.getValue()).intValue()));
                        }
                        break;
                    }
                }
            }
        } catch (DataAccessException ex) {
            logger.warn("No se pudo leer Tecnologia desde tbl_Sucursal.", ex);
        }
    }

    private boolean isMissingStoredProcedure(DataAccessException ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) root;
            if (sqlEx.getErrorCode() == 2812) {
                return true;
            }
            String msg = sqlEx.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                return lower.contains("procedimiento almacenado") && lower.contains("no se encontr");
            }
        }
        return false;
    }
}
