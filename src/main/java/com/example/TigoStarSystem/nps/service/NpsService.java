package com.example.TigoStarSystem.nps.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.nps.repository.NpsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NpsService {
    private final NpsRepository repository;
    private final AuthService authService;
    private final DbConnectionManager dbConnectionManager;
    private final String defaultDbUsername;
    private final String defaultDbPassword;

    public NpsService(
            NpsRepository repository,
            AuthService authService,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String defaultDbUsername,
            @Value("${spring.datasource.password}") String defaultDbPassword) {
        this.repository = repository;
        this.authService = authService;
        this.dbConnectionManager = dbConnectionManager;
        this.defaultDbUsername = defaultDbUsername;
        this.defaultDbPassword = defaultDbPassword;
    }

    public Map<String, Object> obtenerDashboard(
            String token,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idSucursal,
            Integer idSupervisor,
            Integer idTecnico,
            String supervisorNombre,
            String tecnicoNombre) {
        Map<String, Object> scope = resolveScope(token, idSucursal, idSupervisor, idTecnico, supervisorNombre, tecnicoNombre);
        Integer idUsuarioSesion = (Integer) scope.get("idUsuarioSesion");
        Integer sucursalObjetivo = (Integer) scope.get("idSucursal");
        Integer supervisorObjetivo = (Integer) scope.get("idSupervisor");
        Integer tecnicoObjetivo = (Integer) scope.get("idTecnico");
        String supervisorNombreObjetivo = (String) scope.get("supervisorNombre");
        String tecnicoNombreObjetivo = (String) scope.get("tecnicoNombre");
        String rolConsulta = (String) scope.get("scope");
        JdbcTemplate centralTemplate = dbConnectionManager.connDb("central");

        List<Map<String, Object>> data = repository.obtenerDashboard(
                centralTemplate,
                fechaInicio,
                fechaFin,
                sucursalObjetivo,
                supervisorObjetivo,
                tecnicoObjetivo,
                supervisorNombreObjetivo,
                tecnicoNombreObjetivo,
                rolConsulta,
                idUsuarioSesion
        );

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("scope", rolConsulta);
        out.put("idSucursal", sucursalObjetivo);
        out.put("idSupervisor", supervisorObjetivo);
        out.put("idTecnico", tecnicoObjetivo);
        out.put("fallbackUltimaFecha", false);
        out.put("rows", data);
        out.put("filtros", obtenerFiltrosInterno(scope));
        return out;
    }

    public Map<String, Object> obtenerFiltros(
            String token,
            Integer idSucursal,
            Integer idSupervisor,
            Integer idTecnico,
            String supervisorNombre,
            String tecnicoNombre) {
        Map<String, Object> scope = resolveScope(token, idSucursal, idSupervisor, idTecnico, supervisorNombre, tecnicoNombre);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("scope", scope.get("scope"));
        out.put("idSucursal", scope.get("idSucursal"));
        out.put("idSupervisor", scope.get("idSupervisor"));
        out.put("idTecnico", scope.get("idTecnico"));
        out.put("filtros", obtenerFiltrosInterno(scope));
        return out;
    }

    private Map<String, Object> resolveScope(
            String token,
            Integer idSucursal,
            Integer idSupervisor,
            Integer idTecnico,
            String supervisorNombre,
            String tecnicoNombre) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = requireUsuario(me);

        Integer idUsuarioSesion = usuario.getIdUsuario();
        Integer idSucursalSesion = usuario.getIdSucursal();

        boolean esTecnico = isRol(usuario, "tecnico");
        boolean esSupervisor = isRol(usuario, "supervisor");
        boolean esCentral = isRolCentral(usuario);

        if (!esTecnico && !esSupervisor && !esCentral) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Rol sin acceso a NPS.");
        }

        Integer sucursalObjetivo = esCentral ? requireParam(idSucursal, "idSucursal") : idSucursalSesion;
        if (sucursalObjetivo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "No se pudo resolver sucursal de sesion.");
        }
        JdbcTemplate centralTemplate = dbConnectionManager.connDb("central");

        Integer supervisorObjetivo = idSupervisor;
        Integer tecnicoObjetivo = idTecnico;
        String rolConsulta;

        if (esTecnico) {
            rolConsulta = "TECNICO";
            JdbcTemplate sucursalTemplate = resolveSucursalTemplate(sucursalObjetivo);
            List<Integer> idsTecnicoNps = repository.listarIdsTecnicoNpsPorUsuario(sucursalTemplate, idUsuarioSesion);
            tecnicoObjetivo = idsTecnicoNps.isEmpty() ? idUsuarioSesion : idsTecnicoNps.get(0);
            Integer idSupervisorSesion = resolveSupervisorDelTecnico(centralTemplate, sucursalObjetivo, idUsuarioSesion);
            supervisorObjetivo = idSupervisorSesion;
            tecnicoNombre = usuario.getNombre();
        } else if (esSupervisor) {
            rolConsulta = "SUPERVISOR";
            supervisorObjetivo = idUsuarioSesion;
            if (idTecnico != null) {
                JdbcTemplate sucursalTemplate = resolveSucursalTemplate(sucursalObjetivo);
                List<Integer> idsTecnicoNps = repository.listarIdsTecnicoNpsPorUsuario(sucursalTemplate, idTecnico);
                tecnicoObjetivo = idsTecnicoNps.isEmpty() ? idTecnico : idsTecnicoNps.get(0);
            } else {
                tecnicoObjetivo = null;
            }
        } else {
            rolConsulta = "CENTRAL";
        }

        Map<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("idUsuarioSesion", idUsuarioSesion);
        scope.put("idSucursal", sucursalObjetivo);
        scope.put("idSupervisor", supervisorObjetivo);
        scope.put("idTecnico", tecnicoObjetivo);
        scope.put("supervisorNombre", "CENTRAL".equalsIgnoreCase(rolConsulta) ? trimToNull(supervisorNombre) : null);
        scope.put("tecnicoNombre",
                "TECNICO".equalsIgnoreCase(rolConsulta)
                        ? trimToNull(tecnicoNombre)
                        : ("CENTRAL".equalsIgnoreCase(rolConsulta) || "SUPERVISOR".equalsIgnoreCase(rolConsulta)
                            ? trimToNull(tecnicoNombre)
                            : null)
        );
        scope.put("scope", rolConsulta);
        return scope;
    }

    private Map<String, Object> obtenerFiltrosInterno(Map<String, Object> scope) {
        Integer idUsuarioSesion = (Integer) scope.get("idUsuarioSesion");
        Integer sucursalObjetivo = (Integer) scope.get("idSucursal");
        Integer supervisorObjetivo = (Integer) scope.get("idSupervisor");
        String rolConsulta = (String) scope.get("scope");
        JdbcTemplate sucursalTemplate = resolveSucursalTemplate(sucursalObjetivo);
        JdbcTemplate centralTemplate = dbConnectionManager.connDb("central");
        String supervisorNombre = (String) scope.get("supervisorNombre");

        List<Map<String, Object>> filtrosSupervisores = repository.listarSupervisoresSucursal(sucursalTemplate, sucursalObjetivo);
        List<Map<String, Object>> filtrosTecnicos;

        if ("TECNICO".equalsIgnoreCase(rolConsulta)) {
            filtrosTecnicos = repository.listarTecnicosPorSupervisor(
                    sucursalTemplate,
                    sucursalObjetivo,
                    supervisorObjetivo == null ? 0 : supervisorObjetivo
            );
            List<Map<String, Object>> propios = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> row : filtrosTecnicos) {
                if (idUsuarioSesion.equals(asInteger(find(row, "idTecnico", "id_tecnico", "idUsuario", "id_usuario")))) {
                    propios.add(row);
                }
            }
            filtrosTecnicos = propios;
        } else if ("SUPERVISOR".equalsIgnoreCase(rolConsulta)) {
            filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, supervisorObjetivo);
            List<Map<String, Object>> historicos = repository.listarTecnicosHistoricosSupervisorNps(centralTemplate, supervisorObjetivo, sucursalObjetivo);
            filtrosTecnicos = mergeTecnicosSinDuplicados(filtrosTecnicos, historicos);
        } else {
            List<Map<String, Object>> filtroCentral = repository.listarFiltrosCentralPorNombres(centralTemplate, sucursalObjetivo);
            List<Map<String, Object>> sup = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> tec = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> row : filtroCentral) {
                String tipo = asText(find(row, "tipo"));
                String nombre = asText(find(row, "nombre"));
                if (isBlank(nombre)) continue;
                Map<String, Object> out = new LinkedHashMap<String, Object>();
                if ("SUPERVISOR".equalsIgnoreCase(tipo)) {
                    out.put("idSupervisor", nombre);
                    out.put("supervisor", nombre);
                    sup.add(out);
                } else if ("TECNICO".equalsIgnoreCase(tipo)) {
                    if (!isBlank(supervisorNombre)) {
                        String supRow = asText(find(row, "supervisor"));
                        if (!supervisorNombre.equalsIgnoreCase(supRow)) continue;
                    }
                    out.put("idTecnico", nombre);
                    out.put("tecnico", nombre);
                    tec.add(out);
                }
            }
            filtrosSupervisores = sup;
            filtrosTecnicos = tec;
            if (filtrosTecnicos.isEmpty() && supervisorObjetivo != null) {
                filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, supervisorObjetivo);
            } else if (filtrosTecnicos.isEmpty()) {
                filtrosTecnicos = new ArrayList<Map<String, Object>>();
            }
            if (filtrosSupervisores.isEmpty()) {
                filtrosSupervisores = repository.listarSupervisoresSucursal(sucursalTemplate, sucursalObjetivo);
            }
            if (filtrosTecnicos.isEmpty() && supervisorObjetivo != null) {
                filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, supervisorObjetivo);
            } else if (filtrosTecnicos.isEmpty()) {
                filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, 0);
            }
            if (filtrosTecnicos.isEmpty()) {
                filtrosTecnicos = new ArrayList<Map<String, Object>>();
            }
            if (filtrosSupervisores.isEmpty()) {
                filtrosSupervisores = new ArrayList<Map<String, Object>>();
            }
            if (filtrosTecnicos == null) {
                filtrosTecnicos = new ArrayList<Map<String, Object>>();
            }
            if (filtrosSupervisores == null) {
                filtrosSupervisores = new ArrayList<Map<String, Object>>();
            }
            if (filtrosTecnicos.size() > 0 || filtrosSupervisores.size() > 0) {
                // ya resuelto por nombres desde NPS
            } else if (supervisorObjetivo != null) {
                filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, supervisorObjetivo);
            } else {
                filtrosTecnicos = repository.listarTecnicosPorSupervisor(sucursalTemplate, sucursalObjetivo, 0);
            }
        }

        Map<String, Object> filtros = new HashMap<String, Object>();
        filtros.put("supervisores", filtrosSupervisores);
        filtros.put("tecnicos", filtrosTecnicos);
        return filtros;
    }

    private Integer resolveSupervisorDelTecnico(JdbcTemplate centralTemplate, Integer idSucursal, Integer idTecnico) {
        List<Map<String, Object>> rows = repository.listarTecnicosDeSupervisorEnCentral(centralTemplate, idTecnico, idSucursal);
        for (Map<String, Object> row : rows) {
            Integer supervisor = asInteger(find(row, "idSupervisor", "id_supervisor", "idUsuarioSupervisor", "id_usuario_supervisor"));
            if (supervisor != null) return supervisor;
        }
        return null;
    }

    private JdbcTemplate resolveSucursalTemplate(Integer idSucursal) {
        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse s : sucursales) {
            if (s != null && idSucursal.equals(s.getIdSucursal())) {
                return dbConnectionManager.connDb(
                        "nps-sucursal-" + idSucursal,
                        s.getIp(),
                        s.getBaseDeDatos(),
                        defaultDbUsername,
                        defaultDbPassword
                );
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Sucursal no encontrada: " + idSucursal);
    }

    private boolean isRol(AuthLoginResponse usuario, String valor) {
        String rol = usuario == null ? null : usuario.getRol();
        if (rol == null) return false;
        return rol.trim().equalsIgnoreCase(valor);
    }

    private boolean isRolCentral(AuthLoginResponse usuario) {
        String rol = usuario == null ? null : usuario.getRol();
        if (rol == null) return false;
        String n = rol.trim().toLowerCase();
        return n.contains("central") || n.contains("sistema") || n.contains("admin");
    }

    private AuthLoginResponse requireUsuario(AuthMeResponse me) {
        AuthLoginResponse u = me == null ? null : me.getUsuario();
        if (u == null || u.getIdUsuario() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Sesion invalida.");
        }
        return u;
    }

    private Integer requireParam(Integer value, String field) {
        if (value == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field + " es requerido para usuario central.");
        }
        return value;
    }

    private Object find(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) return e.getValue();
            }
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) return null;
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> mergeTecnicosSinDuplicados(
            List<Map<String, Object>> actuales,
            List<Map<String, Object>> historicos) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        addTecnicos(out, seen, actuales);
        addTecnicos(out, seen, historicos);
        return out;
    }

    private void addTecnicos(
            List<Map<String, Object>> out,
            Map<String, Boolean> seen,
            List<Map<String, Object>> source) {
        if (source == null) return;
        for (Map<String, Object> row : source) {
            Object idObj = find(row, "idTecnico", "id_tecnico");
            String nombre = asText(find(row, "tecnico", "nombre", "tecnico_nombre"));
            if (isBlank(nombre)) {
                nombre = asText(idObj);
            }
            if (isBlank(nombre)) continue;
            String key = normalizeKey(nombre);
            if (seen.containsKey(key)) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            if (idObj != null && !isBlank(asText(idObj))) {
                item.put("idTecnico", idObj);
            } else {
                item.put("idTecnico", nombre);
            }
            item.put("tecnico", nombre);
            out.add(item);
            seen.put(key, true);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
