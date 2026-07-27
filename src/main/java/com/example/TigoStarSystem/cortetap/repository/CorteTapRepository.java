package com.example.TigoStarSystem.cortetap.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class CorteTapRepository {
    private final JdbcTemplate jdbcTemplate;

    public CorteTapRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listar() {
        return jdbcTemplate.queryForList("EXEC dbo.spx_ListaCorteTAP");
    }

    public Map<String, Object> buscarPorId(Integer id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM dbo.tbl_CorteTap WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0",
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> buscarNodoZona(String nodo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 Nodos_Asociados, Distrito, Zona "
                        + "FROM dbo.tbl_Nodo_Zona "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 AND UPPER(LTRIM(RTRIM(Nodos_Asociados))) = UPPER(?) "
                        + "ORDER BY id",
                nodo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> buscarNodoDistrito(String nodo, String zona) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT TOP 1 Nodos_Asociados, Distrito, Zona, DistritoNuevo "
                        + "FROM dbo.tbl_Nodo_Distrito "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 "
                        + "AND UPPER(LTRIM(RTRIM(Nodos_Asociados))) = UPPER(?) "
                        + "AND UPPER(LTRIM(RTRIM(Zona))) = UPPER(?) "
                        + "ORDER BY id",
                nodo,
                zona
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> listarZonasDigitacion() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT LTRIM(RTRIM(Zona)) AS Zona "
                        + "FROM dbo.tbl_Nodo_Zona "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 AND NULLIF(LTRIM(RTRIM(Zona)), '') IS NOT NULL "
                        + "ORDER BY Zona"
        );
    }

    public List<Map<String, Object>> listarDistritosDigitacion() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT LTRIM(RTRIM(Zona)) AS Zona, LTRIM(RTRIM(DistritoNuevo)) AS Distrito "
                        + "FROM dbo.tbl_Nodo_Distrito "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 "
                        + "AND NULLIF(LTRIM(RTRIM(Zona)), '') IS NOT NULL "
                        + "AND NULLIF(LTRIM(RTRIM(DistritoNuevo)), '') IS NOT NULL "
                        + "ORDER BY Zona, Distrito"
        );
    }

    public boolean existeZonaDigitacion(String zona) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dbo.tbl_Nodo_Zona "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 AND UPPER(LTRIM(RTRIM(Zona))) = UPPER(?)",
                Integer.class,
                zona
        );
        return count != null && count > 0;
    }

    public boolean existeDistritoDigitacion(String zona, String distrito) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM dbo.tbl_Nodo_Distrito "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 "
                        + "AND UPPER(LTRIM(RTRIM(Zona))) = UPPER(?) "
                        + "AND UPPER(LTRIM(RTRIM(DistritoNuevo))) = UPPER(?)",
                Integer.class,
                zona,
                distrito
        );
        return count != null && count > 0;
    }

    public List<Map<String, Object>> listarNodos() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT Nodos_Asociados, Distrito, Zona FROM dbo.tbl_Nodo_Zona "
                        + "WHERE ISNULL(E_Eliminado, 0) = 0 ORDER BY Nodos_Asociados"
        );
        return rows == null ? Collections.<Map<String, Object>>emptyList() : rows;
    }

    public int actualizarDigitacion(
            Integer id,
            String zonaHfc,
            String zona,
            String distrito,
            String usuario) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap SET Zona_HFC_D2 = ?, Zona_D2 = ?, "
                        + "Distrito_D2 = ?, UsuarioDig_D2 = ?, FechaRegDig_D2 = GETDATE() "
                        + "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 "
                        + "AND UPPER(ISNULL(Estado, '')) NOT IN ('EJECUTADA', 'FINALIZADO')",
                zonaHfc,
                zona,
                distrito,
                usuario,
                id
        );
    }

    public int actualizarEjecucion(
            Integer id,
            String ordenTrabajo,
            String observacion,
            String foto1,
            String foto2,
            Timestamp fechaEjecucion,
            String usuario) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap SET OrdenTrabajo_T3 = ?, Observacion_T3 = ?, Foto1_T3 = ?, Foto2_T3 = ?, "
                        + "FechaEjecucion_T3 = ?, UsuarioTec_T3 = ?, FechaRegTec_T3 = GETDATE(), "
                        + "Estado = 'EJECUTADA', FechaFinalizacion_OT1 = GETDATE() "
                        + "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 AND FechaRegDig_D2 IS NOT NULL "
                        + "AND UPPER(ISNULL(Estado, '')) NOT IN ('EJECUTADA', 'FINALIZADO')",
                ordenTrabajo,
                observacion,
                foto1,
                foto2,
                fechaEjecucion,
                usuario,
                id
        );
    }

    public int finalizar(Integer id) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap SET Estado = 'FINALIZADO' "
                        + "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 "
                        + "AND FechaRegTec_T3 IS NOT NULL AND UPPER(ISNULL(Estado, '')) = 'EJECUTADA'",
                id
        );
    }

    public void insertar(
            String codigoCliente,
            String tor,
            Integer idTecnico,
            String tecnico,
            String sucursal,
            String usuario,
            String nodoTapBoca) {
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                "EXEC dbo.spx_InsertarCorteTap ?, ?, ?, ?, ?, ?, ?, ?",
                codigoCliente,
                tor,
                idTecnico,
                tecnico,
                sucursal,
                usuario,
                ahora,
                ahora
        );
        int updated = jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap SET NodoTapBoca_D2 = ? "
                        + "WHERE id = (SELECT TOP 1 id FROM dbo.tbl_CorteTap "
                        + "WHERE CodigoCliente_OT1 = ? AND TOR_OT1 = ? AND Id_Tecnico_OT1 = ? "
                        + "AND Usuario_OT1 = ? AND NodoTapBoca_D2 IS NULL "
                        + "ORDER BY id DESC)",
                nodoTapBoca,
                codigoCliente,
                tor,
                idTecnico,
                usuario
        );
        if (updated != 1) {
            throw new IllegalStateException("No se pudo asociar Nodo/TAP/Boca al Corte TAP creado.");
        }
    }
}
