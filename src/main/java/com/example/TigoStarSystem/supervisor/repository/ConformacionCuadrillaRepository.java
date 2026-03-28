package com.example.TigoStarSystem.supervisor.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Repositorio para gestionar la conformacion diaria de cuadrillas.
 * Centraliza consultas, altas/ediciones y catalogos relacionados.
 */
@Repository
public class ConformacionCuadrillaRepository {
    private final JdbcTemplate centralJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final SucursalRepository sucursalRepository;
    private final ConformacionCuadrillaDbSupport dbSupport;

    // -------------------------------------------------------------------------
    // Configuracion e inicializacion
    // -------------------------------------------------------------------------

    /**
     * Inicializa templates, credenciales y datos base para conexion por sucursal.
     */
    public ConformacionCuadrillaRepository(
            @Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate,
            JdbcTemplate jdbcTemplate,
            SucursalRepository sucursalRepository,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${spring.datasource.driver-class-name}") String dbDriver,
            @Value("${spring.datasource.url}") String mainDatasourceUrl,
            @Value("${app.central.datasource.url:}") String centralDatasourceUrl,
            @Value("${app.sucre.datasource.url:}") String sucreDatasourceUrl,
            @Value("${auth.login.sucre.database:SucrePrueba}") String sucreDatabase,
            @Value("${app.sucre.datasource.username:${spring.datasource.username}}") String sucreUsername,
            @Value("${app.sucre.datasource.password:${spring.datasource.password}}") String sucrePassword,
            @Value("${app.datasource.params:encrypt=false;trustServerCertificate=true}") String dbParams) {
        this.centralJdbcTemplate = centralJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.sucursalRepository = sucursalRepository;
        this.dbSupport = new ConformacionCuadrillaDbSupport(
                sucursalRepository,
                dbUsername,
                dbPassword,
                dbDriver,
                mainDatasourceUrl,
                centralDatasourceUrl,
                sucreDatasourceUrl,
                sucreDatabase,
                sucreUsername,
                sucrePassword,
                dbParams
        );
    }

    // -------------------------------------------------------------------------
    // Consultas principales
    // -------------------------------------------------------------------------

    /**
     * Lista registros activos por fecha/sucursal con fallback entre fuentes.
     */
    public List<Map<String, Object>> listar(LocalDate fecha, String sucursal, Integer limite, Integer idTecnico) {
        return listarConFallback(fecha, sucursal, limite, idTecnico, false);
    }

    /**
     * Obtiene un registro por id, priorizando la base central.
     */
    public Map<String, Object> obtenerPorId(Long id) {
        return obtenerPorId(id, null);
    }

    public Map<String, Object> obtenerPorId(Long id, String sucursal) {
        if (id == null) {
            return null;
        }
        String sucursalParam = normalizarSucursal(sucursal);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);

        // 1) Sucursal seleccionada (uTecnicos o SucrePrueba)
        if (dbInfo != null) {
            Map<String, Object> rowSucursal = obtenerPorIdEnTemplate(crearJdbcTemplateSucursal(dbInfo), id);
            if (rowSucursal != null) {
                return rowSucursal;
            }
        }

        // 2) BD operativa por defecto (uTecnicos)
        Map<String, Object> rowOperativa = obtenerPorIdEnTemplate(jdbcTemplate, id);
        if (rowOperativa != null) {
            return rowOperativa;
        }

        // 3) Sucre explicito como fallback cuando corresponda
        if (dbSupport.isSucre(dbSupport.normalizeText(sucursalParam))) {
            Map<String, Object> rowSucre = obtenerPorIdEnTemplate(
                    crearSucreJdbcTemplate(),
                    id
            );
            if (rowSucre != null) {
                return rowSucre;
            }
        }

        // 4) Central como ultimo fallback
        return obtenerPorIdEnTemplate(centralJdbcTemplate, id);
    }

    /**
     * Lista registros incluyendo eliminados, con fallback entre fuentes.
     */
    public List<Map<String, Object>> listarConEliminados(
            LocalDate fecha,
            String sucursal,
            Integer limite,
            Integer idTecnico) {
        return listarConFallback(fecha, sucursal, limite, idTecnico, true);
    }

    /**
     * Ejecuta listado con fallback entre sucursal seleccionada, operativa y central.
     */
    private List<Map<String, Object>> listarConFallback(
            LocalDate fecha,
            String sucursal,
            Integer limite,
            Integer idTecnico,
            boolean incluirEliminados) {
        Object fechaParam = fecha == null ? null : Date.valueOf(fecha);
        String sucursalParam = normalizarSucursal(sucursal);
        String sucursalFiltro = resolverSucursalNombreParaConsulta(sucursalParam);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);
        Set<String> filtrosConsulta = dbSupport.construirFiltrosConsulta(sucursalFiltro, dbInfo);

        // 1) Sucursal logueada (uTecnicos o SucrePrueba segun sucursal)
        List<Map<String, Object>> rowsSucursal = listarEnSucursalSeleccionada(
                sucursalFiltro,
                fechaParam,
                limite,
                idTecnico,
                dbInfo,
                incluirEliminados
        );
        if (tieneDatos(rowsSucursal)) {
            return rowsSucursal;
        }

        // 2) BD operativa por defecto (uTecnicos)
        List<Map<String, Object>> rowsOperativa = listarEnTemplatesConFiltros(
                jdbcTemplate,
                filtrosConsulta,
                fechaParam,
                limite,
                idTecnico,
                incluirEliminados
        );
        if (tieneDatos(rowsOperativa)) {
            return rowsOperativa;
        }

        // 3) Central como ultimo fallback
        return listarEnTemplatesConFiltros(
                centralJdbcTemplate,
                filtrosConsulta,
                fechaParam,
                limite,
                idTecnico,
                incluirEliminados
        );
    }

    /**
     * Lista registros (incluyendo eliminados) exclusivamente desde la base central.
     */
    public List<Map<String, Object>> listarConEliminadosCentral(
            LocalDate fecha,
            String sucursal,
            Integer limite,
            Integer idTecnico) {
        if (centralJdbcTemplate == null) {
            return new ArrayList<>();
        }
        Object fechaParam = fecha == null ? null : Date.valueOf(fecha);
        String sucursalParam = normalizarSucursal(sucursal);
        String sucursalFiltro = resolverSucursalNombreParaConsulta(sucursalParam);
        try {
            List<Map<String, Object>> rows = listarConEliminadosEnTemplate(
                    centralJdbcTemplate,
                    fechaParam,
                    sucursalFiltro,
                    limite,
                    idTecnico
            );
            return rows == null ? new ArrayList<>() : rows;
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * Ejecuta el SP de listado en un template y maneja versiones de firma distintas.
     */
    private List<Map<String, Object>> listarEnTemplate(
            JdbcTemplate template,
            Object fechaParam,
            String sucursalParam,
            Integer limite,
            Integer idTecnico) {
        try {
            try {
                return template.queryForList(
                        "EXEC dbo.spx_ObtenerConformacionCuadrillaBackOffice ?, ?, ?, ?",
                        fechaParam,
                        sucursalParam,
                        limite,
                        idTecnico
                );
            } catch (DataAccessException exV4) {
                List<Map<String, Object>> rows = template.queryForList(
                        "EXEC dbo.spx_ObtenerConformacionCuadrillaBackOffice ?, ?, ?",
                        fechaParam,
                        sucursalParam,
                        limite
                );
                return filtrarPorTecnicoId(rows, idTecnico);
            }
        } catch (DataAccessException ex) {
            try {
                return template.queryForList(
                        "EXEC dbo.spx_ObtenerListadoConformacionCuadrillaBackOffice ?, ?, ?, ?",
                        fechaParam,
                        sucursalParam,
                        limite,
                        idTecnico
                );
            } catch (DataAccessException exV4Listado) {
                List<Map<String, Object>> rows = template.queryForList(
                        "EXEC dbo.spx_ObtenerListadoConformacionCuadrillaBackOffice ?, ?, ?",
                        fechaParam,
                        sucursalParam,
                        limite
                );
                return filtrarPorTecnicoId(rows, idTecnico);
            }
        }
    }

    /**
     * Busca un registro por id en un template especifico.
     */
    private Map<String, Object> obtenerPorIdEnTemplate(JdbcTemplate template, Long id) {
        try {
            List<Map<String, Object>> rows = queryForList(
                    template,
                    "SELECT TOP 1 * FROM dbo.tbl_ConformacionCuadrillaDiario WHERE id = ? ORDER BY id DESC",
                    id
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return rows.get(0);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    /**
     * Consulta directo a tabla para incluir filas eliminadas.
     */
    private List<Map<String, Object>> listarConEliminadosEnTemplate(
            JdbcTemplate template,
            Object fechaParam,
            String sucursalParam,
            Integer limite,
            Integer idTecnico) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM dbo.tbl_ConformacionCuadrillaDiario WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();
        if (fechaParam != null) {
            sql.append(" AND fecha = ?");
            args.add(fechaParam);
        }
        if (!isBlank(sucursalParam)) {
            sql.append(" AND sucursal = ?");
            args.add(sucursalParam.trim());
        }
        if (idTecnico != null) {
            sql.append(" AND id_tecnico = ?");
            args.add(idTecnico);
        }
        sql.append(" ORDER BY fechaRegistro DESC, id DESC");

        List<Map<String, Object>> rows = queryForList(template, sql.toString(), args.toArray());
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        if (limite == null || limite <= 0 || rows.size() <= limite) {
            return rows;
        }
        return new ArrayList<>(rows.subList(0, limite));
    }

    // -------------------------------------------------------------------------
    // Persistencia (alta y edicion)
    // -------------------------------------------------------------------------

    /**
     * Guarda una fila confirmada en la base central.
     */
    public int guardarFilaConfirmada(ConformacionCuadrillaRowRequest fila) {
        RuntimeException lastError = null;
        if (centralJdbcTemplate != null) {
            try {
                return ejecutarRegistrar(centralJdbcTemplate, fila);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }

        List<JdbcTemplate> templates = construirTemplatesEscritura(
                fila == null ? null : fila.getSucursal(),
                null
        );
        for (JdbcTemplate template : templates) {
            if (template == centralJdbcTemplate) {
                continue;
            }
            try {
                return ejecutarRegistrar(template, fila);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return 0;
    }

    /**
     * Actualiza una fila existente en la base central.
     */
    public int actualizarFila(Long id, ConformacionCuadrillaRowRequest fila) {
        RuntimeException lastError = null;
        if (centralJdbcTemplate != null) {
            try {
                int affected = ejecutarActualizar(centralJdbcTemplate, id, fila);
                if (affected > 0) {
                    return affected;
                }
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }

        List<JdbcTemplate> templates = construirTemplatesEscritura(
                fila == null ? null : fila.getSucursal(),
                id
        );
        for (JdbcTemplate template : templates) {
            if (template == centralJdbcTemplate) {
                continue;
            }
            try {
                int affected = ejecutarActualizar(template, id, fila);
                if (affected > 0) {
                    return affected;
                }
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Catalogos y datos auxiliares
    // -------------------------------------------------------------------------

    /**
     * Lista tecnicos para formularios de conformacion.
     */
    public List<Map<String, Object>> listarTecnicos() {
        return listarTecnicos(null);
    }

    public List<Map<String, Object>> listarTecnicos(String sucursal) {
        String sql = "EXEC dbo.spx_TraerVendedores_x_FormTecnico";
        String sucursalParam = normalizarSucursal(sucursal);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);

        if (dbInfo != null) {
            try {
                JdbcTemplate sucursalTemplate = crearJdbcTemplateSucursal(dbInfo);
                List<Map<String, Object>> rowsSucursal = queryForList(sucursalTemplate, sql);
                if (rowsSucursal != null && !rowsSucursal.isEmpty()) {
                    return normalizarCatalogoTecnicos(rowsSucursal);
                }
            } catch (DataAccessException ex) {
                // fallback below
            }
        }
        return normalizarCatalogoTecnicos(queryForListFallback(sql));
    }

    /**
     * Lista tecnicos para edicion con fallback de procedimiento almacenado.
     */
    public List<Map<String, Object>> listarTecnicosFiltroEdicion() {
        return listarTecnicosFiltroEdicion(null);
    }

    public List<Map<String, Object>> listarTecnicosFiltroEdicion(String sucursal) {
        String sucursalParam = normalizarSucursal(sucursal);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);
        String sqlPreferido = "EXEC dbo.spr_TraerVendedores_x_FormTecnico";
        String sqlFallback = "EXEC dbo.spx_TraerVendedores_x_FormTecnico";

        if (dbInfo != null) {
            JdbcTemplate sucursalTemplate = crearJdbcTemplateSucursal(dbInfo);
            List<Map<String, Object>> rowsSucursal =
                    queryForListConSpAlternativos(sucursalTemplate, sqlPreferido, sqlFallback);
            if (rowsSucursal != null && !rowsSucursal.isEmpty()) {
                return normalizarCatalogoTecnicos(rowsSucursal);
            }
        }

        return normalizarCatalogoTecnicos(
                queryForListFallbackConSpAlternativos(sqlPreferido, sqlFallback)
        );
    }

    /**
     * Lista auxiliares usando el mismo catalogo de tecnicos.
     */
    public List<Map<String, Object>> listarAuxiliares() {
        return normalizarCatalogoTecnicos(queryForListFallbackConSpAlternativos(
                "EXEC dbo.spr_TraerVendedores_x_FormTecnico",
                "EXEC dbo.spx_TraerVendedores_x_FormTecnico"
        ));
    }

    /**
     * Obtiene detalle de un tecnico especifico.
     */
    public List<Map<String, Object>> obtenerTecnicoDetalle(Integer idTecnico) {
        return normalizarCatalogoTecnicos(
                queryForListFallback("EXEC dbo.spx_ObtenerDatosTecnicoCuadrilla ?", idTecnico)
        );
    }

    /**
     * Lista digitadores disponibles.
     */
    public List<Map<String, Object>> listarDigitadores() {
        return queryForListFallbackConSpAlternativos(
                "EXEC dbo.spx_ObtenerDigitadores",
                "EXEC dbo.spx_ObtenerListaDigitadores"
        );
    }

    /**
     * Lista digitadores para edicion con fallback entre SPs.
     */
    public List<Map<String, Object>> listarDigitadoresFiltroEdicion() {
        return queryForListFallbackConSpAlternativos(
                "EXEC dbo.spx_ObtenerListaDigitadores",
                "EXEC dbo.spx_ObtenerDigitadores"
        );
    }

    /**
     * Lista supervisores disponibles.
     */
    public List<Map<String, Object>> listarSupervisores() {
        return queryForListFallback("EXEC dbo.spx_ObtenerSupervisores");
    }

    /**
     * Lista vehiculos filtrando por placa o texto parcial.
     */
    public List<Map<String, Object>> listarVehiculos(String filtro) {
        String filtroParam = (filtro == null || filtro.trim().isEmpty()) ? null : filtro.trim();
        return queryForListFallback("EXEC dbo.[listar-vehiculo] ?", filtroParam);
    }

    /**
     * Lista vehiculos para edicion, priorizando los del tecnico indicado.
     */
    public List<Map<String, Object>> listarVehiculosFiltroEdicion(Integer idTecnico) {
        if (idTecnico != null) {
            try {
                return queryForListFallback("EXEC dbo.spx_ObtenerPlacavehiculos_Tecnico ?", idTecnico);
            } catch (DataAccessException ex) {
                // fallback sin parametro
            }
        }
        try {
            return queryForListFallback("EXEC dbo.spx_ObtenerPlacavehiculos_Tecnico");
        } catch (DataAccessException ex) {
            return listarVehiculos(null);
        }
    }

    /**
     * Sobrecarga para listar grupos sin sucursal explicita.
     */
    public List<Map<String, Object>> listarGruposFiltroEdicion() {
        return listarGruposFiltroEdicion(null);
    }

    /**
     * Lista grupos/rutas para edicion segun contexto de sucursal.
     */
    public List<Map<String, Object>> listarGruposFiltroEdicion(String sucursal) {
        String sql = "EXEC dbo.spx_ObtenerRutaXIdTecnico";
        String sucursalParam = normalizarSucursal(sucursal);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);

        // 1) Sucursal seleccionada (uTecnicos o SucrePrueba)
        if (dbInfo != null) {
            try {
                JdbcTemplate sucursalTemplate = crearJdbcTemplateSucursal(dbInfo);
                List<Map<String, Object>> rowsSucursal = queryForList(sucursalTemplate, sql);
                if (rowsSucursal != null && !rowsSucursal.isEmpty()) {
                    return rowsSucursal;
                }
            } catch (DataAccessException ex) {
                // fallback below
            }
        }

        // 2) Sucre explicito cuando no se pudo resolver sucursal
        if (dbSupport.isSucre(dbSupport.normalizeText(sucursalParam))) {
            try {
                JdbcTemplate sucreTemplate = crearSucreJdbcTemplate();
                List<Map<String, Object>> rowsSucre = queryForList(sucreTemplate, sql);
                if (rowsSucre != null && !rowsSucre.isEmpty()) {
                    return rowsSucre;
                }
            } catch (DataAccessException ex) {
                // fallback below
            }
        }

        // 3) BD operativa por defecto (uTecnicos)
        try {
            List<Map<String, Object>> operativa = queryForList(jdbcTemplate, sql);
            if (operativa != null && !operativa.isEmpty()) {
                return operativa;
            }
        } catch (DataAccessException ex) {
            // sin fallback adicional
        }

        return new ArrayList<>();
    }

    /**
     * Obtiene sucursales unicas para el selector de la interfaz.
     */
    public List<Map<String, Object>> obtenerSucursalActual() {
        List<Map<String, Object>> rows = sucursalRepository.obtenerSucursales();
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Integer id = asInteger(firstNonNull(row, "idsucursal", "id_sucursal", "Id_Sucursal"));
            String nombre = asString(firstNonNull(row, "sucursal", "Sucursal"));
            if (isBlank(nombre)) {
                continue;
            }
            String key = nombre.trim().toUpperCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idSucursal", id);
            item.put("sucursal", nombre.trim());
            out.add(item);
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Helpers de acceso a datos
    // -------------------------------------------------------------------------

    /**
     * Ejecuta una consulta intentando primero operativa y luego central.
     */
    private List<Map<String, Object>> queryForListFallback(String sql, Object... args) {
        try {
            List<Map<String, Object>> operativa = queryForList(jdbcTemplate, sql, args);
            if (operativa != null && !operativa.isEmpty()) {
                return operativa;
            }
        } catch (DataAccessException ex) {
            // fallback below
        }
        try {
            List<Map<String, Object>> central = queryForList(centralJdbcTemplate, sql, args);
            return central == null ? new ArrayList<>() : central;
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * Ejecuta una lista de SP equivalentes sobre un template hasta obtener datos.
     */
    private List<Map<String, Object>> queryForListConSpAlternativos(
            JdbcTemplate template,
            String... sqlAlternativos) {
        if (template == null || sqlAlternativos == null || sqlAlternativos.length == 0) {
            return new ArrayList<>();
        }
        for (String sql : sqlAlternativos) {
            try {
                List<Map<String, Object>> rows = queryForList(template, sql);
                if (rows != null && !rows.isEmpty()) {
                    return rows;
                }
            } catch (DataAccessException ex) {
                // intenta siguiente SP alternativo
            }
        }
        return new ArrayList<>();
    }

    /**
     * Ejecuta SP equivalentes con fallback operativa->central hasta obtener datos.
     */
    private List<Map<String, Object>> queryForListFallbackConSpAlternativos(String... sqlAlternativos) {
        if (sqlAlternativos == null || sqlAlternativos.length == 0) {
            return new ArrayList<>();
        }
        for (String sql : sqlAlternativos) {
            List<Map<String, Object>> rows = queryForListFallback(sql);
            if (rows != null && !rows.isEmpty()) {
                return rows;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Normaliza claves de tecnico/auxiliar para mantener compatibilidad entre SPs.
     */
    private List<Map<String, Object>> normalizarCatalogoTecnicos(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalizada = new LinkedHashMap<>();
            if (row != null && !row.isEmpty()) {
                normalizada.putAll(row);
            } else {
                out.add(normalizada);
                continue;
            }

            Object cuenta = findValueCaseInsensitive(row, "cuentaSf", "cuenta_sf", "cuentasf", "CuentaSF");
            if (cuenta != null) {
                normalizada.put("cuentaSf", cuenta);
                normalizada.put("cuenta_sf", cuenta);
            }

            Object salesforce = findValueCaseInsensitive(row, "salesforce", "SalesForce");
            if (salesforce != null) {
                normalizada.put("salesforce", salesforce);
            }

            out.add(normalizada);
        }
        return out;
    }

    private Object findValueCaseInsensitive(Map<String, Object> row, String... candidates) {
        if (row == null || row.isEmpty() || candidates == null || candidates.length == 0) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String candidate : candidates) {
                if (current.equals(normalizeKey(candidate))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Wrapper para queryForList con o sin argumentos.
     */
    private List<Map<String, Object>> queryForList(JdbcTemplate template, String sql, Object... args) {
        if (args == null || args.length == 0) {
            return template.queryForList(sql);
        }
        return template.queryForList(sql, args);
    }

    /**
     * Filtra filas por id de tecnico cuando el SP no lo soporta.
     */
    private List<Map<String, Object>> filtrarPorTecnicoId(List<Map<String, Object>> rows, Integer idTecnico) {
        if (idTecnico == null || rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer value = asInteger(
                    firstNonNull(row, "id_tecnico", "Id_Tecnico", "idTecnico", "idTecnicoTitular")
            );
            if (value != null && value.equals(idTecnico)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    /**
     * Devuelve el primer valor no nulo de un mapa segun llaves candidatas.
     */
    private Object firstNonNull(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    /**
     * Convierte un valor dinamico a Integer de forma segura.
     */
    private Integer asInteger(Object value) {
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

    /**
     * Ejecuta el SP de registro de conformacion.
     */
    private int ejecutarRegistrar(JdbcTemplate template, ConformacionCuadrillaRowRequest fila) {
        return template.update(
                "EXEC dbo.spx_RegistrarConformacionCuadrillaBackOffice " +
                        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?",
                fila.getFecha() == null ? null : Date.valueOf(fila.getFecha()),
                fila.getEstado(),
                fila.getActividad(),
                fila.getIdTecnico(),
                fila.getCuentaSf(),
                fila.getSalesforce(),
                fila.getHabilidad(),
                fila.getVehiculo(),
                fila.getGrupo(),
                fila.getAlmacen(),
                fila.getGrupoDigitacion(),
                fila.getIdUsuarioDigitador(),
                fila.getDigitador(),
                fila.getTecnico(),
                fila.getIdTecnicoAuxiliar(),
                fila.getAuxiliar(),
                fila.getIdUsuarioSupervisor(),
                fila.getSupervisorACargo(),
                fila.getSucursal(),
                fila.getObservacion(),
                fila.getIdUsuarioRegistra()
        );
    }

    /**
     * Ejecuta el SP de actualizacion de conformacion.
     */
    private int ejecutarActualizar(JdbcTemplate template, Long id, ConformacionCuadrillaRowRequest fila) {
        return template.update(
                "EXEC dbo.spx_ActualizarConformacionCuadrillaBackOffice " +
                        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?",
                id,
                fila.getFecha() == null ? null : Date.valueOf(fila.getFecha()),
                fila.getEstado(),
                fila.getActividad(),
                fila.getIdTecnico(),
                fila.getCuentaSf(),
                fila.getSalesforce(),
                fila.getHabilidad(),
                fila.getVehiculo(),
                fila.getGrupo(),
                fila.getAlmacen(),
                fila.getGrupoDigitacion(),
                fila.getIdUsuarioDigitador(),
                fila.getDigitador(),
                fila.getTecnico(),
                fila.getIdTecnicoAuxiliar(),
                fila.getAuxiliar(),
                fila.getIdUsuarioSupervisor(),
                fila.getSupervisorACargo(),
                fila.getSucursal(),
                fila.getObservacion(),
                fila.getIdUsuarioRegistra()
        );
    }

    /**
     * Define templates candidatos para operaciones de escritura.
     * Prioriza la sucursal activa, luego operativa por defecto y finalmente central.
     * Cuando hay id (edicion), prioriza templates donde el id ya existe.
     */
    private List<JdbcTemplate> construirTemplatesEscritura(String sucursal, Long id) {
        List<JdbcTemplate> candidatos = new ArrayList<>();
        String sucursalParam = normalizarSucursal(sucursal);
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);
        if (dbInfo != null) {
            try {
                candidatos.add(crearJdbcTemplateSucursal(dbInfo));
            } catch (RuntimeException ignored) {
                // continua con siguientes candidatos
            }
        }
        if (jdbcTemplate != null) {
            candidatos.add(jdbcTemplate);
        }
        if (isSucre(normalizeText(sucursalParam))) {
            try {
                candidatos.add(crearSucreJdbcTemplate());
            } catch (RuntimeException ignored) {
                // continua con siguientes candidatos
            }
        }
        if (centralJdbcTemplate != null) {
            candidatos.add(centralJdbcTemplate);
        }

        List<JdbcTemplate> unicos = dedupeTemplates(candidatos);
        if (id == null) {
            return unicos;
        }

        List<JdbcTemplate> priorizados = new ArrayList<>();
        for (JdbcTemplate template : unicos) {
            try {
                if (obtenerPorIdEnTemplate(template, id) != null) {
                    priorizados.add(template);
                }
            } catch (RuntimeException ignored) {
                // si no se puede consultar ese template, queda como fallback
            }
        }
        for (JdbcTemplate template : unicos) {
            if (!containsTemplate(priorizados, template)) {
                priorizados.add(template);
            }
        }
        return priorizados;
    }

    private List<JdbcTemplate> dedupeTemplates(List<JdbcTemplate> templates) {
        List<JdbcTemplate> out = new ArrayList<>();
        if (templates == null || templates.isEmpty()) {
            return out;
        }
        for (JdbcTemplate template : templates) {
            if (template == null || containsTemplate(out, template)) {
                continue;
            }
            out.add(template);
        }
        return out;
    }

    private boolean containsTemplate(List<JdbcTemplate> templates, JdbcTemplate candidate) {
        if (templates == null || templates.isEmpty() || candidate == null) {
            return false;
        }
        for (JdbcTemplate template : templates) {
            if (template == candidate) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lista registros activos conectando directo a la sucursal seleccionada.
     */
    private List<Map<String, Object>> listarEnSucursalSeleccionada(
            String sucursalParam,
            Object fechaParam,
            Integer limite,
            Integer idTecnico,
            ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo,
            boolean incluirEliminados) {
        if (sucursalParam == null || sucursalParam.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (dbInfo == null) {
            return new ArrayList<>();
        }
        JdbcTemplate sucursalTemplate = crearJdbcTemplateSucursal(dbInfo);
        Set<String> filtros = dbSupport.construirFiltrosConsulta(sucursalParam, dbInfo);
        return listarEnTemplatesConFiltros(
                sucursalTemplate,
                filtros,
                fechaParam,
                limite,
                idTecnico,
                incluirEliminados
        );
    }

    /**
     * Recorre filtros de sucursal sobre un template y devuelve el primer resultado con datos.
     */
    private List<Map<String, Object>> listarEnTemplatesConFiltros(
            JdbcTemplate template,
            Set<String> filtros,
            Object fechaParam,
            Integer limite,
            Integer idTecnico,
            boolean incluirEliminados) {
        if (template == null) {
            return new ArrayList<>();
        }
        if (filtros == null || filtros.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            for (String filtro : filtros) {
                List<Map<String, Object>> rows = listarEnTemplateSegunModo(
                        template,
                        fechaParam,
                        filtro,
                        limite,
                        idTecnico,
                        incluirEliminados
                );
                if (tieneDatos(rows)) {
                    return rows;
                }
            }
            return new ArrayList<>();
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * Ejecuta listado activo o listado con eliminados segun modo.
     */
    private List<Map<String, Object>> listarEnTemplateSegunModo(
            JdbcTemplate template,
            Object fechaParam,
            String sucursalParam,
            Integer limite,
            Integer idTecnico,
            boolean incluirEliminados) {
        if (incluirEliminados) {
            return listarConEliminadosEnTemplate(template, fechaParam, sucursalParam, limite, idTecnico);
        }
        return listarEnTemplate(template, fechaParam, sucursalParam, limite, idTecnico);
    }

    private boolean tieneDatos(List<Map<String, Object>> rows) {
        return rows != null && !rows.isEmpty();
    }

    private String normalizarSucursal(String sucursal) {
        return isBlank(sucursal) ? null : sucursal.trim();
    }

    private ConformacionCuadrillaDbSupport.SucursalDbInfo resolverSucursalDbInfo(String sucursalParam) {
        if (sucursalParam == null) {
            return null;
        }
        return dbSupport.resolverSucursalDbInfo(sucursalParam);
    }

    private JdbcTemplate crearJdbcTemplateSucursal(ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo) {
        return dbSupport.crearJdbcTemplateSucursal(dbInfo);
    }

    private JdbcTemplate crearSucreJdbcTemplate() {
        return dbSupport.crearJdbcTemplateSucre();
    }

    private String normalizeText(String value) {
        return dbSupport.normalizeText(value);
    }

    private String asString(Object value) {
        return dbSupport.asString(value);
    }

    private boolean isSucre(String value) {
        return dbSupport.isSucre(value);
    }

    private boolean isBlank(String value) {
        return dbSupport.isBlank(value);
    }

    private String resolverSucursalNombreParaConsulta(String sucursalParam) {
        if (isBlank(sucursalParam)) {
            return sucursalParam;
        }
        ConformacionCuadrillaDbSupport.SucursalDbInfo dbInfo = resolverSucursalDbInfo(sucursalParam);
        if (dbInfo != null && !isBlank(dbInfo.getNombreSucursal())) {
            return dbInfo.getNombreSucursal().trim();
        }
        return sucursalParam;
    }
}
