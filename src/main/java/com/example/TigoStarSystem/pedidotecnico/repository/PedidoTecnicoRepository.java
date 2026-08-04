package com.example.TigoStarSystem.pedidotecnico.repository;

import com.example.TigoStarSystem.pedidotecnico.dto.PedidoTecnicoItemRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
public class PedidoTecnicoRepository {
    private static final String TABLA_PEDIDO = "tbl_PedidoTecnico";
    private static final String TABLA_DETALLE = "tbl_CODIGOPedidoTecnico";

    public List<Map<String, Object>> listar(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String idPedido = firstColumn(template, TABLA_PEDIDO, "Id_PedidoTecnico", "IdPedidoTecnico", "Id_Pedido", "Id", "id");
        String fkPedido = firstColumn(template, TABLA_DETALLE, "Id_PedidoTecnico", "IdPedidoTecnico", "Id_Pedido", "IdPedido");
        String fechaPedido = firstColumn(template, TABLA_PEDIDO, "Fecha_Registro", "FechaRegistro", "fecha_registro", "Fecha", "fecha", "FechaPedido");
        String materialColumn = materialColumn(template);
        String idProductoColumn = firstColumn(template, TABLA_DETALLE, "Id_Producto", "IdProducto", "idProducto", "id_producto");
        String cantidadColumn = firstColumn(template, TABLA_DETALLE, "Cantidad", "cantidad", "Cant", "cant");
        if (idPedido != null && fkPedido != null) {
            return template.queryForList(
                    "SELECT p." + bracket(idPedido) + " AS PedidoIdAlias, " +
                            (fechaPedido == null ? "CAST(NULL AS DATETIME)" : "p." + bracket(fechaPedido)) + " AS PedidoFechaAlias, " +
                            (materialColumn == null ? "CAST(NULL AS VARCHAR(4000))" : "d." + bracket(materialColumn)) + " AS MaterialPedidoAlias, " +
                            (idProductoColumn == null ? "CAST(NULL AS INT)" : "d." + bracket(idProductoColumn)) + " AS ProductoPedidoAlias, " +
                            (cantidadColumn == null ? "CAST(NULL AS DECIMAL(18,2))" : "d." + bracket(cantidadColumn)) + " AS CantidadPedidoAlias, " +
                            "p.*, d.* FROM dbo." + TABLA_PEDIDO + " p " +
                            "LEFT JOIN dbo." + TABLA_DETALLE + " d ON d." + bracket(fkPedido) + " = p." + bracket(idPedido) + " " +
                            "ORDER BY p." + bracket(idPedido) + " DESC"
            );
        }
        return template.queryForList("SELECT * FROM dbo." + TABLA_PEDIDO + " ORDER BY 1 DESC");
    }

    public Integer crear(
            DataSource dataSource,
            Integer idUsuario,
            Integer idTecnico,
            Integer idRuta,
            String tecnico,
            Integer idSucursal,
            String observacion,
            List<PedidoTecnicoItemRequest> items) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        Set<String> pedidoColumns = columns(template, TABLA_PEDIDO);
        Set<String> detalleColumns = columns(template, TABLA_DETALLE);
        String idPedidoColumn = firstColumn(pedidoColumns, "Id_PedidoTecnico", "IdPedidoTecnico", "Id_Pedido", "Id", "id");
        String materialColumn = materialColumn(template);
        try {
            return template.execute((Connection connection) -> {
                boolean previousAutoCommit = connection.getAutoCommit();
                try {
                    connection.setAutoCommit(false);
                    Integer idPedido = insertarPedido(connection, pedidoColumns, idPedidoColumn, idUsuario, idTecnico, idRuta, tecnico, idSucursal, observacion);
                    if (idPedido == null || idPedido <= 0) {
                        throw new IllegalStateException("No se pudo obtener el id del pedido tecnico generado.");
                    }
                    for (PedidoTecnicoItemRequest item : items) {
                        insertarDetalle(connection, detalleColumns, materialColumn, idPedido, idTecnico, item);
                    }
                    connection.commit();
                    return idPedido;
                } catch (Exception ex) {
                    connection.rollback();
                    throw new RuntimeException(ex);
                } finally {
                    connection.setAutoCommit(previousAutoCommit);
                }
            });
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    private Integer insertarPedido(
            Connection connection,
            Set<String> columns,
            String idPedidoColumn,
            Integer idUsuario,
            Integer idTecnico,
            Integer idRuta,
            String tecnico,
            Integer idSucursal,
            String observacion) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, columns, Arrays.asList("Id_Usuario", "IdUsuario", "idUsuario", "id_usuario"), idUsuario);
        put(values, columns, Arrays.asList("Id_Tecnico", "IdTecnico", "idTecnico", "id_tecnico"), idTecnico);
        putAll(values, columns, Arrays.asList("Id_Vendedor", "IdVendedor", "idVendedor", "id_vendedor"), idTecnico);
        put(values, columns, Arrays.asList("Id_Ruta", "IdRuta", "idRuta", "id_ruta", "Ruta", "ruta"), idRuta);
        put(values, columns, Arrays.asList("Id_PedidoVendedor", "IdPedidoVendedor", "idPedidoVendedor", "id_pedido_vendedor"), 0);
        put(values, columns, Arrays.asList("Usuario", "usuario", "NombreUsuario", "Nombre_Usuario", "Tecnico", "NombreTecnico", "Nombre_Tecnico", "tecnico", "nombreTecnico"), tecnico);
        put(values, columns, Arrays.asList("Id_Sucursal", "IdSucursal", "idSucursal", "id_sucursal"), idSucursal);
        putAll(values, columns, Arrays.asList("Fecha_Registro", "FechaRegistro", "fecha_registro", "Fecha", "fecha", "FechaPedido"), Timestamp.valueOf(LocalDateTime.now()));
        put(values, columns, Arrays.asList("Observacion", "observacion", "Detalle", "detalle"), clean(observacion));
        put(values, columns, Arrays.asList("Estado", "estado"), "PENDIENTE");
        put(values, columns, Arrays.asList("E_Eliminado", "EEliminado", "e_eliminado"), false);

        InsertSql sql = buildInsertSql(TABLA_PEDIDO, columns, values, idPedidoColumn);
        try (PreparedStatement ps = connection.prepareStatement(sql.sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, sql.values);
            boolean hasResult = ps.execute();
            if (sql.usesOutput) {
                while (true) {
                    if (hasResult) {
                        try (ResultSet rs = ps.getResultSet()) {
                            if (rs != null && rs.next()) {
                                return rs.getInt(1);
                            }
                        }
                    }
                    int updateCount = ps.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                    hasResult = ps.getMoreResults();
                }
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs != null && rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    private void insertarDetalle(Connection connection, Set<String> columns, String materialColumn, Integer idPedido, Integer idTecnico, PedidoTecnicoItemRequest item) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        Timestamp fechaRegistro = Timestamp.valueOf(LocalDateTime.now());
        put(values, columns, Arrays.asList("Id_PedidoTecnico", "IdPedidoTecnico", "Id_Pedido", "IdPedido"), idPedido);
        put(values, columns, Arrays.asList("Id_Producto", "IdProducto", "idProducto", "id_producto"), item.getIdProducto());
        putAll(values, columns, Arrays.asList("Id_Vendedor", "IdVendedor", "idVendedor", "id_vendedor"), idTecnico);
        if (materialColumn != null) {
            values.put(materialColumn, clean(item.getMaterial()));
        }
        put(values, columns, Arrays.asList("Cantidad", "cantidad", "Cant", "cant"), item.getCantidad());
        putAll(values, columns, Arrays.asList("Fecha", "fecha", "Fecha_Registro", "FechaRegistro", "fecha_registro"), fechaRegistro);
        put(values, columns, Arrays.asList("E_Eliminado", "EEliminado", "e_eliminado"), false);
        InsertSql sql = buildInsertSql(TABLA_DETALLE, columns, values, null);
        try (PreparedStatement ps = connection.prepareStatement(sql.sql)) {
            bind(ps, sql.values);
            ps.executeUpdate();
        }
    }

    private String[] materialColumnCandidates() {
        return new String[] {
                "Material",
                "NombreMaterial",
                "Nombre_Material",
                "MaterialPedido",
                "Material_Pedido",
                "Producto",
                "NombreProducto",
                "Nombre_Producto",
                "Nombre",
                "Descripcion",
                "DescripcionMaterial",
                "Descripcion_Material",
                "Detalle",
                "Item",
                "Articulo",
                "Codigo",
                "CodigoMaterial",
                "Codigo_Material",
                "CodMaterial",
                "Cod_Material",
                "Cod_Inicio",
                "CodInicio",
                "Pedido",
                "NombrePedido"
        };
    }

    private String materialColumn(JdbcTemplate template) {
        String named = firstColumn(template, TABLA_DETALLE, materialColumnCandidates());
        if (named != null) {
            return named;
        }
        for (ColumnInfo column : columnInfos(template, TABLA_DETALLE)) {
            String normalized = norm(column.name);
            String type = column.type.toLowerCase(Locale.ROOT);
            boolean textType = type.contains("char") || type.contains("text");
            boolean blocked = normalized.contains("id")
                    || normalized.contains("cantidad")
                    || normalized.contains("fecha")
                    || normalized.contains("eliminado")
                    || normalized.contains("chip")
                    || normalized.contains("serie")
                    || normalized.contains("serial");
            if (textType && !blocked) {
                return column.name;
            }
        }
        return null;
    }

    private InsertSql buildInsertSql(String table, Set<String> columns, Map<String, Object> values, String idColumn) {
        List<String> insertColumns = new ArrayList<>();
        List<Object> insertValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            insertColumns.add(entry.getKey());
            insertValues.add(entry.getValue());
        }
        StringBuilder sql = new StringBuilder("INSERT INTO dbo.").append(table).append(" (");
        for (int i = 0; i < insertColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(bracket(insertColumns.get(i)));
        }
        sql.append(") ");
        boolean output = idColumn != null && columns.contains(norm(idColumn));
        if (output) {
            sql.append("OUTPUT INSERTED.").append(bracket(idColumn)).append(" ");
        }
        sql.append("VALUES (");
        for (int i = 0; i < insertColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return new InsertSql(sql.toString(), insertValues, output);
    }

    private void bind(PreparedStatement ps, List<Object> values) throws Exception {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value instanceof BigDecimal) {
                ps.setBigDecimal(i + 1, (BigDecimal) value);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    private void put(Map<String, Object> out, Set<String> columns, List<String> candidates, Object value) {
        String column = firstColumn(columns, candidates.toArray(new String[0]));
        if (column != null) {
            out.put(column, value);
        }
    }

    private void putAll(Map<String, Object> out, Set<String> columns, List<String> candidates, Object value) {
        Set<String> candidateNorms = new LinkedHashSet<>();
        for (String candidate : candidates) {
            candidateNorms.add(norm(candidate));
        }
        for (String column : columns) {
            if (candidateNorms.contains(norm(column))) {
                out.put(column, value);
            }
        }
    }

    private Set<String> columns(JdbcTemplate template, String table) {
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?",
                table
        );
        Set<String> out = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Object name = row.get("COLUMN_NAME");
            if (name != null) {
                out.add(String.valueOf(name));
            }
        }
        return out;
    }

    private List<ColumnInfo> columnInfos(JdbcTemplate template, String table) {
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                table
        );
        List<ColumnInfo> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object name = row.get("COLUMN_NAME");
            Object type = row.get("DATA_TYPE");
            if (name != null) {
                out.add(new ColumnInfo(String.valueOf(name), type == null ? "" : String.valueOf(type)));
            }
        }
        return out;
    }

    private String firstColumn(JdbcTemplate template, String table, String... candidates) {
        return firstColumn(columns(template, table), candidates);
    }

    private String firstColumn(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            String candidateNorm = norm(candidate);
            for (String column : columns) {
                if (norm(column).equals(candidateNorm)) {
                    return column;
                }
            }
        }
        return null;
    }

    private String bracket(String column) {
        return "[" + column.replace("]", "]]") + "]";
    }

    private String norm(String value) {
        return value == null ? "" : value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static class InsertSql {
        private final String sql;
        private final List<Object> values;
        private final boolean usesOutput;

        private InsertSql(String sql, List<Object> values, boolean usesOutput) {
            this.sql = sql;
            this.values = values;
            this.usesOutput = usesOutput;
        }
    }

    private static class ColumnInfo {
        private final String name;
        private final String type;

        private ColumnInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
