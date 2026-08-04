package com.example.TigoStarSystem.digitador.repository;

import com.example.TigoStarSystem.digitador.dto.DigitadorCruceNoFinalizadoRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class DigitadorCruceNoFinalizadoRepository {
    private final JdbcTemplate centralJdbcTemplate;

    public DigitadorCruceNoFinalizadoRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> listar(LocalDate fecha) {
        List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
                "EXEC dbo.spy_CruceOrdenes_Agenda_vs_Makiro_NO_FINALIZADO ?",
                Date.valueOf(fecha)
        );
        enriquecerIdentificadores(rows, fecha);
        return rows;
    }

    public List<Map<String, Object>> listarEstados() {
        return centralJdbcTemplate.queryForList("EXEC dbo.spx_ObtenerEstadoDigitacionAgendaNoFinalizadas");
    }

    public int actualizar(
            Integer idHistorial,
            LocalDate fechaEjecucion,
            String estado,
            String observacion,
            String usuarioModifica) {
        String sql = "UPDATE dbo.tbl_BO_CITA_MAKIRO_Historial "
                + "SET fecha_Ejecuacion_DIGITACION = ?, Estado_DIGITACION = ?, "
                + "Observacion_DIGITACION = ?, Actualizado_DIGITACION = 1, "
                + "fechaRegistro_DIGITACION = GETDATE(), usuarioModifica_DIGITACION = ? "
                + "WHERE Id_BO_CITA_MAKIRO_Historial = ? AND Vigente = 2 AND Estado <> 'Finalizado'";
        return centralJdbcTemplate.update(
                sql,
                Date.valueOf(fechaEjecucion),
                estado,
                observacion,
                usuarioModifica,
                idHistorial
        );
    }

    private void enriquecerIdentificadores(List<Map<String, Object>> rows, LocalDate fecha) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Map<String, Object>> historial = centralJdbcTemplate.queryForList(
                "SELECT Id_BO_CITA_MAKIRO_Historial, cliente_nro, OT_int "
                        + "FROM dbo.tbl_BO_CITA_MAKIRO_Historial "
                        + "WHERE Vigente = 2 AND Estado <> 'Finalizado' "
                        + "AND dbo.dateonly(CONVERT(DATETIME, inicio_agendado, 120)) = ?",
                Date.valueOf(fecha)
        );
        Map<String, Object> ids = new HashMap<>();
        for (Map<String, Object> row : historial) {
            String key = key(row.get("cliente_nro"), row.get("OT_int"));
            if (!key.isEmpty()) {
                ids.putIfAbsent(key, row.get("Id_BO_CITA_MAKIRO_Historial"));
            }
        }
        for (Map<String, Object> row : rows) {
            Object id = ids.get(key(read(row, "cliente_nro"), read(row, "OT_int")));
            if (id != null) {
                row.put("Id_BO_CITA_MAKIRO_Historial", id);
                row.put("puedeActualizarDigitacion", true);
            } else {
                row.put("puedeActualizarDigitacion", false);
            }
        }
    }

    private Object read(Map<String, Object> row, String name) {
        if (row == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String key(Object cliente, Object ot) {
        String left = cliente == null ? "" : String.valueOf(cliente).trim().toLowerCase(Locale.ROOT);
        String right = ot == null ? "" : String.valueOf(ot).trim().toLowerCase(Locale.ROOT);
        return left.isEmpty() || right.isEmpty() ? "" : left + "|" + right;
    }
}
