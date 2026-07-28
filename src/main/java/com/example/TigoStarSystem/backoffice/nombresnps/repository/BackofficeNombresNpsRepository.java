package com.example.TigoStarSystem.backoffice.nombresnps.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BackofficeNombresNpsRepository {

    public List<Map<String, Object>> listarVendedores(JdbcTemplate template) {
        return template.queryForList(
                "SELECT " +
                        "CAST(v.Id_Vendedor AS INT) AS idVendedor, " +
                        "CAST(v.Nombre AS NVARCHAR(200)) AS nombre, " +
                        "CAST(v.NombreNPS AS NVARCHAR(200)) AS nombreNps " +
                        "FROM dbo.tbl_Vendedor v " +
                        "WHERE v.E_Eliminado = 0 " +
                        "AND v.Id_Vendedor > 0 " +
                        "ORDER BY v.Nombre"
        );
    }

    public int actualizarNombreNps(JdbcTemplate template, Integer idVendedor, String nombreNps) {
        return template.update(
                "UPDATE dbo.tbl_Vendedor " +
                        "SET NombreNPS = ? " +
                        "WHERE Id_Vendedor = ? AND E_Eliminado = 0",
                nombreNps,
                idVendedor
        );
    }

    public List<Map<String, Object>> listarNombresNpsCentral(JdbcTemplate centralTemplate, Integer idSucursal) {
        return centralTemplate.queryForList(
                "DECLARE @SucursalNombre NVARCHAR(120) = CASE ? " +
                        "WHEN 9 THEN 'SANTA CRUZ' " +
                        "WHEN 7 THEN 'TARIJA' " +
                        "WHEN 19 THEN 'MONTERO' " +
                        "ELSE NULL END; " +
                        "WITH nombres AS ( " +
                        "  SELECT DISTINCT LTRIM(RTRIM(r.tecnico_nombre)) AS nombre " +
                        "  FROM dbo.tbl_NPS_RESPUESTAS_MAKIRO r " +
                        "  CROSS APPLY (SELECT " +
                        "      UPPER(LTRIM(RTRIM(ISNULL(r.ciudad, '')))) AS ciudad_norm, " +
                        "      UPPER(LTRIM(RTRIM(ISNULL(r.ciudad_siga, '')))) AS ciudad_siga_norm, " +
                        "      UPPER(LTRIM(RTRIM(ISNULL(r.departamento_siga, '')))) AS departamento_siga_norm " +
                        "  ) n " +
                        "  WHERE ISNULL(LTRIM(RTRIM(r.tecnico_nombre)), '') <> '' " +
                        "    AND (@SucursalNombre IS NULL " +
                        "      OR n.ciudad_norm LIKE '%' + @SucursalNombre + '%' " +
                        "      OR n.ciudad_siga_norm LIKE '%' + @SucursalNombre + '%' " +
                        "      OR n.departamento_siga_norm LIKE '%' + @SucursalNombre + '%') " +
                        ") " +
                        "SELECT nombre FROM nombres ORDER BY nombre",
                idSucursal
        );
    }
}
