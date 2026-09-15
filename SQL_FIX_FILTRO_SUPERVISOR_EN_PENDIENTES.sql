-- ========================================================================
-- FIX: el filtro de "supervisor" en Pendientes filtraba la columna
-- equivocada (nombre_Id_TecnicoPrincipal en vez de nombre_Id_Supervisor)
-- ========================================================================
-- El dropdown "Todos los supervisores" de la pestana Pendientes de
-- /backoffice/supervision envia el nombre del SUPERVISOR seleccionado al
-- backend, que lo pasa como 5to parametro a spx_ListarSupervisionPendienteTodos.
-- Pero el SP desplegado (revisado ahora mismo con OBJECT_DEFINITION) sigue
-- comparando ese valor contra nombre_Id_TecnicoPrincipal -- una columna
-- de TECNICO, no de supervisor. Por eso elegir cualquier supervisor
-- devolvia 0 resultados: el nombre del supervisor nunca coincide con un
-- nombre de tecnico.
--
-- (spx_ListarSupervisionPorEstado, la pestana Completadas, ya filtraba
-- correctamente contra nombre_Id_Supervisor -- ese SP no se toca aqui.)
--
-- Este script solo cambia la columna que compara el filtro (de
-- nombre_Id_TecnicoPrincipal a nombre_Id_Supervisor) y renombra el
-- parametro a @SupervisorNombre para que sea claro. Como el backend llama
-- al SP con "?" posicionales (no por nombre), renombrar el parametro no
-- rompe nada.
-- ========================================================================

USE [BD_TigoHogar]
GO

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
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasId BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'Id') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT TOP (@Limite)
        s.Id_Supervision AS idSupervision,
        ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro AS fechaRegistro' ELSE N'CAST(NULL AS DATETIME) AS fechaRegistro' END + N',
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

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ListarSupervisionPendienteTodos ahora filtra el supervisor seleccionado';
PRINT 'contra nombre_Id_Supervisor (antes comparaba contra nombre_Id_TecnicoPrincipal).';
PRINT '========================================';
