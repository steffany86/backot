package com.example.TigoStarSystem.catalogo.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class CatalogoRepository {
    private static final Logger logger = LoggerFactory.getLogger(CatalogoRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public CatalogoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listarTecnicos() {
        return jdbcTemplate.queryForList("EXEC spx_ObtenerTecnicosEnRuta");
    }

    public List<Map<String, Object>> listarRutasPorTecnico(Integer idTecnico) {
        return jdbcTemplate.queryForList("EXEC spx_ObtenerRutaXIdTecnico ?", idTecnico);
    }

    public List<Map<String, Object>> listarTodasRutas() {
        return jdbcTemplate.queryForList(
                "SELECT Id_Ruta, Nombre, Id_Vendedor, E_Eliminado FROM dbo.tbl_Ruta WHERE E_Eliminado = 0");
    }

    public List<Map<String, Object>> listarTiposServicio() {
        return jdbcTemplate.queryForList("EXEC spx_ObtenerTipoServicio");
    }

    public List<Map<String, Object>> listarEstados() {
        return jdbcTemplate.queryForList("EXEC sp_ObtenerEstado");
    }

    public List<Map<String, Object>> listarTipoMaterial(Integer idTipoServicio) {
        return jdbcTemplate.queryForList("EXEC sp_ObtenerTipoMaterial ?", idTipoServicio);
    }

    public List<Map<String, Object>> listarProductos() {
        return jdbcTemplate.queryForList("EXEC TraerTodosLosProductos");
    }

    public List<Map<String, Object>> listarProductosMascara() {
        return jdbcTemplate.queryForList("EXEC sp_TraerTodosLosProductosMascara");
    }

    public List<Map<String, Object>> traerChipIdSpxTraerChipID2(String serie) {
        return jdbcTemplate.queryForList("EXEC spx_TraerChipID2 ?", serie);
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCuOt(
            String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta) {
        try {
            return jdbcTemplate.queryForList(
                    "EXEC spx_TraerDatoSerieChipIdCU_OT ?, ?, ?, ?",
                    serie, idProducto, tipoMaterial, idRuta);
        } catch (DataAccessException ex) {
            if (esErrorSinResultado(ex)) {
                logger.debug("spx_TraerDatoSerieChipIdCU_OT no retornó conjunto de resultados para serie={}, idProducto={}, tipoMaterial={}, idRuta={}",
                        serie, idProducto, tipoMaterial, idRuta);
                return Collections.emptyList();
            }
            throw ex;
        }
    }

    public Map<String, Object> validarSerieChipIdUnicos(String serie, String chipId) {
        String serieClean = serie == null ? "" : serie.trim();
        String chipClean = chipId == null ? "" : chipId.trim();

        Map<String, Object> serieRow = buscarPorSerie(serieClean);
        Map<String, Object> chipRow = buscarPorChipId(chipClean);

        boolean serieExiste = serieRow != null;
        boolean chipExiste = chipRow != null;
        boolean mismoRegistro = serieExiste
                && chipExiste
                && mismosValores(serieRow, chipRow);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serie", serieClean);
        result.put("chipId", chipClean);
        result.put("serieExiste", serieExiste);
        result.put("chipExiste", chipExiste);
        result.put("mismoRegistro", mismoRegistro);
        result.put("sePuede", mismoRegistro);
        result.put("observacion", construirObservacionUnicidad(serieExiste, chipExiste, mismoRegistro, serieClean, chipClean));
        return result;
    }

    private Map<String, Object> buscarPorSerie(String serie) {
        if (serie == null || serie.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 serial, chipid, id_producto, e_eliminado " +
                        "FROM dbo.tbl_productos " +
                        "WHERE serial = ? AND e_eliminado = 0",
                serie
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> buscarPorChipId(String chipId) {
        if (chipId == null || chipId.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 serial, chipid, id_producto, e_eliminado " +
                        "FROM dbo.tbl_productos " +
                        "WHERE chipid = ? AND e_eliminado = 0",
                chipId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean mismosValores(Map<String, Object> serieRow, Map<String, Object> chipRow) {
        if (serieRow == null || chipRow == null) {
            return false;
        }
        String serieFromSerie = normalizeText(asString(serieRow.get("serial")));
        String chipFromSerie = normalizeText(asString(serieRow.get("chipid")));
        String serieFromChip = normalizeText(asString(chipRow.get("serial")));
        String chipFromChip = normalizeText(asString(chipRow.get("chipid")));
        return serieFromSerie.equals(serieFromChip) && chipFromSerie.equals(chipFromChip);
    }

    private String construirObservacionUnicidad(
            boolean serieExiste,
            boolean chipExiste,
            boolean mismoRegistro,
            String serie,
            String chipId) {
        if (mismoRegistro) {
            return "Serie y ChipID coinciden correctamente.";
        }
        if (!serieExiste && chipExiste) {
            return "La serie no existe, pero el ChipID ya esta registrado.";
        }
        if (serieExiste && !chipExiste) {
            return "La serie ya existe, pero el ChipID no coincide o no existe.";
        }
        if (serieExiste && chipExiste) {
            return "La serie y el ChipID existen, pero no corresponden al mismo registro.";
        }
        return "La serie y el ChipID no existen en saldo.";
    }

    private boolean esErrorSinResultado(DataAccessException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("no devolvio un conjunto de resultados")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public List<Map<String, Object>> listarKitsDecodificadores() {
        return jdbcTemplate.queryForList("EXEC sp_ObtenerKitDecodificadores");
    }
}
