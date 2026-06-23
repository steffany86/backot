package com.example.TigoStarSystem.boletadigital.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BoletaDigitalRepository {
    public List<Map<String, Object>> listarOtArchivo(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("EXEC dbo.spx_EnlistarOtArchivo_web");
    }
}
