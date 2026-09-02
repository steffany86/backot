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

    public DigitadorGeorefRepository(
            @Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public List<Map<String, Object>> listarAnalisisDistancias(LocalDate fecha) {
        return ejecutarSpAnalisisDistancias(centralJdbcTemplate, fecha);
    }

    private List<Map<String, Object>> ejecutarSpAnalisisDistancias(JdbcTemplate template, LocalDate fecha) {
        try {
            return template.queryForList(
                    "EXEC dbo.spy_AnalisisDistancias_GeoReferencias ?",
                    Date.valueOf(fecha)
            );
        } catch (DataAccessException ex) {
            return template.queryForList("EXEC dbo.spy_AnalisisDistancias_GeoReferencias");
        }
    }

    public int confirmarAnalisisDistancia(Long id, boolean confirmarUbicacion, boolean confirmarNodo, String usuarioModifica) {
        return actualizarAnalisisDistancia(centralJdbcTemplate, id, confirmarUbicacion, confirmarNodo, usuarioModifica);
    }

    private int actualizarAnalisisDistancia(
            JdbcTemplate template,
            Long id,
            boolean confirmarUbicacion,
            boolean confirmarNodo,
            String usuarioModifica) {
        StringBuilder sql = new StringBuilder("UPDATE dbo.tbl_BO_CITA_MAKIRO_Historial SET ");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (confirmarUbicacion) {
            sql.append("Actualizado = 1, usuario_ModificaDistancia = ?, fechaRegistro_ModificaDistancia = GETDATE()");
            args.add(usuarioModifica);
        }
        if (confirmarNodo) {
            if (!args.isEmpty()) {
                sql.append(", ");
            }
            sql.append("Actualizado_NODO = 1, usuarioModifica_NODO = ?, fechaRegistroModifica_NODO = GETDATE()");
            args.add(usuarioModifica);
        }
        sql.append(" WHERE Id_BO_CITA_MAKIRO_Historial = ?");
        args.add(id);
        try {
            return template.update(sql.toString(), args.toArray());
        } catch (DataAccessException ex) {
            return 0;
        }
    }
}
