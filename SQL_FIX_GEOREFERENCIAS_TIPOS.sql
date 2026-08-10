USE [BDControlOrdenes];
GO

SET ANSI_NULLS ON;
GO

SET QUOTED_IDENTIFIER ON;
GO

ALTER PROCEDURE dbo.spy_AnalisisDistancias_GeoReferencias
    @FechaConsulta DATE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH Sucursales AS (
        SELECT
            CONVERT(NVARCHAR(100), CodigoCliente) AS CodigoCliente,
            CONVERT(NVARCHAR(100), OrdenTrabajo) AS OrdenTrabajo,
            COALESCE(NULLIF(latitud_venta, 0), Latitud) AS Latitud,
            COALESCE(NULLIF(longitud_venta, 0), Longitud) AS Longitud,
            CONVERT(NVARCHAR(200), Nodo_Ramal_tap) AS Nodo_Ramal_tap
        FROM [tigo.makiro.com.bo].BDSistemaAntenaPM.dbo.tbl_Venta
        WHERE dbo.dateonly(Fecha_Ejecucion) = dbo.dateonly(@FechaConsulta)
          AND e_Eliminado = 0

        UNION ALL

        SELECT
            CONVERT(NVARCHAR(100), CodigoCliente),
            CONVERT(NVARCHAR(100), OrdenTrabajo),
            COALESCE(NULLIF(latitud_venta, 0), Latitud),
            COALESCE(NULLIF(longitud_venta, 0), Longitud),
            CONVERT(NVARCHAR(200), Nodo_Ramal_tap)
        FROM BDSistemaAntenaPMTarija.dbo.tbl_Venta
        WHERE dbo.dateonly(Fecha_Ejecucion) = dbo.dateonly(@FechaConsulta)
          AND e_Eliminado = 0

        UNION ALL

        SELECT
            CONVERT(NVARCHAR(100), CodigoCliente),
            CONVERT(NVARCHAR(100), OrdenTrabajo),
            COALESCE(NULLIF(latitud_venta, 0), Latitud),
            COALESCE(NULLIF(longitud_venta, 0), Longitud),
            CONVERT(NVARCHAR(200), Nodo_Ramal_tap)
        FROM [172.16.0.14].BDSistemaAntenaPMMontero.dbo.tbl_Venta
        WHERE dbo.dateonly(Fecha_Ejecucion) = dbo.dateonly(@FechaConsulta)
          AND e_Eliminado = 0
    )
    UPDATE c
    SET c.Latitud = CASE WHEN c.Actualizado = 2 THEN v.Latitud ELSE c.Latitud END,
        c.Longitud = CASE WHEN c.Actualizado = 2 THEN v.Longitud ELSE c.Longitud END,
        c.N_T_B_V = CASE WHEN c.Actualizado_NODO = 2 THEN v.Nodo_Ramal_tap ELSE c.N_T_B_V END
    FROM dbo.tbl_BO_CITA_MAKIRO_Historial c
    INNER JOIN Sucursales v
        ON LTRIM(RTRIM(v.CodigoCliente)) = LTRIM(RTRIM(CONVERT(NVARCHAR(100), c.cliente_nro)))
       AND LTRIM(RTRIM(v.OrdenTrabajo)) = LTRIM(RTRIM(CONVERT(NVARCHAR(100), c.OT)))
    WHERE c.Vigente = 2
      AND c.Estado = 'Finalizado'
      AND dbo.dateonly(c.Fecha_Registro) = dbo.dateonly(@FechaConsulta)
      AND (c.Actualizado = 2 OR c.Actualizado_NODO = 2);

    UPDATE c
    SET c.DistanciaMetros = hav.DistanciaMetros
    FROM dbo.tbl_BO_CITA_MAKIRO_Historial c
    CROSS APPLY (
        SELECT
            CASE WHEN ISNUMERIC(c.GeoSur) = 1 THEN CAST(c.GeoSur AS FLOAT) ELSE NULL END AS LatC,
            CASE WHEN ISNUMERIC(c.GeoOeste) = 1 THEN CAST(c.GeoOeste AS FLOAT) ELSE NULL END AS LonC,
            CASE WHEN ISNUMERIC(c.Latitud) = 1 THEN CAST(c.Latitud AS FLOAT) ELSE NULL END AS LatV,
            CASE WHEN ISNUMERIC(c.Longitud) = 1 THEN CAST(c.Longitud AS FLOAT) ELSE NULL END AS LonV
    ) conv
    CROSS APPLY (
        SELECT CASE
            WHEN conv.LatV IS NULL OR conv.LonV IS NULL OR conv.LatC IS NULL OR conv.LonC IS NULL THEN NULL
            ELSE 6371000 * 2 * ATN2(
                SQRT(
                    POWER(SIN(RADIANS(conv.LatV - conv.LatC) / 2), 2)
                    + COS(RADIANS(conv.LatC)) * COS(RADIANS(conv.LatV))
                    * POWER(SIN(RADIANS(conv.LonV - conv.LonC) / 2), 2)
                ),
                SQRT(1 - (
                    POWER(SIN(RADIANS(conv.LatV - conv.LatC) / 2), 2)
                    + COS(RADIANS(conv.LatC)) * COS(RADIANS(conv.LatV))
                    * POWER(SIN(RADIANS(conv.LonV - conv.LonC) / 2), 2)
                ))
            )
        END AS DistanciaMetros
    ) hav
    WHERE c.Vigente = 2
      AND c.Estado = 'Finalizado'
      AND dbo.dateonly(c.Fecha_Registro) = dbo.dateonly(@FechaConsulta);

    UPDATE c
    SET Actualizado_NODO = 1
    FROM dbo.tbl_BO_CITA_MAKIRO_Historial c
    WHERE N_T_B_V = N_T_B
      AND Vigente = 2
      AND Estado = 'Finalizado'
      AND dbo.dateonly(Fecha_Registro) = dbo.dateonly(@FechaConsulta)
      AND Actualizado_NODO = 2;

    SELECT
        Id_BO_CITA_MAKIRO_Historial,
        cliente_nro,
        wo_external_id,
        inicio_agendado,
        estado,
        fecha_carga AS ultima_fecha_hora_dia,
        GeoSur AS Latitud_C,
        GeoOeste AS Longitud_C,
        TOR,
        OT,
        Latitud AS Latitud_V,
        Longitud AS Longitud_V,
        TECNICO,
        Grupo,
        N_T_B,
        tecnico_nombre,
        SISTEMA,
        Digitador,
        SUPERVISOR_CARGO,
        Actualizado,
        Fecha_Registro,
        ROUND(DistanciaMetros, 2) AS DistanciaMetros,
        usuario_ModificaDistancia,
        fechaRegistro_ModificaDistancia,
        N_T_B_V,
        Actualizado_NODO,
        usuarioModifica_NODO,
        fechaRegistroModifica_NODO
    FROM dbo.tbl_BO_CITA_MAKIRO_Historial
    WHERE Vigente = 2
      AND Estado = 'Finalizado'
      AND dbo.dateonly(Fecha_Registro) = dbo.dateonly(@FechaConsulta)
    ORDER BY DistanciaMetros DESC, cliente_nro, wo_external_id;
END;
GO
