package com.example.TigoStarSystem.digitador.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
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
        try {
            return centralJdbcTemplate.queryForList(
                    "WITH base AS ( " +
                            "SELECT h.*, " +
                            "CONVERT(float, h.Latitud_C) AS LatitudCNum, " +
                            "CONVERT(float, h.Longitud_C) AS LongitudCNum, " +
                            "CONVERT(float, h.Latitud_V) AS LatitudVNum, " +
                            "CONVERT(float, h.Longitud_V) AS LongitudVNum " +
                            "FROM dbo.tbl_BO_CITA_MAKIRO_Historial h " +
                            "WHERE CAST(h.fecha_hora_dia AS date) = ? " +
                            "), distancias AS ( " +
                            "SELECT base.*, " +
                            "CASE " +
                            "WHEN LatitudCNum BETWEEN -90 AND 90 " +
                            "AND LatitudVNum BETWEEN -90 AND 90 " +
                            "AND LongitudCNum BETWEEN -180 AND 180 " +
                            "AND LongitudVNum BETWEEN -180 AND 180 " +
                            "THEN geography::Point(LatitudCNum, LongitudCNum, 4326).STDistance(geography::Point(LatitudVNum, LongitudVNum, 4326)) " +
                            "ELSE NULL " +
                            "END AS DistanciaMetros " +
                            "FROM base " +
                            ") " +
                            "SELECT * FROM distancias " +
                            "WHERE ISNULL(estado, '') = 'Finalizado' " +
                            "AND DistanciaMetros >= 15 " +
                            "ORDER BY fecha_hora_dia DESC, OT",
                    Date.valueOf(fecha)
            );
        } catch (DataAccessException ex) {
            return centralJdbcTemplate.queryForList(
                    "EXEC dbo.spy_AnalisisDistancias_GeoReferencias ?",
                    Date.valueOf(fecha)
            );
        }
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
