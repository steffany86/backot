package com.example.TigoStarSystem.supervisionv2.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantenimiento de los catalogos de Supervision V2 (alta y baja logica con e_eliminado).
 * Las columnas (id, nombre, e_eliminado) se detectan una vez por tabla leyendo sys.columns.
 */
@Repository
public class SupervisionV2CatalogoRepository {
    private static final Map<String, String> TABLAS = new LinkedHashMap<String, String>();
    static {
        TABLAS.put("tipo-supervision", "tbl_SupTipoSupervision");
        TABLAS.put("tipo-trabajo", "tbl_SupTipoTrabajo");
        TABLAS.put("tipo-penalizacion", "tbl_SupTipoPenalizacion");
        TABLAS.put("supervision-por", "tbl_SupTipoSupervisionPor");
        TABLAS.put("tecnologia", "tbl_SupTipoTecnologia");
        TABLAS.put("tipo-revision", "tbl_SupTipoRevision");
        // Catalogos de Seguimiento y Control Operativo (llamada de atencion): viven en BD_TigoHogar.
        TABLAS.put("tipo-comunicacion", "tbl_TipoComunicacion");
        TABLAS.put("motivo-llamada", "tbl_MotivoLlamadaDeAtencion");
    }

    private static final java.util.Set<String> CATALOGOS_TIGOHOGAR =
            new java.util.HashSet<String>(java.util.Arrays.asList("tipo-comunicacion", "motivo-llamada"));

    private final JdbcTemplate supervisionV2Jdbc;
    private final JdbcTemplate tigohogarJdbc;
    private final Map<String, Esquema> cache = new ConcurrentHashMap<String, Esquema>();

    public SupervisionV2CatalogoRepository(
            @Qualifier("supervisionV2JdbcTemplate") JdbcTemplate supervisionV2Jdbc,
            @Qualifier("tigohogarJdbcTemplate") JdbcTemplate tigohogarJdbc) {
        this.supervisionV2Jdbc = supervisionV2Jdbc;
        this.tigohogarJdbc = tigohogarJdbc;
    }

    private JdbcTemplate jdbcDe(String catalogo) {
        return CATALOGOS_TIGOHOGAR.contains(catalogo) ? tigohogarJdbc : supervisionV2Jdbc;
    }

    public boolean existeCatalogo(String catalogo) {
        return catalogo != null && TABLAS.containsKey(catalogo);
    }

    public List<Map<String, Object>> listar(String catalogo) {
        Esquema e = esquema(catalogo);
        String sql = "SELECT " + q(e.id) + " AS id, " + q(e.nombre) + " AS nombre FROM dbo." + q(e.tabla)
                + (e.eliminado == null ? "" : " WHERE ISNULL(" + q(e.eliminado) + ", 0) = 0")
                + " ORDER BY " + q(e.nombre);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : jdbcDe(catalogo).queryForList(sql)) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", row.get("id"));
            item.put("nombre", row.get("nombre") == null ? "" : String.valueOf(row.get("nombre")).trim());
            out.add(item);
        }
        return out;
    }

    public boolean existeNombreActivo(String catalogo, String nombre) {
        Esquema e = esquema(catalogo);
        String sql = "SELECT COUNT(*) FROM dbo." + q(e.tabla) + " WHERE LTRIM(RTRIM(" + q(e.nombre) + ")) = ?"
                + (e.eliminado == null ? "" : " AND ISNULL(" + q(e.eliminado) + ", 0) = 0");
        Integer count = jdbcDe(catalogo).queryForObject(sql, Integer.class, nombre);
        return count != null && count > 0;
    }

    public void crear(String catalogo, String nombre) {
        Esquema e = esquema(catalogo);
        String cols = q(e.nombre) + (e.eliminado == null ? "" : ", " + q(e.eliminado));
        String eliminadoValor = e.eliminado == null ? "" : ", 0";
        if (e.identity) {
            jdbcDe(catalogo).update("INSERT INTO dbo." + q(e.tabla) + " (" + cols + ") VALUES (?" + eliminadoValor + ")", nombre);
        } else {
            // La tabla perdio el identity al importarse: se calcula el siguiente id en la misma sentencia.
            jdbcDe(catalogo).update("INSERT INTO dbo." + q(e.tabla) + " (" + q(e.id) + ", " + cols + ") "
                    + "SELECT ISNULL(MAX(" + q(e.id) + "), 0) + 1, ?" + eliminadoValor
                    + " FROM dbo." + q(e.tabla) + " WITH (TABLOCKX)", nombre);
        }
    }

    /** @return filas afectadas; -1 si la tabla no tiene columna de baja logica. */
    public int baja(String catalogo, long id) {
        Esquema e = esquema(catalogo);
        if (e.eliminado == null) {
            return -1;
        }
        return jdbcDe(catalogo).update("UPDATE dbo." + q(e.tabla) + " SET " + q(e.eliminado) + " = 1 WHERE " + q(e.id)
                + " = ? AND ISNULL(" + q(e.eliminado) + ", 0) = 0", id);
    }

    private Esquema esquema(String catalogo) {
        String tabla = TABLAS.get(catalogo);
        if (tabla == null) {
            throw new IllegalArgumentException("Catalogo desconocido: " + catalogo);
        }
        Esquema cached = cache.get(catalogo);
        if (cached != null) {
            return cached;
        }
        List<Map<String, Object>> cols = jdbcDe(catalogo).queryForList(
                "SELECT c.name AS name, c.is_identity AS isIdentity, t.name AS tipo "
                        + "FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id "
                        + "WHERE c.object_id = OBJECT_ID(?) ORDER BY c.column_id",
                "dbo." + tabla);
        if (cols.isEmpty()) {
            throw new IllegalStateException("No existe la tabla dbo." + tabla + " en el servidor de Supervision V2.");
        }

        String id = null;
        boolean identity = false;
        String eliminado = null;
        for (Map<String, Object> c : cols) {
            String name = String.valueOf(c.get("name"));
            String lower = name.toLowerCase(Locale.ROOT);
            if (Boolean.TRUE.equals(c.get("isIdentity")) || "1".equals(String.valueOf(c.get("isIdentity")))) {
                id = name;
                identity = true;
            }
            if (eliminado == null && (lower.equals("e_eliminado") || lower.equals("eliminado"))) {
                eliminado = name;
            }
        }
        if (id == null) {
            for (Map<String, Object> c : cols) {
                String name = String.valueOf(c.get("name"));
                if (name.equalsIgnoreCase("id")) {
                    id = name;
                    break;
                }
            }
        }
        if (id == null) {
            id = String.valueOf(cols.get(0).get("name"));
        }

        String nombre = null;
        for (Map<String, Object> c : cols) {
            String name = String.valueOf(c.get("name"));
            String tipo = String.valueOf(c.get("tipo")).toLowerCase(Locale.ROOT);
            boolean texto = tipo.equals("nvarchar") || tipo.equals("varchar") || tipo.equals("nchar") || tipo.equals("char");
            if (!texto || name.equals(id) || name.equals(eliminado)) {
                continue;
            }
            if (nombre == null) {
                nombre = name;
            }
            if (name.toLowerCase(Locale.ROOT).startsWith("tipo")) {
                nombre = name;
                break;
            }
        }
        if (nombre == null) {
            throw new IllegalStateException("No se encontro la columna de nombre en dbo." + tabla + ".");
        }

        Esquema esquema = new Esquema(tabla, id, nombre, eliminado, identity);
        cache.put(catalogo, esquema);
        return esquema;
    }

    private static String q(String name) {
        return "[" + name.replace("]", "]]") + "]";
    }

    private static final class Esquema {
        private final String tabla;
        private final String id;
        private final String nombre;
        private final String eliminado;
        private final boolean identity;

        private Esquema(String tabla, String id, String nombre, String eliminado, boolean identity) {
            this.tabla = tabla;
            this.id = id;
            this.nombre = nombre;
            this.eliminado = eliminado;
            this.identity = identity;
        }
    }
}
