-- ========================================================================
-- FILTRO DE TECNICO PRINCIPAL EN LA PESTAÑA "PENDIENTES" DE
-- /backoffice/supervision
-- ========================================================================
-- Usa la columna nombre_Id_TecnicoPrincipal de dbo.tbl_Supervision (agregada
-- junto con los demas campos denormalizados) tanto para el catalogo del
-- filtro como para filtrar el listado, sin conectarse a ninguna otra
-- tabla/base.
--
-- Requiere haber corrido antes SQL_ADD_FILTRO_SUCURSAL_SUPERVISION_PENDIENTES.sql
-- (crea spx_ListarSupervisionPendienteTodos con @Sucursal); este script le
-- agrega el parametro @TecnicoPrincipal a esa misma SP.
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ListarTecnicosPrincipalesSupervision
AS
BEGIN
    SET NOCOUNT ON;

    IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL
    BEGIN
        SELECT TOP 0 CAST(NULL AS NVARCHAR(250)) AS tecnicoPrincipal;
        RETURN;
    END

    SELECT DISTINCT LTRIM(RTRIM(nombre_Id_TecnicoPrincipal)) AS tecnicoPrincipal
    FROM dbo.tbl_Supervision
    WHERE nombre_Id_TecnicoPrincipal IS NOT NULL AND LTRIM(RTRIM(nombre_Id_TecnicoPrincipal)) <> ''
    ORDER BY tecnicoPrincipal;
END
GO

CREATE OR ALTER PROC dbo.spx_ListarSupervisionPendienteTodos
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 200,
    @Sucursal NVARCHAR(250) = NULL,
    @TecnicoPrincipal NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    IF @Limite IS NULL OR @Limite <= 0 SET @Limite = 200;

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
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
        ' + CASE WHEN @HasSucursal = 1 THEN N's.sucursal AS sucursal' ELSE N'CAST(NULL AS NVARCHAR(250)) AS sucursal' END + N'
    FROM dbo.tbl_Supervision s
    WHERE 1 = 1 ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
    CASE WHEN @HasEstadoSup = 1 THEN N' AND s.estado_sup = ''pendiente'' ' ELSE N' AND 1 = 0 ' END + -- Si no existe la columna, devolver vacío
    CASE WHEN @HasSucursal = 1 THEN N' AND (@Sucursal IS NULL OR LTRIM(RTRIM(s.sucursal)) = @Sucursal) ' ELSE N'' END +
    CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N' AND (@TecnicoPrincipal IS NULL OR LTRIM(RTRIM(s.nombre_Id_TecnicoPrincipal)) = @TecnicoPrincipal) ' ELSE N'' END +
    CASE WHEN @HasFecha = 1 THEN N'
      AND (@FechaDesde IS NULL OR CAST(s.FechaRegistro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(s.FechaRegistro AS DATE) <= @FechaHasta)
    ' ELSE N'' END +
    N' ORDER BY ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro DESC' ELSE N's.Id_Supervision DESC' END + N';';

    EXEC sp_executesql
        @Sql,
        N'@FechaDesde DATE, @FechaHasta DATE, @Limite INT, @Sucursal NVARCHAR(250), @TecnicoPrincipal NVARCHAR(250)',
        @FechaDesde = @FechaDesde,
        @FechaHasta = @FechaHasta,
        @Limite = @Limite,
        @Sucursal = @Sucursal,
        @TecnicoPrincipal = @TecnicoPrincipal;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT '1. spx_ListarTecnicosPrincipalesSupervision creado (catalogo de tecnicos desde tbl_Supervision).';
PRINT '2. spx_ListarSupervisionPendienteTodos actualizado con filtro @TecnicoPrincipal.';
PRINT '========================================';
