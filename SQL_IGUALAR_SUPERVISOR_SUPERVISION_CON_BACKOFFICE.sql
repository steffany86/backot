-- ========================================================================
-- LLEVAR spx_ListarSupervisionManual AL MISMO PATRON QUE BACKOFFICE
-- ========================================================================
-- /supervisor/supervision (pestana "Completadas" dentro de Agenda Supervision,
-- y "Notas registradas") usaba spx_ListarSupervisionManual, que devolvia
-- supervisor/tecnico/tipo como NULL fijo -- el backend (Java) los resolvia
-- despues, fila por fila, consultando otras bases (el mismo problema que
-- hacia lenta Completadas en backoffice, ver
-- SQL_DEVOLVER_NOMBRES_DENORMALIZADOS_SIN_JOINS.sql).
--
-- Este script hace que spx_ListarSupervisionManual lea esos nombres
-- directo de las columnas denormalizadas de tbl_Supervision (igual que las
-- SPs de backoffice), y le agrega tambien "sucursal" y un filtro @Sucursal,
-- para poder aplicar el mismo filtro de sucursal que ya existe en
-- backoffice (el frontend de /supervisor/supervision ya enviaba ese
-- parametro, pero el backend lo ignoraba por completo).
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC [dbo].[spx_ListarSupervisionManual]
    @IdSupervisor NVARCHAR(50) = NULL,
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Sucursal NVARCHAR(250) = NULL,
    @TipoFecha NVARCHAR(20) = N'registro'
AS
BEGIN
    SET NOCOUNT ON;

	DECLARE @Limite INT
	SET @Limite = 500
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 200;

    SET @TipoFecha = LOWER(LTRIM(RTRIM(ISNULL(@TipoFecha, N'registro'))));
    IF @TipoFecha NOT IN (N'registro', N'supervision') SET @TipoFecha = N'registro';

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasFechaSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasCreadoPor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'creado_por') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT TOP (@Limite)
        s.Id_Supervision AS idSupervision,
		ID,
        ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro AS fechaRegistro' ELSE N'CAST(NULL AS DATETIME) AS fechaRegistro' END + N',
        ' + CASE WHEN @HasFechaSupervision = 1 THEN N's.FechaSupervision AS fechaSupervision' ELSE N'CAST(NULL AS DATETIME) AS fechaSupervision' END + N',
        ' + CASE WHEN @HasCreadoPor = 1 THEN N'NULLIF(LTRIM(RTRIM(s.creado_por)), '''') AS creadoPor' ELSE N'CAST(NULL AS NVARCHAR(200)) AS creadoPor' END + N',
        s.Id_Supervisor AS idSupervisor,
        ' + CASE WHEN @HasNombreSupervisor = 1 THEN N'NULLIF(LTRIM(RTRIM(s.nombre_Id_Supervisor)), '''') AS supervisor' ELSE N'CAST(NULL AS NVARCHAR(200)) AS supervisor' END + N',
        s.Id_TecnicoPrincipal AS idTecnicoPrincipal,
        ' + CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N'NULLIF(LTRIM(RTRIM(s.nombre_Id_TecnicoPrincipal)), '''') AS tecnicoPrincipal' ELSE N'CAST(NULL AS NVARCHAR(200)) AS tecnicoPrincipal' END + N',
        s.Id_TecnicoAuxiliar AS idTecnicoAuxiliar,
        NULLIF(LTRIM(RTRIM(s.Id_TecnicoAuxiliar)), '''') AS tecnicoAuxiliar,
        s.Id_TipoSupervision AS idTipoSupervision,
        ' + CASE WHEN @HasNombreTipoSupervision = 1 THEN N'NULLIF(LTRIM(RTRIM(s.nombre_id_TipoSupervision)), '''') AS tipoSupervision' ELSE N'CAST(NULL AS NVARCHAR(200)) AS tipoSupervision' END + N',
        s.Id_TipoTrabajo AS idTipoTrabajo,
        ' + CASE WHEN @HasNombreTipoTrabajo = 1 THEN N'NULLIF(LTRIM(RTRIM(s.nombre_id_Tipotrabajo)), '''') AS tipoTrabajo' ELSE N'CAST(NULL AS NVARCHAR(200)) AS tipoTrabajo' END + N',
        s.Id_TipoPenalizacion AS idTipoPenalizacion,
        ' + CASE WHEN @HasNombreTipoPenalizacion = 1 THEN N'NULLIF(LTRIM(RTRIM(s.nombre_id_TipoPenalizacion)), '''') AS tipoPenalizacion' ELSE N'CAST(NULL AS NVARCHAR(200)) AS tipoPenalizacion' END + N',
        s.Supervision_Por AS supervisionPor,
        s.Tecnologia AS tecnologia,
        s.Codigo AS codigo,
        s.OrdenTrabajo AS ordenTrabajo,
        RTRIM(LTRIM(s.TipoRevision)) AS tipoRevision,
        s.Observacion AS observacion,
        s.DescripcionAdicionalObservacion AS descripcionAdicionalObservacion,
        s.Ubicacion AS ubicacion,
        ' + CASE WHEN @HasEstadoSup = 1 THEN N's.estado_sup AS estadoSup' ELSE N'''completado'' AS estadoSup' END + N',
        ' + CASE WHEN @HasSucursal = 1 THEN N's.sucursal AS sucursal' ELSE N'CAST(NULL AS NVARCHAR(250)) AS sucursal' END + N'
    FROM dbo.tbl_Supervision s
    WHERE 1 = 1 ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
--    CASE WHEN @HasEstadoSup = 1 THEN N' AND s.estado_sup = ''completado'' ' ELSE N'' END +
    N' AND (@IdSupervisor IS NULL OR s.Id_Supervisor = @IdSupervisor) ' +
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(s.sucursal)) = @Sucursal) ' ELSE N'' END +
    CASE
        WHEN @TipoFecha = N'supervision' AND @HasFechaSupervision = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaSupervision AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaSupervision AS DATE) <= @FechaHasta)
    '
        WHEN @TipoFecha = N'registro' AND @HasFecha = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaRegistro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaRegistro AS DATE) <= @FechaHasta)
    '
        ELSE N''
    END +
    N' ORDER BY ' +
    CASE
        WHEN @TipoFecha = N'supervision' AND @HasFechaSupervision = 1 THEN N's.FechaSupervision DESC'
        WHEN @HasFecha = 1 THEN N's.FechaRegistro DESC'
        ELSE N's.Id DESC'
    END + N';';

    EXEC sp_executesql
        @Sql,
        N'@IdSupervisor NVARCHAR(50), @FechaDesde DATE, @FechaHasta DATE, @Limite INT, @Sucursal NVARCHAR(250)',
        @IdSupervisor = @IdSupervisor,
        @FechaDesde = @FechaDesde,
        @FechaHasta = @FechaHasta,
        @Limite = @Limite,
        @Sucursal = @Sucursal;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ListarSupervisionManual ahora lee los nombres directo de tbl_Supervision';
PRINT '(sin consultar otra base) y acepta @Sucursal para filtrar.';
PRINT '========================================';
