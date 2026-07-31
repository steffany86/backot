/*
  Indices para acelerar Verificacion Boleta Digital.
  No modifica SPs ni datos. Solo crea indices si no existen y si las columnas requeridas estan presentes.
*/

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_WARNINGS ON;
SET ANSI_PADDING ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

/* Ejecutar en la BD central donde existe dbo.tbl_BO_CITA_MAKIRO_Historial. */
IF OBJECT_ID(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Vigente') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Id_BO_CITA_MAKIRO_Historial') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.tbl_BO_CITA_MAKIRO_Historial')
         AND name = N'IX_BO_CITA_MAKIRO_Hist_Boleta_Vigente'
   )
BEGIN
    CREATE INDEX IX_BO_CITA_MAKIRO_Hist_Boleta_Vigente
    ON dbo.tbl_BO_CITA_MAKIRO_Historial (Vigente, Id_BO_CITA_MAKIRO_Historial)
    WHERE Vigente = 2;
END;
GO

IF OBJECT_ID(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Fecha_Ejecucion') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Vigente') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'OT_FISICA') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'cliente_nro') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'OT') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Estado') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_BO_CITA_MAKIRO_Historial', N'Id_BO_CITA_MAKIRO_Historial') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.tbl_BO_CITA_MAKIRO_Historial')
         AND name = N'IX_BO_CITA_MAKIRO_Hist_Boleta_Fecha'
   )
BEGIN
    CREATE INDEX IX_BO_CITA_MAKIRO_Hist_Boleta_Fecha
    ON dbo.tbl_BO_CITA_MAKIRO_Historial (Fecha_Ejecucion, Vigente)
    INCLUDE (cliente_nro, OT, OT_FISICA, Estado, Id_BO_CITA_MAKIRO_Historial)
    WHERE Vigente = 2 AND OT_FISICA IS NOT NULL;
END;
GO

/* Ejecutar en cada BD operativa donde existe dbo.tbl_Venta. */
IF OBJECT_ID(N'dbo.tbl_Venta', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'Fecha_Ejecucion') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'E_Eliminado') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'Id_Venta') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'Id_Ruta') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'OrdenTrabajo') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'CodigoCliente') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'Origen') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'RutaPdf') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_Venta', N'todoOk') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.tbl_Venta')
         AND name = N'IX_Venta_Boleta_Fecha'
   )
BEGIN
    CREATE INDEX IX_Venta_Boleta_Fecha
    ON dbo.tbl_Venta (E_Eliminado, Fecha_Ejecucion, Id_Venta DESC)
    INCLUDE (Id_Ruta, OrdenTrabajo, CodigoCliente, Origen, RutaPdf, todoOk);
END;
GO

IF OBJECT_ID(N'dbo.tbl_ventaArchivoDigital', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_ventaArchivoDigital', N'id_venta') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_ventaArchivoDigital', N'fechaRegistro') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_ventaArchivoDigital', N'id') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_ventaArchivoDigital', N'nombreArchivoNuevo') IS NOT NULL
   AND COL_LENGTH(N'dbo.tbl_ventaArchivoDigital', N'comparacion') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.tbl_ventaArchivoDigital')
         AND name = N'IX_ventaArchivoDigital_UltimoPorVenta'
   )
BEGIN
    CREATE INDEX IX_ventaArchivoDigital_UltimoPorVenta
    ON dbo.tbl_ventaArchivoDigital (id_venta, fechaRegistro DESC, id DESC)
    INCLUDE (nombreArchivoNuevo, comparacion);
END;
GO
