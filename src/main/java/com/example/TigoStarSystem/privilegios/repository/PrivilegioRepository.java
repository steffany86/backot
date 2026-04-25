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

    public List<Map<String, Object>> guardarPaginasPorMenu(Integer idMenu, String paginasCsv) {
        return queryForListCentral(
                "EXEC dbo.spx_GuardarPaginasPorMenu ?, ?",
                idMenu,
                paginasCsv
        );
    }

    public List<Map<String, Object>> guardarNombreSidebarPorMenu(Integer idMenu, String nombreSidebar) {
        JdbcTemplate central = centralTemplate();
        central.update(
                "UPDATE dbo.tbl_tablamenu " +
                        "SET nombre_sidebar = ? " +
                        "WHERE Id = ? AND ISNULL(e_eliminado, 0) = 0;",
                nombreSidebar,
                idMenu
        );
        return queryForList(
                central,
                "SELECT Id AS Id_Menu, nombre AS Nombre, nombre_sidebar AS NombreSidebar " +
                        "FROM dbo.tbl_tablamenu " +
                        "WHERE Id = ? AND ISNULL(e_eliminado, 0) = 0;",
                idMenu
        );
    }

    private List<Map<String, Object>> queryForListCentral(String sql, Object... args) {
        return queryForList(centralTemplate(), sql, args);
    }

    private JdbcTemplate centralTemplate() {
        return dbConnectionManager.connDb(DB_CENTRAL);
    }

    private List<Map<String, Object>> queryForList(JdbcTemplate template, String sql, Object... args) {
        if (args == null || args.length == 0) {
            return template.queryForList(sql);
        }
        return template.queryForList(sql, args);
    }
}
