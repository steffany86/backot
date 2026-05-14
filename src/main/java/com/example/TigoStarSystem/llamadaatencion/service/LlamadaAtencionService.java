package com.example.TigoStarSystem.llamadaatencion.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.llamadaatencion.dto.LlamadaAtencionCrearRequest;
import com.example.TigoStarSystem.llamadaatencion.repository.LlamadaAtencionRepository;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LlamadaAtencionService {
    private static final String[] SP_TECNICOS_SIN_FILTRO = new String[] {
            "EXEC dbo.spx_Central_ObtenerTecnicosPorSupervisorConformacion ?, ?",
            "EXEC dbo.spx_ObtenerListaUsuario"
    };
    private final LlamadaAtencionRepository repository;
    private final LlamadaAtencionFirmaStorageService firmaStorageService;
    private final DbConnectionManager dbConnectionManager;
    private final AuthService authService;

    public LlamadaAtencionService(
            LlamadaAtencionRepository repository,
            LlamadaAtencionFirmaStorageService firmaStorageService,
            DbConnectionManager dbConnectionManager,
            AuthService authService) {
        this.repository = repository;
        this.firmaStorageService = firmaStorageService;
        this.dbConnectionManager = dbConnectionManager;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(
            String idTecnico,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        authService.me(token);
        validarRangoFechas(fechaDesde, fechaHasta);
        return repository.listarLlamadasAtencion(idTecnico, fechaDesde, fechaHasta, limite);
    }

    public Map<String, Object> registrar(LlamadaAtencionCrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de llamada de atencion es requerido."
            );
        }

        String firmaTecnico = firmaStorageService.guardarFirmaTecnico(request.getFirmaTecnico());
        String firmaTestigo = firmaStorageService.guardarFirmaTestigo(request.getFirmaTestigo());
        Integer idUsuarioSupervisor = me.getUsuario() == null ? null : me.getUsuario().getIdUsuario();
        if (idUsuarioSupervisor == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo identificar el usuario que registra la llamada."
            );
        }

        String idGenerado = repository.insertarLlamadaAtencion(
                request.getIdTecnico(),
                request.getCodEmpleado(),
                idUsuarioSupervisor,
                request.getIdTipoComunicacion(),
                request.getMotivo(),
                request.getDescripcion(),
                request.getComentarioColaborador(),
                request.getAcuerdos(),
                request.getFechaSeguimiento(),
                firmaTecnico,
                firmaTestigo
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idLlamadaAtencion", idGenerado);
        out.put("idUsuarioSesion", me.getUsuario() == null ? null : me.getUsuario().getIdUsuario());
        return out;
    }

    public List<Map<String, Object>> listarTiposComunicacion(String token) {
        authService.me(token);
        return repository.listarTiposComunicacion();
    }

    public LlamadaAtencionFirmaStorageService.FirmaFile obtenerFirma(String path, String token) {
        authService.me(token);
        return firmaStorageService.cargarFirma(path);
    }

    public List<Map<String, Object>> listarTecnicos(
            String q,
            Integer limit,
            String sucursal,
            String token) {
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        JdbcTemplate template = dbConnectionManager.connDb(resolveTecnicosDb(sucursalResuelta));
        String filtro = trimToNull(q);
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;

        List<Map<String, Object>> rows = ejecutarTecnicosConFallback(template, idSupervisor, sucursalResuelta);
        List<Map<String, Object>> normalizadas = normalizarTecnicos(rows, filtro);
        int max = resolveLimit(limit);
        if (normalizadas.size() <= max) {
            return normalizadas;
        }
        return new ArrayList<>(normalizadas.subList(0, max));
    }

    private String resolveTecnicosDb(String sucursal) {
        String normalized = normalizeText(sucursal);
        if (normalized.contains("sucre")) {
            return "sucre";
        }
        return "operativa";
    }

    private String resolveSucursalNombre(String sucursal, String token) {
        if (!isBlank(sucursal)) {
            return SucursalCanonicalizer.canonicalize(sucursal);
        }

        AuthMeResponse me = authService.me(token);
        Integer idSucursal = me.getUsuario() == null ? null : me.getUsuario().getIdSucursal();
        if (idSucursal == null) {
            return null;
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    private List<Map<String, Object>> normalizarTecnicos(List<Map<String, Object>> rows, String filtro) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        String filtroNorm = normalizeText(filtro);
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalizada = new LinkedHashMap<>();
            if (row != null) {
                normalizada.putAll(row);

                Object idTecnico = findValue(row, "id_tecnico", "idtecnico", "id_vendedor", "idvendedor");
                Object tecnico = findValue(row, "tecnico", "nombre", "vendedor", "nombrevendedor");
                Object cuentaSf = findValue(row, "cuenta_sf", "cuentasf", "cuentaSf");
                Object codEmpleado = findValue(row, "cod_empleado", "codempleado", "codEmpleado");
                Object salesforce = findValue(row, "salesforce");
                Object habilidad = findValue(row, "habilidad");
                Object vehiculo = findValue(row, "vehiculo");

                if (idTecnico != null) {
                    normalizada.put("idTecnico", idTecnico);
                    normalizada.put("id_tecnico", idTecnico);
                }
                if (tecnico != null) {
                    normalizada.put("tecnico", tecnico);
                }
                if (cuentaSf != null) {
                    normalizada.put("cuentaSf", cuentaSf);
                    normalizada.put("cuenta_sf", cuentaSf);
                }
                if (codEmpleado != null) {
                    normalizada.put("codEmpleado", codEmpleado);
                    normalizada.put("cod_empleado", codEmpleado);
                }
                if (salesforce != null) {
                    normalizada.put("salesforce", salesforce);
                }
                if (habilidad != null) {
                    normalizada.put("habilidad", habilidad);
                }
                if (vehiculo != null) {
                    normalizada.put("vehiculo", vehiculo);
                }
            }
            if (!matchesFiltro(normalizada, filtroNorm)) {
                continue;
            }
            out.add(normalizada);
        }
        return out;
    }

    private boolean matchesFiltro(Map<String, Object> row, String filtroNorm) {
        if (filtroNorm == null || filtroNorm.isEmpty()) {
            return true;
        }
        String[] keys = new String[] {
                "idTecnico", "id_tecnico", "idVendedor", "id_vendedor",
                "tecnico", "nombre", "nombrevendedor", "vendedor",
                "cuentaSf", "cuenta_sf", "codEmpleado", "cod_empleado", "codempleado", "salesforce", "habilidad", "vehiculo"
        };
        for (String key : keys) {
            Object value = findValue(row, key);
            if (value == null) {
                continue;
            }
            String current = normalizeText(String.valueOf(value));
            if (!current.isEmpty() && current.contains(filtroNorm)) {
                return true;
            }
        }
        return false;
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "fechaDesde no puede ser mayor a fechaHasta."
            );
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 200;
        }
        return Math.min(limit, 1000);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (current.equals(normalizeKey(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<Map<String, Object>> ejecutarTecnicosConFallback(JdbcTemplate template, Integer idSupervisor, String sucursal) {
        DataAccessException last = null;
        for (String sp : SP_TECNICOS_SIN_FILTRO) {
            try {
                if ("EXEC dbo.spx_Central_ObtenerTecnicosPorSupervisorConformacion ?, ?".equals(sp)) {
                    List<Map<String, Object>> rows = dbConnectionManager.connDb("central").queryForList(sp, idSupervisor, trimToNull(sucursal));
                    if (rows != null && !rows.isEmpty()) {
                        return enriquecerRowsConSucursal(template, rows);
                    }
                    continue;
                }
                if (sp.contains("?")) {
                    List<Map<String, Object>> rows = template.queryForList(sp, idSupervisor);
                    if (rows != null && !rows.isEmpty()) {
                        return rows;
                    }
                    continue;
                }
                List<Map<String, Object>> rows = template.queryForList(sp);
                if (rows != null && !rows.isEmpty()) {
                    return rows;
                }
            } catch (DataAccessException ex) {
                last = ex;
                if (!isMissingStoredProcedure(ex)) {
                    throw ex;
                }
            }
        }
        if (last != null && !isMissingStoredProcedure(last)) {
            throw last;
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> enriquecerRowsConSucursal(JdbcTemplate template, List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> mapped = new LinkedHashMap<>(row);
            Object idObj = findValue(row, "idTecnico", "id_tecnico", "id_vendedor");
            String id = idObj == null ? null : String.valueOf(idObj).trim();
            if (id == null || id.isEmpty()) continue;
            mapped.put("idTecnico", id);
            mapped.put("id_tecnico", id);
            String nombre = null;
            try {
                List<Map<String, Object>> v = template.queryForList(
                        "SELECT TOP 1 Nombre, CodEmpleado, CuentaSF, SalesForce, Habilidad, Vehiculo " +
                                "FROM dbo.tbl_Vendedor WHERE Id_Vendedor = ? AND ISNULL(E_Eliminado,0)=0",
                        Integer.parseInt(id)
                );
                if (!v.isEmpty()) {
                    Map<String, Object> first = v.get(0);
                    nombre = first.get("Nombre") == null ? null : String.valueOf(first.get("Nombre")).trim();
                    if (first.get("CodEmpleado") != null) mapped.put("codEmpleado", first.get("CodEmpleado"));
                    if (first.get("CuentaSF") != null) mapped.put("cuentaSf", first.get("CuentaSF"));
                    if (first.get("SalesForce") != null) mapped.put("salesforce", first.get("SalesForce"));
                    if (first.get("Habilidad") != null) mapped.put("habilidad", first.get("Habilidad"));
                    if (first.get("Vehiculo") != null) mapped.put("vehiculo", first.get("Vehiculo"));
                }
            } catch (Exception ignored) {}
            if (nombre == null || nombre.isEmpty()) {
                try {
                    List<Map<String, Object>> u = template.queryForList(
                            "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ? AND ISNULL(E_Eliminado,0)=0",
                            Integer.parseInt(id)
                    );
                    if (!u.isEmpty() && u.get(0).get("Nombre") != null) nombre = String.valueOf(u.get(0).get("Nombre")).trim();
                } catch (Exception ignored) {}
            }
            mapped.put("tecnico", (nombre == null || nombre.isEmpty()) ? ("Tecnico " + id) : nombre);
            out.add(mapped);
        }
        return out;
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
