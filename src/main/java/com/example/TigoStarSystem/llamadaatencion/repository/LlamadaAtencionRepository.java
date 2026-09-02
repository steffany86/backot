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

    public List<Map<String, Object>> listarLlamadasAtencion(
            String idTecnico,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            Integer idSucursal) {
        return tigohogarJdbcTemplate.queryForList(
                "SELECT TOP (?) " +
                        "la.Id AS idLlamadaAtencion, " +
                        "CAST(CodigoEmpleado AS NVARCHAR(30)) AS idTecnico, " +
                        "CAST(CodigoEmpleado AS NVARCHAR(30)) AS codEmpleado, " +
                        "NombreEmpleado AS tecnico, " +
                        "NombreEmpleado AS tecnicoNombre, " +
                        "Tabla AS tabla, " +
                        "Id_UsuarioSupervisor AS idUsuarioSupervisor, " +
                        "la.Id_TipoComunicacion AS idTipoComunicacion, " +
                        "tc.TipoComunicacion AS tipoComunicacion, " +
                        "Fecha_Registro AS fechaRegistro, " +
                        "Motivo AS motivo, " +
                        "Descripcion AS descripcion, " +
                        "ComentarioColaborador AS comentarioColaborador, " +
                        "Acuerdos AS acuerdos, " +
                        "Testigo AS testigo, " +
                        "FechaSeguimiento AS fechaSeguimiento, " +
                        "FirmaTecnico AS firmaTecnico, " +
                        "FirmaTestigo AS firmaTestigo, " +
                        "Id_Sucursal AS idSucursal, " +
                        "Sucursal AS sucursal " +
                        "FROM dbo.tbl_LlamadaAtencion la " +
                        "LEFT JOIN dbo.tbl_TipoComunicacion tc ON tc.Id_TipoComunicacion = la.Id_TipoComunicacion " +
                        "WHERE (? IS NULL OR LTRIM(RTRIM(?)) = '' OR CAST(la.CodigoEmpleado AS NVARCHAR(30)) = LTRIM(RTRIM(?))) " +
                        "  AND (? IS NULL OR CAST(la.Fecha_Registro AS DATE) >= ?) " +
                        "  AND (? IS NULL OR CAST(la.Fecha_Registro AS DATE) <= ?) " +
                        "  AND (? IS NULL OR ISNULL(la.Id_Sucursal, -1) = ?) " +
                        "  AND ISNULL(la.e_eliminado, 0) = 0 " +
                        "ORDER BY la.Fecha_Registro DESC, la.Id DESC",
                resolveLimit(limite),
                trimToNull(idTecnico),
                trimToNull(idTecnico),
                trimToNull(idTecnico),
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                idSucursal,
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
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = tigohogarJdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO dbo.tbl_LlamadaAtencion (" +
                            "CodigoEmpleado, NombreEmpleado, Tabla, Id_TipoComunicacion, " +
                            "Fecha_Registro, Motivo, Descripcion, ComentarioColaborador, Acuerdos, FechaSeguimiento, " +
                            "FirmaTecnico, FirmaTestigo, Id_UsuarioSupervisor, Testigo, Sucursal, Id_Sucursal, e_eliminado" +
                            ") VALUES (?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
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
