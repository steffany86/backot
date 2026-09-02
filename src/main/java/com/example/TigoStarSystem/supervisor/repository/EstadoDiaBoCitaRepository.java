package com.example.TigoStarSystem.supervisor.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class EstadoDiaBoCitaRepository {
    private static final String SP_ULTIMO_ESTADO_DIA_BO_CITA_MAKIRO =
            "EXEC dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO ?, ?";
    private static final String SP_CRUCE_ORDENES_AGENDA_VS_MAKIRO =
            "EXEC dbo.spy_CruceOrdenes_Agenda_vs_Makiro ?";

    private final JdbcTemplate centralJdbcTemplate;

    public EstadoDiaBoCitaRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> obtenerUltimoEstadoDia(LocalDate fecha, String tecnico) {
        return centralJdbcTemplate.queryForList(
                SP_ULTIMO_ESTADO_DIA_BO_CITA_MAKIRO,
                Date.valueOf(fecha),
                tecnico
        );
    }

    public List<Map<String, Object>> obtenerCruceOrdenesAgendaMakiro(LocalDate fecha) {
        return centralJdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(SP_CRUCE_ORDENES_AGENDA_VS_MAKIRO);
            statement.setDate(1, Date.valueOf(fecha));
            return statement;
        }, resultSet -> {
            List<Map<String, Object>> rows = new java.util.ArrayList<>();
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                Map<String, Integer> seen = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String key = uniqueColumnName(metadata.getColumnLabel(i), seen);
                    row.put(key, normalizeSqlValue(resultSet.getObject(i)));
                }
                rows.add(row);
            }
            enriquecerIdsHistorial(rows, fecha);
            return rows;
        });
    }

    /**
     * Algunos procedimientos de cruce no exponen el ID de historial aunque
     * la orden sí exista. Lo recuperamos con la misma llave usada por
     * digitación para que las acciones de verificación puedan guardar.
     */
    private void enriquecerIdsHistorial(List<Map<String, Object>> rows, LocalDate fecha) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Map<String, Object>> historial = centralJdbcTemplate.queryForList(
                "SELECT Id_BO_CITA_MAKIRO_Historial, cliente_nro, OT_int "
                        + "FROM dbo.tbl_BO_CITA_MAKIRO_Historial "
                        + "WHERE Vigente = 2 AND Estado <> 'Finalizado'"
        );
        Map<String, Object> ids = new HashMap<>();
        for (Map<String, Object> item : historial) {
            String key = key(readFirst(item, "cliente_nro", "CodigoCliente", "Cliente_Nro"),
                    readFirst(item, "OT_int", "OT", "OrdenTrabajo"));
            if (!key.isEmpty()) {
                ids.putIfAbsent(key, readFirst(item, "Id_BO_CITA_MAKIRO_Historial"));
            }
        }
        for (Map<String, Object> row : rows) {
            Object currentId = readFirst(row, "Id_BO_CITA_MAKIRO_Historial", "idHistorial", "idBoCitaMakiroHistorial");
            if (currentId != null && !String.valueOf(currentId).trim().isEmpty()) {
                continue;
            }
            String key = key(readFirst(row, "cliente_nro", "CodigoCliente", "Cliente_Nro", "clienteNro"),
                    readFirst(row, "OT_int", "OT", "OrdenTrabajo", "ordenTrabajo"));
            Object id = ids.get(key);
            if (id != null) {
                row.put("Id_BO_CITA_MAKIRO_Historial", id);
            }
        }
    }

    private Object readFirst(Map<String, Object> row, String... names) {
        if (row == null) {
            return null;
        }
        for (String name : names) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)
                        && entry.getValue() != null && !String.valueOf(entry.getValue()).trim().isEmpty()) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String key(Object cliente, Object ot) {
        String left = normalizeKey(cliente);
        String right = normalizeKey(ot);
        return left.isEmpty() || right.isEmpty() ? "" : left + "|" + right;
    }

    private String normalizeKey(Object value) {
        if (value == null) {
            return "";
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".0") ? normalized.substring(0, normalized.length() - 2) : normalized;
    }

    public int marcarVerificaBack(Integer idHistorial, int actualizado, String usuario, String observacion) {
        return centralJdbcTemplate.update(
                "UPDATE dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "SET Actualizado_VERIFICABACK = ?, " +
                        "usuarioModificacion_VERIFICABACK = ?, " +
                        "fechaRegistro_VERIFICABACK = GETDATE(), " +
                        "Observacion_VERIFICABACK = ? " +
                        "WHERE Id_BO_CITA_MAKIRO_Historial = ?",
                actualizado,
                usuario,
                observacion,
                idHistorial
        );
    }

    private String uniqueColumnName(String columnName, Map<String, Integer> seen) {
        String base = columnName == null || columnName.trim().isEmpty() ? "columna" : columnName.trim();
        Integer count = seen.get(base.toLowerCase());
        if (count == null) {
            seen.put(base.toLowerCase(), 1);
            return base;
        }
        int next = count + 1;
        seen.put(base.toLowerCase(), next);
        return base + "_" + next;
    }

    private Object normalizeSqlValue(Object value) throws SQLException {
        if (value instanceof Clob) {
            Clob clob = (Clob) value;
            long length = clob.length();
            if (length <= 0) {
                return "";
            }
            return clob.getSubString(1, (int) Math.min(length, Integer.MAX_VALUE));
        }
        return value;
    }
}
