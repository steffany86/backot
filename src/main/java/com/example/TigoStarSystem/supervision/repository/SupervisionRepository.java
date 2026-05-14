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
import java.util.Locale;
import java.util.Map;

@Repository
public class SupervisionRepository {
    private static final String SP_LISTAR =
            "EXEC dbo.spx_ListarSupervisionManual ?, ?, ?, ?";
    private static final String SP_OBTENER_DETALLE =
            "EXEC dbo.spx_ObtenerSupervisionManualPorId ?, ?";
    private static final String SP_REGISTRAR =
            "EXEC dbo.spx_RegistrarSupervisionManual ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    private static final String SP_LISTAR_TECNICOS_SUPERVISOR_CENTRAL =
            "EXEC dbo.spx_Central_ObtenerTecnicosPorSupervisorConformacion ?, ?";

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

    public Map<String, Object> obtenerDetalle(String idSupervision, String idSupervisor) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(SP_OBTENER_DETALLE, idSupervision, idSupervisor);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
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
        JdbcTemplate central = dbConnectionManager.connDb("central");
        List<Map<String, Object>> ids = central.queryForList(SP_LISTAR_TECNICOS_SUPERVISOR_CENTRAL, idSupervisor, trimToNull(sucursal));
        JdbcTemplate sucursalTemplate = resolveJdbcTemplateBySucursalNombre(sucursal);
        return enriquecerTecnicosDesdeSucursal(ids, sucursalTemplate);
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
        JdbcTemplate sucursalTemplate = null;
        try {
            sucursalTemplate = resolveJdbcTemplateBySucursalNombre(sucursal);
        } catch (Exception ignored) {}
        Map<Integer, String> usuarios = cargarUsuariosDesdeSucursalPreferida(sucursal);
        if (usuarios.isEmpty()) {
            usuarios = cargarUsuariosDesdeTecnicosSp();
        }
        for (Map<String, Object> row : rows) {
            Integer idTecnico = toInteger(findValue(row, "idTecnico", "id_tecnico"));
            Integer idAuxiliar = toInteger(findValue(row, "idAuxiliar", "id_auxiliar"));
            if ((toText(findValue(row, "tecnicoNombre", "tecnico_nombre", "tecnico")) == null) && idTecnico != null) {
                String nombre = usuarios.get(idTecnico);
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idTecnico);
                }
                if (nombre != null && !nombre.trim().isEmpty()) {
                    row.put("tecnicoNombre", nombre.trim());
                }
            }
            if ((toText(findValue(row, "auxiliarNombre", "auxiliar_nombre", "auxiliar")) == null) && idAuxiliar != null) {
                String nombre = usuarios.get(idAuxiliar);
                if ((nombre == null || nombre.trim().isEmpty()) && sucursalTemplate != null) {
                    nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idAuxiliar);
                }
                if (nombre != null && !nombre.trim().isEmpty()) {
                    row.put("auxiliarNombre", nombre.trim());
                }
            }
        }
        return rows;
    }

    private Map<Integer, String> cargarUsuariosDesdeSucursalPreferida(String sucursal) {
        Map<Integer, String> out = new LinkedHashMap<>();
        JdbcTemplate template;
        try {
            template = resolveJdbcTemplateBySucursalNombre(sucursal);
        } catch (Exception ex) {
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

    private List<Map<String, Object>> enriquecerTecnicosDesdeSucursal(List<Map<String, Object>> idsRows, JdbcTemplate sucursalTemplate) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (idsRows == null || idsRows.isEmpty()) return out;
        for (Map<String, Object> row : idsRows) {
            Integer idTecnico = toInteger(findValue(row, "idTecnico", "id_tecnico", "id_vendedor", "idUsuarioTecnico"));
            if (idTecnico == null || idTecnico <= 0) continue;
            String nombre = obtenerNombreTecnicoSucursal(sucursalTemplate, idTecnico);
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("idTecnico", idTecnico);
            mapped.put("id_tecnico", idTecnico);
            mapped.put("tecnico", nombre == null ? ("Tecnico " + idTecnico) : nombre);
            out.add(mapped);
        }
        return out;
    }

    private String obtenerNombreTecnicoSucursal(JdbcTemplate template, Integer idTecnico) {
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
        if (sucursal == null || sucursal.trim().isEmpty()) return dbConnectionManager.connDb("operativa");
        String value = sucursal.trim().toLowerCase(Locale.ROOT);
        if (value.contains("sucre")) return dbConnectionManager.connDb("sucre");
        return dbConnectionManager.connDb("operativa");
    }
}
