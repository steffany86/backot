package com.example.TigoStarSystem.supervision.repository;

import com.example.TigoStarSystem.config.DbConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
public class SupervisionRepository {
    private static final String SP_LISTAR =
            "EXEC dbo.spx_ListarSupervisionManual ?, ?, ?, ?";
    private static final String SP_LISTAR_PENDIENTES =
            "EXEC dbo.spx_ListarSupervisionPendiente ?, ?, ?, ?";
    private static final String SP_LISTAR_POR_ESTADO =
            "EXEC dbo.spx_ListarSupervisionPorEstado ?, ?, ?, ?, ?";
    private static final String SP_OBTENER_DETALLE =
            "EXEC dbo.spx_ObtenerSupervisionManualPorId ?, ?";
    private static final String SP_REGISTRAR =
            "EXEC dbo.spx_RegistrarSupervisionManual ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    private static final String SP_REGISTRAR_PENDIENTE =
            "EXEC dbo.spx_RegistrarSupervisionPendiente ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    private static final String SP_REALIZAR_PENDIENTE =
            "EXEC dbo.spx_RealizarSupervisionPendiente ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    private static final String SP_TECNICOS_AGENDA_SUP =
            "EXEC dbo.SP_TecnicosAgendaSup ?, ?, ?";
    private static final String SP_LISTAR_SUPERVISORES =
            "EXEC dbo.spx_ObtenerSupervisoresConformacionCuadrillaWeb";
    private static final String SP_LISTAR_SUPERVISORES_ALT =
            "EXEC spx_ObtenerSupervisoresConformacionCuadrillaWeb";
    private static final String SP_LISTAR_SUPERVISORES_ALT2 =
            "EXEC dbo.spx_ObtenerSupervisores";
    private static final String SP_LISTAR_SUPERVISORES_ALT3 =
            "EXEC spx_ObtenerSupervisores";
    private static final String SP_REVISIONES_PENALIZADAS =
            "EXEC dbo.spy_REV_PENALIZADA ?";

    private final JdbcTemplate tigohogarJdbcTemplate;
    private final DbConnectionManager dbConnectionManager;
    private final String dbUsername;
    private final String dbPassword;

    public SupervisionRepository(
            @Qualifier("tigohogarJdbcTemplate") JdbcTemplate tigohogarJdbcTemplate,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword
    ) {
        this.tigohogarJdbcTemplate = tigohogarJdbcTemplate;
        this.dbConnectionManager = dbConnectionManager;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public List<Map<String, Object>> listar(String idSupervisor, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta, Integer limite) {
        return tigohogarJdbcTemplate.queryForList(
                SP_LISTAR,
                idSupervisor,
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                resolveLimit(limite)
        );
    }

    public List<Map<String, Object>> listarPendientes(String idSupervisor, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta, Integer limite) {
        return tigohogarJdbcTemplate.queryForList(
                SP_LISTAR_PENDIENTES,
                idSupervisor,
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                resolveLimit(limite)
        );
    }

    public List<Map<String, Object>> listarRevisionesPenalizadasSupervisor(Integer idSupervisor) {
        if (idSupervisor == null || idSupervisor <= 0) {
            return new ArrayList<>();
        }
        try {
            JdbcTemplate central = dbConnectionManager.connDb("bdcontrolordenes");
            List<Map<String, Object>> rows = central.queryForList(SP_REVISIONES_PENALIZADAS, idSupervisor);
            List<Map<String, Object>> out = new ArrayList<>();
            int idx = 0;
            for (Map<String, Object> row : rows) {
                out.add(normalizarRevisionPenalizada(row, idSupervisor, idx++));
            }
            return out;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarPorEstado(String estadoSup, String idSupervisor, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta, Integer limite) {
        return tigohogarJdbcTemplate.queryForList(
                SP_LISTAR_POR_ESTADO,
                trimToNull(estadoSup),
                trimToNull(idSupervisor),
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                resolveLimit(limite)
        );
    }

    public Map<String, Object> obtenerDetalle(String idSupervision, String idSupervisor) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(SP_OBTENER_DETALLE, idSupervision, idSupervisor);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private Map<String, Object> normalizarRevisionPenalizada(Map<String, Object> row, Integer idSupervisor, int index) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        String ot = firstNonBlankText(findValue(row, "OT"), findValue(row, "orden_nro"), findValue(row, "OrdenTrabajo"));
        String codigo = firstNonBlankText(findValue(row, "CODIGO"), findValue(row, "cliente_nro"), findValue(row, "codigo"));
        String tecnico = firstNonBlankText(findValue(row, "tecnico_nombre"), findValue(row, "tecnico"));
        String tor = firstNonBlankText(findValue(row, "TOR"), findValue(row, "tor"));
        String estadoGestion = firstNonBlankText(findValue(row, "Estado_Gestion"), findValue(row, "TipoRev"), findValue(row, "tipoRevision"));
        String obs = firstNonBlankText(findValue(row, "obs_penalizada"), findValue(row, "observacion"));
        String key = firstNonBlankText(ot, codigo, tecnico, String.valueOf(index));

        out.put("idSupervision", "REV_PENALIZADA:" + key.replaceAll("[^A-Za-z0-9_-]", "_") + ":" + index);
        out.put("id_supervision", out.get("idSupervision"));
        out.put("fechaRegistro", findValue(row, "Fecha"));
        out.put("fecha_registro", findValue(row, "Fecha"));
        out.put("idSupervisor", idSupervisor);
        out.put("id_supervisor", idSupervisor);
        out.put("supervisor", firstNonBlankText(findValue(row, "supervisorACargo"), findValue(row, "supervisor")));
        out.put("supervisorNombre", out.get("supervisor"));
        out.put("tecnicoPrincipal", tecnico);
        out.put("tecnicoPrincipalNombre", tecnico);
        out.put("tecnicoNombre", tecnico);
        out.put("codigo", codigo);
        out.put("Codigo", codigo);
        out.put("ordenTrabajo", ot);
        out.put("OrdenTrabajo", ot);
        out.put("tipoRevision", estadoGestion);
        out.put("TipoRevision", estadoGestion);
        out.put("observacion", obs);
        out.put("Observacion", obs);
        out.put("descripcionAdicionalObservacion", tor == null ? null : "TOR: " + tor);
        out.put("estadoSup", "pendiente");
        out.put("estado_sup", "pendiente");
        out.put("origen", "REV_PENALIZADA");
        out.put("origenExterno", true);
        return out;
    }

    public int realizarPendiente(
            String idSupervision,
            Integer idSupervisor,
            String fotoBoletaSupervision,
            String fotoCanalesPilos,
            String fotoNivelesDocsis,
            String fotoMedicionRuido,
            String fotoBarridoCanales,
            String fotoObservacion1,
            String fotoObservacion2,
            String fotoObservacion3,
            String fotoObservacion4,
            String observacion,
            String descripcionAdicionalObservacion,
            String ubicacion) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                SP_REALIZAR_PENDIENTE,
                trimToNull(idSupervision),
                idSupervisor == null ? null : String.valueOf(idSupervisor),
                trimToNull(fotoBoletaSupervision),
                trimToNull(fotoCanalesPilos),
                trimToNull(fotoNivelesDocsis),
                trimToNull(fotoMedicionRuido),
                trimToNull(fotoBarridoCanales),
                trimToNull(fotoObservacion1),
                trimToNull(fotoObservacion2),
                trimToNull(fotoObservacion3),
                trimToNull(fotoObservacion4),
                trimToNull(observacion),
                trimToNull(descripcionAdicionalObservacion),
                trimToNull(ubicacion)
        );
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Object updated = findValue(rows.get(0), "actualizados", "updated", "rowsUpdated");
        Integer count = toInteger(updated);
        return count == null ? 0 : count;
    }

    public Map<String, Object> enriquecerDetalleConNombres(Map<String, Object> detalle, String sucursal) {
        if (detalle == null || detalle.isEmpty()) {
            return detalle;
        }
        Map<Integer, String> usuarios = cargarUsuariosDesdeSucursalPreferida(sucursal);
        if (usuarios.isEmpty()) {
            usuarios = cargarUsuariosDesdeTecnicosSp();
        }
        JdbcTemplate sucursalTemplate = null;
        try {
            sucursalTemplate = resolveJdbcTemplateBySucursalNombre(sucursal);
        } catch (Exception ignored) {}

        Integer idTecnicoPrincipal = toInteger(findValue(
                detalle,
                "idTecnicoPrincipal",
                "id_tecnico_principal",
                "id_tecnico",
                "idTecnico"
        ));
        Integer idTecnicoAuxiliar = toInteger(findValue(
                detalle,
                "idTecnicoAuxiliar",
                "id_tecnico_auxiliar",
                "id_auxiliar",
                "idAuxiliar"
        ));
        Integer idSupervisor = toInteger(findValue(
                detalle,
                "idSupervisor",
                "id_supervisor",
                "idUsuarioSupervisor",
                "id_usuariosupervisor",
                "supervisor"
        ));

        String nombrePrincipal = resolveNombrePersona(idTecnicoPrincipal, usuarios, sucursalTemplate);
        String nombreAuxiliar = resolveNombrePersona(idTecnicoAuxiliar, usuarios, sucursalTemplate);
        String nombreSupervisor = resolveNombrePersona(idSupervisor, usuarios, sucursalTemplate);

        if (nombrePrincipal != null) {
            detalle.put("tecnicoPrincipalNombre", nombrePrincipal);
            detalle.put("tecnicoNombre", nombrePrincipal);
            if (isNumericText(toText(findValue(detalle, "tecnicoPrincipal", "tecnico", "tecnico_nombre")))) {
                detalle.put("tecnicoPrincipal", nombrePrincipal);
                detalle.put("tecnico", nombrePrincipal);
            }
        }
        if (nombreAuxiliar != null) {
            detalle.put("tecnicoAuxiliarNombre", nombreAuxiliar);
            detalle.put("auxiliarNombre", nombreAuxiliar);
            if (isNumericText(toText(findValue(detalle, "tecnicoAuxiliar", "auxiliar", "auxiliar_nombre")))) {
                detalle.put("tecnicoAuxiliar", nombreAuxiliar);
                detalle.put("auxiliar", nombreAuxiliar);
            }
        }
        if (nombreSupervisor != null) {
            detalle.put("supervisorNombre", nombreSupervisor);
            if (isNumericText(toText(findValue(detalle, "supervisor", "nombreSupervisor")))) {
                detalle.put("supervisor", nombreSupervisor);
            }
        }
        return detalle;
    }

    public String registrar(
            Integer idSupervisor,
            String idTecnicoPrincipal,
            String idTecnicoAuxiliar,
            String idTipoSupervision,
            String idTipoTrabajo,
            String idTipoPenalizacion,
            String supervisionPor,
            String tecnologia,
            String codigo,
            String ordenTrabajo,
            String tipoRevision,
            String fotoBoletaSupervision,
            String fotoCanalesPilos,
            String fotoNivelesDocsis,
            String fotoMedicionRuido,
            String fotoBarridoCanales,
            String fotoObservacion1,
            String fotoObservacion2,
            String fotoObservacion3,
            String fotoObservacion4,
            String observacion,
            String descripcionAdicionalObservacion,
            String ubicacion) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                SP_REGISTRAR,
                idSupervisor,
                trimToNull(idTecnicoPrincipal),
                trimToNull(idTecnicoAuxiliar),
                trimToNull(idTipoSupervision),
                trimToNull(idTipoTrabajo),
                trimToNull(idTipoPenalizacion),
                trimToNull(supervisionPor),
                trimToNull(tecnologia),
                trimToNull(codigo),
                trimToNull(ordenTrabajo),
                trimToNull(tipoRevision),
                trimToNull(fotoBoletaSupervision),
                trimToNull(fotoCanalesPilos),
                trimToNull(fotoNivelesDocsis),
                trimToNull(fotoMedicionRuido),
                trimToNull(fotoBarridoCanales),
                trimToNull(fotoObservacion1),
                trimToNull(fotoObservacion2),
                trimToNull(fotoObservacion3),
                trimToNull(fotoObservacion4),
                trimToNull(observacion),
                trimToNull(descripcionAdicionalObservacion),
                trimToNull(ubicacion)
        );

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            Object id = findValue(row, "idSupervision", "id_supervision", "Id_Supervision");
            if (id == null && !row.isEmpty()) {
                id = row.values().iterator().next();
            }
            if (id != null) {
                String text = String.valueOf(id).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        throw new IllegalStateException("No se pudo obtener Id_Supervision generado.");
    }

    public List<Map<String, Object>> listarTiposSupervision() {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Supervision_ListarTiposSupervision"
        );
    }

    public List<Map<String, Object>> listarTiposTrabajo() {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Supervision_ListarTiposTrabajo"
        );
    }

    public List<Map<String, Object>> listarTiposPenalizacion() {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Supervision_ListarTiposPenalizacion"
        );
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisor(Integer idSupervisor, String sucursal) {
        return listarTecnicosPorSupervisor(idSupervisor, sucursal, null);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisor(Integer idSupervisor, String sucursal, String supervisorNombre) {
        JdbcTemplate central = dbConnectionManager.connDb("bdcontrolordenes");
        JdbcTemplate sucursalTemplate = resolveJdbcTemplateBySucursalNombre(sucursal);
        List<Map<String, Object>> ids = new ArrayList<>();
        ids.addAll(cargarTecnicosDesdeGruposSupervisor(sucursalTemplate, idSupervisor));
        if (ids.isEmpty()) {
            ids.addAll(cargarTecnicosAgendaSup(central, idSupervisor, sucursal));
        }
        if (ids.isEmpty()) {
            ids.addAll(cargarTecnicosDesdeConformacionDiariaPorEncargado(central, idSupervisor, sucursal));
        }
        ids = dedupeTecnicos(ids);
        return enriquecerTecnicosDesdeSucursal(ids, sucursalTemplate);
    }

    public List<Map<String, Object>> listarTecnicosDeGrupos(String sucursal) {
        JdbcTemplate sucursalTemplate = resolveJdbcTemplateBySucursalNombre(sucursal);
        List<Map<String, Object>> ids = cargarTecnicosDesdeTodosLosGrupos(sucursalTemplate);
        if (ids.isEmpty()) {
            ids.addAll(cargarTecnicosActivosDesdeVendedor(sucursalTemplate));
        }
        ids = dedupeTecnicos(ids);
        return enriquecerTecnicosDesdeSucursal(ids, sucursalTemplate);
    }

    private List<Map<String, Object>> cargarTecnicosActivosDesdeVendedor(JdbcTemplate template) {
        if (template == null) {
            return new ArrayList<>();
        }
        String sql =
                "SELECT " +
                        "  CAST(v.Id_Vendedor AS INT) AS idTecnico, " +
                        "  CAST(v.Id_Vendedor AS INT) AS id_tecnico, " +
                        "  CAST(v.Nombre AS NVARCHAR(200)) AS tecnico, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codigo, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codEmpleado " +
                        "FROM dbo.tbl_Vendedor v " +
                        "WHERE ISNULL(v.E_Eliminado, 0) = 0 " +
                        "  AND v.Id_Vendedor IS NOT NULL " +
                        "ORDER BY v.Nombre, v.Id_Vendedor";
        try {
            return template.queryForList(sql);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> cargarTecnicosDesdeGruposSupervisor(
            JdbcTemplate template,
            Integer idSupervisor) {
        if (template == null || idSupervisor == null || idSupervisor <= 0) {
            return new ArrayList<>();
        }
        String sql =
                "SELECT DISTINCT " +
                        "  g.id_grupo, " +
                        "  g.nombre AS grupo, " +
                        "  gs.id_usuario AS idSupervisor, " +
                        "  dg.id_usuario_tecnico AS idUsuarioTecnico, " +
                        "  COALESCE(CAST(v.Id_Vendedor AS INT), CAST(ut.id_Vendedor AS INT), CAST(dg.id_usuario_tecnico AS INT)) AS idTecnico, " +
                        "  COALESCE(NULLIF(LTRIM(RTRIM(v.Nombre)), ''), 'Tecnico ' + CONVERT(NVARCHAR(20), COALESCE(v.Id_Vendedor, ut.id_Vendedor, dg.id_usuario_tecnico))) AS tecnico, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codigo, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codEmpleado " +
                        "FROM dbo.tbl_GrupoSup gs " +
                        "INNER JOIN dbo.tbl_Grupo g ON g.id_grupo = gs.id_grupo " +
                        "INNER JOIN dbo.tbl_DetalleGrupo dg ON dg.id_grupo = gs.id_grupo " +
                        "LEFT JOIN dbo.tbl_UsuarioTecnico ut ON ut.id = dg.id_usuario_tecnico AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "LEFT JOIN dbo.tbl_Vendedor v ON v.Id_Vendedor = ut.id_Vendedor AND ISNULL(v.E_Eliminado, 0) = 0 " +
                        "WHERE gs.id_usuario = ? " +
                        "  AND ISNULL(g.e_eliminado, 0) = 0 " +
                        "  AND COALESCE(CAST(v.Id_Vendedor AS INT), CAST(ut.id_Vendedor AS INT), CAST(dg.id_usuario_tecnico AS INT)) > 0 " +
                        "ORDER BY tecnico, idTecnico";
        try {
            return template.queryForList(sql, idSupervisor);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> cargarTecnicosDesdeTodosLosGrupos(JdbcTemplate template) {
        if (template == null) {
            return new ArrayList<>();
        }
        String sql =
                "SELECT DISTINCT " +
                        "  g.id_grupo, " +
                        "  g.nombre AS grupo, " +
                        "  dg.id_usuario_tecnico AS idUsuarioTecnico, " +
                        "  COALESCE(CAST(v.Id_Vendedor AS INT), CAST(ut.id_Vendedor AS INT), CAST(dg.id_usuario_tecnico AS INT)) AS idTecnico, " +
                        "  COALESCE(NULLIF(LTRIM(RTRIM(v.Nombre)), ''), 'Tecnico ' + CONVERT(NVARCHAR(20), COALESCE(v.Id_Vendedor, ut.id_Vendedor, dg.id_usuario_tecnico))) AS tecnico, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codigo, " +
                        "  CAST(v.CodEmpleado AS NVARCHAR(100)) AS codEmpleado " +
                        "FROM dbo.tbl_Grupo g " +
                        "INNER JOIN dbo.tbl_DetalleGrupo dg ON dg.id_grupo = g.id_grupo " +
                        "LEFT JOIN dbo.tbl_UsuarioTecnico ut ON ut.id = dg.id_usuario_tecnico AND ISNULL(ut.e_eliminado, 0) = 0 " +
                        "LEFT JOIN dbo.tbl_Vendedor v ON v.Id_Vendedor = ut.id_Vendedor AND ISNULL(v.E_Eliminado, 0) = 0 " +
                        "WHERE ISNULL(g.e_eliminado, 0) = 0 " +
                        "  AND COALESCE(CAST(v.Id_Vendedor AS INT), CAST(ut.id_Vendedor AS INT), CAST(dg.id_usuario_tecnico AS INT)) > 0 " +
                        "ORDER BY tecnico, idTecnico";
        try {
            return template.queryForList(sql);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> cargarTecnicosAgendaSup(
            JdbcTemplate central,
            Integer idSupervisor,
            String sucursal) {
        if (central == null || idSupervisor == null || idSupervisor <= 0) {
            return new ArrayList<>();
        }
        try {
            return central.queryForList(
                    SP_TECNICOS_AGENDA_SUP,
                    idSupervisor,
                    trimToNull(sucursal),
                    0
            );
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> cargarTecnicosDesdeConformacionDiariaPorEncargado(
            JdbcTemplate central,
            Integer idSupervisor,
            String sucursal) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (central == null || idSupervisor == null || idSupervisor <= 0) {
            return out;
        }
        String sucursalNorm = trimToNull(sucursal);
        String[] sqlCandidates = new String[] {
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT id_tecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND id_encargado=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?))) " +
                        "  UNION " +
                        "  SELECT id_tecnico_auxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND id_encargado=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?)))" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT idTecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idEncargado=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?))) " +
                        "  UNION " +
                        "  SELECT idTecnicoAuxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idEncargado=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?)))" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT id_tecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=? " +
                        "  UNION " +
                        "  SELECT id_tecnicoAuxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=?" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT id_tecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND id_encargado=? " +
                        "  UNION " +
                        "  SELECT id_tecnico_auxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND id_encargado=?" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT id_tecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?))) " +
                        "  UNION " +
                        "  SELECT id_tecnicoAuxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=? AND (? IS NULL OR LTRIM(RTRIM(ISNULL(sucursal,''))) = LTRIM(RTRIM(?)))" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT id_tecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=? " +
                        "  UNION " +
                        "  SELECT id_tecnicoAuxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idUsuarioSupervisor=?" +
                        ") x WHERE x.idTecnico IS NOT NULL",
                "SELECT DISTINCT x.idTecnico, x.tecnico " +
                        "FROM (" +
                        "  SELECT idTecnico AS idTecnico, tecnico AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idEncargado=? " +
                        "  UNION " +
                        "  SELECT idTecnicoAuxiliar AS idTecnico, auxiliar AS tecnico FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "  WHERE ISNULL(e_eliminado,0)=0 AND CONVERT(date, fecha)=CONVERT(date, GETDATE()) " +
                        "    AND idEncargado=?" +
                        ") x WHERE x.idTecnico IS NOT NULL"
        };
        for (String sql : sqlCandidates) {
            try {
                List<Map<String, Object>> rows;
                if (sql.contains("? IS NULL")) {
                    rows = central.queryForList(sql, idSupervisor, sucursalNorm, sucursalNorm, idSupervisor, sucursalNorm, sucursalNorm);
                } else {
                    rows = central.queryForList(sql, idSupervisor, idSupervisor);
                }
                if (rows != null && !rows.isEmpty()) {
                    out.addAll(rows);
                    break;
                }
            } catch (Exception ignored) {
                // probar siguiente variante de columnas
            }
        }
        return out;
    }

    private List<Map<String, Object>> dedupeTecnicos(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        Set<Integer> seen = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Integer id = toInteger(findValue(row, "idTecnico", "id_tecnico", "id_vendedor", "idUsuarioTecnico", "id"));
            if (id == null || id <= 0 || !seen.add(id)) {
                continue;
            }
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("idTecnico", id);
            mapped.put("id_tecnico", id);
            String nombre = toText(findValue(row, "tecnico", "Tecnico", "nombre", "Nombre"));
            if (nombre != null) {
                mapped.put("tecnico", nombre);
            }
            String codigo = toText(findValue(row, "codigo", "Codigo", "codEmpleado", "CodEmpleado", "cod_empleado"));
            if (codigo != null) {
                mapped.put("codigo", codigo);
                mapped.put("codEmpleado", codigo);
                mapped.put("cod_empleado", codigo);
            }
            out.add(mapped);
        }
        return out;
    }

    public List<Map<String, Object>> listarIniciosJornadaPendientesSupervisor(Integer idSupervisor, String sucursal) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Inicio_ListarPendientesSupervisorHoy ?",
                idSupervisor
        );
        return enriquecerNombresTecnicos(rows, sucursal);
    }

    public List<Map<String, Object>> listarIniciosJornadaPendientesTodos() {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Inicio_ListarPendientesHoyTodos"
        );
        return enriquecerNombresTecnicos(rows, null);
    }

    public List<Map<String, Object>> listarIniciosJornadaConfirmadosHoyTodos() {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Inicio_ListarConfirmadosHoyTodos"
        );
        return enriquecerNombresTecnicos(rows, null);
    }

    public List<Map<String, Object>> listarIniciosJornadaConfirmadosHoySupervisor(Integer idSupervisor, String sucursal) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.SP_Inicio_ListarConfirmadosSupervisorHoy ?",
                idSupervisor
        );
        return enriquecerNombresTecnicos(rows, sucursal);
    }

    public List<Map<String, Object>> listarHistoricoJornadas(
            java.time.LocalDate fecha,
            String sucursal,
            Integer idSupervisor,
            Integer idTecnico,
            boolean limitarSupervisor) {
        java.time.LocalDate fechaConsulta = fecha == null ? java.time.LocalDate.now() : fecha;
        List<Map<String, Object>> esperados = listarTecnicosEsperadosJornada(sucursal, limitarSupervisor ? idSupervisor : null);
        Map<Integer, Map<String, Object>> esperadosPorTecnico = new LinkedHashMap<>();
        Map<String, Map<String, Object>> esperadosPorSucursalNombre = new LinkedHashMap<>();
        for (Map<String, Object> esperado : esperados) {
            Integer id = toInteger(findValue(esperado, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico"));
            if (id == null || id <= 0) {
                continue;
            }
            if (idTecnico != null && !idTecnico.equals(id)) {
                continue;
            }
            esperadosPorTecnico.putIfAbsent(id, esperado);
            String claveSucursalNombre = claveSucursalTecnico(esperado);
            if (claveSucursalNombre != null) {
                esperadosPorSucursalNombre.putIfAbsent(claveSucursalNombre, esperado);
            }
        }

        Set<Integer> idsParaResolverUsuario = new LinkedHashSet<>(esperadosPorTecnico.keySet());
        if (idTecnico != null && idTecnico > 0) {
            idsParaResolverUsuario.add(idTecnico);
        }
        Map<Integer, Set<Integer>> usuariosPorTecnico = resolverUsuariosPorTecnico(sucursal, idsParaResolverUsuario);
        List<Map<String, Object>> jornadas = queryHistoricoJornadas(fechaConsulta, sucursal, idTecnico, esperadosPorTecnico.keySet(), usuariosPorTecnico);
        jornadas = enriquecerNombresTecnicos(jornadas, sucursal);

        List<Map<String, Object>> out = new ArrayList<>();
        Set<Integer> conRegistro = new LinkedHashSet<>();
        for (Map<String, Object> row : jornadas) {
            Integer tecnicoId = resolverTecnicoHistorico(row, esperadosPorTecnico, usuariosPorTecnico);
            if (tecnicoId == null || tecnicoId <= 0) {
                continue;
            }
            Map<String, Object> esperado = esperadosPorTecnico.get(tecnicoId);
            if (esperado == null) {
                esperado = buscarEsperadoPorSucursalTecnico(row, esperadosPorSucursalNombre);
            }
            Integer tecnicoIdResuelto = esperado == null
                    ? tecnicoId
                    : toInteger(findValue(esperado, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico"));
            if (idTecnico != null && !idTecnico.equals(tecnicoIdResuelto)) {
                continue;
            }
            boolean tecnicoEsperado = esperado != null;
            if (!tecnicoEsperado && limitarSupervisor && !registroPerteneceSupervisor(row, idSupervisor)) {
                continue;
            }
            Map<String, Object> mapped = normalizarHistoricoJornada(row, esperado, fechaConsulta);
            if (!tecnicoEsperado) {
                mapped.put("usuarioRetirado", true);
                mapped.put("usuario_retirado", true);
            }
            out.add(mapped);
            Integer idEsperado = toInteger(findValue(mapped, "idTecnico", "id_tecnico"));
            conRegistro.add(idEsperado == null ? tecnicoId : idEsperado);
        }

        for (Map.Entry<Integer, Map<String, Object>> entry : esperadosPorTecnico.entrySet()) {
            if (conRegistro.contains(entry.getKey())) {
                continue;
            }
            out.add(crearJornadaSinInicio(entry.getKey(), entry.getValue(), fechaConsulta));
        }

        out.sort((a, b) -> {
            String sucA = toText(findValue(a, "sucursal"));
            String sucB = toText(findValue(b, "sucursal"));
            int sucCompare = String.valueOf(sucA == null ? "" : sucA).compareToIgnoreCase(String.valueOf(sucB == null ? "" : sucB));
            if (sucCompare != 0) return sucCompare;
            String tecA = toText(findValue(a, "tecnicoNombre", "tecnico"));
            String tecB = toText(findValue(b, "tecnicoNombre", "tecnico"));
            return String.valueOf(tecA == null ? "" : tecA).compareToIgnoreCase(String.valueOf(tecB == null ? "" : tecB));
        });
        return out;
    }

    public List<Map<String, Object>> listarHistoricoJornadasRango(
            java.time.LocalDate desde,
            java.time.LocalDate hasta,
            String sucursal,
            Integer idSupervisor,
            Integer idTecnico,
            boolean limitarSupervisor) {
        java.time.LocalDate fechaDesde = desde == null ? java.time.LocalDate.now() : desde;
        java.time.LocalDate fechaHasta = hasta == null ? fechaDesde : hasta;
        if (fechaHasta.isBefore(fechaDesde)) {
            java.time.LocalDate tmp = fechaDesde;
            fechaDesde = fechaHasta;
            fechaHasta = tmp;
        }

        List<Map<String, Object>> esperados = listarTecnicosEsperadosJornada(sucursal, limitarSupervisor ? idSupervisor : null);
        Map<Integer, Map<String, Object>> esperadosPorTecnico = new LinkedHashMap<>();
        Map<String, Map<String, Object>> esperadosPorSucursalNombre = new LinkedHashMap<>();
        for (Map<String, Object> esperado : esperados) {
            Integer id = toInteger(findValue(esperado, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico"));
            if (id == null || id <= 0) {
                continue;
            }
            if (idTecnico != null && !idTecnico.equals(id)) {
                continue;
            }
            esperadosPorTecnico.putIfAbsent(id, esperado);
            String claveSucursalNombre = claveSucursalTecnico(esperado);
            if (claveSucursalNombre != null) {
                esperadosPorSucursalNombre.putIfAbsent(claveSucursalNombre, esperado);
            }
        }

        Set<Integer> idsParaResolverUsuario = new LinkedHashSet<>(esperadosPorTecnico.keySet());
        if (idTecnico != null && idTecnico > 0) {
            idsParaResolverUsuario.add(idTecnico);
        }
        Map<Integer, Set<Integer>> usuariosPorTecnico = resolverUsuariosPorTecnico(sucursal, idsParaResolverUsuario);
        Map<Integer, Integer> filtroUsuarioTecnico = construirFiltroUsuarioTecnico(idTecnico, esperadosPorTecnico.keySet(), usuariosPorTecnico);

        List<Map<String, Object>> jornadas = queryHistoricoJornadasRangoRows(fechaDesde, fechaHasta, sucursal);
        anotarTecnicosEsperados(jornadas, filtroUsuarioTecnico);
        jornadas = enriquecerNombresTecnicos(jornadas, sucursal);

        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> conRegistroPorFechaTecnico = new LinkedHashSet<>();
        for (Map<String, Object> row : jornadas) {
            Integer tecnicoId = resolverTecnicoHistorico(row, esperadosPorTecnico, usuariosPorTecnico);
            if (tecnicoId == null || tecnicoId <= 0) {
                continue;
            }
            Map<String, Object> esperado = esperadosPorTecnico.get(tecnicoId);
            if (esperado == null) {
                esperado = buscarEsperadoPorSucursalTecnico(row, esperadosPorSucursalNombre);
            }
            Integer tecnicoIdResuelto = esperado == null
                    ? tecnicoId
                    : toInteger(findValue(esperado, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico"));
            if (idTecnico != null && !idTecnico.equals(tecnicoIdResuelto)) {
                continue;
            }
            boolean tecnicoEsperado = esperado != null;
            if (!tecnicoEsperado && limitarSupervisor && !registroPerteneceSupervisor(row, idSupervisor)) {
                continue;
            }
            java.time.LocalDate fechaRegistro = fechaHistoricoDesdeRow(row, fechaDesde);
            Map<String, Object> mapped = normalizarHistoricoJornada(row, esperado, fechaRegistro);
            if (!tecnicoEsperado) {
                mapped.put("usuarioRetirado", true);
                mapped.put("usuario_retirado", true);
            }
            out.add(mapped);
            Integer idEsperado = toInteger(findValue(mapped, "idTecnico", "id_tecnico"));
            conRegistroPorFechaTecnico.add(claveFechaTecnico(fechaRegistro, idEsperado == null ? tecnicoId : idEsperado));
        }

        java.time.LocalDate cursor = fechaDesde;
        while (!cursor.isAfter(fechaHasta)) {
            for (Map.Entry<Integer, Map<String, Object>> entry : esperadosPorTecnico.entrySet()) {
                if (!conRegistroPorFechaTecnico.contains(claveFechaTecnico(cursor, entry.getKey()))) {
                    out.add(crearJornadaSinInicio(entry.getKey(), entry.getValue(), cursor));
                }
            }
            cursor = cursor.plusDays(1);
        }

        out.sort((a, b) -> {
            String fechaA = toText(findValue(a, "fecha"));
            String fechaB = toText(findValue(b, "fecha"));
            int fechaCompare = String.valueOf(fechaA == null ? "" : fechaA).compareToIgnoreCase(String.valueOf(fechaB == null ? "" : fechaB));
            if (fechaCompare != 0) return fechaCompare;
            String sucA = toText(findValue(a, "sucursal"));
            String sucB = toText(findValue(b, "sucursal"));
            int sucCompare = String.valueOf(sucA == null ? "" : sucA).compareToIgnoreCase(String.valueOf(sucB == null ? "" : sucB));
            if (sucCompare != 0) return sucCompare;
            String tecA = toText(findValue(a, "tecnicoNombre", "tecnico"));
            String tecB = toText(findValue(b, "tecnicoNombre", "tecnico"));
            return String.valueOf(tecA == null ? "" : tecA).compareToIgnoreCase(String.valueOf(tecB == null ? "" : tecB));
        });
        return out;
    }

    private Map<Integer, Integer> construirFiltroUsuarioTecnico(
            Integer idTecnico,
            Set<Integer> idsEsperados,
            Map<Integer, Set<Integer>> usuariosPorTecnico) {
        Map<Integer, Integer> filtroUsuarioTecnico = new LinkedHashMap<>();
        if (idsEsperados != null && !idsEsperados.isEmpty()) {
            for (Integer idEsperado : idsEsperados) {
                Set<Integer> idsUsuario = usuariosPorTecnico == null ? null : usuariosPorTecnico.get(idEsperado);
                if (idsUsuario == null || idsUsuario.isEmpty()) {
                    filtroUsuarioTecnico.put(idEsperado, idEsperado);
                } else {
                    for (Integer idUsuario : idsUsuario) {
                        filtroUsuarioTecnico.put(idUsuario, idEsperado);
                    }
                }
            }
        }
        if (idTecnico != null && idTecnico > 0 && filtroUsuarioTecnico.isEmpty()) {
            Set<Integer> idsUsuario = usuariosPorTecnico == null ? null : usuariosPorTecnico.get(idTecnico);
            if (idsUsuario == null || idsUsuario.isEmpty()) {
                filtroUsuarioTecnico.put(idTecnico, idTecnico);
            } else {
                for (Integer idUsuario : idsUsuario) {
                    filtroUsuarioTecnico.put(idUsuario, idTecnico);
                }
            }
        }
        return filtroUsuarioTecnico;
    }

    private String claveFechaTecnico(java.time.LocalDate fecha, Integer idTecnico) {
        return String.valueOf(fecha) + "|" + String.valueOf(idTecnico == null ? 0 : idTecnico);
    }

    private java.time.LocalDate fechaHistoricoDesdeRow(Map<String, Object> row, java.time.LocalDate fallback) {
        Object value = findValue(row, "fechaRegistro", "fecha_registro", "fechaInicio", "fecha_inicio");
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        String text = toText(value);
        if (text != null && text.length() >= 10) {
            try {
                return java.time.LocalDate.parse(text.substring(0, 10));
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private Map<String, Object> buscarEsperadoPorSucursalTecnico(
            Map<String, Object> row,
            Map<String, Map<String, Object>> esperadosPorSucursalNombre) {
        if (row == null || esperadosPorSucursalNombre == null || esperadosPorSucursalNombre.isEmpty()) {
            return null;
        }
        String sucursal = toText(findValue(row, "sucursal"));
        String tecnico = toText(findValue(row, "tecnicoNombre", "tecnico", "nombreTecnico", "nombre_tecnico", "tecnico_nombre"));
        String clave = claveSucursalTecnico(sucursal, tecnico);
        return clave == null ? null : esperadosPorSucursalNombre.get(clave);
    }

    private String claveSucursalTecnico(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return claveSucursalTecnico(
                toText(findValue(row, "sucursal")),
                toText(findValue(row, "tecnicoNombre", "tecnico", "nombreTecnico", "nombre_tecnico", "tecnico_nombre"))
        );
    }

    private String claveSucursalTecnico(String sucursal, String tecnico) {
        String sucursalNorm = normText(sucursal);
        String tecnicoNorm = normalizePersonName(tecnico);
        if (sucursalNorm == null || tecnicoNorm == null) {
            return null;
        }
        return sucursalNorm + "|" + tecnicoNorm;
    }

    private boolean registroPerteneceSupervisor(Map<String, Object> row, Integer idSupervisor) {
        if (idSupervisor == null || idSupervisor <= 0) {
            return true;
        }
        Integer supervisorRegistro = toInteger(findValue(row, "idSupervisor", "id_supervisor", "id_encargado", "id_usuario_supervisor_grupo"));
        return supervisorRegistro != null && supervisorRegistro.equals(idSupervisor);
    }

    private List<Map<String, Object>> queryHistoricoJornadas(
            java.time.LocalDate fecha,
            String sucursal,
            Integer idTecnico,
            Set<Integer> idsEsperados,
            Map<Integer, Set<Integer>> usuariosPorTecnico) {
        Map<Integer, Integer> filtroUsuarioTecnico = new LinkedHashMap<>();
        if (idsEsperados != null && !idsEsperados.isEmpty()) {
            for (Integer idEsperado : idsEsperados) {
                Set<Integer> idsUsuario = usuariosPorTecnico == null ? null : usuariosPorTecnico.get(idEsperado);
                if (idsUsuario == null || idsUsuario.isEmpty()) {
                    filtroUsuarioTecnico.put(idEsperado, idEsperado);
                } else {
                    for (Integer idUsuario : idsUsuario) {
                        filtroUsuarioTecnico.put(idUsuario, idEsperado);
                    }
                }
            }
        }
        if (idTecnico != null && idTecnico > 0 && filtroUsuarioTecnico.isEmpty()) {
            Set<Integer> idsUsuario = usuariosPorTecnico == null ? null : usuariosPorTecnico.get(idTecnico);
            if (idsUsuario == null || idsUsuario.isEmpty()) {
                filtroUsuarioTecnico.put(idTecnico, idTecnico);
            } else {
                for (Integer idUsuario : idsUsuario) {
                    filtroUsuarioTecnico.put(idUsuario, idTecnico);
                }
            }
        }
        List<Map<String, Object>> rows = queryHistoricoJornadasChunk(fecha, sucursal, java.util.Collections.emptyMap());
        anotarTecnicosEsperados(rows, filtroUsuarioTecnico);
        return rows;
    }

    private List<Map<String, Object>> queryHistoricoJornadasChunk(java.time.LocalDate fecha, String sucursal, Map<Integer, Integer> idsUsuarioTecnico) {
        StringBuilder sql = new StringBuilder(
                "SELECT ij.id_inicio, ij.id_tecnico, ij.id_auxiliar, ij.id_encargado, " +
                        "ij.fecha_registro, ij.fecha_cierre, ij.pendiente, ij.e_eliminado, ij.no_marco_cierre, " +
                        "ij.id_usuario_supervisor_grupo, ij.id_sucursal, ij.sucursal, " +
                        "ij.nombre_tecnico, ij.tecnico_nombre, ij.firma_inicio, ij.firma_cierre " +
                        "FROM dbo.tbl_InicioJornadaAlturas ij " +
                        "WHERE ij.fecha_registro >= ? " +
                        "  AND ij.fecha_registro < ? " +
                        "  AND ISNULL(ij.e_eliminado, 0) = 0 "
        );
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(fecha));
        params.add(Date.valueOf(fecha.plusDays(1)));
        String sucursalNorm = trimToNull(sucursal);
        if (sucursalNorm != null) {
            sql.append(" AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(ij.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = ")
                    .append("LOWER(REPLACE(REPLACE(REPLACE(?, '_', ''), '-', ''), ' ', '')) ");
            params.add(sucursalNorm);
        }
        if (idsUsuarioTecnico != null && !idsUsuarioTecnico.isEmpty()) {
            sql.append(" AND ij.id_tecnico IN (");
            int i = 0;
            for (Integer idUsuario : idsUsuarioTecnico.keySet()) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append("?");
                params.add(idUsuario);
                i++;
            }
            sql.append(") ");
        }
        sql.append("ORDER BY ij.fecha_registro ASC, ij.id_inicio ASC");
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(sql.toString(), params.toArray());
            if (idsUsuarioTecnico != null && !idsUsuarioTecnico.isEmpty()) {
                for (Map<String, Object> row : rows) {
                    Integer idUsuario = toInteger(findValue(row, "id_tecnico", "idTecnico"));
                    Integer idTecnicoEsperado = idsUsuarioTecnico.get(idUsuario);
                    if (idTecnicoEsperado != null && idTecnicoEsperado > 0) {
                        row.put("idTecnicoEsperado", idTecnicoEsperado);
                        row.put("id_tecnico_esperado", idTecnicoEsperado);
                    }
                }
            }
            return rows;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> queryHistoricoJornadasRangoRows(
            java.time.LocalDate desde,
            java.time.LocalDate hasta,
            String sucursal) {
        StringBuilder sql = new StringBuilder(
                "SELECT ij.id_inicio, ij.id_tecnico, ij.id_auxiliar, ij.id_encargado, " +
                        "ij.fecha_registro, ij.fecha_cierre, ij.pendiente, ij.e_eliminado, ij.no_marco_cierre, " +
                        "ij.id_usuario_supervisor_grupo, ij.id_sucursal, ij.sucursal, " +
                        "ij.nombre_tecnico, ij.tecnico_nombre, ij.firma_inicio, ij.firma_cierre " +
                        "FROM dbo.tbl_InicioJornadaAlturas ij " +
                        "WHERE ij.fecha_registro >= ? " +
                        "  AND ij.fecha_registro < ? " +
                        "  AND ISNULL(ij.e_eliminado, 0) = 0 "
        );
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(desde));
        params.add(Date.valueOf(hasta.plusDays(1)));
        String sucursalNorm = trimToNull(sucursal);
        if (sucursalNorm != null) {
            sql.append(" AND LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(ij.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = ")
                    .append("LOWER(REPLACE(REPLACE(REPLACE(?, '_', ''), '-', ''), ' ', '')) ");
            params.add(sucursalNorm);
        }
        sql.append("ORDER BY ij.fecha_registro ASC, ij.id_inicio ASC");
        try {
            return tigohogarJdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private Map<Integer, Set<Integer>> resolverUsuariosPorTecnico(String sucursal, Set<Integer> idsTecnicos) {
        Map<Integer, Set<Integer>> out = new LinkedHashMap<>();
        if (idsTecnicos == null || idsTecnicos.isEmpty()) {
            return out;
        }
        JdbcTemplate template;
        try {
            template = resolveJdbcTemplateBySucursalNombre(sucursal);
        } catch (Exception ex) {
            template = tigohogarJdbcTemplate;
        }
        for (Integer idTecnico : idsTecnicos) {
            if (idTecnico != null && idTecnico > 0) {
                out.computeIfAbsent(idTecnico, ignored -> new LinkedHashSet<>()).add(idTecnico);
            }
        }
        List<Integer> ids = new ArrayList<>(idsTecnicos);
        int chunkSize = 800;
        for (int from = 0; from < ids.size(); from += chunkSize) {
            int to = Math.min(from + chunkSize, ids.size());
            resolverUsuariosPorTecnicoChunk(template, ids.subList(from, to), out);
        }
        return out;
    }

    private void resolverUsuariosPorTecnicoChunk(JdbcTemplate template, List<Integer> idsTecnicos, Map<Integer, Set<Integer>> out) {
        if (template == null || idsTecnicos == null || idsTecnicos.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT ut.Id_Vendedor, ut.id_Usuario " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ISNULL(ut.e_eliminado,0)=0 AND ut.Id_Vendedor IN ("
        );
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < idsTecnicos.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
            params.add(idsTecnicos.get(i));
        }
        sql.append(") ORDER BY ut.id DESC");
        try {
            List<Map<String, Object>> rows = template.queryForList(sql.toString(), params.toArray());
            for (Map<String, Object> row : rows) {
                Integer idTecnico = toInteger(findValue(row, "Id_Vendedor", "id_vendedor"));
                Integer idUsuario = toInteger(findValue(row, "id_Usuario", "idUsuario"));
                if (idTecnico != null && idTecnico > 0 && idUsuario != null && idUsuario > 0) {
                    out.computeIfAbsent(idTecnico, ignored -> new LinkedHashSet<>()).add(idUsuario);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void anotarTecnicosEsperados(List<Map<String, Object>> rows, Map<Integer, Integer> usuarioTecnico) {
        if (rows == null || rows.isEmpty() || usuarioTecnico == null || usuarioTecnico.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Integer idUsuario = toInteger(findValue(row, "id_tecnico", "idTecnico"));
            Integer idTecnicoEsperado = idUsuario == null ? null : usuarioTecnico.get(idUsuario);
            if (idTecnicoEsperado != null && idTecnicoEsperado > 0) {
                row.put("idTecnicoEsperado", idTecnicoEsperado);
                row.put("id_tecnico_esperado", idTecnicoEsperado);
            }
        }
    }

    private Integer resolverTecnicoHistorico(
            Map<String, Object> row,
            Map<Integer, Map<String, Object>> esperadosPorTecnico,
            Map<Integer, Set<Integer>> usuariosPorTecnico) {
        Integer idTecnicoEsperado = toInteger(findValue(row, "idTecnicoEsperado", "id_tecnico_esperado"));
        if (idTecnicoEsperado != null && idTecnicoEsperado > 0) {
            return idTecnicoEsperado;
        }
        Integer idRow = toInteger(findValue(row, "idTecnico", "id_tecnico", "id_vendedor", "idUsuarioTecnico"));
        if (idRow != null && esperadosPorTecnico != null && esperadosPorTecnico.containsKey(idRow)) {
            return idRow;
        }
        if (idRow != null && usuariosPorTecnico != null) {
            for (Map.Entry<Integer, Set<Integer>> entry : usuariosPorTecnico.entrySet()) {
                if (entry.getValue() != null && entry.getValue().contains(idRow)) {
                    return entry.getKey();
                }
            }
        }
        String nombreInicio = normalizePersonName(toText(findValue(row, "nombre_tecnico", "tecnico_nombre", "nombreTecnico", "tecnicoNombre", "tecnico")));
        if (nombreInicio != null && esperadosPorTecnico != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : esperadosPorTecnico.entrySet()) {
                String nombreEsperado = normalizePersonName(toText(findValue(entry.getValue(), "tecnicoNombre", "tecnico")));
                if (nombreInicio.equals(nombreEsperado)) {
                    return entry.getKey();
                }
            }
        }
        return idRow;
    }

    private String normalizePersonName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Map<String, Object>> listarTecnicosEsperadosJornada(String sucursal, Integer idSupervisor) {
        JdbcTemplate central = dbConnectionManager.connDb("bdcontrolordenes");
        String sucursalNorm = trimToNull(sucursal);
        Object[] params = new Object[]{
                sucursalNorm, sucursalNorm, idSupervisor, idSupervisor,
                sucursalNorm, sucursalNorm, idSupervisor, idSupervisor
        };
        String baseWhere =
                "WHERE ISNULL(c.e_eliminado, 0) = 0 " +
                        "  AND (? IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(c.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                        "                  LOWER(REPLACE(REPLACE(REPLACE(?, '_', ''), '-', ''), ' ', ''))) " +
                        "  AND (? IS NULL OR CAST(c.idUsuarioSupervisor AS INT) = ?) ";
        String sqlAuxCamel = construirSqlTecnicosEsperados(baseWhere, "c.id_tecnicoAuxiliar");
        String sqlAuxSnake = construirSqlTecnicosEsperados(baseWhere, "c.id_tecnico_auxiliar");
        try {
            return central.queryForList(sqlAuxCamel, params);
        } catch (Exception ex) {
            try {
                return central.queryForList(sqlAuxSnake, params);
            } catch (Exception ignored) {
                return new ArrayList<>();
            }
        }
    }

    private String construirSqlTecnicosEsperados(String baseWhere, String auxColumn) {
        return "WITH tecnicos AS ( " +
                "  SELECT c.sucursal, c.grupo, c.idUsuarioSupervisor, c.supervisorACargo, CAST(c.id_tecnico AS INT) AS idTecnico, c.tecnico AS tecnico, c.fecha, c.fechaRegistro, c.id " +
                "  FROM dbo.tbl_ConformacionCuadrillaDiario c " +
                baseWhere +
                "    AND c.id_tecnico IS NOT NULL AND c.id_tecnico > 0 " +
                "  UNION ALL " +
                "  SELECT c.sucursal, c.grupo, c.idUsuarioSupervisor, c.supervisorACargo, CAST(" + auxColumn + " AS INT) AS idTecnico, c.auxiliar AS tecnico, c.fecha, c.fechaRegistro, c.id " +
                "  FROM dbo.tbl_ConformacionCuadrillaDiario c " +
                baseWhere +
                "    AND " + auxColumn + " IS NOT NULL AND " + auxColumn + " > 0 " +
                "), ranked AS ( " +
                "  SELECT *, ROW_NUMBER() OVER (PARTITION BY idTecnico ORDER BY ISNULL(fecha, '19000101') DESC, ISNULL(fechaRegistro, '19000101') DESC, id DESC) AS rn " +
                "  FROM tecnicos " +
                ") " +
                "SELECT CAST(idTecnico AS INT) AS idTecnico, CAST(idTecnico AS INT) AS id_tecnico, tecnico AS tecnicoNombre, tecnico, " +
                "       sucursal, grupo, CAST(idUsuarioSupervisor AS INT) AS idSupervisor, supervisorACargo AS supervisorNombre " +
                "FROM ranked WHERE rn = 1 ORDER BY sucursal, grupo, tecnico";
    }

    private Map<String, Object> normalizarHistoricoJornada(
            Map<String, Object> row,
            Map<String, Object> esperado,
            java.time.LocalDate fechaConsulta) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        Integer idInicio = toInteger(findValue(row, "idInicio", "id_inicio"));
        Integer idTecnico = toInteger(findValue(row, "idTecnico", "id_tecnico", "id_vendedor", "idUsuarioTecnico"));
        Integer idTecnicoEsperado = toInteger(findValue(row, "idTecnicoEsperado", "id_tecnico_esperado"));
        if (idTecnicoEsperado != null && idTecnicoEsperado > 0) {
            idTecnico = idTecnicoEsperado;
        }
        if (esperado != null) {
            Integer idEsperado = toInteger(findValue(esperado, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico"));
            if (idEsperado != null && idEsperado > 0) {
                idTecnico = idEsperado;
            }
        }
        Object fechaInicio = findValue(row, "fechaInicio", "fecha_inicio", "fechaRegistro", "fecha_registro");
        Object fechaCierre = findValue(row, "fechaCierre", "fecha_cierre");
        Integer pendiente = toInteger(findValue(row, "pendiente"));
        Integer noMarcoCierre = toInteger(findValue(row, "noMarcoCierre", "no_marco_cierre"));
        Integer idUsuarioInicio = toInteger(findValue(row, "id_tecnico", "idTecnico"));
        String tecnicoNombre = toText(findValue(row, "tecnicoNombre", "tecnico", "nombreTecnico", "nombre_tecnico"));
        if (tecnicoNombre == null && esperado != null) {
            tecnicoNombre = toText(findValue(esperado, "tecnicoNombre", "tecnico"));
        }
        String sucursal = toText(findValue(row, "sucursal"));
        if (sucursal == null && esperado != null) {
            sucursal = toText(findValue(esperado, "sucursal"));
        }
        String grupo = toText(findValue(row, "grupo"));
        if (grupo == null && esperado != null) {
            grupo = toText(findValue(esperado, "grupo"));
        }
        Object idSupervisor = findValue(row, "idSupervisor", "id_supervisor", "id_encargado");
        if (idSupervisor == null && esperado != null) {
            idSupervisor = findValue(esperado, "idSupervisor");
        }
        String supervisorNombre = toText(findValue(row, "supervisorNombre", "supervisor", "nombreSupervisor"));
        if (supervisorNombre == null && esperado != null) {
            supervisorNombre = toText(findValue(esperado, "supervisorNombre"));
        }

        out.put("idInicio", idInicio);
        out.put("idTecnico", idTecnico);
        out.put("idUsuarioInicio", idUsuarioInicio);
        out.put("id_usuario_inicio", idUsuarioInicio);
        out.put("tecnicoNombre", tecnicoNombre == null ? ("Tecnico " + idTecnico) : tecnicoNombre);
        out.put("fecha", fechaConsulta.toString());
        out.put("fechaInicio", fechaInicio);
        out.put("fechaCierre", fechaCierre);
        out.put("pendiente", pendiente);
        out.put("noMarcoCierre", noMarcoCierre);
        out.put("no_marco_cierre", noMarcoCierre);
        out.put("sucursal", sucursal);
        out.put("grupo", grupo);
        out.put("idSupervisor", idSupervisor);
        out.put("supervisorNombre", supervisorNombre);
        boolean sinCierre = (noMarcoCierre != null && noMarcoCierre == 1) || fechaCierre == null;
        out.put("sinInicio", false);
        out.put("sinCierre", sinCierre);
        if (pendiente != null && pendiente == 1) {
            out.put("estadoJornada", "NO_APROBADO_SUPERVISOR");
        } else if (sinCierre) {
            out.put("estadoJornada", "NO_REALIZO_CIERRE");
        } else {
            out.put("estadoJornada", "JORNADA_COMPLETA");
        }
        return out;
    }

    private Map<String, Object> crearJornadaSinInicio(Integer idTecnico, Map<String, Object> esperado, java.time.LocalDate fechaConsulta) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idInicio", null);
        out.put("idTecnico", idTecnico);
        out.put("tecnicoNombre", toText(findValue(esperado, "tecnicoNombre", "tecnico")));
        out.put("fecha", fechaConsulta.toString());
        out.put("fechaInicio", null);
        out.put("fechaCierre", null);
        out.put("sucursal", toText(findValue(esperado, "sucursal")));
        out.put("grupo", toText(findValue(esperado, "grupo")));
        out.put("idSupervisor", findValue(esperado, "idSupervisor"));
        out.put("supervisorNombre", toText(findValue(esperado, "supervisorNombre")));
        out.put("sinInicio", true);
        out.put("sinCierre", true);
        out.put("estadoJornada", "NO_INICIO");
        return out;
    }

    public Map<String, Object> obtenerDetalleInicioJornada(Integer idInicio) {
        if (idInicio == null || idInicio <= 0) {
            return java.util.Collections.emptyMap();
        }
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                    "SELECT id_inicio, id_tecnico, id_auxiliar, id_encargado, fecha_registro, fecha_cierre, " +
                            "pendiente, capacitado, charla, botiquin, extintor, fecha_vencimiento, equipo_epp, " +
                            "estado_epp, apr, escalera, anclaje, e_eliminado, codigo_cliente, dano_material, " +
                            "observacion_material, dano_persona, observacion_persona, novedades_trabajo, " +
                            "observacion_novedades, ubicacion_georef, no_marco_cierre, id_usuario_supervisor_grupo, " +
                            "id_sucursal, sucursal, nombre_tecnico, tecnico_nombre, firma_inicio, firma_cierre " +
                            "FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ?",
                    idInicio
            );
            return rows == null || rows.isEmpty() ? java.util.Collections.emptyMap() : rows.get(0);
        } catch (Exception ex) {
            return java.util.Collections.emptyMap();
        }
    }

    public Object obtenerImagenInicioJornada(Integer idInicio) {
        if (idInicio == null || idInicio <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                    "SELECT imagen FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ? AND ISNULL(e_eliminado, 0) = 0",
                    idInicio
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return findValue(rows.get(0), "imagen", "Imagen");
        } catch (Exception ex) {
            return null;
        }
    }

    public Object obtenerImagenAuxiliarInicioJornada(Integer idInicio) {
        if (idInicio == null || idInicio <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                    "SELECT imagen_auxiliar FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ? AND ISNULL(e_eliminado, 0) = 0",
                    idInicio
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return findValue(rows.get(0), "imagen_auxiliar", "imagenAuxiliar");
        } catch (Exception ex) {
            return null;
        }
    }

    public Object obtenerFirmaInicioJornada(Integer idInicio) {
        if (idInicio == null || idInicio <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                    "SELECT firma_inicio FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ? AND ISNULL(e_eliminado, 0) = 0",
                    idInicio
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return findValue(rows.get(0), "firma_inicio", "firmaInicio");
        } catch (Exception ex) {
            return null;
        }
    }

    public Object obtenerFirmaCierreJornada(Integer idInicio) {
        if (idInicio == null || idInicio <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                    "SELECT firma_cierre FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ? AND ISNULL(e_eliminado, 0) = 0",
                    idInicio
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return findValue(rows.get(0), "firma_cierre", "firmaCierre");
        } catch (Exception ex) {
            return null;
        }
    }

    public int aprobarInicioJornada(Integer idSupervisor, Integer idInicio) {
        Integer updated = tigohogarJdbcTemplate.queryForObject(
                "EXEC dbo.SP_Inicio_AprobarSupervisor ?, ?",
                Integer.class,
                idInicio,
                idSupervisor
        );
        return updated == null ? 0 : updated;
    }

    public int rechazarInicioJornada(Integer idSupervisor, Integer idInicio) {
        Integer updated = tigohogarJdbcTemplate.queryForObject(
                "EXEC dbo.SP_Inicio_RechazarSupervisor ?, ?",
                Integer.class,
                idInicio,
                idSupervisor
        );
        return updated == null ? 0 : updated;
    }

    public int aprobarInicioJornadaPorId(Integer idInicio) {
        Integer updated = tigohogarJdbcTemplate.queryForObject(
                "EXEC dbo.SP_Inicio_AprobarPorIdHoy ?",
                Integer.class,
                idInicio
        );
        return updated == null ? 0 : updated;
    }

    public int rechazarInicioJornadaPorId(Integer idInicio) {
        Integer updated = tigohogarJdbcTemplate.queryForObject(
                "EXEC dbo.SP_Inicio_RechazarPorIdHoy ?",
                Integer.class,
                idInicio
        );
        return updated == null ? 0 : updated;
    }

    private int resolveLimit(Integer limite) {
        if (limite == null || limite <= 0) {
            return 200;
        }
        return Math.min(limite, 1000);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(normalize(entry.getKey()), entry.getValue());
        }
        for (String key : keys) {
            String normalizedKey = normalize(key);
            if (normalized.containsKey(normalizedKey)) {
                return normalized.get(normalizedKey);
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private List<Map<String, Object>> enriquecerNombresTecnicos(List<Map<String, Object>> rows, String sucursal) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? new ArrayList<>() : rows;
        }
        String sucursalBase = toText(sucursal);
        Map<String, JdbcTemplate> templatesPorSucursal = new HashMap<>();
        Map<String, Map<Integer, String>> usuariosPorSucursal = new HashMap<>();
        Map<Integer, JdbcTemplate> templatesPorIdSucursal = new HashMap<>();
        Map<Integer, Map<Integer, String>> usuariosPorIdSucursal = new HashMap<>();
        JdbcTemplate sucursalTemplateBase = null;
        Map<Integer, String> usuariosBase = new LinkedHashMap<>();
        if (sucursalBase != null) {
            try {
                sucursalTemplateBase = resolveJdbcTemplateBySucursalNombre(sucursalBase);
            } catch (Exception ignored) {}
            usuariosBase = cargarUsuariosDesdeSucursalPreferida(sucursalBase);
        } else {
            usuariosBase = cargarUsuariosDesdeTecnicosSp();
        }
        Map<Integer, Map<String, Object>> detallesInicio = cargarDetallesInicioJornada(rows);
        for (Map<String, Object> row : rows) {
            Integer idInicio = toInteger(findValue(row, "idInicio", "id_inicio"));
            Map<String, Object> inicioDetalle = idInicio == null ? null : detallesInicio.get(idInicio);
            Integer idSucursalFila = toInteger(findValue(inicioDetalle, "id_sucursal", "idSucursal", "Id_Sucursal"));
            if (idSucursalFila == null || idSucursalFila <= 0) {
                idSucursalFila = toInteger(findValue(row, "id_sucursal", "idSucursal", "Id_Sucursal"));
            }
            String sucursalFila = firstText(
                    toText(findValue(inicioDetalle, "sucursal", "Sucursal")),
                    toText(findValue(row, "sucursal", "Sucursal")),
                    sucursalBase
            );
            JdbcTemplate sucursalTemplate = sucursalTemplateBase;
            Map<Integer, String> usuarios = usuariosBase;
            if (idSucursalFila != null && idSucursalFila > 0) {
                final Integer idSucursalKey = idSucursalFila;
                sucursalTemplate = templatesPorIdSucursal.computeIfAbsent(idSucursalKey, ignored -> {
                    try {
                        return resolveJdbcTemplateBySucursal(idSucursalKey);
                    } catch (Exception ex) {
                        return null;
                    }
                });
                JdbcTemplate templateFinal = sucursalTemplate;
                usuarios = usuariosPorIdSucursal.computeIfAbsent(idSucursalKey, ignored -> cargarUsuariosDesdeTemplate(templateFinal));
            } else if (sucursalFila != null && !normalize(sucursalFila).equals(normalize(sucursalBase))) {
                final String key = normalize(sucursalFila);
                sucursalTemplate = templatesPorSucursal.computeIfAbsent(key, ignored -> {
                    try {
                        return resolveJdbcTemplateBySucursalNombre(sucursalFila);
                    } catch (Exception ex) {
                        return null;
                    }
                });
                usuarios = usuariosPorSucursal.computeIfAbsent(key, ignored -> cargarUsuariosDesdeSucursalPreferida(sucursalFila));
            }
            Integer idTecnico = toInteger(findValue(row, "idTecnico", "id_tecnico"));
            Integer idAuxiliar = toInteger(findValue(row, "idAuxiliar", "id_auxiliar"));
            Integer idSupervisor = toInteger(findValue(row, "idSupervisor", "id_supervisor", "id_encargado"));
            String tecnicoNombreInicio = toText(findValue(inicioDetalle, "nombre_tecnico", "tecnico_nombre", "nombreTecnico", "tecnicoNombre"));
            if (idTecnico != null) {
                String nombre = usuarios.get(idTecnico);
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreUsuarioSucursal(sucursalTemplate, idTecnico);
                }
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idTecnico);
                }
                if ((nombre == null || nombre.trim().isEmpty()) && tecnicoNombreInicio != null) {
                    nombre = tecnicoNombreInicio.trim();
                }
                if (nombre != null && !nombre.trim().isEmpty()) {
                    row.put("tecnicoNombre", nombre.trim());
                }
            } else if (tecnicoNombreInicio != null && !tecnicoNombreInicio.trim().isEmpty()) {
                row.put("tecnicoNombre", tecnicoNombreInicio.trim());
            }
            if (idAuxiliar != null) {
                String nombre = usuarios.get(idAuxiliar);
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreUsuarioSucursal(sucursalTemplate, idAuxiliar);
                }
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idAuxiliar);
                }
                if (nombre != null && !nombre.trim().isEmpty()) {
                    row.put("auxiliarNombre", nombre.trim());
                }
            }
            if ((toText(findValue(row, "supervisorNombre", "supervisor", "nombreSupervisor")) == null) && idSupervisor != null) {
                String nombre = usuarios.get(idSupervisor);
                if (nombre != null && !nombre.trim().isEmpty()) {
                    row.put("supervisorNombre", nombre.trim());
                }
            }
            if (inicioDetalle != null && !inicioDetalle.isEmpty()) {
                copyIfMissing(row, inicioDetalle, "capacitado", "charla", "botiquin", "extintor", "fecha_vencimiento", "equipo_epp", "estado_epp", "apr", "escalera", "anclaje", "ubicacion_georef");
                copyIfMissing(row, inicioDetalle,
                        "codigo_cliente_cierre", "codigoClienteCierre", "codigo_cliente", "codigoCliente",
                        "dano_material", "danoMaterial",
                        "observacion_material", "observacionMaterial", "observacion_dano_material",
                        "dano_persona", "danoPersona",
                        "observacion_persona", "observacionPersona", "observacion_dano_persona",
                        "novedades_trabajo", "novedadesTrabajo",
                        "observacion_novedades", "observacionNovedades", "observacion_novedades_trabajo",
                        "ubicacion_cierre_georef", "ubicacionCierreGeoref", "ubicacion_georef_cierre", "ubicacionGeoRefCierre"
                );
            }
        }
        return rows;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = toText(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private Map<Integer, Map<String, Object>> cargarDetallesInicioJornada(List<Map<String, Object>> rows) {
        Map<Integer, Map<String, Object>> out = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        Set<Integer> idsSet = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Integer idInicio = toInteger(findValue(row, "idInicio", "id_inicio"));
            if (idInicio != null && idInicio > 0) {
                idsSet.add(idInicio);
            }
        }
        List<Integer> ids = new ArrayList<>(idsSet);
        if (ids.isEmpty()) {
            return out;
        }
        int chunkSize = 800;
        for (int from = 0; from < ids.size(); from += chunkSize) {
            int to = Math.min(from + chunkSize, ids.size());
            cargarDetallesInicioJornadaChunk(ids.subList(from, to), out);
        }
        return out;
    }

    private void cargarDetallesInicioJornadaChunk(List<Integer> ids, Map<Integer, Map<String, Object>> out) {
        if (ids == null || ids.isEmpty() || out == null) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id_inicio, id_tecnico, id_auxiliar, id_encargado, fecha_registro, fecha_cierre, " +
                        "pendiente, capacitado, charla, botiquin, extintor, fecha_vencimiento, equipo_epp, " +
                        "estado_epp, apr, escalera, anclaje, e_eliminado, codigo_cliente, dano_material, " +
                        "observacion_material, dano_persona, observacion_persona, novedades_trabajo, " +
                        "observacion_novedades, ubicacion_georef, no_marco_cierre, id_usuario_supervisor_grupo, " +
                        "id_sucursal, sucursal, nombre_tecnico, tecnico_nombre, firma_inicio, firma_cierre " +
                        "FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio IN ("
        );
        Object[] params = new Object[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
            params[i] = ids.get(i);
        }
        sql.append(")");
        try {
            List<Map<String, Object>> detalles = tigohogarJdbcTemplate.queryForList(sql.toString(), params);
            if (detalles == null || detalles.isEmpty()) {
                return;
            }
            for (Map<String, Object> detalle : detalles) {
                Integer idInicio = toInteger(findValue(detalle, "id_inicio", "idInicio"));
                if (idInicio != null && idInicio > 0 && !out.containsKey(idInicio)) {
                    out.put(idInicio, detalle);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void copyIfMissing(Map<String, Object> target, Map<String, Object> source, String... keys) {
        if (target == null || source == null || keys == null) {
            return;
        }
        for (String key : keys) {
            Object value = findValue(source, key);
            if (value == null) {
                continue;
            }
            if (findValue(target, key) == null) {
                target.put(key, value);
            }
        }
    }

    private Map<Integer, String> cargarUsuariosDesdeSucursalPreferida(String sucursal) {
        Map<Integer, String> out = new LinkedHashMap<>();
        JdbcTemplate template;
        try {
            template = resolveJdbcTemplateBySucursalNombre(sucursal);
        } catch (Exception ex) {
            return out;
        }
        return cargarUsuariosDesdeTemplate(template);
    }

    private Map<Integer, String> cargarUsuariosDesdeTemplate(JdbcTemplate template) {
        Map<Integer, String> out = new LinkedHashMap<>();
        if (template == null) {
            return out;
        }
        List<Map<String, Object>> usuariosRows = new ArrayList<>();
        try {
            usuariosRows.addAll(template.queryForList(
                    "EXEC dbo.SP_Usuario_ListarActivosBasico"
            ));
        } catch (Exception ignored) {}

        for (Map<String, Object> row : usuariosRows) {
            Integer id = toInteger(findValue(row, "idUsuario", "id_usuario", "Id_Usuario"));
            String nombre = toText(findValue(row, "nombre", "Nombre", "tecnico"));
            if (id == null || id <= 0 || nombre == null || nombre.trim().isEmpty()) continue;
            out.put(id, nombre.trim());
        }
        return out;
    }

    private Map<Integer, String> cargarUsuariosDesdeTecnicosSp() {
        Map<Integer, String> fromTigohogar = cargarUsuariosDesdeTigoHogar();
        if (!fromTigohogar.isEmpty()) {
            return fromTigohogar;
        }
        Map<Integer, String> out = new LinkedHashMap<>();
        JdbcTemplate operativa;
        try {
            operativa = dbConnectionManager.connDb("operativa");
        } catch (Exception ex) {
            return out;
        }
        List<Map<String, Object>> rows;
        try {
            rows = operativa.queryForList("EXEC dbo.spx_ObtenerListaUsuario");
        } catch (Exception ex) {
            return out;
        }
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Integer id = toInteger(findValue(row, "Id_Usuario", "id_usuario", "idusuario", "IdUsuario"));
            String nombre = toText(findValue(row, "Nombre", "nombre", "tecnico", "usuario"));
            if (id == null || id <= 0 || nombre == null || nombre.trim().isEmpty()) {
                continue;
            }
            out.put(id, nombre.trim());
        }
        return out;
    }

    private Map<Integer, String> cargarUsuariosDesdeTigoHogar() {
        Map<Integer, String> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows;
        try {
            rows = tigohogarJdbcTemplate.queryForList(
                    "EXEC dbo.SP_Usuario_ListarActivosBasico"
            );
        } catch (Exception ex) {
            return out;
        }
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Integer id = toInteger(findValue(row, "idUsuario", "Id_Usuario", "id_usuario"));
            String nombre = toText(findValue(row, "nombre", "Nombre"));
            if (id == null || id <= 0 || nombre == null || nombre.trim().isEmpty()) {
                continue;
            }
            out.put(id, nombre.trim());
        }
        return out;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlankText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String text = toText(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String normText(String value) {
        String text = toText(value);
        if (text == null) return null;
        String normalized = text
                .toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Map<String, Object>> enriquecerTecnicosDesdeSucursal(List<Map<String, Object>> idsRows, JdbcTemplate sucursalTemplate) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (idsRows == null || idsRows.isEmpty()) return out;
        for (Map<String, Object> row : idsRows) {
            Integer idTecnico = toInteger(findValue(row, "idTecnico", "id_tecnico", "id_vendedor", "idUsuarioTecnico"));
            if (idTecnico == null || idTecnico <= 0) continue;
            Map<String, Object> vendedor = obtenerDatosVendedorSucursal(sucursalTemplate, idTecnico);
            String nombre = toText(findValue(vendedor, "Nombre", "nombre"));
            if (nombre == null) {
                nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idTecnico);
                if (nombre == null) {
                    nombre = toText(findValue(row, "tecnico", "Tecnico", "nombre", "Nombre"));
                }
            }
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("idTecnico", idTecnico);
            mapped.put("id_tecnico", idTecnico);
            mapped.put("tecnico", nombre == null ? ("Tecnico " + idTecnico) : nombre);
            String codEmpleado = toText(findValue(row, "codigo", "Codigo", "codEmpleado", "CodEmpleado", "cod_empleado"));
            if (codEmpleado == null) {
                codEmpleado = toText(findValue(vendedor, "CodEmpleado", "codEmpleado", "cod_empleado"));
            }
            if (codEmpleado != null) {
                mapped.put("codigo", codEmpleado);
                mapped.put("codEmpleado", codEmpleado);
                mapped.put("cod_empleado", codEmpleado);
            }
            out.add(mapped);
        }
        return out;
    }

    private Map<String, Object> obtenerDatosVendedorSucursal(JdbcTemplate template, Integer idTecnico) {
        if (template == null || idTecnico == null || idTecnico <= 0) {
            return java.util.Collections.emptyMap();
        }
        try {
            List<Map<String, Object>> rows = template.queryForList(
                    "SELECT TOP 1 Id_Vendedor, Nombre, CodEmpleado " +
                            "FROM dbo.tbl_Vendedor " +
                            "WHERE Id_Vendedor = ? AND ISNULL(E_Eliminado,0)=0",
                    idTecnico
            );
            if (rows != null && !rows.isEmpty()) {
                return rows.get(0);
            }
        } catch (Exception ignored) {}
        return java.util.Collections.emptyMap();
    }

    private String obtenerNombreUsuarioSucursal(JdbcTemplate template, Integer idUsuario) {
        if (template == null || idUsuario == null || idUsuario <= 0) {
            return null;
        }
        String nombre = queryNombre(
                template,
                "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ? AND ISNULL(E_Eliminado,0)=0",
                idUsuario
        );
        if (nombre != null) return nombre;
        nombre = queryNombre(
                template,
                "SELECT TOP 1 u.Nombre AS Nombre " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario " +
                        "WHERE ut.id_Usuario = ? AND ISNULL(ut.e_eliminado,0)=0",
                idUsuario
        );
        return nombre;
    }

    private String obtenerNombreTecnicoSucursal(JdbcTemplate template, Integer idTecnico) {
        if (template == null || idTecnico == null || idTecnico <= 0) {
            return null;
        }
        String nombre = queryNombre(
                template,
                "SELECT TOP 1 Nombre FROM dbo.tbl_Vendedor WHERE Id_Vendedor = ? AND ISNULL(E_Eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;
        nombre = queryNombre(
                template,
                "SELECT TOP 1 u.Nombre AS Nombre " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario " +
                        "WHERE ut.Id_Vendedor = ? AND ISNULL(ut.e_eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;
        nombre = queryNombre(
                template,
                "SELECT TOP 1 u.Nombre AS Nombre " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario " +
                        "WHERE ut.id_Usuario = ? AND ISNULL(ut.e_eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;
        nombre = queryNombre(
                template,
                "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ? AND ISNULL(E_Eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;
        try {
            List<Map<String, Object>> v = template.queryForList(
                    "EXEC dbo.SP_Tecnico_ObtenerNombrePorId ?",
                    idTecnico
            );
            if (!v.isEmpty()) {
                String n = toText(v.get(0).get("Nombre"));
                if (n != null && !n.isEmpty()) return n;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String queryNombre(JdbcTemplate template, String sql, Integer id) {
        try {
            List<Map<String, Object>> rows = template.queryForList(sql, id);
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            Object value = findValue(rows.get(0), "Nombre", "nombre", "tecnico");
            String text = toText(value);
            return text == null || text.trim().isEmpty() ? null : text.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer queryInteger(JdbcTemplate template, String sql, Integer id) {
        try {
            List<Map<String, Object>> rows = template.queryForList(sql, id);
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            for (Object value : rows.get(0).values()) {
                Integer parsed = toInteger(value);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveNombrePersona(Integer idPersona, Map<Integer, String> usuarios, JdbcTemplate sucursalTemplate) {
        if (idPersona == null || idPersona <= 0) {
            return null;
        }
        String nombre = usuarios == null ? null : usuarios.get(idPersona);
        if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
            nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idPersona);
        }
        if (nombre == null) {
            return null;
        }
        String out = nombre.trim();
        return out.isEmpty() ? null : out;
    }

    private boolean isNumericText(String value) {
        if (value == null) {
            return false;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return false;
        }
        return text.matches("^\\d+$");
    }

    private JdbcTemplate resolveJdbcTemplateBySucursal(Integer idSucursal) {
        if (idSucursal == null || idSucursal <= 0) {
            return dbConnectionManager.connDb("operativa");
        }
        List<Map<String, Object>> sucursales = dbConnectionManager.connDb("operativa")
                .queryForList("EXEC dbo.spx_ObtenerSucursalesConexion");
        for (Map<String, Object> row : sucursales) {
            Integer id = toInteger(findValue(row, "Id_Sucursal", "idSucursal", "id_sucursal"));
            if (id == null || !idSucursal.equals(id)) {
                continue;
            }
            String host = toText(findValue(row, "ip", "IP", "host"));
            if (host == null) {
                host = toText(findValue(row, "ip2", "IP2", "hostAlterno"));
            }
            String base = toText(findValue(row, "BaseDeDatos", "baseDeDatos", "database", "db"));
            if (host != null && base != null) {
                return dbConnectionManager.connDb("sucursal-" + idSucursal, host, base, dbUsername, dbPassword);
            }
            break;
        }
        return dbConnectionManager.connDb("operativa");
    }

    private JdbcTemplate resolveJdbcTemplateBySucursalNombre(String sucursal) {
        if (sucursal == null || sucursal.trim().isEmpty()) {
            return dbConnectionManager.connDb("operativa");
        }
        String value = normalize(sucursal);
        List<Map<String, Object>> sucursales = dbConnectionManager.connDb("operativa")
                .queryForList("EXEC dbo.spx_ObtenerSucursalesConexion");
        for (Map<String, Object> row : sucursales) {
            String nombre = toText(findValue(row, "Sucursal", "sucursal"));
            if (nombre == null || !value.equals(normalize(nombre))) {
                continue;
            }
            String host = toText(findValue(row, "ip", "IP", "host"));
            if (host == null) {
                host = toText(findValue(row, "ip2", "IP2", "hostAlterno"));
            }
            String base = toText(findValue(row, "BaseDeDatos", "baseDeDatos", "database", "db"));
            Integer id = toInteger(findValue(row, "Id_Sucursal", "idSucursal", "id_sucursal"));
            if (host != null && base != null && id != null) {
                return dbConnectionManager.connDb("sucursal-" + id, host, base, dbUsername, dbPassword);
            }
            break;
        }
        return dbConnectionManager.connDb("operativa");
    }

    public String registrarPendiente(
            String idSupervisorAsignado,
            String idTecnicoPrincipal,
            String idTecnicoAuxiliar,
            String idTipoSupervision,
            String idTipoTrabajo,
            String idTipoPenalizacion,
            String supervisionPor,
            String tecnologia,
            String codigo,
            String ordenTrabajo,
            String tipoRevision,
            String fotoBoletaSupervision,
            String fotoCanalesPilos,
            String fotoNivelesDocsis,
            String fotoMedicionRuido,
            String fotoBarridoCanales,
            String fotoObservacion1,
            String fotoObservacion2,
            String fotoObservacion3,
            String fotoObservacion4,
            String observacion,
            String descripcionAdicionalObservacion,
            String ubicacion,
            String creadoPor) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                SP_REGISTRAR_PENDIENTE,
                trimToNull(idSupervisorAsignado),
                trimToNull(idTecnicoPrincipal),
                trimToNull(idTecnicoAuxiliar),
                trimToNull(idTipoSupervision),
                trimToNull(idTipoTrabajo),
                trimToNull(idTipoPenalizacion),
                trimToNull(supervisionPor),
                trimToNull(tecnologia),
                trimToNull(codigo),
                trimToNull(ordenTrabajo),
                trimToNull(tipoRevision),
                trimToNull(fotoBoletaSupervision),
                trimToNull(fotoCanalesPilos),
                trimToNull(fotoNivelesDocsis),
                trimToNull(fotoMedicionRuido),
                trimToNull(fotoBarridoCanales),
                trimToNull(fotoObservacion1),
                trimToNull(fotoObservacion2),
                trimToNull(fotoObservacion3),
                trimToNull(fotoObservacion4),
                trimToNull(observacion),
                trimToNull(descripcionAdicionalObservacion),
                trimToNull(ubicacion),
                trimToNull(creadoPor)
        );

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            Object id = findValue(row, "idSupervision", "id_supervision", "Id_Supervision");
            if (id == null && !row.isEmpty()) {
                id = row.values().iterator().next();
            }
            if (id != null) {
                String text = String.valueOf(id).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        throw new IllegalStateException("No se pudo obtener Id_Supervision generado.");
    }

    public List<Map<String, Object>> listarSupervisores(String sucursal) {
        JdbcTemplate template = resolveTemplateSupervisores(sucursal);
        List<Map<String, Object>> rows = filtrarSupervisoresActivos(listarSupervisoresDesdeTemplate(template));
        if (rows != null && !rows.isEmpty()) {
            return rows;
        }
        return filtrarSupervisoresActivos(listarSupervisoresDesdeCentral(sucursal));
    }

    private List<Map<String, Object>> listarSupervisoresDesdeCentral(String sucursal) {
        String sucursalNorm = trimToNull(sucursal);
        try {
            JdbcTemplate central = dbConnectionManager.connDb("bdcontrolordenes");
            return central.queryForList(
                    "SELECT " +
                            "  CAST(c.idUsuarioSupervisor AS INT) AS idSupervisor, " +
                            "  CAST(c.idUsuarioSupervisor AS INT) AS idUsuarioSupervisor, " +
                            "  CAST(MAX(NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo,''))), '')) AS NVARCHAR(200)) AS nombre, " +
                            "  CAST(MAX(NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo,''))), '')) AS NVARCHAR(200)) AS supervisor, " +
                            "  CAST(MIN(c.sucursal) AS NVARCHAR(100)) AS sucursal " +
                            "FROM dbo.tbl_ConformacionCuadrillaDiario c " +
                            "INNER JOIN dbo.tbl_Usuario u ON u.Id_Usuario = c.idUsuarioSupervisor AND ISNULL(u.E_Eliminado,0)=0 " +
                            "WHERE ISNULL(c.e_eliminado,0)=0 " +
                            "  AND c.idUsuarioSupervisor IS NOT NULL " +
                            "  AND NULLIF(LTRIM(RTRIM(ISNULL(c.supervisorACargo,''))), '') IS NOT NULL " +
                            "  AND (? IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(ISNULL(c.sucursal,''), ' ', ''), '_', ''), '-', '')) = " +
                            "                  LOWER(REPLACE(REPLACE(REPLACE(?, ' ', ''), '_', ''), '-', ''))) " +
                            "GROUP BY c.idUsuarioSupervisor " +
                            "ORDER BY supervisor",
                    sucursalNorm,
                    sucursalNorm
            );
        } catch (Exception ex) {
            return java.util.Collections.emptyList();
        }
    }

    private List<Map<String, Object>> listarSupervisoresDesdeTemplate(JdbcTemplate template) {
        try {
            return template.queryForList(SP_LISTAR_SUPERVISORES);
        } catch (Exception e1) {
            try {
                return template.queryForList(SP_LISTAR_SUPERVISORES_ALT);
            } catch (Exception e2) {
                try {
                    return template.queryForList(SP_LISTAR_SUPERVISORES_ALT2);
                } catch (Exception e3) {
                    try {
                        return template.queryForList(SP_LISTAR_SUPERVISORES_ALT3);
                    } catch (Exception e4) {
                        return listarSupervisoresDesdeTigoHogar();
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> listarSupervisoresDesdeTigoHogar() {
        try {
            return tigohogarJdbcTemplate.queryForList(SP_LISTAR_SUPERVISORES);
        } catch (Exception e1) {
            try {
                return tigohogarJdbcTemplate.queryForList(SP_LISTAR_SUPERVISORES_ALT);
            } catch (Exception e2) {
                try {
                    return tigohogarJdbcTemplate.queryForList(SP_LISTAR_SUPERVISORES_ALT2);
                } catch (Exception e3) {
                    try {
                        return tigohogarJdbcTemplate.queryForList(SP_LISTAR_SUPERVISORES_ALT3);
                    } catch (Exception e4) {
                        return java.util.Collections.emptyList();
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> filtrarSupervisoresActivos(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object eliminado = findValue(row, "E_Eliminado", "e_eliminado", "eliminado");
            if (toInteger(eliminado) != null && toInteger(eliminado) != 0) {
                continue;
            }
            Object id = findValue(row, "idSupervisor", "idUsuarioSupervisor", "Id_Usuario", "idUsuario", "id_usuario", "id");
            String key = id == null ? "" : String.valueOf(id).trim().replaceAll("[^0-9]", "");
            if (key.isEmpty()) {
                key = id == null ? "" : String.valueOf(id).trim();
            }
            if (!key.isEmpty() && !vistos.add(key)) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    private JdbcTemplate resolveTemplateSupervisores(String sucursal) {
        return resolveJdbcTemplateBySucursalNombre(sucursal);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal) {
        return listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursal, null);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal, String supervisorNombre) {
        // Requisito funcional: en agendar supervision se debe mostrar siempre el universo de tecnicos.
        // Se ignora idSupervisor y se lista desde todos los grupos de la sucursal.
        return listarTecnicosDeGrupos(sucursal);
    }
}
