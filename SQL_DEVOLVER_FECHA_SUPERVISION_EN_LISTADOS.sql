-- ========================================================================
-- DEVOLVER FechaSupervision Y creado_por EN LOS LISTADOS DE /backoffice/supervision
-- ========================================================================
-- SQL_AGREGAR_FECHA_SUPERVISION.sql hizo que FechaSupervision se guarde al
-- completar/registrar una supervision, pero ningun SP de listado la
-- devolvia todavia. Este script agrega esa columna (como "fechaSupervision")
-- y tambien creado_por (como "creadoPor", quien agendo/registro la fila) a
-- spx_ListarSupervisionPendienteTodos y spx_ListarSupervisionPorEstado, para
-- que el frontend pueda mostrarlas en las cartillas y en el detalle.
--
-- Construido sobre las versiones mas recientes de ambos SPs (con el fix
-- del filtro de supervisor en Pendientes y sin las fotos en Completadas).
-- ========================================================================

USE [BD_TigoHogar]
GO

-- ------------------------------------------------------------------------
-- 1) Pendientes
-- ------------------------------------------------------------------------
CREATE OR ALTER PROC dbo.spx_ListarSupervisionPendienteTodos
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 200,
    @Sucursal NVARCHAR(250) = NULL,
    @SupervisorNombre NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 200;

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasFechaSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'Id') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasCreadoPor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'creado_por') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT TOP (@Limite)
        s.Id_Supervision AS idSupervision,
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
        ' + CASE WHEN @HasEstadoSup = 1 THEN N's.estado_sup AS estadoSup' ELSE N'''pendiente'' AS estadoSup' END + N',
        ' + CASE WHEN @HasSucursal = 1 THEN N's.sucursal AS sucursal' ELSE N'CAST(NULL AS NVARCHAR(250)) AS sucursal' END + N',
        ' + CASE WHEN @HasId = 1 THEN N's.Id AS id' ELSE N'CAST(NULL AS NVARCHAR(50)) AS id' END + N'
    FROM dbo.tbl_Supervision s
    WHERE 1 = 1 ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
    CASE WHEN @HasEstadoSup = 1 THEN N' AND s.estado_sup = ''pendiente'' ' ELSE N' AND 1 = 0 ' END + -- Si no existe la columna, devolver vacío
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(s.sucursal)) = @Sucursal) ' ELSE N'' END +
    CASE WHEN @HasNombreSupervisor = 1 THEN N' AND (@SupervisorNombre IS NULL OR LTRIM(RTRIM(s.nombre_Id_Supervisor)) = @SupervisorNombre) ' ELSE N'' END +
    CASE WHEN @HasFecha = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaRegistro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaRegistro AS DATE) <= @FechaHasta)
    ' ELSE N'' END +
    N' ORDER BY ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro DESC' ELSE N's.Id_Supervision DESC' END + N';';

    EXEC sp_executesql
        @Sql,
        N'@FechaDesde DATE, @FechaHasta DATE, @Limite INT, @Sucursal NVARCHAR(250), @SupervisorNombre NVARCHAR(250)',
        @FechaDesde = @FechaDesde,
        @FechaHasta = @FechaHasta,
        @Limite = @Limite,
        @Sucursal = @Sucursal,
        @SupervisorNombre = @SupervisorNombre;
END
GO

-- ------------------------------------------------------------------------
-- 2) Completadas
-- ------------------------------------------------------------------------
CREATE OR ALTER PROC dbo.spx_ListarSupervisionPorEstado
    @EstadoSup NVARCHAR(20) = N'pendiente',
    @IdSupervisor NVARCHAR(50) = NULL,
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 300,
    @Sucursal NVARCHAR(250) = NULL,
    @SupervisorNombre NVARCHAR(250) = NULL,
    @TipoFecha NVARCHAR(20) = N'registro'
AS
BEGIN
    SET NOCOUNT ON;

    SET @EstadoSup = LOWER(LTRIM(RTRIM(ISNULL(@EstadoSup, N'pendiente'))));
    SET @TipoFecha = LOWER(LTRIM(RTRIM(ISNULL(@TipoFecha, N'registro'))));
    IF @TipoFecha NOT IN (N'registro', N'supervision') SET @TipoFecha = N'registro';
    IF @EstadoSup IN (N'pendientes') SET @EstadoSup = N'pendiente';
    IF @EstadoSup IN (N'completada', N'completadas', N'completados') SET @EstadoSup = N'completado';
    IF @EstadoSup NOT IN (N'pendiente', N'completado') SET @EstadoSup = N'pendiente';
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 300;

    DECLARE @Tabla NVARCHAR(256);
    IF OBJECT_ID(N'dbo.tbl_Supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.tbl_Supervision';
    ELSE IF OBJECT_ID(N'dbo.supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.supervision';
    ELSE IF OBJECT_ID(N'dbo.Supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.Supervision';
    ELSE
    BEGIN
        SELECT TOP 0
            CAST(NULL AS NVARCHAR(50)) AS idSupervision,
            CAST(NULL AS DATETIME) AS fechaRegistro,
            CAST(NULL AS DATETIME) AS fechaSupervision,
            CAST(NULL AS NVARCHAR(200)) AS creadoPor,
            CAST(NULL AS NVARCHAR(50)) AS idSupervisor,
            CAST(NULL AS NVARCHAR(200)) AS supervisor,
            CAST(NULL AS NVARCHAR(50)) AS idTecnicoPrincipal,
            CAST(NULL AS NVARCHAR(200)) AS tecnicoPrincipal,
            CAST(NULL AS NVARCHAR(150)) AS idTecnicoAuxiliar,
            CAST(NULL AS NVARCHAR(200)) AS tecnicoAuxiliar,
            CAST(NULL AS NVARCHAR(50)) AS idTipoSupervision,
            CAST(NULL AS NVARCHAR(200)) AS tipoSupervision,
            CAST(NULL AS NVARCHAR(50)) AS idTipoTrabajo,
            CAST(NULL AS NVARCHAR(200)) AS tipoTrabajo,
            CAST(NULL AS NVARCHAR(50)) AS idTipoPenalizacion,
            CAST(NULL AS NVARCHAR(200)) AS tipoPenalizacion,
            CAST(NULL AS NVARCHAR(100)) AS supervisionPor,
            CAST(NULL AS NVARCHAR(100)) AS tecnologia,
            CAST(NULL AS NVARCHAR(100)) AS codigo,
            CAST(NULL AS NVARCHAR(100)) AS ordenTrabajo,
            CAST(NULL AS NVARCHAR(50)) AS tipoRevision,
            CAST(NULL AS NVARCHAR(1000)) AS observacion,
            CAST(NULL AS NVARCHAR(1000)) AS descripcionAdicionalObservacion,
            CAST(NULL AS NVARCHAR(300)) AS ubicacion,
            CAST(NULL AS NVARCHAR(20)) AS estadoSup,
            CAST(NULL AS NVARCHAR(250)) AS sucursal,
            CAST(NULL AS NVARCHAR(50)) AS id;
        RETURN;
    END

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH(@Tabla, 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasFechaSupervision BIT = CASE WHEN COL_LENGTH(@Tabla, 'FechaSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH(@Tabla, 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH(@Tabla, 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH(@Tabla, 'Id') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasCreadoPor BIT = CASE WHEN COL_LENGTH(@Tabla, 'creado_por') IS NULL THEN 0 ELSE 1 END;
    DECLARE @EstadoColumn SYSNAME = NULL;

    IF COL_LENGTH(@Tabla, 'estdo_sup') IS NOT NULL
        SET @EstadoColumn = N'estdo_sup';
    ELSE IF COL_LENGTH(@Tabla, 'estado_sup') IS NOT NULL
        SET @EstadoColumn = N'estado_sup';

    DECLARE @Sql NVARCHAR(MAX);
    SET @Sql = N'
    SELECT TOP (@Limite)
        s.Id_Supervision AS idSupervision,
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
        ' + CASE WHEN @EstadoColumn IS NOT NULL THEN N's.' + QUOTENAME(@EstadoColumn) + N' AS estadoSup' ELSE N'@EstadoSup AS estadoSup' END + N',
        ' + CASE WHEN @HasSucursal = 1 THEN N's.sucursal AS sucursal' ELSE N'CAST(NULL AS NVARCHAR(250)) AS sucursal' END + N',
        ' + CASE WHEN @HasId = 1 THEN N's.Id AS id' ELSE N'CAST(NULL AS NVARCHAR(50)) AS id' END + N'
    FROM ' + @Tabla + N' s
    WHERE 1 = 1 ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
    CASE WHEN @EstadoColumn IS NOT NULL THEN N' AND (
        (@EstadoSup = N''pendiente'' AND LOWER(LTRIM(RTRIM(ISNULL(s.' + QUOTENAME(@EstadoColumn) + N', N'''')))) IN (N''pendiente'', N''pendientes''))
        OR
        (@EstadoSup = N''completado'' AND LOWER(LTRIM(RTRIM(ISNULL(s.' + QUOTENAME(@EstadoColumn) + N', N'''')))) IN (N''completado'', N''completada'', N''completados'', N''completadas''))
    ) ' ELSE N' AND 1 = 0 ' END +
    N' AND (@IdSupervisor IS NULL OR s.Id_Supervisor = @IdSupervisor) ' +
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(s.sucursal)) = @Sucursal) ' ELSE N'' END +
    CASE WHEN @HasNombreSupervisor = 1 THEN N' AND (@SupervisorNombre IS NULL OR LTRIM(RTRIM(s.nombre_Id_Supervisor)) = @SupervisorNombre) ' ELSE N'' END +
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
        ELSE N's.Id_Supervision DESC'
    END + N';';

    EXEC sp_executesql
        @Sql,
        N'@EstadoSup NVARCHAR(20), @IdSupervisor NVARCHAR(50), @FechaDesde DATE, @FechaHasta DATE, @Limite INT, @Sucursal NVARCHAR(250), @SupervisorNombre NVARCHAR(250)',
        @EstadoSup = @EstadoSup,
        @IdSupervisor = @IdSupervisor,
        @FechaDesde = @FechaDesde,
        @FechaHasta = @FechaHasta,
        @Limite = @Limite,
        @Sucursal = @Sucursal,
        @SupervisorNombre = @SupervisorNombre;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'Ambos listados (Pendientes y Completadas) ahora devuelven fechaSupervision y creadoPor.';
PRINT '========================================';
