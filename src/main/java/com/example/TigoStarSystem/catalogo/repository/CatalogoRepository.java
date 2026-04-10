package com.example.TigoStarSystem.catalogo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CatalogoRepository {
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

    public List<Map<String, Object>> listarProductosSinFungibleWeb() {
        return jdbcTemplate.queryForList("EXEC TraerTodosLosProductos_SinFungibleWeb");
    }

    public List<Map<String, Object>> listarProductosPorRuta(Integer idRuta) {
        return jdbcTemplate.queryForList("EXEC TraerTodosLosProductos_x_IdRutaWeb ?", idRuta);
    }

    public List<Map<String, Object>> listarProductosCargoUsuarioWeb() {
        return jdbcTemplate.queryForList("EXEC spx_ObtenerProductosPCargoUsuario");
    }

    public List<Map<String, Object>> buscarSerialCargoUsuario(String serial, String chipId, Integer tipoCodigo) {
        return jdbcTemplate.queryForList(
                "EXEC spx_BuscarSerialCargoUsuario ?, ?, ?",
                serial,
                chipId,
                tipoCodigo
        );
    }

    public List<Map<String, Object>> traerChipIdPorSerie(String serie) {
        return jdbcTemplate.queryForList("EXEC spx_TraerChipID2 ?", serie);
    }

    public Map<String, Object> validarSerieChipUnico(String serie, String chipId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 serial, chipid, id_producto, e_eliminado " +
                        "FROM dbo.tbl_productos " +
                        "WHERE (serial = ? OR chipid = ?) AND e_eliminado = 0",
                serie,
                chipId
        );
        if (rows.isEmpty()) {
            return buildSerieChipResult(false, false, false, serie, chipId);
        }

        Map<String, Object> row = rows.get(0);
        String serieDb = normalizeText(asString(row.get("serial")));
        String chipDb = normalizeText(asString(row.get("chipid")));
        String serieInput = normalizeText(serie);
        String chipInput = normalizeText(chipId);
        boolean serieExiste = !serieDb.isEmpty() && serieDb.equals(serieInput);
        boolean chipExiste = !chipDb.isEmpty() && chipDb.equals(chipInput);
        boolean mismoRegistro = serieExiste && chipExiste;
        return buildSerieChipResult(serieExiste, chipExiste, mismoRegistro, serie, chipId);
    }

    public List<Map<String, Object>> validarSerieSaldo(String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta) {
        return jdbcTemplate.queryForList(
                "EXEC spx_TraerDatoSerieChipIdCU_OT ?, ?, ?, ?",
                serie,
                idProducto,
                tipoMaterial,
                idRuta
        );
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCU(String serie) {
        return jdbcTemplate.queryForList("EXEC spx_TraerDatoSerieChipIdCU ?", serie);
    }

    public List<Map<String, Object>> listarProductosMascara() {
        return jdbcTemplate.queryForList("EXEC sp_TraerTodosLosProductosMascara");
    }

    public List<Map<String, Object>> listarKitsDecodificadores() {
        return jdbcTemplate.queryForList("EXEC sp_ObtenerKitDecodificadores");
    }

    private Map<String, Object> buildSerieChipResult(
            boolean serieExiste,
            boolean chipExiste,
            boolean mismoRegistro,
            String serie,
            String chipId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("serie", serie);
        result.put("chipId", chipId);
        result.put("serieExiste", serieExiste);
        result.put("chipExiste", chipExiste);
        result.put("mismoRegistro", mismoRegistro);
        result.put("sePuede", mismoRegistro);
        if (mismoRegistro) {
            result.put("observacion", "Serie y ChipID coinciden correctamente.");
        } else if (!serieExiste && chipExiste) {
            result.put("observacion", "La serie no existe, pero el ChipID ya esta registrado.");
        } else if (serieExiste && !chipExiste) {
            result.put("observacion", "La serie ya existe, pero el ChipID no coincide o no existe.");
        } else if (serieExiste) {
            result.put("observacion", "La serie y el ChipID existen, pero no corresponden al mismo registro.");
        } else {
            result.put("observacion", "La serie y el ChipID no existen en saldo.");
        }
        return result;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
