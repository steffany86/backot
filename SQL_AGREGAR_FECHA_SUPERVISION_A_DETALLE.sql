-- ========================================================================
-- AGREGAR fechaSupervision AL DETALLE DE UNA SUPERVISION (por id)
-- ========================================================================
-- spx_ObtenerSupervisionManualPorId es la SP detras de
-- GET /supervisor/supervision/{idSupervision}, usada tanto por el modal de
-- detalle del supervisor (SupervisorSupervisionPage) como por el fetch de
-- fotos del detalle en backoffice. No devolvia FechaSupervision.
--
-- Este script solo agrega esa columna, con el mismo patron idempotente
-- COL_LENGTH del resto de las SPs de supervision.
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ObtenerSupervisionManualPorId
    @IdSupervision NVARCHAR(50),
    @IdSupervisor NVARCHAR(50) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasFechaSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    SELECT TOP 1
        s.Id_Supervision AS idSupervision,
        ' + CASE WHEN @HasFecha = 1 THEN N's.FechaRegistro AS fechaRegistro' ELSE N'CAST(NULL AS DATETIME) AS fechaRegistro' END + N',
        ' + CASE WHEN @HasFechaSupervision = 1 THEN N's.FechaSupervision AS fechaSupervision' ELSE N'CAST(NULL AS DATETIME) AS fechaSupervision' END + N',
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
        s.Ubicacion AS ubicacion
    FROM dbo.tbl_Supervision s
    WHERE s.Id_Supervision = @IdSupervision ' +
    CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END +
    N' AND (@IdSupervisor IS NULL OR s.Id_Supervisor = @IdSupervisor);';

    EXEC sp_executesql
        @Sql,
        N'@IdSupervision NVARCHAR(50), @IdSupervisor NVARCHAR(50)',
        @IdSupervision = @IdSupervision,
        @IdSupervisor = @IdSupervisor;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ObtenerSupervisionManualPorId ahora devuelve fechaSupervision.';
PRINT '========================================';
