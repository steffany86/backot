package com.example.TigoStarSystem.boletadigital.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BoletaDigitalRepository {
    private final JdbcTemplate centralJdbcTemplate;

    public BoletaDigitalRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> listarOtArchivo(JdbcTemplate jdbcTemplate, LocalDate fechaInicio, LocalDate fechaFin) {
        asegurarTablaArchivoDigital(jdbcTemplate);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "EXEC dbo.spx_EnlistarOtArchivo_web ?, ?",
                fechaInicio == null ? null : java.sql.Date.valueOf(fechaInicio),
                fechaFin == null ? null : java.sql.Date.valueOf(fechaFin)
        );
        Map<String, CitaInfo> citas = cargarCitasFinalizadas();
        Map<String, CambioInfo> cambios = cargarUltimosCambios(jdbcTemplate);
        for (int i = 0; i < rows.size(); i++) {
            rows.set(i, normalizarRow(rows.get(i), citas, cambios));
        }
        return rows;
    }

    public Map<String, Object> obtenerVenta(JdbcTemplate jdbcTemplate, Integer idVenta) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 " +
                        "Id_Venta AS idVenta, " +
                        "CodigoCliente AS codigoCliente, " +
                        "OrdenTrabajo AS ordenTrabajo, " +
                        "Fecha_Ejecucion AS fechaEjecucion, " +
                        "RutaPdf AS rutaPdf " +
                        "FROM dbo.tbl_Venta " +
                        "WHERE Id_Venta = ? AND ISNULL(E_Eliminado, 0) = 0",
                idVenta
        );
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> obtenerCita(Integer codigoCliente, Integer ordenTrabajo) {
        List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
                "SELECT * " +
                        "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "WHERE Vigente = 2 " +
                        "  AND OT_FISICA IS NOT NULL " +
                        "  AND cliente_nro = ? " +
                        "  AND OT = ?",
                String.valueOf(codigoCliente),
                String.valueOf(ordenTrabajo)
        );
        return elegirMejorCita(rows);
    }

    public void asegurarTablaArchivoDigital(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(
                "IF OBJECT_ID('dbo.tbl_ventaArchivoDigital', 'U') IS NULL " +
                        "BEGIN " +
                        "CREATE TABLE dbo.tbl_ventaArchivoDigital (" +
                        "id INT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "id_venta INT NULL, " +
                        "nombreArchivoAnterior NVARCHAR(MAX) NULL, " +
                        "fechaRegistro DATETIME NULL, " +
                        "usuario NVARCHAR(150) NULL" +
                        "); " +
                        "END; " +
                        "IF COL_LENGTH('dbo.tbl_ventaArchivoDigital', 'nombreArchivoNuevo') IS NULL " +
                        "BEGIN ALTER TABLE dbo.tbl_ventaArchivoDigital ADD nombreArchivoNuevo NVARCHAR(MAX) NULL; END; " +
                        "IF COL_LENGTH('dbo.tbl_ventaArchivoDigital', 'comparacion') IS NULL " +
                        "BEGIN ALTER TABLE dbo.tbl_ventaArchivoDigital ADD comparacion NVARCHAR(30) NULL; END;"
        );
    }

    public int actualizarRutaPdf(JdbcTemplate jdbcTemplate, Integer idVenta, String nuevaRutaPdf) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_Venta SET RutaPdf = ? WHERE Id_Venta = ?",
                nuevaRutaPdf,
                idVenta
        );
    }

    public int registrarCambioArchivo(
            JdbcTemplate jdbcTemplate,
            Integer idVenta,
            String nombreArchivoAnterior,
            String nombreArchivoNuevo,
            String comparacion,
            String usuario) {
        asegurarTablaArchivoDigital(jdbcTemplate);
        return jdbcTemplate.update(
                "INSERT INTO dbo.tbl_ventaArchivoDigital " +
                        "(id_venta, nombreArchivoAnterior, nombreArchivoNuevo, comparacion, fechaRegistro, usuario) " +
                        "VALUES (?, ?, ?, ?, GETDATE(), ?)",
                idVenta,
                nombreArchivoAnterior,
                nombreArchivoNuevo,
                comparacion,
                usuario
        );
    }

    private Map<String, CitaInfo> cargarCitasFinalizadas() {
        List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
                "SELECT * " +
                        "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "WHERE Vigente = 2 " +
                        "  AND OT_FISICA IS NOT NULL"
        );
        Map<String, CitaInfo> out = new HashMap<String, CitaInfo>();
        for (Map<String, Object> row : rows) {
            String cliente = normalizeNumber(findValue(row, "cliente_nro"));
            String ot = normalizeNumber(findValue(row, "OT"));
            if (cliente == null || ot == null) {
                continue;
            }
            CitaInfo info = toCitaInfo(row);
            String key = buildKey(cliente, ot);
            CitaInfo current = out.get(key);
            if (esMejorCita(info, current)) {
                out.put(key, info);
            }
        }
        return out;
    }

    private Map<String, Object> elegirMejorCita(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Map<String, Object> bestRow = null;
        CitaInfo bestInfo = null;
        for (Map<String, Object> row : rows) {
            CitaInfo info = toCitaInfo(row);
            if (esMejorCita(info, bestInfo)) {
                bestInfo = info;
                bestRow = row;
            }
        }
        return bestRow;
    }

    private CitaInfo toCitaInfo(Map<String, Object> row) {
        return new CitaInfo(
                toStringValue(findValue(row, "OT_FISICA")),
                toStringValue(findValue(row, "Estado")),
                estadoRank(findValue(row, "Estado")),
                fechaRank(row),
                idRank(row)
        );
    }

    private boolean esMejorCita(CitaInfo candidate, CitaInfo current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (candidate.estadoRank != current.estadoRank) {
            return candidate.estadoRank > current.estadoRank;
        }
        if (candidate.fechaRank != current.fechaRank) {
            return candidate.fechaRank > current.fechaRank;
        }
        if (candidate.idRank != current.idRank) {
            return candidate.idRank > current.idRank;
        }
        return false;
    }

    private int estadoRank(Object value) {
        String estado = toStringValue(value);
        if (estado == null) {
            return 0;
        }
        String normalized = estado.trim().toUpperCase();
        if ("FINALIZADO".equals(normalized)) {
            return 3;
        }
        if (normalized.contains("FINAL")) {
            return 2;
        }
        return 1;
    }

    private long fechaRank(Map<String, Object> row) {
        Object value = findValue(
                row,
                "Fecha", "fecha",
                "Fecha_Carga", "fecha_carga",
                "Fecha_Registro", "fecha_registro",
                "FechaActualizacion", "fechaActualizacion",
                "Fecha_Modificacion", "fecha_modificacion"
        );
        return temporalRank(value);
    }

    private long idRank(Map<String, Object> row) {
        Object value = findValue(row, "Id", "ID", "id", "Id_Cita", "id_cita", "IdBOCita", "idBOCita");
        return numericRank(value);
    }

    private long temporalRank(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).getTime();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        String raw = toStringValue(value);
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        String text = raw.trim();
        try {
            return OffsetDateTime.parse(text).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(text.substring(0, Math.min(10, text.length()))).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (RuntimeException ignored) {
        }
        return numericRank(value);
    }

    private long numericRank(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String raw = toStringValue(value);
        if (raw == null) {
            return 0L;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Map<String, Object> normalizarRow(
            Map<String, Object> row,
            Map<String, CitaInfo> citas,
            Map<String, CambioInfo> cambios) {
        Map<String, Object> out = new LinkedHashMap<String, Object>(row);
        Object idVenta = findValue(row, "Id_Venta", "idVenta", "id_venta", "idventa", "nventa", "NVenta", "NVENTA");
        Object cuadrilla = findValue(row, "Cuadrilla", "cuadrilla");
        Object ordenTrabajo = findValue(row, "OrdenTrabajo", "ordentrabajo");
        Object codigoCliente = findValue(row, "CodigoCliente", "codigocliente");
        String rutaPdf = toStringValue(findValue(row, "RutaPdf", "rutapdf"));
        String estadoOt = toStringValue(findValue(row, "Estado", "estado", "EstadoOT", "estadoOT", "EstadoOt", "estado_ot", "EstadoBO", "estadoBO"));
        Object otFisica = findValue(row, "OT_FIsica", "OT_FISICA", "otFisica", "ot_fisica");
        boolean tienePdf = rutaPdf != null && !rutaPdf.trim().isEmpty();

        out.put("Tecnico", cuadrilla);
        out.put("NroTransaccion", idVenta);
        out.put("OT", ordenTrabajo);
        out.put("cliente", codigoCliente);
        out.put("EstadoArchivo", tienePdf ? "CON_PDF" : "SIN_PDF");
        out.put("OT_FIsica", otFisica);
        out.put("VerPdfUrl", tienePdf ? buildArchivoUrl(rutaPdf, false) : null);
        out.put("DescargarPdfUrl", tienePdf ? buildArchivoUrl(rutaPdf, true) : null);

        CitaInfo cita = citas.get(buildKey(normalizeNumber(codigoCliente), normalizeNumber(ordenTrabajo)));
        if (cita != null) {
            out.put("OT_FISICA", otFisica != null ? otFisica : cita.otFisica);
            out.put("OT_FIsica", otFisica != null ? otFisica : cita.otFisica);
            out.put("EstadoBO", cita.estado);
            if (estadoOt == null || estadoOt.trim().isEmpty()) {
                estadoOt = cita.estado;
            }
        }
        out.put("Estado", estadoOt);
        CambioInfo cambio = cambios.get(normalizeNumber(idVenta));
        out.put("PreviamenteModificada", cambio != null);
        if (cambio != null && cambio.comparacion != null && !cambio.comparacion.trim().isEmpty()) {
            out.put("Comparacion", cambio.comparacion);
            out.put("NombreArchivoNuevo", cambio.nombreArchivoNuevo);
        } else {
            out.put("Comparacion", calcularComparacion(tienePdf, rutaPdf, cita));
        }
        return out;
    }

    private Map<String, CambioInfo> cargarUltimosCambios(JdbcTemplate jdbcTemplate) {
        asegurarTablaArchivoDigital(jdbcTemplate);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "WITH ultimos AS (" +
                        "SELECT id_venta, nombreArchivoNuevo, comparacion, " +
                        "ROW_NUMBER() OVER (PARTITION BY id_venta ORDER BY fechaRegistro DESC, id DESC) AS rn " +
                        "FROM dbo.tbl_ventaArchivoDigital " +
                        "WHERE id_venta IS NOT NULL" +
                        ") " +
                        "SELECT id_venta, nombreArchivoNuevo, comparacion FROM ultimos WHERE rn = 1"
        );
        Map<String, CambioInfo> out = new HashMap<String, CambioInfo>();
        for (Map<String, Object> row : rows) {
            String idVenta = normalizeNumber(findValue(row, "id_venta", "idVenta"));
            if (idVenta == null) {
                continue;
            }
            out.put(idVenta, new CambioInfo(
                    toStringValue(findValue(row, "nombreArchivoNuevo")),
                    toStringValue(findValue(row, "comparacion"))
            ));
        }
        return out;
    }

    private String calcularComparacion(boolean tienePdf, String rutaPdf, CitaInfo cita) {
        if (!tienePdf) {
            return "SIN_PDF";
        }
        if (cita == null || cita.otFisica == null || cita.otFisica.trim().isEmpty()) {
            return "SIN_HISTORIAL";
        }
        return rutaPdf.toUpperCase().contains(cita.otFisica.trim().toUpperCase()) ? "IGUAL" : "DIFERENTE";
    }

    private String buildArchivoUrl(String rutaPdf, boolean download) {
        return "/boleta-digital/archivo?ruta=" + urlEncode(rutaPdf) + "&download=" + download;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String buildKey(String cliente, String ot) {
        if (cliente == null || ot == null) {
            return "";
        }
        return cliente + "|" + ot;
    }

    private String normalizeNumber(Object value) {
        String raw = toStringValue(value);
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            trimmed = trimmed.substring(0, dot);
        }
        while (trimmed.length() > 1 && trimmed.startsWith("0")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class CitaInfo {
        private final String otFisica;
        private final String estado;
        private final int estadoRank;
        private final long fechaRank;
        private final long idRank;

        private CitaInfo(String otFisica, String estado, int estadoRank, long fechaRank, long idRank) {
            this.otFisica = otFisica;
            this.estado = estado;
            this.estadoRank = estadoRank;
            this.fechaRank = fechaRank;
            this.idRank = idRank;
        }
    }

    private static final class CambioInfo {
        private final String nombreArchivoNuevo;
        private final String comparacion;

        private CambioInfo(String nombreArchivoNuevo, String comparacion) {
            this.nombreArchivoNuevo = nombreArchivoNuevo;
            this.comparacion = comparacion;
        }
    }
}
