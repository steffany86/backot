package com.example.TigoStarSystem.llamadaatencion.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
                        "Id_LlamadaAtencion AS idLlamadaAtencion, " +
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
                        "ORDER BY la.Fecha_Registro DESC, la.Id_LlamadaAtencion DESC",
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
        String nuevoId = generarIdLlamadaAtencion();
        int inserted = tigohogarJdbcTemplate.update(
                "INSERT INTO dbo.tbl_LlamadaAtencion (" +
                        "Id_LlamadaAtencion, CodigoEmpleado, NombreEmpleado, Tabla, Id_TipoComunicacion, " +
                        "Fecha_Registro, Motivo, Descripcion, ComentarioColaborador, Acuerdos, FechaSeguimiento, " +
                        "FirmaTecnico, FirmaTestigo, Id_UsuarioSupervisor, Testigo, Sucursal, Id_Sucursal" +
                        ") VALUES (?, ?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                nuevoId,
                toInteger(codEmpleado),
                trimToNull(tecnicoNombre),
                trimToNull(tabla),
                trimToNull(idTipoComunicacion),
                trimToNull(motivo),
                trimToNull(descripcion),
                trimToNull(comentarioColaborador),
                trimToNull(acuerdos),
                fechaSeguimiento == null ? null : Timestamp.valueOf(fechaSeguimiento),
                trimToNull(firmaTecnico),
                trimToNull(firmaTestigo),
                idUsuarioSupervisor,
                trimToNull(testigo),
                trimToNull(sucursal),
                idSucursal
        );
        if (inserted > 0) {
            return nuevoId;
        }

        throw new IllegalStateException("No se pudo obtener Id_LlamadaAtencion generado.");
    }

    private String generarIdLlamadaAtencion() {
        for (int i = 0; i < 20; i++) {
            String id = UUID.randomUUID().toString().replace("-", "");
            id = id.substring(id.length() - 8);
            Integer existe = tigohogarJdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM dbo.tbl_LlamadaAtencion WHERE Id_LlamadaAtencion = ?",
                    Integer.class,
                    id
            );
            if (existe == null || existe == 0) {
                return id;
            }
        }
        throw new IllegalStateException("No se pudo generar Id_LlamadaAtencion unico.");
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
