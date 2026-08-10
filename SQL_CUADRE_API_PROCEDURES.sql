/*
   Procedimientos usados por POST /cuadre/tecnico/registrar.
   La API no inserta directamente en las tablas: la transaccion Java ejecuta
   estos procedimientos sobre la misma conexion y hace COMMIT/ROLLBACK.
*/

CREATE OR ALTER PROCEDURE dbo.spx_RegistrarCuadre
    @Id_Ruta INT,
    @Id_Vendedor INT,
    @Id_Usuario INT,
    @Fecha DATETIME,
    @Observacion NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.tbl_Cuadre
        (Id_Ruta, Id_Vendedor, Id_Usuario, Fecha, Fecha_Registro, Observacion, Total, E_Eliminado)
    VALUES
        (@Id_Ruta, @Id_Vendedor, @Id_Usuario, @Fecha, GETDATE(), ISNULL(@Observacion, N''), 0, 0);

    SELECT CONVERT(INT, SCOPE_IDENTITY()) AS Id_Cuadre;
END;
GO

CREATE OR ALTER PROCEDURE dbo.spx_RegistrarCodigoCuadre
    @Id_Cuadre INT,
    @Id_Producto INT,
    @ItemsSobrantes DECIMAL(18, 4),
    @ItemsVendidos DECIMAL(18, 4),
    @ItemsRetirados DECIMAL(18, 4),
    @Precio DECIMAL(18, 4),
    @TotalVendidos DECIMAL(18, 4)
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.tbl_CodigoCuadre
        (Id_Cuadre, Id_Producto, ItemsSobrantes, ItemsVendidos, ItemsRetirados, Precio, TotalVendidos, E_Eliminado)
    VALUES
        (@Id_Cuadre, @Id_Producto, @ItemsSobrantes, @ItemsVendidos, @ItemsRetirados, @Precio, @TotalVendidos, 0);
END;
GO

CREATE OR ALTER PROCEDURE dbo.spx_RegistrarSaldoRetiro
    @Id_Cuadre INT,
    @Id_Venta INT = NULL,
    @Id_Devolucion INT = NULL,
    @NroOrdenTrabajo NVARCHAR(50) = NULL,
    @Fecha DATETIME = NULL,
    @Id_Producto INT,
    @Nombre NVARCHAR(250) = NULL,
    @Cod_Inicio NVARCHAR(250) = NULL,
    @ChipID NVARCHAR(250) = NULL,
    @Cantidad DECIMAL(18, 4)
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.tbl_SaldoRetiro
        (Id_Cuadre, Id_Venta, Id_Devolucion, NroOrdenTrabajo, Fecha, Id_Producto,
         Nombre, Cod_Inicio, ChipID, Cantidad, E_Eliminado)
    VALUES
        (@Id_Cuadre, @Id_Venta, @Id_Devolucion, @NroOrdenTrabajo, ISNULL(@Fecha, GETDATE()), @Id_Producto,
         @Nombre, @Cod_Inicio, @ChipID, @Cantidad, 0);
END;
GO

CREATE OR ALTER PROCEDURE dbo.spx_ActualizarSaldoTarjetaCuadre
    @Id_Ruta INT,
    @Id_Producto INT,
    @Cantidad DECIMAL(18, 4)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_saldotarjetas
       SET Cantidad = @Cantidad
     WHERE Id_Ruta = @Id_Ruta
       AND Id_Producto = @Id_Producto;
END;
GO
