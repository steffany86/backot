/*
SP para registrar una venta/OT en dbo.tbl_venta incluyendo validacion de Nro Orden unico.
Si el numero de orden ya existe, lanza error y no inserta.
*/

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID('dbo.spx_RegistrarVentaParaRegistroOTwb', 'P') IS NULL
BEGIN
    EXEC('CREATE PROCEDURE dbo.spx_RegistrarVentaParaRegistroOTwb AS BEGIN SET NOCOUNT ON; END');
END
GO

ALTER PROCEDURE dbo.spx_RegistrarVentaParaRegistroOTwb
    @Id_Usuario INT,
    @Id_Vendedor INT,
    @Id_Grupo INT,
    @Id_TipoServicio INT,
    @OrdenTrabajo INT,
    @Observacion NVARCHAR(MAX) = NULL,
    @Total DECIMAL(18, 2) = 0,
    @Id_UsuarioE INT = NULL,
    @E_Eliminado BIT = 0,
    @Nombre NVARCHAR(250) = NULL,
    @Origen NVARCHAR(100),
    @Id_Estado INT,
    @Id_Sucursal INT,
    @CodigoCliente INT,
    @TieneObservacion BIT = 0,
    @Latitud DECIMAL(9, 6) = NULL,
    @Longitud DECIMAL(9, 6) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET ANSI_NULLS ON;
    SET QUOTED_IDENTIFIER ON;
    SET ANSI_WARNINGS ON;
    SET ANSI_PADDING ON;
    SET CONCAT_NULL_YIELDS_NULL ON;
    SET ARITHABORT ON;
    SET NUMERIC_ROUNDABORT OFF;
    -- El endpoint puede ejecutar este SP dentro de una transaccion Spring.
    -- XACT_ABORT OFF permite volver al savepoint sin destruir la transaccion externa.
    SET XACT_ABORT OFF;

    DECLARE @TranCountInicial INT = @@TRANCOUNT;

    BEGIN TRY
        IF @Origen IS NULL OR LTRIM(RTRIM(@Origen)) = ''
        BEGIN
            RAISERROR('Origen es requerido.',16,1);
            RETURN;
        END

        IF COL_LENGTH('dbo.tbl_venta', 'Origen') IS NULL
        BEGIN
            RAISERROR('La columna Origen no existe en dbo.tbl_venta.',16,1);
            RETURN;
        END

        IF @TranCountInicial = 0
            BEGIN TRANSACTION;
        ELSE
            SAVE TRANSACTION RegistrarVentaOTwb;

        IF EXISTS (
            SELECT 1
            FROM dbo.tbl_venta WITH (UPDLOCK, HOLDLOCK)
            WHERE OrdenTrabajo = @OrdenTrabajo
              AND CodigoCliente = @CodigoCliente
              AND ISNULL(E_Eliminado, 0) = 0
        )
        BEGIN
            RAISERROR('Ya existe una OT activa registrada con el mismo numero de orden y codigo cliente.',16,1);
        END

        INSERT INTO dbo.tbl_venta (
            Id_Usuario,
            Id_Vendedor,
            Id_Ruta,
            Id_TipoServicio,
            Fecha_Ejecucion,
            Fecha_Registro,
            OrdenTrabajo,
            Observacion,
            Total,
            Id_UsuarioE,
            E_Eliminado,
            Nombre,
            Origen,
            Id_Estado,
            Id_Sucursal,
            CodigoCliente,
            TieneObservacion,
            Latitud,
            Longitud
        )
        VALUES (
            @Id_Usuario,
            @Id_Vendedor,
            @Id_Grupo,
            @Id_TipoServicio,
            GETDATE(),
            GETDATE(),
            @OrdenTrabajo,
            @Observacion,
            ISNULL(@Total, 0),
            @Id_UsuarioE,
            ISNULL(@E_Eliminado, 0),
            @Nombre,
            @Origen,
            @Id_Estado,
            @Id_Sucursal,
            @CodigoCliente,
            ISNULL(@TieneObservacion, 0),
            @Latitud,
            @Longitud
        );

        DECLARE @Id_Venta INT;
        SET @Id_Venta = CAST(SCOPE_IDENTITY() AS INT);

        IF @TranCountInicial = 0
            COMMIT TRANSACTION;

        SELECT
            @Id_Venta AS Id_Venta,
            @OrdenTrabajo AS OrdenTrabajo,
            @CodigoCliente AS CodigoCliente,
            @Id_Sucursal AS Id_Sucursal,
            @Origen AS Origen,
            @Latitud AS Latitud,
            @Longitud AS Longitud;
    END TRY
    BEGIN CATCH
        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrState INT = ERROR_STATE();

        IF XACT_STATE() = 1
        BEGIN
            IF @TranCountInicial = 0
                ROLLBACK TRANSACTION;
            ELSE
                ROLLBACK TRANSACTION RegistrarVentaOTwb;
        END
        ELSE IF XACT_STATE() = -1
        BEGIN
            -- SQL Server no permite volver a un savepoint si la transaccion quedo
            -- no confirmable; en ese caso solo es valido revertirla completamente.
            ROLLBACK TRANSACTION;
        END

        RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
    END CATCH
END;
GO
