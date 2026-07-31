package com.example.TigoStarSystem.supervisor.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        List<Map<String, Object>> rows = centralJdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(SP_CRUCE_ORDENES_AGENDA_VS_MAKIRO);
            statement.setDate(1, Date.valueOf(fecha));
            return statement;
        }, resultSet -> {
            List<Map<String, Object>> mappedRows = new ArrayList<>();
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                Map<String, Integer> seen = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String key = uniqueColumnName(metadata.getColumnLabel(i), seen);
                    row.put(key, normalizeSqlValue(resultSet.getObject(i)));
                }
                mappedRows.add(row);
            }
            return mappedRows;
        });
        enriquecerVerificaBack(rows);
        return rows;
    }

    public int marcarVerificaBack(Integer idHistorial, String usuarioModificacion, String observacion) {
        return centralJdbcTemplate.update(
                "UPDATE dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "SET Actualizado_VERIFICABACK = 1, " +
                        "    usuarioModificacion_VERIFICABACK = ?, " +
                        "    fechaRegistro_VERIFICABACK = GETDATE(), " +
                        "    Observacion_VERIFICABACK = ? " +
                        "WHERE Id_BO_CITA_MAKIRO_Historial = ?",
                usuarioModificacion,
                observacion,
                idHistorial
        );
    }

    public Map<String, Object> obtenerVerificaBack(Integer idHistorial) {
        List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
                "SELECT Id_BO_CITA_MAKIRO_Historial, " +
                        "Actualizado_VERIFICABACK, " +
                        "usuarioModificacion_VERIFICABACK, " +
                        "fechaRegistro_VERIFICABACK, " +
                        "Observacion_VERIFICABACK " +
                        "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "WHERE Id_BO_CITA_MAKIRO_Historial = ?",
                idHistorial
        );
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    private void enriquecerVerificaBack(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Integer> ids = new LinkedHashSet<>();
        Set<String> paresClienteOt = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Integer id = toInteger(firstValue(row,
                    "Id_BO_CITA_MAKIRO_Historial",
                    "id_BO_CITA_MAKIRO_Historial",
                    "idBoCitaMakiroHistorial"
            ));
            if (id != null) {
                ids.add(id);
                continue;
            }
            String cliente = toLookupText(firstValue(row,
                    "cliente_nro",
                    "CodigoCliente",
                    "codigoCliente",
                    "Cliente_Nro",
                    "clienteNro"
            ));
            String ot = toLookupText(firstValue(row,
                    "OT_int",
                    "OrdenTrabajo",
                    "ordenTrabajo",
                    "OT",
                    "ot",
                    "numeroOrden",
                    "NumeroOrden",
                    "nroOT"
            ));
            if (cliente != null && ot != null) {
                paresClienteOt.add(cliente + "|" + ot);
            }
        }
        if (ids.isEmpty() && paresClienteOt.isEmpty()) {
            return;
        }

        try {
            Map<Integer, Map<String, Object>> verificaPorId = new HashMap<>();
            Map<String, Map<String, Object>> verificaPorClienteOt = new HashMap<>();
            List<Integer> idList = new ArrayList<>(ids);
            int chunkSize = 500;
            for (int start = 0; start < idList.size(); start += chunkSize) {
                int end = Math.min(start + chunkSize, idList.size());
                List<Integer> chunk = idList.subList(start, end);
                String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
                List<Map<String, Object>> estados = centralJdbcTemplate.queryForList(
                        "SELECT Id_BO_CITA_MAKIRO_Historial, " +
                                "Actualizado_VERIFICABACK, " +
                                "usuarioModificacion_VERIFICABACK, " +
                                "fechaRegistro_VERIFICABACK, " +
                                "Observacion_VERIFICABACK " +
                                "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                                "WHERE Id_BO_CITA_MAKIRO_Historial IN (" + placeholders + ")",
                        chunk.toArray()
                );
                for (Map<String, Object> estado : estados) {
                    Integer id = toInteger(firstValue(estado, "Id_BO_CITA_MAKIRO_Historial"));
                    if (id != null) {
                        verificaPorId.put(id, estado);
                    }
                }
            }
            List<String> pares = new ArrayList<>(paresClienteOt);
            int pairChunkSize = 200;
            for (int start = 0; start < pares.size(); start += pairChunkSize) {
                int end = Math.min(start + pairChunkSize, pares.size());
                List<String> chunk = pares.subList(start, end);
                StringBuilder where = new StringBuilder();
                List<Object> args = new ArrayList<>();
                for (String par : chunk) {
                    String[] parts = par.split("\\|", 2);
                    if (parts.length != 2) {
                        continue;
                    }
                    if (where.length() > 0) {
                        where.append(" OR ");
                    }
                    where.append("(cliente_nro = ? AND OT = ?)");
                    args.add(parts[0]);
                    args.add(parts[1]);
                }
                if (args.isEmpty()) {
                    continue;
                }
                List<Map<String, Object>> estados = centralJdbcTemplate.queryForList(
                        "SELECT Id_BO_CITA_MAKIRO_Historial, " +
                                "cliente_nro, " +
                                "OT, " +
                                "Actualizado_VERIFICABACK, " +
                                "usuarioModificacion_VERIFICABACK, " +
                                "fechaRegistro_VERIFICABACK, " +
                                "Observacion_VERIFICABACK " +
                                "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                                "WHERE " + where +
                                " ORDER BY Id_BO_CITA_MAKIRO_Historial DESC",
                        args.toArray()
                );
                for (Map<String, Object> estado : estados) {
                    String cliente = toLookupText(firstValue(estado, "cliente_nro"));
                    String ot = toLookupText(firstValue(estado, "OT"));
                    if (cliente != null && ot != null) {
                        String key = cliente + "|" + ot;
                        if (!verificaPorClienteOt.containsKey(key)) {
                            verificaPorClienteOt.put(key, estado);
                        }
                    }
                }
            }

            for (Map<String, Object> row : rows) {
                Integer id = toInteger(firstValue(row,
                        "Id_BO_CITA_MAKIRO_Historial",
                        "id_BO_CITA_MAKIRO_Historial",
                        "idBoCitaMakiroHistorial"
                ));
                Map<String, Object> estado = id == null ? null : verificaPorId.get(id);
                if (estado == null) {
                    String cliente = toLookupText(firstValue(row,
                            "cliente_nro",
                            "CodigoCliente",
                            "codigoCliente",
                            "Cliente_Nro",
                            "clienteNro"
                    ));
                    String ot = toLookupText(firstValue(row,
                            "OT_int",
                            "OrdenTrabajo",
                            "ordenTrabajo",
                            "OT",
                            "ot",
                            "numeroOrden",
                            "NumeroOrden",
                            "nroOT"
                    ));
                    estado = cliente == null || ot == null ? null : verificaPorClienteOt.get(cliente + "|" + ot);
                }
                if (estado == null) {
                    continue;
                }
                row.put("Id_BO_CITA_MAKIRO_Historial", estado.get("Id_BO_CITA_MAKIRO_Historial"));
                row.put("Actualizado_VERIFICABACK", estado.get("Actualizado_VERIFICABACK"));
                row.put("usuarioModificacion_VERIFICABACK", estado.get("usuarioModificacion_VERIFICABACK"));
                row.put("fechaRegistro_VERIFICABACK", estado.get("fechaRegistro_VERIFICABACK"));
                row.put("Observacion_VERIFICABACK", estado.get("Observacion_VERIFICABACK"));
            }
        } catch (DataAccessException ignored) {
            // Si los campos aun no existen en algun ambiente, el cruce sigue funcionando con el SP original.
        }
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

    private Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object direct = row.get(key);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toLookupText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "-".equals(text)) {
            return null;
        }
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }
}
