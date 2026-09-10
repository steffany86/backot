-- ========================================================================
-- REEMPLAZAR EL FILTRO DE "TECNICO" POR "SUPERVISOR" EN LA PESTAÑA
-- "PENDIENTES" DE /backoffice/supervision
-- ========================================================================
-- El combo agregado en SQL_ADD_FILTRO_TECNICO_SUPERVISION_PENDIENTES.sql
-- estaba mal: el dato que se queria mostrar ahi es el supervisor, no el
-- tecnico principal. Este script reemplaza ese filtro por uno basado en la
-- columna nombre_Id_Supervisor de tbl_Supervision (mismo criterio: sin
-- conectarse a otra tabla/base), y deja spx_ListarSupervisionPendienteTodos
-- con @Supervisor en vez de @TecnicoPrincipal.
--
-- Correr DESPUES de SQL_MOSTRAR_ID_NUMERICO_EN_PENDIENTES.sql (mantiene la
-- columna Id AS id agregada ahi). El SP dbo.spx_ListarTecnicosPrincipalesSupervision
-- que quedo de la version anterior ya no se usa y se puede eliminar
-- si se quiere (no lo borra este script para no arriesgar nada).
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ListarNombresSupervisorSupervision
AS
BEGIN
    SET NOCOUNT ON;

    IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL
    BEGIN
        SELECT TOP 0 CAST(NULL AS NVARCHAR(250)) AS supervisor;
        RETURN;
    END

    SELECT DISTINCT LTRIM(RTRIM(nombre_Id_Supervisor)) AS supervisor
    FROM dbo.tbl_Supervision
    WHERE nombre_Id_Supervisor IS NOT NULL AND LTRIM(RTRIM(nombre_Id_Supervisor)) <> ''
    ORDER BY supervisor;
END
GO

CREATE OR ALTER PROC dbo.spx_ListarSupervisionPendienteTodos
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 200,
    @Sucursal NVARCHAR(250) = NULL,
    @Supervisor NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 200;

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'Id') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT TOP (@Limite)
        s.Id_Supervision AS idSupervision,
        ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro AS fechaRegistro' ELSE N'CAST(NULL AS DATETIME) AS fechaRegistro' END + N',
        s.Id_Supervisor AS idSupervisor,
        CAST(NULL AS NVARCHAR(200)) AS supervisor,
        s.Id_TecnicoPrincipal AS idTecnicoPrincipal,
        CAST(NULL AS NVARCHAR(200)) AS tecnicoPrincipal,
        s.Id_TecnicoAuxiliar AS idTecnicoAuxiliar,
        CAST(NULL AS NVARCHAR(200)) AS tecnicoAuxiliar,
        s.Id_TipoSupervision AS idTipoSupervision,
        CAST(NULL AS NVARCHAR(200)) AS tipoSupervision,
        s.Id_TipoTrabajo AS idTipoTrabajo,
        CAST(NULL AS NVARCHAR(200)) AS tipoTrabajo,
        s.Id_TipoPenalizacion AS idTipoPenalizacion,
        CAST(NULL AS NVARCHAR(200)) AS tipoPenalizacion,
        s.Supervision_Por AS supervisionPor,
        s.Tecnologia AS tecnologia,
        s.Codigo AS codigo,
        s.OrdenTrabajo AS ordenTrabajo,
        RTRIM(LTRIM(s.TipoRevision)) AS tipoRevision,
        s.Observacion AS observacion,
        s.DescripcionAdicionalObservacion AS descripcionAdicionalObservacion,
        s.Ubicacion AS ubicacion,
        ' + CASE WHEN @HasEstadoSup = 1 THEN N's.estado_sup AS estadoSup' ELSE N'''pendiente'' AS estadoSup' END + N',
        ' + CASE WHEN @HasSucursal = 1 THEN N's.sucursal AS sucursal' ELSE N'CAST(NULL AS NVARCHAR(250)) AS sucursal' END + N',
        ' + CASE WHEN @HasId = 1 THEN N's.Id AS id' ELSE N'CAST(NULL AS NVARCHAR(50)) AS id' END + N'
    FROM dbo.tbl_Supervision s
    WHERE 1 = 1 ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
    CASE WHEN @HasEstadoSup = 1 THEN N' AND s.estado_sup = ''pendiente'' ' ELSE N' AND 1 = 0 ' END + -- Si no existe la columna, devolver vacío
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(s.sucursal)) = @Sucursal) ' ELSE N'' END +
    CASE WHEN @HasNombreSupervisor = 1 THEN N' AND (@Supervisor IS NULL OR LTRIM(RTRIM(s.nombre_Id_Supervisor)) = @Supervisor) ' ELSE N'' END +
    CASE WHEN @HasFecha = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaRegistro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaRegistro AS DATE) <= @FechaHasta)
    ' ELSE N'' END +
    N' ORDER BY ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro DESC' ELSE N's.Id_Supervision DESC' END + N';';

    EXEC sp_executesql
        @Sql,
        N'@FechaDesde DATE, @FechaHasta DATE, @Limite INT, @Sucursal NVARCHAR(250), @Supervisor NVARCHAR(250)',
        @FechaDesde = @FechaDesde,
        @FechaHasta = @FechaHasta,
        @Limite = @Limite,
        @Sucursal = @Sucursal,
        @Supervisor = @Supervisor;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT '1. spx_ListarNombresSupervisorSupervision creado (catalogo de supervisores desde tbl_Supervision).';
PRINT '2. spx_ListarSupervisionPendienteTodos actualizado: @TecnicoPrincipal reemplazado por @Supervisor.';
PRINT '========================================';
