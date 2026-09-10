-- ========================================================================
-- AGREGAR CAMPOS DENORMALIZADOS (nombres/sucursal/origen) A tbl_Supervision
-- ========================================================================
-- Columnas agregadas manualmente por el usuario:
--   nombre_Id_Supervisor        NVARCHAR(250)
--   sucursal                    NVARCHAR(250)
--   origen                      NVARCHAR(250)
--   nombre_Id_TecnicoPrincipal  NVARCHAR(250)
--   nombre_id_TipoSupervision   NVARCHAR(250)
--   nombre_id_Tipotrabajo       NVARCHAR(250)
--   nombre_id_TipoPenalizacion  NVARCHAR(250)
--
-- Este script agrega las columnas si faltan (idempotente) y actualiza los
-- SPs de registro para completarlas al momento de insertar. Usa el mismo
-- patron COL_LENGTH ya usado para estado_sup/creado_por, para que el SP
-- siga funcionando en entornos donde alguna columna aun no exista.
-- ========================================================================

USE [BD_TigoHogar]
GO

IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD nombre_Id_Supervisor NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD sucursal NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'origen') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD origen NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD nombre_Id_TecnicoPrincipal NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD nombre_id_TipoSupervision NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD nombre_id_Tipotrabajo NVARCHAR(250) NULL;
GO
IF COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD nombre_id_TipoPenalizacion NVARCHAR(250) NULL;
GO

-- ------------------------------------------------------------------------
-- spx_RegistrarSupervisionManual (agrega los 7 parametros nuevos al final)
-- ------------------------------------------------------------------------
CREATE OR ALTER PROC dbo.spx_RegistrarSupervisionManual
    @Id_Supervisor NVARCHAR(50),
    @Id_TecnicoPrincipal NVARCHAR(50),
    @Id_TecnicoAuxiliar NVARCHAR(150),
    @Id_TipoSupervision NVARCHAR(50),
    @Id_TipoTrabajo NVARCHAR(50),
    @Id_TipoPenalizacion NVARCHAR(50),
    @Supervision_Por NVARCHAR(100),
    @Tecnologia NVARCHAR(100),
    @Codigo NVARCHAR(100),
    @OrdenTrabajo NVARCHAR(100),
    @TipoRevision NVARCHAR(50),
    @FotoBoletaSupervision NVARCHAR(MAX) = NULL,
    @FotoCanalesPilos NVARCHAR(MAX) = NULL,
    @FotoNivelesDocsis NVARCHAR(MAX) = NULL,
    @FotoMedicionRuido NVARCHAR(MAX) = NULL,
    @FotoBarridoCanales NVARCHAR(MAX) = NULL,
    @FotoObservacion1 NVARCHAR(MAX) = NULL,
    @FotoObservacion2 NVARCHAR(MAX) = NULL,
    @FotoObservacion3 NVARCHAR(MAX) = NULL,
    @FotoObservacion4 NVARCHAR(MAX) = NULL,
    @Observacion NVARCHAR(1000) = NULL,
    @DescripcionAdicionalObservacion NVARCHAR(1000) = NULL,
    @Ubicacion NVARCHAR(300),
    @NombreSupervisor NVARCHAR(250) = NULL,
    @Sucursal NVARCHAR(250) = NULL,
    @Origen NVARCHAR(250) = NULL,
    @NombreTecnicoPrincipal NVARCHAR(250) = NULL,
    @NombreTipoSupervision NVARCHAR(250) = NULL,
    @NombreTipoTrabajo NVARCHAR(250) = NULL,
    @NombreTipoPenalizacion NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdSupervision NVARCHAR(50) = LEFT(REPLACE(CONVERT(NVARCHAR(36), NEWID()), '-', ''), 8);
    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasOrigen BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'origen') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'INSERT INTO dbo.tbl_Supervision (' +
               N'Id_Supervision, Id_Supervisor, Id_TecnicoPrincipal, Id_TecnicoAuxiliar, Id_TipoSupervision, ' +
               N'Id_TipoTrabajo, Id_TipoPenalizacion, Supervision_Por, Tecnologia, Codigo, OrdenTrabajo, TipoRevision, ' +
               N'FotoBoletaSupervision, FotoCanalesPilotos, FotoNivelesDocsis, FotoMedicionRuido, FotoBarridoCanales, ' +
               N'FotoObservacion1, FotoObservacion2, FotoObservacion3, FotoObservacion4, Observacion, DescripcionAdicionalObservacion, Ubicacion' +
               CASE WHEN @HasFecha = 1 THEN N', FechaRegistro' ELSE N'' END +
               CASE WHEN @HasEliminado = 1 THEN N', E_Eliminado' ELSE N'' END +
               CASE WHEN @HasEstadoSup = 1 THEN N', estado_sup' ELSE N'' END +
               CASE WHEN @HasNombreSupervisor = 1 THEN N', nombre_Id_Supervisor' ELSE N'' END +
               CASE WHEN @HasSucursal = 1 THEN N', sucursal' ELSE N'' END +
               CASE WHEN @HasOrigen = 1 THEN N', origen' ELSE N'' END +
               CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N', nombre_Id_TecnicoPrincipal' ELSE N'' END +
               CASE WHEN @HasNombreTipoSupervision = 1 THEN N', nombre_id_TipoSupervision' ELSE N'' END +
               CASE WHEN @HasNombreTipoTrabajo = 1 THEN N', nombre_id_Tipotrabajo' ELSE N'' END +
               CASE WHEN @HasNombreTipoPenalizacion = 1 THEN N', nombre_id_TipoPenalizacion' ELSE N'' END +
               N') VALUES (' +
               N'@IdSupervision, @Id_Supervisor, @Id_TecnicoPrincipal, @Id_TecnicoAuxiliar, @Id_TipoSupervision, ' +
               N'@Id_TipoTrabajo, @Id_TipoPenalizacion, @Supervision_Por, @Tecnologia, @Codigo, @OrdenTrabajo, @TipoRevision, ' +
               N'@FotoBoletaSupervision, @FotoCanalesPilos, @FotoNivelesDocsis, @FotoMedicionRuido, @FotoBarridoCanales, ' +
               N'@FotoObservacion1, @FotoObservacion2, @FotoObservacion3, @FotoObservacion4, @Observacion, @DescripcionAdicionalObservacion, @Ubicacion' +
               CASE WHEN @HasFecha = 1 THEN N', GETDATE()' ELSE N'' END +
               CASE WHEN @HasEliminado = 1 THEN N', 0' ELSE N'' END +
               CASE WHEN @HasEstadoSup = 1 THEN N', ''completado''' ELSE N'' END +
               CASE WHEN @HasNombreSupervisor = 1 THEN N', @NombreSupervisor' ELSE N'' END +
               CASE WHEN @HasSucursal = 1 THEN N', @Sucursal' ELSE N'' END +
               CASE WHEN @HasOrigen = 1 THEN N', @Origen' ELSE N'' END +
               CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N', @NombreTecnicoPrincipal' ELSE N'' END +
               CASE WHEN @HasNombreTipoSupervision = 1 THEN N', @NombreTipoSupervision' ELSE N'' END +
               CASE WHEN @HasNombreTipoTrabajo = 1 THEN N', @NombreTipoTrabajo' ELSE N'' END +
               CASE WHEN @HasNombreTipoPenalizacion = 1 THEN N', @NombreTipoPenalizacion' ELSE N'' END +
               N');';

    EXEC sp_executesql
        @Sql,
        N'@IdSupervision NVARCHAR(50),
          @Id_Supervisor NVARCHAR(50),
          @Id_TecnicoPrincipal NVARCHAR(50),
          @Id_TecnicoAuxiliar NVARCHAR(150),
          @Id_TipoSupervision NVARCHAR(50),
          @Id_TipoTrabajo NVARCHAR(50),
          @Id_TipoPenalizacion NVARCHAR(50),
          @Supervision_Por NVARCHAR(100),
          @Tecnologia NVARCHAR(100),
          @Codigo NVARCHAR(100),
          @OrdenTrabajo NVARCHAR(100),
          @TipoRevision NVARCHAR(50),
          @FotoBoletaSupervision NVARCHAR(MAX),
          @FotoCanalesPilos NVARCHAR(MAX),
          @FotoNivelesDocsis NVARCHAR(MAX),
          @FotoMedicionRuido NVARCHAR(MAX),
          @FotoBarridoCanales NVARCHAR(MAX),
          @FotoObservacion1 NVARCHAR(MAX),
          @FotoObservacion2 NVARCHAR(MAX),
          @FotoObservacion3 NVARCHAR(MAX),
          @FotoObservacion4 NVARCHAR(MAX),
          @Observacion NVARCHAR(1000),
          @DescripcionAdicionalObservacion NVARCHAR(1000),
          @Ubicacion NVARCHAR(300),
          @NombreSupervisor NVARCHAR(250),
          @Sucursal NVARCHAR(250),
          @Origen NVARCHAR(250),
          @NombreTecnicoPrincipal NVARCHAR(250),
          @NombreTipoSupervision NVARCHAR(250),
          @NombreTipoTrabajo NVARCHAR(250),
          @NombreTipoPenalizacion NVARCHAR(250)',
        @IdSupervision = @IdSupervision,
        @Id_Supervisor = @Id_Supervisor,
        @Id_TecnicoPrincipal = @Id_TecnicoPrincipal,
        @Id_TecnicoAuxiliar = @Id_TecnicoAuxiliar,
        @Id_TipoSupervision = @Id_TipoSupervision,
        @Id_TipoTrabajo = @Id_TipoTrabajo,
        @Id_TipoPenalizacion = @Id_TipoPenalizacion,
        @Supervision_Por = @Supervision_Por,
        @Tecnologia = @Tecnologia,
        @Codigo = @Codigo,
        @OrdenTrabajo = @OrdenTrabajo,
        @TipoRevision = @TipoRevision,
        @FotoBoletaSupervision = @FotoBoletaSupervision,
        @FotoCanalesPilos = @FotoCanalesPilos,
        @FotoNivelesDocsis = @FotoNivelesDocsis,
        @FotoMedicionRuido = @FotoMedicionRuido,
        @FotoBarridoCanales = @FotoBarridoCanales,
        @FotoObservacion1 = @FotoObservacion1,
        @FotoObservacion2 = @FotoObservacion2,
        @FotoObservacion3 = @FotoObservacion3,
        @FotoObservacion4 = @FotoObservacion4,
        @Observacion = @Observacion,
        @DescripcionAdicionalObservacion = @DescripcionAdicionalObservacion,
        @Ubicacion = @Ubicacion,
        @NombreSupervisor = @NombreSupervisor,
        @Sucursal = @Sucursal,
        @Origen = @Origen,
        @NombreTecnicoPrincipal = @NombreTecnicoPrincipal,
        @NombreTipoSupervision = @NombreTipoSupervision,
        @NombreTipoTrabajo = @NombreTipoTrabajo,
        @NombreTipoPenalizacion = @NombreTipoPenalizacion;

    SELECT @IdSupervision AS Id_Supervision;
END
GO

-- ------------------------------------------------------------------------
-- spx_RegistrarSupervisionPendiente (agrega los 7 parametros nuevos al final)
-- ------------------------------------------------------------------------
CREATE OR ALTER PROC dbo.spx_RegistrarSupervisionPendiente
    @Id_Supervisor NVARCHAR(50),
    @Id_TecnicoPrincipal NVARCHAR(50),
    @Id_TecnicoAuxiliar NVARCHAR(50),
    @Id_TipoSupervision NVARCHAR(50),
    @Id_TipoTrabajo NVARCHAR(50),
    @Id_TipoPenalizacion NVARCHAR(50),
    @Supervision_Por NVARCHAR(100),
    @Tecnologia NVARCHAR(100),
    @Codigo NVARCHAR(100),
    @OrdenTrabajo NVARCHAR(100),
    @TipoRevision NVARCHAR(50),
    @FotoBoletaSupervision NVARCHAR(MAX) = NULL,
    @FotoCanalesPilos NVARCHAR(MAX) = NULL,
    @FotoNivelesDocsis NVARCHAR(MAX) = NULL,
    @FotoMedicionRuido NVARCHAR(MAX) = NULL,
    @FotoBarridoCanales NVARCHAR(MAX) = NULL,
    @FotoObservacion1 NVARCHAR(MAX) = NULL,
    @FotoObservacion2 NVARCHAR(MAX) = NULL,
    @FotoObservacion3 NVARCHAR(MAX) = NULL,
    @FotoObservacion4 NVARCHAR(MAX) = NULL,
    @Observacion NVARCHAR(1000) = NULL,
    @DescripcionAdicionalObservacion NVARCHAR(1000) = NULL,
    @Ubicacion NVARCHAR(300),
    @CreadoPor NVARCHAR(200) = NULL,
    @NombreSupervisor NVARCHAR(250) = NULL,
    @Sucursal NVARCHAR(250) = NULL,
    @Origen NVARCHAR(250) = NULL,
    @NombreTecnicoPrincipal NVARCHAR(250) = NULL,
    @NombreTipoSupervision NVARCHAR(250) = NULL,
    @NombreTipoTrabajo NVARCHAR(250) = NULL,
    @NombreTipoPenalizacion NVARCHAR(250) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdSupervision NVARCHAR(50) = LEFT(REPLACE(CONVERT(NVARCHAR(36), NEWID()), '-', ''), 8);
    DECLARE @HasFecha BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'FechaRegistro') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasEstadoSup BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'estado_sup') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasCreadoPor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'creado_por') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreSupervisor BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_Supervisor') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasSucursal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'sucursal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasOrigen BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'origen') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTecnicoPrincipal BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_Id_TecnicoPrincipal') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoSupervision BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoSupervision') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoTrabajo BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_Tipotrabajo') IS NULL THEN 0 ELSE 1 END;
    DECLARE @HasNombreTipoPenalizacion BIT = CASE WHEN COL_LENGTH('dbo.tbl_Supervision', 'nombre_id_TipoPenalizacion') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'INSERT INTO dbo.tbl_Supervision (' +
               N'Id_Supervision, Id_Supervisor, Id_TecnicoPrincipal, Id_TecnicoAuxiliar, Id_TipoSupervision, ' +
               N'Id_TipoTrabajo, Id_TipoPenalizacion, Supervision_Por, Tecnologia, Codigo, OrdenTrabajo, TipoRevision, ' +
               N'FotoBoletaSupervision, FotoCanalesPilotos, FotoNivelesDocsis, FotoMedicionRuido, FotoBarridoCanales, ' +
               N'FotoObservacion1, FotoObservacion2, FotoObservacion3, FotoObservacion4, Observacion, DescripcionAdicionalObservacion, Ubicacion' +
               CASE WHEN @HasFecha = 1 THEN N', FechaRegistro' ELSE N'' END +
               CASE WHEN @HasEliminado = 1 THEN N', E_Eliminado' ELSE N'' END +
               CASE WHEN @HasEstadoSup = 1 THEN N', estado_sup' ELSE N'' END +
               CASE WHEN @HasCreadoPor = 1 THEN N', creado_por' ELSE N'' END +
               CASE WHEN @HasNombreSupervisor = 1 THEN N', nombre_Id_Supervisor' ELSE N'' END +
               CASE WHEN @HasSucursal = 1 THEN N', sucursal' ELSE N'' END +
               CASE WHEN @HasOrigen = 1 THEN N', origen' ELSE N'' END +
               CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N', nombre_Id_TecnicoPrincipal' ELSE N'' END +
               CASE WHEN @HasNombreTipoSupervision = 1 THEN N', nombre_id_TipoSupervision' ELSE N'' END +
               CASE WHEN @HasNombreTipoTrabajo = 1 THEN N', nombre_id_Tipotrabajo' ELSE N'' END +
               CASE WHEN @HasNombreTipoPenalizacion = 1 THEN N', nombre_id_TipoPenalizacion' ELSE N'' END +
               N') VALUES (' +
               N'@IdSupervision, @Id_Supervisor, @Id_TecnicoPrincipal, @Id_TecnicoAuxiliar, @Id_TipoSupervision, ' +
               N'@Id_TipoTrabajo, @Id_TipoPenalizacion, @Supervision_Por, @Tecnologia, @Codigo, @OrdenTrabajo, @TipoRevision, ' +
               N'@FotoBoletaSupervision, @FotoCanalesPilos, @FotoNivelesDocsis, @FotoMedicionRuido, @FotoBarridoCanales, ' +
               N'@FotoObservacion1, @FotoObservacion2, @FotoObservacion3, @FotoObservacion4, @Observacion, @DescripcionAdicionalObservacion, @Ubicacion' +
               CASE WHEN @HasFecha = 1 THEN N', GETDATE()' ELSE N'' END +
               CASE WHEN @HasEliminado = 1 THEN N', 0' ELSE N'' END +
               CASE WHEN @HasEstadoSup = 1 THEN N', ''pendiente''' ELSE N'' END +
               CASE WHEN @HasCreadoPor = 1 THEN N', @CreadoPor' ELSE N'' END +
               CASE WHEN @HasNombreSupervisor = 1 THEN N', @NombreSupervisor' ELSE N'' END +
               CASE WHEN @HasSucursal = 1 THEN N', @Sucursal' ELSE N'' END +
               CASE WHEN @HasOrigen = 1 THEN N', @Origen' ELSE N'' END +
               CASE WHEN @HasNombreTecnicoPrincipal = 1 THEN N', @NombreTecnicoPrincipal' ELSE N'' END +
               CASE WHEN @HasNombreTipoSupervision = 1 THEN N', @NombreTipoSupervision' ELSE N'' END +
               CASE WHEN @HasNombreTipoTrabajo = 1 THEN N', @NombreTipoTrabajo' ELSE N'' END +
               CASE WHEN @HasNombreTipoPenalizacion = 1 THEN N', @NombreTipoPenalizacion' ELSE N'' END +
               N');';

    EXEC sp_executesql
        @Sql,
        N'@IdSupervision NVARCHAR(50),
          @Id_Supervisor NVARCHAR(50),
          @Id_TecnicoPrincipal NVARCHAR(50),
          @Id_TecnicoAuxiliar NVARCHAR(50),
          @Id_TipoSupervision NVARCHAR(50),
          @Id_TipoTrabajo NVARCHAR(50),
          @Id_TipoPenalizacion NVARCHAR(50),
          @Supervision_Por NVARCHAR(100),
          @Tecnologia NVARCHAR(100),
          @Codigo NVARCHAR(100),
          @OrdenTrabajo NVARCHAR(100),
          @TipoRevision NVARCHAR(50),
          @FotoBoletaSupervision NVARCHAR(MAX),
          @FotoCanalesPilos NVARCHAR(MAX),
          @FotoNivelesDocsis NVARCHAR(MAX),
          @FotoMedicionRuido NVARCHAR(MAX),
          @FotoBarridoCanales NVARCHAR(MAX),
          @FotoObservacion1 NVARCHAR(MAX),
          @FotoObservacion2 NVARCHAR(MAX),
          @FotoObservacion3 NVARCHAR(MAX),
          @FotoObservacion4 NVARCHAR(MAX),
          @Observacion NVARCHAR(1000),
          @DescripcionAdicionalObservacion NVARCHAR(1000),
          @Ubicacion NVARCHAR(300),
          @CreadoPor NVARCHAR(200),
          @NombreSupervisor NVARCHAR(250),
          @Sucursal NVARCHAR(250),
          @Origen NVARCHAR(250),
          @NombreTecnicoPrincipal NVARCHAR(250),
          @NombreTipoSupervision NVARCHAR(250),
          @NombreTipoTrabajo NVARCHAR(250),
          @NombreTipoPenalizacion NVARCHAR(250)',
        @IdSupervision = @IdSupervision,
        @Id_Supervisor = @Id_Supervisor,
        @Id_TecnicoPrincipal = @Id_TecnicoPrincipal,
        @Id_TecnicoAuxiliar = @Id_TecnicoAuxiliar,
        @Id_TipoSupervision = @Id_TipoSupervision,
        @Id_TipoTrabajo = @Id_TipoTrabajo,
        @Id_TipoPenalizacion = @Id_TipoPenalizacion,
        @Supervision_Por = @Supervision_Por,
        @Tecnologia = @Tecnologia,
        @Codigo = @Codigo,
        @OrdenTrabajo = @OrdenTrabajo,
        @TipoRevision = @TipoRevision,
        @FotoBoletaSupervision = @FotoBoletaSupervision,
        @FotoCanalesPilos = @FotoCanalesPilos,
        @FotoNivelesDocsis = @FotoNivelesDocsis,
        @FotoMedicionRuido = @FotoMedicionRuido,
        @FotoBarridoCanales = @FotoBarridoCanales,
        @FotoObservacion1 = @FotoObservacion1,
        @FotoObservacion2 = @FotoObservacion2,
        @FotoObservacion3 = @FotoObservacion3,
        @FotoObservacion4 = @FotoObservacion4,
        @Observacion = @Observacion,
        @DescripcionAdicionalObservacion = @DescripcionAdicionalObservacion,
        @Ubicacion = @Ubicacion,
        @CreadoPor = @CreadoPor,
        @NombreSupervisor = @NombreSupervisor,
        @Sucursal = @Sucursal,
        @Origen = @Origen,
        @NombreTecnicoPrincipal = @NombreTecnicoPrincipal,
        @NombreTipoSupervision = @NombreTipoSupervision,
        @NombreTipoTrabajo = @NombreTipoTrabajo,
        @NombreTipoPenalizacion = @NombreTipoPenalizacion;

    SELECT @IdSupervision AS Id_Supervision;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'Columnas denormalizadas agregadas/confirmadas y SPs de registro actualizados.';
PRINT '========================================';
