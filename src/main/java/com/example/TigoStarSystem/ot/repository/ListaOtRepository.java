package com.example.TigoStarSystem.ot.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class ListaOtRepository {
    private final JdbcTemplate centralJdbcTemplate;

    public ListaOtRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> listarPorFecha(LocalDate fecha) {
        return centralJdbcTemplate.queryForList(
                "EXEC dbo.spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO ?",
                Date.valueOf(fecha)
        );
    }
}

