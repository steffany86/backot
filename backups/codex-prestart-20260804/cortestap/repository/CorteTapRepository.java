package com.example.TigoStarSystem.cortestap.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class CorteTapRepository {
    private final JdbcTemplate jdbcTemplate;

    public CorteTapRepository(@Qualifier("centralJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listar() {
        return jdbcTemplate.queryForList(
                "SELECT id, CodigoCliente_OT1, TOR_OT1, Id_Tecnico_OT1, Tecnico1_OT1, Sucursal_OT1, Estado, " +
                        "Usuario_OT1, FechaReg_OT1, FechaFinalizacion_OT1, " +
                        "DATEDIFF(DAY, FechaFinalizacion_OT1, GETDATE()) + 1 AS ContadorDias, " +
                        "NodoTapBoca_D2, Zona_HFC_D2, Zona_D2, Distrito_D2, UsuarioDig_D2, FechaRegDig_D2, " +
                        "OrdenTrabajo_T3, Observacion_T3, NULL AS Foto1_T3, NULL AS Foto2_T3, FechaEjecucion_T3, " +
                        "UsuarioTec_T3, FechaRegTec_T3, E_Eliminado " +
                        "FROM dbo.tbl_CorteTap " +
                        "WHERE ISNULL(E_Eliminado, 0) = 0 " +
                        "ORDER BY id DESC"
        );
    }

    public List<Map<String, Object>> obtenerPorId(Integer id) {
        return jdbcTemplate.queryForList(
                "SELECT id, CodigoCliente_OT1, TOR_OT1, Id_Tecnico_OT1, Tecnico1_OT1, Sucursal_OT1, Estado, " +
                        "Usuario_OT1, FechaReg_OT1, FechaFinalizacion_OT1, " +
                        "DATEDIFF(DAY, FechaFinalizacion_OT1, GETDATE()) + 1 AS ContadorDias, " +
                        "NodoTapBoca_D2, Zona_HFC_D2, Zona_D2, Distrito_D2, UsuarioDig_D2, FechaRegDig_D2, " +
                        "OrdenTrabajo_T3, Observacion_T3, Foto1_T3, Foto2_T3, FechaEjecucion_T3, " +
                        "UsuarioTec_T3, FechaRegTec_T3, E_Eliminado " +
                        "FROM dbo.tbl_CorteTap " +
                        "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0",
                id
        );
    }

    public Map<String, Object> crear(
            String codigoCliente,
            String tor,
            Integer idTecnico,
            String tecnico,
            String sucursal,
            String nodoTapBoca,
            String usuario,
            Timestamp fechaRegistro) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO dbo.tbl_CorteTap (" +
                        "CodigoCliente_OT1, TOR_OT1, Id_Tecnico_OT1, Tecnico1_OT1, Sucursal_OT1, Estado, " +
                        "Usuario_OT1, FechaReg_OT1, FechaFinalizacion_OT1, ContadorDias, NodoTapBoca_D2, E_Eliminado" +
                        ") OUTPUT INSERTED.* VALUES (?, ?, ?, ?, ?, 'PENDIENTE', ?, ?, ?, 1, ?, 0)",
                codigoCliente,
                tor,
                idTecnico,
                tecnico,
                sucursal,
                usuario,
                fechaRegistro,
                fechaRegistro,
                nodoTapBoca
        );
    }

    public List<Map<String, Object>> listarZonas() {
        return jdbcTemplate.queryForList(
                "SELECT Id AS NroTrans, Nodos_Asociados, Distrito, Zona " +
                        "FROM dbo.tbl_Nodo_Distrito " +
                        "WHERE ISNULL(E_Eliminado, 0) = 0 " +
                        "ORDER BY Nodos_Asociados"
        );
    }

    public List<Map<String, Object>> listarDistritos() {
        return jdbcTemplate.queryForList(
                "SELECT Id AS NroTrans, Nodos_Asociados, Distrito, Zona, DistritoNuevo " +
                        "FROM dbo.tbl_Nodo_Distrito " +
                        "WHERE ISNULL(E_Eliminado, 0) = 0 " +
                        "ORDER BY Nodos_Asociados"
        );
    }

    public List<Map<String, Object>> resolverZonaHfc(String raw, String withNodo, String compactRaw, String compactWithNodo) {
        return jdbcTemplate.queryForList(
                "SELECT TOP 1 Nodos_Asociados AS nodo, Zona AS zona, DistritoNuevo AS distrito, Distrito AS distritoBase " +
                        "FROM dbo.tbl_Nodo_Distrito " +
                        "WHERE ISNULL(E_Eliminado, 0) = 0 AND (" +
                        "UPPER(LTRIM(RTRIM(Nodos_Asociados))) = ? OR " +
                        "UPPER(LTRIM(RTRIM(Nodos_Asociados))) = ? OR " +
                        "REPLACE(UPPER(LTRIM(RTRIM(Nodos_Asociados))), ' ', '') = ? OR " +
                        "REPLACE(UPPER(LTRIM(RTRIM(Nodos_Asociados))), ' ', '') = ?" +
                        ") ORDER BY id DESC",
                raw,
                withNodo,
                compactRaw,
                compactWithNodo
        );
    }

    public int guardarDigitacion(Integer id, String zonaHfc, String zona, String distrito, String usuario) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap " +
                        "SET Zona_HFC_D2 = ?, Zona_D2 = ?, Distrito_D2 = ?, UsuarioDig_D2 = ?, FechaRegDig_D2 = GETDATE() " +
                        "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 AND UPPER(ISNULL(Estado, '')) <> 'FINALIZADO'",
                zonaHfc,
                zona,
                distrito,
                usuario,
                id
        );
    }

    public int guardarEjecucion(
            Integer id,
            String ordenTrabajo,
            String observacion,
            String foto1,
            String foto2,
            Timestamp fechaEjecucion,
            String usuario) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap " +
                        "SET Estado = 'EJECUTADA', OrdenTrabajo_T3 = ?, Observacion_T3 = ?, Foto1_T3 = ?, Foto2_T3 = ?, " +
                        "FechaEjecucion_T3 = ?, UsuarioTec_T3 = ?, FechaRegTec_T3 = GETDATE() " +
                        "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 AND FechaRegDig_D2 IS NOT NULL " +
                        "AND UPPER(ISNULL(Estado, '')) <> 'FINALIZADO'",
                ordenTrabajo,
                observacion,
                foto1,
                foto2,
                fechaEjecucion,
                usuario,
                id
        );
    }

    public int finalizar(Integer id, String usuario) {
        return jdbcTemplate.update(
                "UPDATE dbo.tbl_CorteTap " +
                        "SET Estado = 'FINALIZADO', FechaFinalizacion_OT1 = ISNULL(FechaFinalizacion_OT1, GETDATE()) " +
                        "WHERE id = ? AND ISNULL(E_Eliminado, 0) = 0 AND UPPER(ISNULL(Estado, '')) = 'EJECUTADA'",
                id
        );
    }
}
