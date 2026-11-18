-- =====================================================================
-- POST-MIGRACION DE FOTOS tbl_Supervision_bak  (SQL Server 2008, 172.16.0.13)
-- Ejecutar DESPUES de correr MigradorFotosSupervision.exe --aplicar
-- =====================================================================
USE bdControlOrdenes;
GO

-- 1) Verificar: todas las fotos deben ser rutas cortas (nada de base64 pendiente)
SELECT
    SUM(CASE WHEN LEN(FotoBoletaSupervision) > 300 THEN 1 ELSE 0 END) AS boleta_pendiente,
    MAX(LEN(FotoBoletaSupervision)) AS boleta_max_len
FROM dbo.tbl_Supervision_bak;

-- Ejemplo de como quedaron las rutas
SELECT TOP 10 Codigo, FechaRegistro, FotoBoletaSupervision, FotoObservacion1
FROM dbo.tbl_Supervision_bak
WHERE FotoBoletaSupervision IS NOT NULL;
GO

-- 2) Recuperar el espacio: los datos LOB solo se compactan con REBUILD
--    (necesita espacio libre temporal; con la BD en SIMPLE el log no crece tanto)
ALTER DATABASE bdControlOrdenes SET RECOVERY SIMPLE;
GO
ALTER TABLE dbo.tbl_Supervision_bak REBUILD;
GO

-- 3) Devolver el espacio al disco (ajusta el nombre logico: SELECT name FROM sys.database_files)
-- DBCC SHRINKFILE (N'bdControlOrdenes', 1024);   -- tamano objetivo en MB
-- DBCC SHRINKFILE (N'bdControlOrdenes_log', 256);
GO

-- 4) Resultado
EXEC sp_spaceused 'dbo.tbl_Supervision_bak';
GO

-- 5) OPCIONAL (solo cuando estes seguro): estrechar las columnas a rutas
-- ALTER TABLE dbo.tbl_Supervision_bak ALTER COLUMN FotoBoletaSupervision NVARCHAR(300) NULL;
-- (repetir para las otras columnas Foto*)
