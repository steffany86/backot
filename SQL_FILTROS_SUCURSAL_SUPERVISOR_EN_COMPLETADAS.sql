-- ========================================================================
-- APLICAR EL MISMO FILTRO DE SUCURSAL + SUPERVISOR (tabla tbl_Supervision)
-- A LA PESTAÑA "COMPLETADAS" DE /backoffice/supervision
-- ========================================================================
-- 1) spx_ListarNombresSupervisorSupervision ahora recibe tambien @Estado,
--    para poder pedir el catalogo de supervisores tanto de pendientes como
--    de completadas (antes solo servia para 'pendiente').
-- 2) spx_ListarSupervisionPorEstado (la que arma la pestana Completadas)
--    suma @Sucursal y @SupervisorNombre, filtrando por las columnas
--    sucursal / nombre_Id_Supervisor de tbl_Supervision. Tambien se agrega
--    la columna Id AS id, igual que se hizo antes en pendientes, para que
--    "NroTrans" muestre el numero corto en vez del codigo hexadecimal.
--
-- Correr DESPUES de SQL_SUPERVISORES_POR_SUCURSAL_PENDIENTES.sql.
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ListarNombresSupervisorSupervision
    @Sucursal NVARCHAR(250) = NULL,
    @Estado NVARCHAR(20) = N'pendiente'
AS
BEGIN
    SET NOCOUNT ON;

    SET @Estado = LOWER(LTRIM(RTRIM(ISNULL(@Estado, N'pendiente'))));
    IF @Estado IN (N'completada', N'completadas', N'completados') SET @Estado = N'completado';
    IF @Estado NOT IN (N'pendiente', N'completado') SET @Estado = N'pendiente';

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
    CASE WHEN @HasEstadoSup = 1 THEN N' AND estado_sup = @Estado ' ELSE N'' END +
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(sucursal)) = @Sucursal) ' ELSE N'' END +
    N' ORDER BY supervisor;';

    EXEC sp_executesql
        @Sql,
        N'@Sucursal NVARCHAR(250), @Estado NVARCHAR(20)',
        @Sucursal = @Sucursal,
        @Estado = @Estado;
END
GO

CREATE OR ALTER PROC dbo.spx_ListarSupervisionPorEstado
    @EstadoSup NVARCHAR(20) = N'pendiente',
    @IdSupervisor NVARCHAR(50) = NULL,
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 300,
    @Sucursal NVARCHAR(250) = NULL,
    @SupervisorNombre NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SET @EstadoSup = LOWER(LTRIM(RTRIM(ISNULL(@EstadoSup, N'pendiente'))));
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
            CAST(NULL AS NVARCHAR(MAX)) AS fotoBoletaSupervision,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoCanalesPilos,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoNivelesDocsis,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoMedicionRuido,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoBarridoCanales,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoObservacion1,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoObservacion2,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoObservacion3,
            CAST(NULL AS NVARCHAR(MAX)) AS fotoObservacion4,
            CAST(NULL AS NVARCHAR(1000)) AS observacion,
            CAST(NULL AS NVARCHAR(1000)) AS descripcionAdicionalObservacion,
            CAST(NULL AS NVARCHAR(300)) AS ubicacion,
            CAST(NULL AS NVARCHAR(20)) AS estadoSup,
            CAST(NULL AS NVARCHAR(250)) AS sucursal,
            CAST(NULL AS NVARCHAR(50)) AS id;
        RETURN;
    END

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH(@Tabla, 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH(@Tabla, 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH(@Tabla, 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH(@Tabla, 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH(@Tabla, 'Id') IS NULL THEN 0 ELSE 1 END;
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
        s.FotoBoletaSupervision AS fotoBoletaSupervision,
        s.FotoCanalesPilotos AS fotoCanalesPilos,
        s.FotoNivelesDocsis AS fotoNivelesDocsis,
        s.FotoMedicionRuido AS fotoMedicionRuido,
        s.FotoBarridoCanales AS fotoBarridoCanales,
        s.FotoObservacion1 AS fotoObservacion1,
        s.FotoObservacion2 AS fotoObservacion2,
        s.FotoObservacion3 AS fotoObservacion3,
        s.FotoObservacion4 AS fotoObservacion4,
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
    CASE WHEN @HasFecha = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaRegistro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaRegistro AS DATE) <= @FechaHasta)
    ' ELSE N'' END +
    N' ORDER BY ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro DESC' ELSE N's.Id_Supervision DESC' END + N';';

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
PRINT '1. spx_ListarNombresSupervisorSupervision ahora acepta @Estado (pendiente/completado).';
PRINT '2. spx_ListarSupervisionPorEstado (pestana Completadas) suma @Sucursal, @SupervisorNombre y columna id.';
PRINT '========================================';
