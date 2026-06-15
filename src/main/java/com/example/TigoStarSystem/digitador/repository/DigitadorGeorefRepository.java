package com.example.TigoStarSystem.digitador.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class DigitadorGeorefRepository {
    private final JdbcTemplate centralJdbcTemplate;

    public DigitadorGeorefRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> listarAnalisisDistancias(LocalDate fecha) {
        return centralJdbcTemplate.queryForList(
                "EXEC dbo.spy_AnalisisDistancias_GeoReferencias ?",
                Date.valueOf(fecha)
        );
    }

    public int confirmarAnalisisDistancia(Long id) {
        return centralJdbcTemplate.update(
                "UPDATE dbo.tbl_BO_CITA_MAKIRO_Historial " +
                        "SET Actualizado = 1 " +
                        "WHERE Id_BO_CITA_MAKIRO_Historial = ?",
                id
        );
    }
}
