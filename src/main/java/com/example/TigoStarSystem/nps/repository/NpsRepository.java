package com.example.TigoStarSystem.nps.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class NpsRepository {

    public List<Map<String, Object>> listarSupervisoresSucursal(JdbcTemplate sucursalTemplate, Integer idSucursal) {
        return sucursalTemplate.queryForList("EXEC dbo.SP_NPS_LISTAR_SUPERVISORES_SUCURSAL ?", idSucursal);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisor(JdbcTemplate sucursalTemplate, Integer idSucursal, Integer idSupervisor) {
        return sucursalTemplate.queryForList("EXEC dbo.SP_NPS_LISTAR_TECNICOS_POR_SUPERVISOR ?, ?", idSucursal, idSupervisor);
    }

    public List<Map<String, Object>> listarTecnicosNps(JdbcTemplate sucursalTemplate, Integer idUsuarioSesion, Integer idSucursal, Integer idSupervisor) {
        return sucursalTemplate.queryForList("EXEC dbo.SP_tecnicos_NPS ?, ?, ?", idUsuarioSesion, idSucursal, idSupervisor);
    }

    public List<Map<String, Object>> listarTecnicosDeSupervisorEnCentral(JdbcTemplate centralTemplate, Integer idSupervisor, Integer idSucursal) {
        return centralTemplate.queryForList("EXEC dbo.SP_NPS_LISTAR_TECNICOS_SUPERVISOR_CENTRAL ?, ?", idSupervisor, idSucursal);
    }

    public List<Map<String, Object>> listarTecnicosHistoricosSupervisorNps(JdbcTemplate centralTemplate, Integer idSupervisor, Integer idSucursal) {
        return centralTemplate.queryForList("EXEC dbo.SP_NPS_LISTAR_TECNICOS_SUPERVISOR_HISTORICO ?, ?", idSupervisor, idSucursal);
    }

    public List<Map<String, Object>> obtenerDashboard(
            JdbcTemplate centralTemplate,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idSucursal,
            Integer idSupervisor,
            Integer idTecnico,
            String supervisorNombre,
            String tecnicoNombre,
            String rolConsulta,
            Integer idUsuarioSesion) {
        Date fechaInicioSql = fechaInicio == null ? null : Date.valueOf(fechaInicio);
        Date fechaFinSql = fechaFin == null ? null : Date.valueOf(fechaFin);
        return centralTemplate.queryForList(
                "EXEC dbo.SP_NPS_DASHBOARD_CONSULTA ?, ?, ?, ?, ?, ?, ?, ?, ?",
                fechaInicioSql,
                fechaFinSql,
                idSucursal,
                idSupervisor,
                idTecnico,
                supervisorNombre,
                tecnicoNombre,
                rolConsulta,
                idUsuarioSesion
        );
    }

    public List<Map<String, Object>> listarFiltrosCentralPorNombres(JdbcTemplate centralTemplate, Integer idSucursal) {
        return centralTemplate.queryForList("EXEC dbo.SP_NPS_FILTROS_CENTRAL_NOMBRES ?", idSucursal);
    }

    public List<Map<String, Object>> listarNombresTecnicosNpsCentral(JdbcTemplate centralTemplate, Integer idSucursal) {
        return centralTemplate.queryForList(
                "DECLARE @SucursalNombre NVARCHAR(120) = CASE ? " +
                        "WHEN 9 THEN 'SANTA CRUZ CENTRAL' WHEN 20 THEN 'SANTA CRUZ' WHEN 4 THEN 'SUCRE' WHEN 7 THEN 'TARIJA' WHEN 2 THEN 'YACUIBA' WHEN 15 THEN 'RIBERALTA' WHEN 19 THEN 'MONTERO' " +
                        "WHEN 5 THEN 'CAMIRI' WHEN 10 THEN 'CHIQUITANIA' WHEN 16 THEN 'COBIJA' WHEN 12 THEN 'IVIRGARZAMA' WHEN 6 THEN 'PUERTO SUAREZ' WHEN 11 THEN 'SAN IGNACIO' WHEN 17 THEN 'TRINIDAD' WHEN 14 THEN 'YAPACANI' " +
                        "ELSE NULL END; " +
                        "SELECT DISTINCT LTRIM(RTRIM(r.tecnico_nombre)) AS nombre " +
                        "FROM dbo.tbl_NPS_RESPUESTAS_MAKIRO r " +
                        "CROSS APPLY (SELECT UPPER(LTRIM(RTRIM(ISNULL(r.ciudad, '')))) ciudad_norm, UPPER(LTRIM(RTRIM(ISNULL(r.ciudad_siga, '')))) ciudad_siga_norm, UPPER(LTRIM(RTRIM(ISNULL(r.departamento_siga, '')))) departamento_siga_norm) n " +
                        "WHERE ISNULL(LTRIM(RTRIM(r.tecnico_nombre)), '') <> '' " +
                        "AND (? IS NULL OR @SucursalNombre IS NULL OR n.ciudad_norm LIKE '%' + @SucursalNombre + '%' OR n.ciudad_siga_norm LIKE '%' + @SucursalNombre + '%' OR n.departamento_siga_norm LIKE '%' + @SucursalNombre + '%')",
                idSucursal,
                idSucursal
        );
    }

    public List<Map<String, Object>> obtenerDashboardInvitado(
            JdbcTemplate centralTemplate,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idSucursal,
            Integer idSupervisor,
            Integer idTecnico,
            String supervisorNombre,
            String tecnicoNombre) {
        return centralTemplate.queryForList(
                "EXEC dbo.spx_ObtenerNpsInvitaciones ?, ?, ?, ?",
                idSucursal,
                tecnicoNombre,
                idSupervisor,
                supervisorNombre
        );
    }

    public List<Integer> listarIdsTecnicoNpsPorUsuario(JdbcTemplate sucursalTemplate, Integer idUsuario) {
        List<Integer> out = new ArrayList<Integer>();
        String[] sqls = new String[] {
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_UsuarioTecnico ut WHERE ut.id_usuario = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_UsuarioTecnico ut WHERE ut.id_tecnico = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_UsuarioTecnico ut WHERE ut.Id_Tecnico = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_UsuarioTecnico ut WHERE ut.Id_Usuario = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_usuariotecnico ut WHERE ut.id_usuario = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_usuariotecnico ut WHERE ut.id_tecnico = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_usuaritecnico ut WHERE ut.id_usuario = ? AND ut.id_vendedor IS NOT NULL",
                "SELECT DISTINCT ut.id_vendedor AS idTecnicoNps FROM dbo.tbl_usuaritecnico ut WHERE ut.id_tecnico = ? AND ut.id_vendedor IS NOT NULL"
        };
        for (String sql : sqls) {
            try {
                List<Map<String, Object>> rows = sucursalTemplate.queryForList(sql, idUsuario);
                for (Map<String, Object> row : rows) {
                    Object value = row.get("idTecnicoNps");
                    if (value instanceof Number) {
                        out.add(((Number) value).intValue());
                        continue;
                    }
                    if (value != null) {
                        try {
                            out.add(Integer.parseInt(String.valueOf(value).trim()));
                        } catch (Exception ignore) {
                            // skip invalid ids
                        }
                    }
                }
                if (!out.isEmpty()) break;
            } catch (DataAccessException ex) {
                // Try next variant for schema compatibility.
            }
        }
        return out;
    }

    public String obtenerNombreNpsVendedor(JdbcTemplate sucursalTemplate, Integer idVendedor) {
        if (sucursalTemplate == null || idVendedor == null) return null;
        try {
            List<Map<String, Object>> rows = sucursalTemplate.queryForList(
                    "SELECT TOP 1 NULLIF(LTRIM(RTRIM(CAST(NombreNPS AS NVARCHAR(200)))), '') AS nombreNps " +
                            "FROM dbo.tbl_Vendedor " +
                            "WHERE Id_Vendedor = ? AND ISNULL(E_Eliminado, 0) = 0",
                    idVendedor
            );
            if (rows == null || rows.isEmpty()) return null;
            Object value = rows.get(0).get("nombreNps");
            if (value == null) return null;
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : text;
        } catch (DataAccessException ex) {
            return null;
        }
    }
}
