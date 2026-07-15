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
            return rows;
        });
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
