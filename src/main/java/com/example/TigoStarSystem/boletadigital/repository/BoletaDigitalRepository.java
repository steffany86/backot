package com.example.TigoStarSystem.boletadigital.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
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
                "SELECT TOP 1 cliente_nro, OT, OT_FISICA, Estado " +
                        "FROM dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "WHERE Vigente = 2 " +
                        "  AND OT_FISICA IS NOT NULL " +
                        "  AND cliente_nro = ? " +
                        "  AND OT = ?",
                String.valueOf(codigoCliente),
                String.valueOf(ordenTrabajo)
        );
        return rows == null || rows.isEmpty() ? null : rows.get(0);
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
                "SELECT cliente_nro, OT, OT_FISICA, Estado " +
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
            CitaInfo info = new CitaInfo(
                    toStringValue(findValue(row, "OT_FISICA")),
                    toStringValue(findValue(row, "Estado"))
            );
            out.put(buildKey(cliente, ot), info);
        }
        return out;
    }

    private Map<String, Object> normalizarRow(
            Map<String, Object> row,
            Map<String, CitaInfo> citas,
            Map<String, CambioInfo> cambios) {
        Map<String, Object> out = new LinkedHashMap<String, Object>(row);
        Object idVenta = findValue(row, "Id_Venta", "idVenta", "id_venta", "idventa");
        Object cuadrilla = findValue(row, "Cuadrilla", "cuadrilla");
        Object ordenTrabajo = findValue(row, "OrdenTrabajo", "ordentrabajo");
        Object codigoCliente = findValue(row, "CodigoCliente", "codigocliente");
        String rutaPdf = toStringValue(findValue(row, "RutaPdf", "rutapdf"));
        boolean tienePdf = rutaPdf != null && !rutaPdf.trim().isEmpty();

        out.put("Tecnico", cuadrilla);
        out.put("OT", ordenTrabajo);
        out.put("cliente", codigoCliente);
        out.put("Estado", tienePdf ? "CON_PDF" : "SIN_PDF");
        out.put("VerPdfUrl", tienePdf ? buildArchivoUrl(rutaPdf, false) : null);
        out.put("DescargarPdfUrl", tienePdf ? buildArchivoUrl(rutaPdf, true) : null);

        CitaInfo cita = citas.get(buildKey(normalizeNumber(codigoCliente), normalizeNumber(ordenTrabajo)));
        if (cita != null) {
            out.put("OT_FISICA", cita.otFisica);
            out.put("EstadoBO", cita.estado);
        }
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

        private CitaInfo(String otFisica, String estado) {
            this.otFisica = otFisica;
            this.estado = estado;
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
