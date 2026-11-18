package com.example.TigoStarSystem.llamadaatencion.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class LlamadaAtencionRepository {
    private final JdbcTemplate tigohogarJdbcTemplate;

    public LlamadaAtencionRepository(@Qualifier("tigohogarJdbcTemplate") JdbcTemplate tigohogarJdbcTemplate) {
        this.tigohogarJdbcTemplate = tigohogarJdbcTemplate;
    }

    public List<Map<String, Object>> listarTiposComunicacion() {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.spx_ListarTipoComunicacionLlamadaAtencion"
        );
    }

    public List<Map<String, Object>> listarMotivosIncidencia() {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.spx_ListarMotivoLlamadaDeAtencion"
        );
    }

    public List<Map<String, Object>> listarLlamadasAtencion(
            String idTecnico,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            Integer idSucursal) {
        return tigohogarJdbcTemplate.queryForList(
                "EXEC dbo.spx_ObtenerLlamadasDeAtencion ?, ?, ?, ?, ?",
                resolveLimit(limite),
                trimToNull(idTecnico),
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                idSucursal
        );
    }

    public String insertarLlamadaAtencion(
            String idTecnico,
            String codEmpleado,
            Integer idUsuarioSupervisor,
            String idTipoComunicacion,
            String motivo,
            String descripcion,
            String comentarioColaborador,
            String acuerdos,
            String testigo,
            LocalDateTime fechaSeguimiento,
            String firmaTecnico,
            String firmaTestigo,
            Integer idSucursal,
            String sucursal,
            String tecnicoNombre,
            String tabla) {
        // Sin estado: se usa el valor por defecto de la columna Estado (COMPLETO).
        return insertarLlamadaAtencion(idTecnico, codEmpleado, idUsuarioSupervisor, idTipoComunicacion, motivo, descripcion,
                comentarioColaborador, acuerdos, testigo, fechaSeguimiento, firmaTecnico, firmaTestigo, idSucursal,
                sucursal, tecnicoNombre, tabla, null);
    }

    /** Igual que el anterior, pero permite fijar el Estado de la llamada (por ejemplo "Pendiente" hasta que firme el tecnico). */
    public String insertarLlamadaAtencion(
            String idTecnico,
            String codEmpleado,
            Integer idUsuarioSupervisor,
            String idTipoComunicacion,
            String motivo,
            String descripcion,
            String comentarioColaborador,
            String acuerdos,
            String testigo,
            LocalDateTime fechaSeguimiento,
            String firmaTecnico,
            String firmaTestigo,
            Integer idSucursal,
            String sucursal,
            String tecnicoNombre,
            String tabla,
            String estado) {
        final boolean conEstado = trimToNull(estado) != null;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = tigohogarJdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO dbo.tbl_LlamadaAtencion (" +
                            "CodigoEmpleado, NombreEmpleado, Tabla, Id_TipoComunicacion, " +
                            "Fecha_Registro, Motivo, Descripcion, ComentarioColaborador, Acuerdos, FechaSeguimiento, " +
                            "FirmaTecnico, FirmaTestigo, Id_UsuarioSupervisor, Testigo, Sucursal, Id_Sucursal, e_eliminado" +
                            (conEstado ? ", Estado" : "") +
                            ") VALUES (?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0" +
                            (conEstado ? ", ?" : "") + ")",
                    Statement.RETURN_GENERATED_KEYS);
            if (conEstado) {
                statement.setObject(16, trimToNull(estado));
            }
            statement.setObject(1, toInteger(codEmpleado));
            statement.setObject(2, trimToNull(tecnicoNombre));
            statement.setObject(3, trimToNull(tabla));
            statement.setObject(4, trimToNull(idTipoComunicacion));
            statement.setObject(5, trimToNull(motivo));
            statement.setObject(6, trimToNull(descripcion));
            statement.setObject(7, trimToNull(comentarioColaborador));
            statement.setObject(8, trimToNull(acuerdos));
            statement.setObject(9, fechaSeguimiento == null ? null : Timestamp.valueOf(fechaSeguimiento));
            statement.setObject(10, trimToNull(firmaTecnico));
            statement.setObject(11, trimToNull(firmaTestigo));
            statement.setObject(12, idUsuarioSupervisor);
            statement.setObject(13, trimToNull(testigo));
            statement.setObject(14, trimToNull(sucursal));
            statement.setObject(15, idSucursal);
            return statement;
        }, keyHolder);
        if (inserted > 0) {
            Number key = keyHolder.getKey();
            if (key != null) {
                return String.valueOf(key.intValue());
            }
        }

        throw new IllegalStateException("No se pudo obtener el Id identity generado.");
    }

    /** Guarda las firmas (rutas) y pasa la llamada a "COMPLETO". Devuelve las filas actualizadas (0 si no existe o ya esta completa). */
    public int completarConFirmas(int idLlamada, String firmaTecnico, String firmaTestigo, String testigo) {
        return tigohogarJdbcTemplate.update(
                "UPDATE dbo.tbl_LlamadaAtencion SET FirmaTecnico = ?, FirmaTestigo = ?, Testigo = ISNULL(?, Testigo), Estado = 'COMPLETO' "
                        + "WHERE Id = ? AND ISNULL(e_eliminado, 0) = 0 AND LOWER(LTRIM(RTRIM(ISNULL(Estado, '')))) = 'pendiente'",
                trimToNull(firmaTecnico), trimToNull(firmaTestigo), trimToNull(testigo), idLlamada);
    }

    /** Resumen de una llamada de atencion (estado y datos basicos); null si no existe. */
    public Map<String, Object> obtenerResumen(int idLlamada) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                "SELECT Id AS idLlamadaAtencion, Estado AS estado, NombreEmpleado AS tecnico, Motivo AS motivo, "
                        + "Fecha_Registro AS fechaRegistro, Descripcion AS descripcion, ComentarioColaborador AS comentarioColaborador, "
                        + "Acuerdos AS acuerdos, FechaSeguimiento AS fechaSeguimiento, FirmaTecnico AS firmaTecnico, FirmaTestigo AS firmaTestigo, Testigo AS testigo "
                        + "FROM dbo.tbl_LlamadaAtencion WHERE Id = ? AND ISNULL(e_eliminado, 0) = 0",
                idLlamada);
        return rows.isEmpty() ? null : rows.get(0);
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

    private Integer toInteger(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
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

}
