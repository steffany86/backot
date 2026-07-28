package com.example.TigoStarSystem.centralgrupos.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class CentralGruposRepository {

    public List<Map<String, Object>> listarGrupos(JdbcTemplate template, Integer idUsuarioEjecutor) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_ListarCentral ?",
                idUsuarioEjecutor
        );
    }

    public List<Map<String, Object>> listarSupervisoresFiltro(JdbcTemplate template) {
        return deduplicarSupervisores(template.queryForList("EXEC dbo.spx_Grupo_FiltroSupervisoresCentral"));
    }

    public List<Map<String, Object>> listarSupervisoresDesdeConformacionCentral(JdbcTemplate centralTemplate, String sucursal) {
        return deduplicarSupervisores(centralTemplate.queryForList(
                "SELECT " +
                        "  CAST(c.idUsuarioSupervisor AS INT) AS idUsuarioSupervisor, " +
                        "  CAST(c.idUsuarioSupervisor AS INT) AS id_usuario_supervisor, " +
                        "  LTRIM(RTRIM(CAST(MAX(NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo, ''))), '')) AS NVARCHAR(200)))) AS supervisorACargo, " +
                        "  LTRIM(RTRIM(CAST(MAX(NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo, ''))), '')) AS NVARCHAR(200)))) AS supervisor, " +
                        "  LTRIM(RTRIM(CAST(MIN(c.sucursal) AS NVARCHAR(100)))) AS sucursal " +
                        "FROM dbo.tbl_ConformacionCuadrillaDiario c " +
                        "INNER JOIN dbo.tbl_Usuario u ON u.Id_Usuario = c.idUsuarioSupervisor AND ISNULL(u.E_Eliminado, 0) = 0 " +
                        "WHERE ISNULL(c.e_eliminado, 0) = 0 " +
                        "  AND c.idUsuarioSupervisor IS NOT NULL " +
                        "  AND c.idUsuarioSupervisor > 0 " +
                        "  AND NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo, ''))), '') IS NOT NULL " +
                        "  AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(c.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                        "      LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', '')) " +
                        "GROUP BY c.idUsuarioSupervisor " +
                        "ORDER BY supervisorACargo",
                sucursal
        ));
    }

    private List<Map<String, Object>> deduplicarSupervisores(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object id = firstValue(row, "idUsuarioSupervisor", "id_usuario_supervisor", "idSupervisor", "id_usuario", "idUsuario", "id");
            String key = id == null ? "" : String.valueOf(id).trim().replaceAll("[^0-9]", "");
            if (key.isEmpty() || "0".equals(key) || !vistos.add(key)) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    private Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null && !"".equals(row.get(key))) {
                return row.get(key);
            }
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = entry.getKey() == null ? "" : entry.getKey().replace("_", "").toLowerCase();
            for (String key : keys) {
                if (current.equals(key.replace("_", "").toLowerCase())
                        && entry.getValue() != null
                        && !"".equals(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public List<Map<String, Object>> listarTecnicosFiltro(JdbcTemplate template) {
        return template.queryForList("EXEC dbo.spx_Grupo_FiltroTecnicosCentral");
    }

    public List<Map<String, Object>> crearGrupo(JdbcTemplate template, Integer idUsuarioEjecutor, String nombre) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_CrearCentral ?, ?",
                idUsuarioEjecutor,
                nombre
        );
    }

    public List<Map<String, Object>> asignarSupervisor(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioSupervisor) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_AsignarSupervisorCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioSupervisor
        );
    }

    public List<Map<String, Object>> asignarTecnico(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_AsignarTecnicoCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> quitarTecnico(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_QuitarTecnicoCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> eliminarGrupo(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_EliminarCentral ?, ?",
                idUsuarioEjecutor,
                idGrupo
        );
    }

    public List<Map<String, Object>> marcarSupervisorAusente(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_MarcarSupervisorAusenteCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> restaurarSupervisor(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_RestaurarSupervisorCentral ?, ?",
                idUsuarioEjecutor,
                idGrupo
        );
    }

    public List<Map<String, Object>> cambiarColaboradorBackup(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_CambiarColaboradorBackupCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> listarGruposDesdeConformacionCentral(JdbcTemplate centralTemplate, String sucursal) {
        return centralTemplate.queryForList(
                "WITH base AS ( " +
                        "  SELECT " +
                        "    LTRIM(RTRIM(ISNULL(grupo, ''))) AS grupo, " +
                        "    LTRIM(RTRIM(ISNULL(supervisorACargo, ''))) AS supervisor, " +
                        "    CAST(NULL AS INT) AS id_usuario_tecnico, " +
                        "    CAST(id_tecnico AS INT) AS id_tecnico, " +
                        "    LTRIM(RTRIM(ISNULL(tecnico, ''))) AS tecnico, " +
                        "    fechaRegistro, " +
                        "    ROW_NUMBER() OVER ( " +
                        "      PARTITION BY LTRIM(RTRIM(ISNULL(grupo, ''))), CAST(id_tecnico AS INT) " +
                        "      ORDER BY fecha DESC, fechaRegistro DESC, id DESC " +
                        "    ) AS rn_tecnico " +
                        "  FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "    AND LTRIM(RTRIM(ISNULL(grupo, ''))) <> '' " +
                        "    AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                        "        LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', '')) " +
                        ") " +
                        "SELECT " +
                        "  CAST(DENSE_RANK() OVER (ORDER BY grupo) AS INT) AS id_grupo, " +
                        "  grupo AS nombre, " +
                        "  supervisor AS supervisor, " +
                        "  id_usuario_tecnico, " +
                        "  id_tecnico, " +
                        "  tecnico, " +
                        "  fechaRegistro AS fecha_registro " +
                        "FROM base " +
                        "WHERE rn_tecnico = 1 " +
                        "ORDER BY grupo, tecnico",
                sucursal
        );
    }

    public List<Map<String, Object>> listarGruposPorSupervisor(JdbcTemplate template, Integer idUsuarioSupervisor) {
        return template.queryForList(
                "SELECT DISTINCT CAST(v.id_grupo AS INT) AS id_grupo " +
                        "FROM dbo.vw_GruposUnicosCuadrilla v " +
                        "WHERE CAST(v.id_usuario_supervisor AS INT) = ? " +
                        "ORDER BY CAST(v.id_grupo AS INT)",
                idUsuarioSupervisor
        );
    }

    public int actualizarSupervisorEnConformacion(
            JdbcTemplate template,
            String nombreGrupo,
            Integer idUsuarioSupervisor,
            String nombreSupervisor) {
        if (nombreGrupo == null || nombreGrupo.trim().isEmpty()) {
            return 0;
        }
        String grupo = nombreGrupo.trim();
        String supervisor = nombreSupervisor == null ? "" : nombreSupervisor.trim();

        // Esquema actual (camelCase).
        try {
            return template.update(
                    "UPDATE dbo.tbl_ConformacionCuadrillaDiario " +
                            "SET idUsuarioSupervisor = ?, supervisorACargo = ? " +
                            "WHERE LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                            "  AND ISNULL(e_eliminado, 0) = 0",
                    idUsuarioSupervisor,
                    supervisor,
                    grupo
            );
        } catch (DataAccessException ex) {
            // Esquema alterno (snake_case).
            return template.update(
                    "UPDATE dbo.tbl_ConformacionCuadrillaDiario " +
                            "SET id_usuario_supervisor = ?, supervisor_a_cargo = ? " +
                            "WHERE LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                            "  AND ISNULL(e_eliminado, 0) = 0",
                    idUsuarioSupervisor,
                    supervisor,
                    grupo
            );
        }
    }

    public int actualizarSupervisorEnConformacionCentral(
            JdbcTemplate centralTemplate,
            String sucursal,
            String nombreGrupo,
            Integer idUsuarioSupervisor,
            String nombreSupervisor) {
        try {
            return centralTemplate.update(
                    "EXEC dbo.spx_Central_ActualizarSupervisorConformacion ?, ?, ?, ?",
                    sucursal,
                    nombreGrupo,
                    idUsuarioSupervisor,
                    nombreSupervisor
            );
        } catch (DataAccessException ex) {
            String supervisor = nombreSupervisor == null ? "" : nombreSupervisor.trim();
            try {
                return centralTemplate.update(
                        "UPDATE dbo.tbl_ConformacionCuadrillaDiario " +
                                "SET idUsuarioSupervisor = ?, supervisorACargo = ? " +
                                "WHERE LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                                "  AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                                "      LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', '')) " +
                                "  AND ISNULL(e_eliminado, 0) = 0",
                        idUsuarioSupervisor,
                        supervisor,
                        nombreGrupo,
                        sucursal
                );
            } catch (DataAccessException ignored) {
                return centralTemplate.update(
                        "UPDATE dbo.tbl_ConformacionCuadrillaDiario " +
                                "SET id_usuario_supervisor = ?, supervisor_a_cargo = ? " +
                                "WHERE LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                                "  AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                                "      LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', '')) " +
                                "  AND ISNULL(e_eliminado, 0) = 0",
                        idUsuarioSupervisor,
                        supervisor,
                        nombreGrupo,
                        sucursal
                );
            }
        }
    }

    public int actualizarSupervisorIniciosPendientesPorGrupo(
            JdbcTemplate tigohogarTemplate,
            JdbcTemplate centralTemplate,
            String sucursal,
            String nombreGrupo,
            Integer idSupervisorOrigen,
            Integer idSupervisorDestino) {
        if (tigohogarTemplate == null
                || centralTemplate == null
                || nombreGrupo == null
                || nombreGrupo.trim().isEmpty()
                || idSupervisorDestino == null
                || idSupervisorDestino <= 0) {
            return 0;
        }

        List<Integer> idsTecnicos = listarIdsTecnicosGrupoCentral(centralTemplate, sucursal, nombreGrupo);
        if (idsTecnicos.isEmpty()) {
            return 0;
        }

        String placeholders = buildPlaceholders(idsTecnicos.size());
        StringBuilder sql = new StringBuilder(
                "UPDATE dbo.tbl_InicioJornadaAlturas " +
                        "SET id_encargado = ?, id_usuario_supervisor_grupo = ? " +
                        "WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "  AND ISNULL(pendiente, 0) = 1 " +
                        "  AND fecha_cierre IS NULL " +
                        "  AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal, ''))) = LTRIM(RTRIM(?))) "
        );
        if (idSupervisorOrigen != null && idSupervisorOrigen > 0) {
            sql.append("  AND (id_encargado = ? OR id_usuario_supervisor_grupo = ?) ");
        }
        sql.append("  AND (id_tecnico IN (")
                .append(placeholders)
                .append(") OR id_auxiliar IN (")
                .append(placeholders)
                .append("))");

        List<Object> params = new ArrayList<>();
        params.add(idSupervisorDestino);
        params.add(idSupervisorDestino);
        params.add(sucursal);
        params.add(sucursal);
        if (idSupervisorOrigen != null && idSupervisorOrigen > 0) {
            params.add(idSupervisorOrigen);
            params.add(idSupervisorOrigen);
        }
        params.addAll(idsTecnicos);
        params.addAll(idsTecnicos);

        try {
            return tigohogarTemplate.update(sql.toString(), params.toArray());
        } catch (DataAccessException ex) {
            String fallbackSql = sql.toString().replace(", id_usuario_supervisor_grupo = ?", "");
            List<Object> fallbackParams = new ArrayList<>(params);
            fallbackParams.remove(1);
            return tigohogarTemplate.update(fallbackSql, fallbackParams.toArray());
        }
    }

    private List<Integer> listarIdsTecnicosGrupoCentral(JdbcTemplate centralTemplate, String sucursal, String nombreGrupo) {
        List<Map<String, Object>> rows = centralTemplate.queryForList(
                "SELECT DISTINCT idTecnico FROM ( " +
                        "  SELECT CAST(id_tecnico AS INT) AS idTecnico " +
                        "  FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "    AND id_tecnico IS NOT NULL " +
                        "    AND LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                        "    AND (? IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                        "                    LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', ''))) " +
                        "  UNION ALL " +
                        "  SELECT CAST(id_tecnicoAuxiliar AS INT) AS idTecnico " +
                        "  FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "    AND id_tecnicoAuxiliar IS NOT NULL " +
                        "    AND LTRIM(RTRIM(ISNULL(grupo, ''))) = LTRIM(RTRIM(?)) " +
                        "    AND (? IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                        "                    LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', ''))) " +
                        ") t WHERE idTecnico IS NOT NULL AND idTecnico > 0",
                nombreGrupo,
                sucursal,
                sucursal,
                nombreGrupo,
                sucursal,
                sucursal
        );
        Set<Integer> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Object value = firstValue(row, "idTecnico", "id_tecnico");
            if (value instanceof Number) {
                ids.add(((Number) value).intValue());
                continue;
            }
            try {
                ids.add(Integer.parseInt(String.valueOf(value).trim()));
            } catch (Exception ignored) {
                // Omite ids no numericos.
            }
        }
        return new ArrayList<>(ids);
    }

    private String buildPlaceholders(int size) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append("?");
        }
        return out.toString();
    }
}
