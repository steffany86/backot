SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

USE [BD_TigoHogar]
GO

IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_tbl_LlamadaAtencion_tbl_Users'
      AND parent_object_id = OBJECT_ID('dbo.tbl_LlamadaAtencion')
)
BEGIN
    ALTER TABLE dbo.tbl_LlamadaAtencion DROP CONSTRAINT FK_tbl_LlamadaAtencion_tbl_Users;
END
GO

IF OBJECT_ID('dbo.spx_ListarLlamadaAtencion', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ListarLlamadaAtencion @IdTecnico NVARCHAR(16)=NULL, @FechaDesde DATE=NULL, @FechaHasta DATE=NULL, @Limite INT=200 AS BEGIN SET NOCOUNT ON; SELECT 1 AS placeholder; END');
END
GO

ALTER PROC dbo.spx_ListarLlamadaAtencion
    @IdTecnico NVARCHAR(16) = NULL,
    @FechaDesde DATE = NULL,
    @FechaHasta DATE = NULL,
    @Limite INT = 200
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Top INT = ISNULL(@Limite, 200);
    IF (@Top <= 0) SET @Top = 200;
    IF (@Top > 1000) SET @Top = 1000;

    SELECT TOP (@Top)
        la.Id_LlamadaAtencion AS idLlamadaAtencion,
        la.Id_Tecnico AS idTecnico,
        ISNULL(NULLIF(LTRIM(RTRIM(u.Nombre)), ''), la.Id_Tecnico) AS tecnico,
        la.Id_TipoComunicacion AS idTipoComunicacion,
        tc.TipoComunicacion AS tipoComunicacion,
        la.Fecha_Registro AS fechaRegistro,
        la.Motivo AS motivo,
        la.Descripcion AS descripcion,
        la.ComentarioColaborador AS comentarioColaborador,
        la.Acuerdos AS acuerdos,
        la.FechaSeguimiento AS fechaSeguimiento,
        la.FirmaTecnico AS firmaTecnico,
        la.FirmaTestigo AS firmaTestigo
    FROM dbo.tbl_LlamadaAtencion la
    LEFT JOIN dbo.tbl_Users u ON u.Id_Usuario = la.Id_Tecnico
    LEFT JOIN dbo.tbl_TipoComunicacion tc ON tc.Id_TipoComunicacion = la.Id_TipoComunicacion
    WHERE (@IdTecnico IS NULL OR LTRIM(RTRIM(@IdTecnico)) = '' OR la.Id_Tecnico = LTRIM(RTRIM(@IdTecnico)))
      AND (@FechaDesde IS NULL OR CAST(la.Fecha_Registro AS DATE) >= @FechaDesde)
      AND (@FechaHasta IS NULL OR CAST(la.Fecha_Registro AS DATE) <= @FechaHasta)
    ORDER BY la.Fecha_Registro DESC, la.Id_LlamadaAtencion DESC;
END
GO

IF OBJECT_ID('dbo.spx_RegistrarLlamadaAtencion', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_RegistrarLlamadaAtencion @IdTecnico NVARCHAR(16), @IdTipoComunicacion NVARCHAR(16), @Motivo NVARCHAR(500), @Descripcion NVARCHAR(500)=NULL, @ComentarioColaborador NVARCHAR(500)=NULL, @Acuerdos NVARCHAR(500)=NULL, @FechaSeguimiento DATETIME=NULL, @FirmaTecnico NVARCHAR(500)=NULL, @FirmaTestigo NVARCHAR(500)=NULL AS BEGIN SET NOCOUNT ON; SELECT 1 AS placeholder; END');
END
GO

ALTER PROC dbo.spx_RegistrarLlamadaAtencion
    @IdTecnico NVARCHAR(16),
    @IdTipoComunicacion NVARCHAR(16),
    @Motivo NVARCHAR(500),
    @Descripcion NVARCHAR(500) = NULL,
    @ComentarioColaborador NVARCHAR(500) = NULL,
    @Acuerdos NVARCHAR(500) = NULL,
    @FechaSeguimiento DATETIME = NULL,
    @FirmaTecnico NVARCHAR(500) = NULL,
    @FirmaTestigo NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdTecnicoNorm NVARCHAR(16) = NULLIF(LTRIM(RTRIM(@IdTecnico)), '');
    DECLARE @IdTipoNorm NVARCHAR(16) = NULLIF(LTRIM(RTRIM(@IdTipoComunicacion)), '');
    DECLARE @MotivoNorm NVARCHAR(500) = NULLIF(LTRIM(RTRIM(@Motivo)), '');

    IF @IdTecnicoNorm IS NULL
    BEGIN
        RAISERROR('IdTecnico es requerido.', 16, 1);
        RETURN;
    END

    IF @IdTipoNorm IS NULL
    BEGIN
        RAISERROR('IdTipoComunicacion es requerido.', 16, 1);
        RETURN;
    END

    IF @MotivoNorm IS NULL
    BEGIN
        RAISERROR('Motivo es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.tbl_TipoComunicacion WHERE Id_TipoComunicacion = @IdTipoNorm)
    BEGIN
        RAISERROR('El tipo de comunicacion seleccionado no existe.', 16, 1);
        RETURN;
    END

    DECLARE @NuevoId NVARCHAR(16) = NULL;
    DECLARE @Intentos INT = 0;

    WHILE (@Intentos < 20)
    BEGIN
        SET @Intentos = @Intentos + 1;
        SET @NuevoId = RIGHT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 8);

        IF NOT EXISTS (SELECT 1 FROM dbo.tbl_LlamadaAtencion WHERE Id_LlamadaAtencion = @NuevoId)
            BREAK;
    END

    IF @NuevoId IS NULL OR EXISTS (SELECT 1 FROM dbo.tbl_LlamadaAtencion WHERE Id_LlamadaAtencion = @NuevoId)
    BEGIN
        RAISERROR('No se pudo generar Id_LlamadaAtencion unico.', 16, 1);
        RETURN;
    END

    INSERT INTO dbo.tbl_LlamadaAtencion (
        Id_LlamadaAtencion,
        Id_Tecnico,
        Id_TipoComunicacion,
        Fecha_Registro,
        Motivo,
        Descripcion,
        ComentarioColaborador,
        Acuerdos,
        FechaSeguimiento,
        FirmaTecnico,
        FirmaTestigo
    )
    VALUES (
        @NuevoId,
        @IdTecnicoNorm,
        @IdTipoNorm,
        GETDATE(),
        @MotivoNorm,
        NULLIF(LTRIM(RTRIM(@Descripcion)), ''),
        NULLIF(LTRIM(RTRIM(@ComentarioColaborador)), ''),
        NULLIF(LTRIM(RTRIM(@Acuerdos)), ''),
        @FechaSeguimiento,
        NULLIF(LTRIM(RTRIM(@FirmaTecnico)), ''),
        NULLIF(LTRIM(RTRIM(@FirmaTestigo)), '')
    );

    SELECT @NuevoId AS idLlamadaAtencion;
END
GO

IF OBJECT_ID('dbo.spx_ListarTipoComunicacionLlamadaAtencion', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ListarTipoComunicacionLlamadaAtencion AS BEGIN SET NOCOUNT ON; SELECT 1 AS placeholder; END');
END
GO

ALTER PROC dbo.spx_ListarTipoComunicacionLlamadaAtencion
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        Id_TipoComunicacion AS idTipoComunicacion,
        TipoComunicacion AS tipoComunicacion
    FROM dbo.tbl_TipoComunicacion
    ORDER BY TipoComunicacion;
END
GO
