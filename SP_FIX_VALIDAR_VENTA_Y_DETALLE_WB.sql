-- Ajuste de SP para validar detalle de venta OT web.
-- Cambio solicitado: considerar tambien registros de tbl_CodigoVentaCargoUsuario.
-- Ejecutar en la BD de cada sucursal.

IF OBJECT_ID('dbo.spx_ValidarVentaYDetallewb', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ValidarVentaYDetallewb AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_ValidarVentaYDetallewb
    @Fecha DATETIME,
    @NroOT INT,
    @NumeroCliente INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CantidadVentas INT = 0;
    DECLARE @CantidadDetallesVenta INT = 0;
    DECLARE @CantidadDetallesCargoUsuario INT = 0;
    DECLARE @CantidadDetalles INT = 0;

    SELECT @CantidadVentas = COUNT(1)
    FROM dbo.tbl_Venta v
    WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
      AND v.OrdenTrabajo = @NroOT
      AND v.CodigoCliente = @NumeroCliente
      AND ISNULL(v.E_Eliminado, 0) = 0;

    IF (@CantidadVentas > 0)
    BEGIN
        SELECT @CantidadDetallesVenta = COUNT(1)
        FROM dbo.tbl_CodigoVenta cv
        INNER JOIN dbo.tbl_Venta v
            ON v.Id_Venta = cv.Id_Venta
        WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
          AND v.OrdenTrabajo = @NroOT
          AND v.CodigoCliente = @NumeroCliente
          AND ISNULL(v.E_Eliminado, 0) = 0
          AND ISNULL(cv.E_Eliminado, 0) = 0;

        SELECT @CantidadDetallesCargoUsuario = COUNT(1)
        FROM dbo.tbl_CodigoVentaCargoUsuario cvu
        INNER JOIN dbo.tbl_Venta v
            ON v.Id_Venta = cvu.Id_Venta
        WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
          AND v.OrdenTrabajo = @NroOT
          AND v.CodigoCliente = @NumeroCliente
          AND ISNULL(v.E_Eliminado, 0) = 0
          AND ISNULL(cvu.E_Eliminado, 0) = 0;
    END

    SET @CantidadDetalles = ISNULL(@CantidadDetallesVenta, 0) + ISNULL(@CantidadDetallesCargoUsuario, 0);

    SELECT
        CONVERT(DATE, @Fecha) AS Fecha,
        @NroOT AS NroOT,
        @NumeroCliente AS NumeroCliente,
        CASE WHEN @CantidadVentas > 0 THEN 1 ELSE 0 END AS ExisteVenta,
        @CantidadVentas AS CantidadVentas,
        CASE WHEN @CantidadDetalles > 0 THEN 1 ELSE 0 END AS TieneDetalleEnCodigoVenta,
        @CantidadDetalles AS CantidadDetalles;
END
GO
