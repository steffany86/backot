USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_RealizarSupervisionPendiente
    @IdSupervision NVARCHAR(50),
    @IdSupervisor NVARCHAR(50),
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
    @Ubicacion NVARCHAR(300) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Tabla NVARCHAR(256);
    IF OBJECT_ID(N'dbo.tbl_Supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.tbl_Supervision';
    ELSE IF OBJECT_ID(N'dbo.supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.supervision';
    ELSE IF OBJECT_ID(N'dbo.Supervision', N'U') IS NOT NULL
        SET @Tabla = N'dbo.Supervision';
    ELSE
    BEGIN
        SELECT CAST(0 AS INT) AS actualizados;
        RETURN;
    END

    DECLARE @EstadoColumn SYSNAME = NULL;
    IF COL_LENGTH(@Tabla, 'estdo_sup') IS NOT NULL
        SET @EstadoColumn = N'estdo_sup';
    ELSE IF COL_LENGTH(@Tabla, 'estado_sup') IS NOT NULL
        SET @EstadoColumn = N'estado_sup';

    IF @EstadoColumn IS NULL
    BEGIN
        SELECT CAST(0 AS INT) AS actualizados;
        RETURN;
    END

    DECLARE @HasEliminado BIT = CASE WHEN COL_LENGTH(@Tabla, 'E_Eliminado') IS NULL THEN 0 ELSE 1 END;
    DECLARE @Sql NVARCHAR(MAX);

    SET @Sql = N'
    UPDATE s
    SET
        s.FotoBoletaSupervision = @FotoBoletaSupervision,
        s.FotoCanalesPilotos = @FotoCanalesPilos,
        s.FotoNivelesDocsis = @FotoNivelesDocsis,
        s.FotoMedicionRuido = @FotoMedicionRuido,
        s.FotoBarridoCanales = @FotoBarridoCanales,
        s.FotoObservacion1 = @FotoObservacion1,
        s.FotoObservacion2 = @FotoObservacion2,
        s.FotoObservacion3 = @FotoObservacion3,
        s.FotoObservacion4 = @FotoObservacion4,
        s.Observacion = @Observacion,
        s.DescripcionAdicionalObservacion = @DescripcionAdicionalObservacion,
        s.Ubicacion = @Ubicacion,
        s.' + QUOTENAME(@EstadoColumn) + N' = N''completado''
    FROM ' + @Tabla + N' s
    WHERE s.Id_Supervision = @IdSupervision
      AND s.Id_Supervisor = @IdSupervisor
      AND LOWER(LTRIM(RTRIM(ISNULL(s.' + QUOTENAME(@EstadoColumn) + N', N'''')))) IN (N''pendiente'', N''pendientes'') ' +
      CASE WHEN @HasEliminado = 1 THEN N' AND ISNULL(s.E_Eliminado,0) = 0 ' ELSE N'' END + N';

    SELECT @@ROWCOUNT AS actualizados;';

    EXEC sp_executesql
        @Sql,
        N'@IdSupervision NVARCHAR(50),
          @IdSupervisor NVARCHAR(50),
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
          @Ubicacion NVARCHAR(300)',
        @IdSupervision = @IdSupervision,
        @IdSupervisor = @IdSupervisor,
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
        @Ubicacion = @Ubicacion;
END
GO
