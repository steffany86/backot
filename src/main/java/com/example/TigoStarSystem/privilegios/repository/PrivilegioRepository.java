package com.example.TigoStarSystem.privilegios.repository;

import com.example.TigoStarSystem.config.DbConnectionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PrivilegioRepository {
    private static final String DB_CENTRAL = "central";
    private final DbConnectionManager dbConnectionManager;

    public PrivilegioRepository(DbConnectionManager dbConnectionManager) {
        this.dbConnectionManager = dbConnectionManager;
    }

    public List<Map<String, Object>> listarRoles() {
        return queryForListCentral("EXEC dbo.spx_ObtenerPrivilegiosRoles");
    }

    public List<Map<String, Object>> obtenerPrivilegiosRolDetalle(Integer idRol) {
        return queryForListCentral(
                "EXEC dbo.spx_ObtenerPrivilegiosRolDetalle ?",
                idRol
        );
    }

    public List<Map<String, Object>> guardarPrivilegiosRol(Integer idRol, String menuIdsCsv) {
        return queryForListCentral(
                "EXEC dbo.spx_GuardarPrivilegiosRol ?, ?",
                idRol,
                menuIdsCsv
        );
    }

    private List<Map<String, Object>> queryForListCentral(String sql, Object... args) {
        JdbcTemplate central = dbConnectionManager.connDb(DB_CENTRAL);
        return queryForList(central, sql, args);
    }

    private List<Map<String, Object>> queryForList(JdbcTemplate template, String sql, Object... args) {
        if (args == null || args.length == 0) {
            return template.queryForList(sql);
        }
        return template.queryForList(sql, args);
    }
}
