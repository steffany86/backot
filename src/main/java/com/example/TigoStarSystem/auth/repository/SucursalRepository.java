package com.example.TigoStarSystem.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SucursalRepository {
    private final JdbcTemplate jdbcTemplate;

    public SucursalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> obtenerSucursales() {
        return jdbcTemplate.queryForList("EXEC dbo.spx_ObtenerSucursalesConexion");
    }
}
