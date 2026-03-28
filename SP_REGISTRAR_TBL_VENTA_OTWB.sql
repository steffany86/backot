/*
SP para registrar una venta/OT en dbo.tbl_venta incluyendo latitud y longitud.

Uso:
EXEC dbo.spx_RegistrarVentaParaRegistroOTwb
    @Id_Usuario = 10,
    @Id_Vendedor = 354,
    @Id_Grupo = 6,
    @Id_TipoServicio = 7,
    @OrdenTrabajo = 28761686,
    @Observacion = N'Registro desde OT web',
    @Id_Estado = 1,
    @Id_Sucursal = 9,
    @CodigoCliente = 2589990,
    @Nombre = N'KEVIN GEOVANI BARROSO NAVA',
    @Latitud = -17.783333,
    @Longitud = -63.182222,
    @Id_UsuarioE = 10;
*/

CREATE OR ALTER PROCEDURE dbo.spx_RegistrarVentaParaRegistroOTwb
    @Id_Usuario INT,
    @Id_Vendedor INT,
    @Id_Grupo INT,                 -- Antes Id_Ruta
    @Id_TipoServicio INT,
    @OrdenTrabajo INT,
    @Observacion NVARCHAR(MAX) = NULL,
    @Total DECIMAL(18, 2) = 0,
    @Id_UsuarioE INT = NULL,
    @E_Eliminado BIT = 0,
    @Nombre NVARCHAR(250) = NULL,
    @Origen NVARCHAR(100) = NULL,
    @Id_Estado INT,
    @Id_Sucursal INT,
    @CodigoCliente INT,
    @TieneObservacion BIT = 0,
    @Latitud DECIMAL(9, 6) = NULL,
    @Longitud DECIMAL(9, 6) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        -- Validaciones de negocio basicas.
        IF @Id_Usuario IS NULL OR @Id_Usuario <= 0
            THROW 51001, 'Id_Usuario es requerido y debe ser mayor a 0.', 1;
        IF @Id_Vendedor IS NULL OR @Id_Vendedor <= 0
            THROW 51002, 'Id_Vendedor es requerido y debe ser mayor a 0.', 1;
        IF @Id_Grupo IS NULL OR @Id_Grupo <= 0
            THROW 51003, 'Id_Grupo es requerido y debe ser mayor a 0.', 1;
        IF @Id_TipoServicio IS NULL OR @Id_TipoServicio <= 0
            THROW 51004, 'Id_TipoServicio es requerido y debe ser mayor a 0.', 1;
        IF @OrdenTrabajo IS NULL OR @OrdenTrabajo <= 0
            THROW 51005, 'OrdenTrabajo es requerido y debe ser mayor a 0.', 1;
        IF @Id_Estado IS NULL OR @Id_Estado <= 0
            THROW 51006, 'Id_Estado es requerido y debe ser mayor a 0.', 1;
        IF @Id_Sucursal IS NULL OR @Id_Sucursal <= 0
            THROW 51007, 'Id_Sucursal es requerido y debe ser mayor a 0.', 1;
        IF @CodigoCliente IS NULL OR @CodigoCliente <= 0
            THROW 51008, 'CodigoCliente es requerido y debe ser mayor a 0.', 1;
        IF @Origen IS NULL OR LTRIM(RTRIM(@Origen)) = ''
            THROW 51013, 'Origen es requerido.', 1;

        IF @Latitud IS NOT NULL AND (@Latitud < -90 OR @Latitud > 90)
            THROW 51009, 'Latitud fuera de rango (-90 a 90).', 1;
        IF @Longitud IS NOT NULL AND (@Longitud < -180 OR @Longitud > 180)
            THROW 51010, 'Longitud fuera de rango (-180 a 180).', 1;

        -- Verifica existencia de columnas de geolocalizacion.
        IF COL_LENGTH('dbo.tbl_venta', 'Latitud') IS NULL
            THROW 51011, 'La columna Latitud no existe en dbo.tbl_venta.', 1;
        IF COL_LENGTH('dbo.tbl_venta', 'Longitud') IS NULL
            THROW 51012, 'La columna Longitud no existe en dbo.tbl_venta.', 1;

        BEGIN TRANSACTION;

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

        DECLARE @Id_Venta INT = CAST(SCOPE_IDENTITY() AS INT);

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
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrNum INT = ERROR_NUMBER();
        DECLARE @ErrState INT = ERROR_STATE();

        THROW @ErrNum, @ErrMsg, @ErrState;
    END CATCH
END;
GO
