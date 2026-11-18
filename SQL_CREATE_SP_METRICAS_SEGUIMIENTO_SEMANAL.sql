-- Servidor 172.16.0.3 - BD BDSeguimientoE
-- Volumen semanal por sucursal destino (solo EstadoFinalizado = 'Cerrado').
-- Semana inicia en lunes (independiente de SET DATEFIRST). Compatible con SQL 2008.
USE BDSeguimientoE
GO

IF OBJECT_ID('dbo.sp_ObtenerMetricasSeguimientoSemanal') IS NOT NULL
    DROP PROCEDURE dbo.sp_ObtenerMetricasSeguimientoSemanal
GO

CREATE PROCEDURE [dbo].[sp_ObtenerMetricasSeguimientoSemanal]
    @FechaDesde DATETIME = NULL,
    @FechaHasta DATETIME = NULL,   -- inclusive
    @Sucursal   VARCHAR(100) = NULL -- NULL = todas (filtra por sucursal destino)
AS
BEGIN
    SET NOCOUNT ON;

    IF @FechaDesde IS NULL SET @FechaDesde = DATEADD(WEEK, -12, GETDATE());
    IF @FechaHasta IS NULL SET @FechaHasta = GETDATE();

    ;WITH base AS (
        SELECT
            v.NombreSucursalDestino AS SucDestino,
            CASE WHEN ISNUMERIC(v.Total) = 1 THEN CONVERT(DECIMAL(18,2), v.Total) ELSE 0 END AS Total,
            CONVERT(DATE, v.FechaPC) AS Fecha
        FROM vw_TraerSeguimiento5 v
        WHERE LTRIM(RTRIM(v.EstadoFinalizado)) = 'Cerrado'
          AND ISDATE(v.FechaPC) = 1
          AND (@Sucursal IS NULL OR v.NombreSucursalDestino = @Sucursal)
    ),
    semanal AS (
        SELECT
            SucDestino,
            DATEADD(DAY, -(DATEDIFF(DAY, 0, Fecha) % 7), Fecha) AS SemanaInicio,
            Total
        FROM base
        WHERE Fecha >= CONVERT(DATE, @FechaDesde)
          AND Fecha <= CONVERT(DATE, @FechaHasta)
    )
    SELECT
        SucDestino,
        SemanaInicio,
        DATEADD(DAY, 6, SemanaInicio)  AS SemanaFin,
        COUNT(*)                       AS CantidadEnvios,
        SUM(Total)                     AS MontoTotal
    FROM semanal
    GROUP BY SucDestino, SemanaInicio
    ORDER BY SemanaInicio DESC, SucDestino;

    -- Promedio semanal por sucursal en el rango
    ;WITH base AS (
        SELECT
            v.NombreSucursalDestino AS SucDestino,
            CASE WHEN ISNUMERIC(v.Total) = 1 THEN CONVERT(DECIMAL(18,2), v.Total) ELSE 0 END AS Total,
            CONVERT(DATE, v.FechaPC) AS Fecha
        FROM vw_TraerSeguimiento5 v
        WHERE LTRIM(RTRIM(v.EstadoFinalizado)) = 'Cerrado'
          AND ISDATE(v.FechaPC) = 1
          AND (@Sucursal IS NULL OR v.NombreSucursalDestino = @Sucursal)
    ),
    semanal AS (
        SELECT
            SucDestino,
            DATEADD(DAY, -(DATEDIFF(DAY, 0, Fecha) % 7), Fecha) AS SemanaInicio,
            COUNT(*) AS Cant,
            SUM(Total) AS Monto
        FROM base
        WHERE Fecha >= CONVERT(DATE, @FechaDesde)
          AND Fecha <= CONVERT(DATE, @FechaHasta)
        GROUP BY SucDestino, DATEADD(DAY, -(DATEDIFF(DAY, 0, Fecha) % 7), Fecha)
    )
    SELECT
        SucDestino,
        COUNT(*)                          AS SemanasConMovimiento,
        SUM(Cant)                         AS TotalEnvios,
        CONVERT(DECIMAL(18,2), AVG(CONVERT(DECIMAL(18,2), Cant)))  AS PromEnviosSemana,
        MIN(Cant)                         AS MinEnviosSemana,
        MAX(Cant)                         AS MaxEnviosSemana,
        SUM(Monto)                        AS MontoTotal,
        CONVERT(DECIMAL(18,2), AVG(Monto)) AS PromMontoSemana
    FROM semanal
    GROUP BY SucDestino
    ORDER BY PromEnviosSemana DESC;
END
GO

-- Ejemplos:
-- EXEC sp_ObtenerMetricasSeguimientoSemanal
-- EXEC sp_ObtenerMetricasSeguimientoSemanal '2026-07-01', '2026-10-14', 'SUCRE'
