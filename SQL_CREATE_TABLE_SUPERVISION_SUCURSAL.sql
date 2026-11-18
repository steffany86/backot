-- ========================================================================
-- CREAR tbl_SupervisionSucursal
-- ========================================================================
-- Permite otorgarle a un usuario (supervisor) permiso sobre una sucursal
-- adicional a la suya, para que pueda ver/agendar supervisiones de personal
-- de esa sucursal sin necesidad de crear un usuario nuevo para cada una.
--   id_sucursalUsuario / nombreSucursalUsuario: sucursal propia del usuario
--                                                (la que se usa para listarlo).
--   id_usuario / nombreUsuario: el supervisor al que se le da el permiso.
--   id_SucursalPermiso / nombreSucursalPermiso: la sucursal adicional a la
--                                                que se le da acceso.
-- Script idempotente: se puede correr varias veces sin duplicar la tabla.
-- ========================================================================

USE [BD_TigoHogar]
GO

IF OBJECT_ID('dbo.tbl_SupervisionSucursal', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tbl_SupervisionSucursal (
        id INT PRIMARY KEY IDENTITY,
        id_sucursalUsuario INT NULL,
        nombreSucursalUsuario NVARCHAR(150) NULL,
        id_usuario INT NOT NULL,
        nombreUsuario NVARCHAR(150) NULL,
        id_SucursalPermiso INT NOT NULL,
        nombreSucursalPermiso NVARCHAR(150) NULL,
        fechaRegistro DATETIME NOT NULL DEFAULT GETDATE(),
        id_UsuarioRegistro INT NULL,
        e_eliminado BIT NOT NULL DEFAULT 0
    );
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'tbl_SupervisionSucursal creada/confirmada.';
PRINT '========================================';
