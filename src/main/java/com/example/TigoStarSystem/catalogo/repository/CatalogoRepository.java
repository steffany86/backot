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

    public List<Map<String, Object>> listarProductosMascara() {
        return jdbcTemplate.queryForList("EXEC sp_TraerTodosLosProductosMascara");
    }

    public List<Map<String, Object>> listarKitsDecodificadores() {
        return jdbcTemplate.queryForList("EXEC sp_ObtenerKitDecodificadores");
    }
}
