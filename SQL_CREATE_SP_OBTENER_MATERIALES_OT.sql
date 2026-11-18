-- =====================================================================
-- SUPERVISION V2 - materiales registrados en una OT (cruce de materiales, cant_sistema)
-- Se ejecuta en la base de CADA sucursal (Santa Cruz, Montero, Tarija): el backend
-- llama al SP en la base a la que esta conectado. Compatible con SQL Server 2008.
--
-- Busca la venta (tbl_Venta) por OrdenTrabajo + CodigoCliente + tecnico (tbl_Vendedor,
-- por Nombre o SalesForce; el tecnico es opcional) y devuelve los materiales de
-- tbl_CodigoVenta:
--   - con Cod_Inicio (series: decodificador, modem, ...): 1 por cada serie
--   - sin Cod_Inicio: se suma Cantidad
-- Si la OT no existe devuelve 0 filas.
-- =====================================================================
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'[dbo].[spx_ObtenerMaterialesOT]', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[spx_ObtenerMaterialesOT] AS BEGIN SET NOCOUNT ON; END');
GO
ALTER PROCEDURE [dbo].[spx_ObtenerMaterialesOT]
    @OrdenTrabajo  NVARCHAR(100),
    @CodigoCliente NVARCHAR(100),
    @Tecnico       NVARCHAR(300) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Ot NVARCHAR(100), @Codigo NVARCHAR(100), @Tec NVARCHAR(300);
    SET @Ot     = LTRIM(RTRIM(ISNULL(@OrdenTrabajo, N'')));
    SET @Codigo = LTRIM(RTRIM(ISNULL(@CodigoCliente, N'')));
    SET @Tec    = LTRIM(RTRIM(ISNULL(@Tecnico, N'')));

    IF @Ot = N'' OR @Codigo = N''
    BEGIN
        RAISERROR('OrdenTrabajo y CodigoCliente son requeridos.', 16, 1);
        RETURN;
    END

    SELECT
        v.Id_Venta                              AS idVenta,
        LTRIM(RTRIM(v.OrdenTrabajo))            AS ordenTrabajo,
        LTRIM(RTRIM(v.CodigoCliente))           AS codigoCliente,
        vd.Nombre                               AS tecnico,
        p.Id_Producto                           AS idProducto,
        p.Nombre                                AS material,
        p.Alias                                 AS alias,
        SUM(CASE WHEN ISNULL(cv.Cod_Inicio, N'') <> N'' THEN 1 ELSE ISNULL(cv.Cantidad, 0) END) AS cantidad
    FROM dbo.tbl_Venta v
    INNER JOIN dbo.tbl_CodigoVenta cv ON cv.Id_Venta = v.Id_Venta AND ISNULL(cv.E_Eliminado, 0) = 0
    INNER JOIN dbo.tbl_Producto p     ON p.Id_Producto = cv.Id_Producto
    LEFT  JOIN dbo.tbl_Vendedor vd    ON vd.Id_Vendedor = v.Id_Vendedor
    WHERE ISNULL(v.E_Eliminado, 0) = 0
      AND LTRIM(RTRIM(v.OrdenTrabajo))  = @Ot
      AND LTRIM(RTRIM(v.CodigoCliente)) = @Codigo
      AND (
            @Tec = N''
            OR vd.Nombre     LIKE N'%' + @Tec + N'%'
            OR vd.SalesForce LIKE N'%' + @Tec + N'%'
          )
    GROUP BY v.Id_Venta, v.OrdenTrabajo, v.CodigoCliente, vd.Nombre, p.Id_Producto, p.Nombre, p.Alias
    ORDER BY p.Nombre;
END
GO

-- Prueba (cambia los valores):
-- EXEC dbo.spx_ObtenerMaterialesOT @OrdenTrabajo = N'29892660', @CodigoCliente = N'2742202', @Tecnico = N'LENIN GOMEZ';
