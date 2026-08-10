package com.example.TigoStarSystem.backoffice.nodozona.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BackofficeNodoZonaRepository {
    private final JdbcTemplate jdbcTemplate;

    public BackofficeNodoZonaRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listar() {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_ListadoNodoZona"
        );
    }

    public List<Map<String, Object>> listarDistrito() {
        return jdbcTemplate.queryForList("EXEC dbo.spx_ListadoNodoDistrito");
    }

    public List<Map<String, Object>> listarEstadoCorteTap() {
        return jdbcTemplate.queryForList("EXEC dbo.spx_ListadoEstadoCorteTap");
    }

    public List<Map<String, Object>> crear(String nodosAsociados, String distrito, String zona, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_CrearNodoZona ?, ?, ?, ?",
                nodosAsociados,
                distrito,
                zona,
                usuario
        );
    }

    public List<Map<String, Object>> eliminar(Integer id, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_EliminarNodoZona ?, ?",
                id,
                usuario
        );
    }

    public List<Map<String, Object>> crearDistrito(String nodosAsociados, String distrito, String zona, String distritoNuevo, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_CrearNodoDistrito ?, ?, ?, ?, ?",
                nodosAsociados,
                distrito,
                zona,
                distritoNuevo,
                usuario
        );
    }

    public List<Map<String, Object>> eliminarDistrito(Integer id, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_EliminarNodoDistrito ?, ?",
                id,
                usuario
        );
    }

    public List<Map<String, Object>> crearEstadoCorteTap(String estado, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_CrearEstadoCorteTap ?, ?",
                estado,
                usuario
        );
    }

    public List<Map<String, Object>> eliminarEstadoCorteTap(Integer id, String usuario) {
        return jdbcTemplate.queryForList(
                "EXEC dbo.spx_EliminarEstadoCorteTap ?, ?",
                id,
                usuario
        );
    }
}
