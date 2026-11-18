-- ========================================================================
-- COMPLETADAS (Supervision V2): respetar el selector "Buscar por"
-- (Fecha Registro / Fecha Supervision).
-- ========================================================================
-- Problema: spx_ListarSupervisionPorEstado siempre filtraba por FechaRegistro,
-- asi que una pendiente creada dias atras y realizada hoy no aparecia en
-- Completadas al buscar por "Fecha Supervision" con la fecha de hoy.
-- Cambio: nuevo parametro @TipoFecha ('registro' por defecto | 'supervision').
--
-- Ejecutar en la BD de Supervision V2 (172.16.0.13) ANTES de desplegar el backend
-- (el backend ahora envia 8 parametros).
-- Antes de correrlo, compara con: EXEC sp_helptext 'dbo.spx_ListarSupervisionPorEstado'
-- por si esa base tiene columnas adicionales respecto a este script.
-- ========================================================================

-- SQL Server 2008 no soporta CREATE OR ALTER: el procedimiento ya existe, por eso ALTER.
ALTER PROC dbo.spx_ListarSupervisionPorEstado
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
    IF @EstadoSup IN (N'pendientes') SET @EstadoSup = N'pendiente';
    IF @EstadoSup IN (N'completada', N'completadas', N'completados') SET @EstadoSup = N'completado';
    IF @EstadoSup NOT IN (N'pendiente', N'completado') SET @EstadoSup = N'pendiente';
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 300;
    SET @TipoFecha = LOWER(LTRIM(RTRIM(ISNULL(@TipoFecha, N'registro'))));

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasFechaSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'Id') IS NULL THEN 0 ELSE 1 END;
    DECLARE @EstadoColumn SYSNAME = NULL;

    IF COL_LENGTH('dbo.tbl_Supervision', 'estdo_sup') IS NOT NULL
        SET @EstadoColumn = N'estdo_sup';
    ELSE IF COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NOT NULL
        SET @EstadoColumn = N'estado_sup';

    -- En la tabla de V2 el id es "id" (no existe Id_Supervision) y el codigo se llama CodigoCliente.
    DECLARE @ColId SYSNAME =
        CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'Id_Supervision') IS NOT NULL THEN N'Id_Supervision' ELSE N'id' END;
    DECLARE @ColCodigo SYSNAME =
        CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'CodigoCliente') IS NOT NULL THEN N'CodigoCliente' ELSE N'Codigo' END;

    -- Columnas propias de V2 que la pantalla muestra (se agregan solo si existen en la tabla).
    DECLARE @Extra NVARCHAR(MAX) = N'';
    DECLARE @Map TABLE (col SYSNAME, alias SYSNAME);
    INSERT INTO @Map (col, alias) VALUES
        (N'FechaSupervision', N'fechaSupervision'),
        (N'creado_por', N'creadoPor'),
        (N'cargo_creado_por', N'cargoCreadoPor'),
        (N'cargoCreado_por', N'cargoCreadoPor'),
        (N'origen', N'origen'),
        (N'Id_LlamadaDeAtencion', N'idLlamadaAtencion'),
        (N'FirmaTecnico', N'firmaTecnico'),
        (N'FirmaAuxiliar', N'firmaAuxiliar'),
        (N'FirmaCliente', N'firmaCliente'),
        (N'id_supervisor_asignado', N'idSupervisorAsignado'),
        (N'nombre_supervisor_asignado', N'supervisorAsignado');
    SELECT @Extra = @Extra + N', s.' + QUOTENAME(col) + N' AS ' + QUOTENAME(alias)
    FROM @Map
    WHERE COL_LENGTH('dbo.tbl_Supervision', col) IS NOT NULL;

    -- Columna por la que se aplica el rango de fechas.
    DECLARE @ColFiltro NVARCHAR(40) = NULL;
    IF @TipoFecha = N'supervision' AND @HasFechaSup = 1 SET @ColFiltro = N'FechaSupervision';
    ELSE IF @HasFecha = 1 SET @ColFiltro = N'FechaRegistro';

    DECLARE @Sql NVARCHAR(MAX);
    SET @Sql = N'
    SELECT TOP (@Limite)
        s.' + QUOTENAME(@ColId) + N' AS idSupervision,
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
        s.' + QUOTENAME(@ColCodigo) + N' AS codigo,
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
        ' + CASE WHEN @HasId = 1 THEN N's.Id AS id' ELSE N'CAST(NULL AS NVARCHAR(50)) AS id' END + @Extra + N'
    FROM dbo.tbl_Supervision s
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
    CASE WHEN @ColFiltro IS NOT NULL THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.' + QUOTENAME(@ColFiltro) + N' AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.' + QUOTENAME(@ColFiltro) + N' AS DATE) <= @FechaHasta)
    ' ELSE N'' END +
    N' ORDER BY ' + CASE WHEN @ColFiltro IS NOT NULL THEN N's.' + QUOTENAME(@ColFiltro) + N' DESC' ELSE N's.' + QUOTENAME(@ColId) + N' DESC' END + N';';

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

PRINT 'spx_ListarSupervisionPorEstado actualizado: acepta @TipoFecha (registro | supervision).';
