package com.example.TigoStarSystem.tor.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TorRepository {
    private final JdbcTemplate centralJdbcTemplate;

    public TorRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate centralJdbcTemplate) {
        this.centralJdbcTemplate = centralJdbcTemplate;
    }

    public int contarDetalleExistente(String detalle) {
        List<Map<String, Object>> rows = centralJdbcTemplate.queryForList(
                "EXEC BDControlOrdenes.dbo.spx_sepuederegistraTor ?",
                detalle
        );
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<String, Object> first = rows.get(0);
        if (first == null || first.isEmpty()) {
            return 0;
        }
        Object value = first.values().iterator().next();
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public List<Map<String, Object>> listarRegistrados() {
        return centralJdbcTemplate.queryForList(
                "EXEC BDControlOrdenes.dbo.spx_ObtenerTORRegitrados"
        );
    }

    public Integer insertar(String detalle, String tor, String tipoServicio, String usuarioRegistra) {
        return centralJdbcTemplate.queryForObject(
                "INSERT INTO BDControlOrdenes.dbo.tbl_TOR_SF (Detalle, TOR, TIPO_SERVICIO, usuarioRegistra) " +
                        "VALUES (?, ?, ?, ?); SELECT CAST(SCOPE_IDENTITY() AS int);",
                Integer.class,
                detalle,
                tor,
                tipoServicio,
                usuarioRegistra
        );
    }
}
