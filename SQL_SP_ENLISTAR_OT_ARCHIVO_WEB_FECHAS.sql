SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

ALTER PROCEDURE dbo.spx_EnlistarOtArchivo_web
    @FechaInicio DATE = NULL,
    @FechaFin DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        v.id_venta,
        r.Nombre AS Cuadrilla,
        v.Fecha_Ejecucion,
        v.OrdenTrabajo,
        v.CodigoCliente,
        v.origen,
        v.RutaPdf
    FROM dbo.tbl_venta v
    INNER JOIN dbo.tbl_ruta r
        ON r.id_ruta = v.id_ruta
    WHERE ISNULL(v.e_eliminado, 0) = 0
      AND dbo.dateonly(v.fecha_ejecucion) >= dbo.dateonly('01/01/2026')
      AND (@FechaInicio IS NULL OR dbo.dateonly(v.Fecha_Ejecucion) >= @FechaInicio)
      AND (@FechaFin IS NULL OR dbo.dateonly(v.Fecha_Ejecucion) <= @FechaFin)
    ORDER BY v.id_venta DESC;
END;
GO
