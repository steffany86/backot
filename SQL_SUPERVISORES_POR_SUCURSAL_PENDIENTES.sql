-- ========================================================================
-- CATALOGO DE SUPERVISORES DEPENDIENTE DE LA SUCURSAL (Pendientes)
-- ========================================================================
-- El combo de supervisores en /backoffice/supervision (pestana Pendientes)
-- mostraba TODOS los supervisores sin importar la sucursal elegida. Este
-- script le agrega el parametro @Sucursal a spx_ListarNombresSupervisorSupervision
-- para que, al elegir por ejemplo "Tarija" en el filtro de sucursal, el
-- combo de supervisores solo muestre los que tienen registros ahi.
-- Tambien se restringe a estado_sup = 'pendiente' para que coincida
-- exactamente con lo que se ve en esa pestana (evita mostrar supervisores
-- que solo tienen supervisiones completadas en esa sucursal).
--
-- Correr DESPUES de SQL_REEMPLAZAR_FILTRO_TECNICO_POR_SUPERVISOR_PENDIENTES.sql.
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ListarNombresSupervisorSupervision
    @Sucursal NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL
    BEGIN
        SELECT TOP 0 CAST(NULL AS NVARCHAR(250)) AS supervisor;
        RETURN;
    END

    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT DISTINCT LTRIM(RTRIM(nombre_Id_Supervisor)) AS supervisor
    FROM dbo.tbl_Supervision
    WHERE nombre_Id_Supervisor IS NOT NULL AND LTRIM(RTRIM(nombre_Id_Supervisor)) <> '''' ' +
    CASE WHEN @HasEstadoSup = 1 THEN N' AND estado_sup = ''pendiente'' ' ELSE N'' END +
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(sucursal)) = @Sucursal) ' ELSE N'' END +
    N' ORDER BY supervisor;';

    EXEC sp_executesql
        @Sql,
        N'@Sucursal NVARCHAR(250)',
        @Sucursal = @Sucursal;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ListarNombresSupervisorSupervision ahora filtra por @Sucursal y solo pendientes.';
PRINT '========================================';
