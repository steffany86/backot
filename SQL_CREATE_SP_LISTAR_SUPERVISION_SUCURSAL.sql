-- ========================================================================
-- CREAR spx_ListarSupervisionSucursal
-- ========================================================================
-- Devuelve las sucursales adicionales (permisos) asignadas a un usuario en
-- tbl_SupervisionSucursal. Usado por /supervisor/supervision para permitir
-- que un supervisor consulte/agende supervisiones de una sucursal distinta
-- a la de su sesion, siempre que tenga un permiso vigente en esa tabla.
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ListarSupervisionSucursal
    @id_usuario INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT *
    FROM dbo.tbl_SupervisionSucursal
    WHERE id_usuario = @id_usuario
      AND ISNULL(e_eliminado, 0) = 0;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ListarSupervisionSucursal creado/actualizado.';
PRINT '========================================';
