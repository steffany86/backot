/* =====================================================================
   BACKUP COMPLETO DE UNA BASE DE DATOS  (SQL Server 2008 / 2008 R2 / 2012)

   Que hace:
     1) Muestra el tamano de la base y el espacio libre de los discos del servidor.
     2) Comprueba que la carpeta destino exista y que SQL Server pueda verla.
     3) Genera el backup completo (COPY_ONLY: no altera la cadena de backups existente,
        con CHECKSUM para detectar paginas danadas) en un archivo con fecha y hora.
     4) Verifica el archivo generado (RESTORE VERIFYONLY).

   IMPORTANTE
     - Si el disco del servidor esta lleno, el destino DEBE ser otro disco u otro equipo
       (por ejemplo una carpeta compartida \\172.16.0.13\Backups).
     - El destino lo escribe el SERVICIO de SQL Server, no tu usuario: la cuenta con que corre
       el servicio (SQL Server Configuration Manager) necesita permiso de ESCRITURA en esa carpeta
       y en el recurso compartido.
     - COMPRESSION: SQL 2008 solo en Enterprise; SQL 2008 R2 y 2012 tambien en Standard.
       Si da error "COMPRESSION is not supported", pon @Comprimir = 0.
     - Un backup grande puede tardar y consume lectura de disco; mejor en horario de poco uso.

   Uso: ajusta las 4 variables de abajo; dejalo con @Simular = 1 la primera vez (no hace backup),
        revisa lo que muestra y luego pon @Simular = 0.
   ===================================================================== */
USE master;
GO
SET NOCOUNT ON;

DECLARE @BaseDatos NVARCHAR(128) = N'bd_tigohogar';          -- base que se respalda
DECLARE @Carpeta   NVARCHAR(260) = N'\\172.16.0.13\Backups'; -- SIN barra final; debe existir
DECLARE @Comprimir BIT           = 1;                         -- 1 = comprimido (menos espacio)
DECLARE @Simular   BIT           = 1;                         -- 1 = solo revisa; 0 = hace el backup

-- 0) La base existe?
IF DB_ID(@BaseDatos) IS NULL
BEGIN
    RAISERROR('La base de datos indicada no existe en este servidor.', 16, 1);
    RETURN;
END

-- 1) Tamano de la base y espacio libre en discos
SELECT DB_NAME(database_id) AS base,
       CAST(SUM(CASE WHEN type = 0 THEN size END) * 8.0 / 1024 AS DECIMAL(18,1)) AS datos_MB,
       CAST(SUM(CASE WHEN type = 1 THEN size END) * 8.0 / 1024 AS DECIMAL(18,1)) AS log_MB
FROM sys.master_files
WHERE database_id = DB_ID(@BaseDatos)
GROUP BY database_id;

PRINT 'Espacio libre por unidad en ESTE servidor (MB libres):';
EXEC master..xp_fixeddrives;

-- 2) La carpeta destino es visible para SQL Server?
DECLARE @chk TABLE (archivo_existe INT, es_carpeta INT, padre_existe INT);
INSERT INTO @chk EXEC master..xp_fileexist @Carpeta;
IF NOT EXISTS (SELECT 1 FROM @chk WHERE es_carpeta = 1)
BEGIN
    RAISERROR('SQL Server no ve la carpeta destino. Revisa que exista y que la cuenta del servicio SQL tenga permiso de escritura.', 16, 1);
    RETURN;
END

-- 3) Nombre del archivo: base_AAAAMMDD_HHMMSS.bak
DECLARE @Archivo NVARCHAR(400) =
    @Carpeta + N'\' + @BaseDatos + N'_' + CONVERT(NVARCHAR(8), GETDATE(), 112) + N'_' +
    REPLACE(CONVERT(NVARCHAR(8), GETDATE(), 108), ':', '') + N'.bak';

DECLARE @sql NVARCHAR(MAX) =
    N'BACKUP DATABASE ' + QUOTENAME(@BaseDatos) + N' TO DISK = N''' + @Archivo + N''' ' +
    N'WITH COPY_ONLY, CHECKSUM, INIT, STATS = 10, NAME = N''Backup ' + @BaseDatos + N' ' +
    CONVERT(NVARCHAR(20), GETDATE(), 120) + N'''' +
    CASE WHEN @Comprimir = 1 THEN N', COMPRESSION' ELSE N'' END + N';';

PRINT 'Archivo destino : ' + @Archivo;
PRINT 'Comando         : ' + @sql;

IF @Simular = 1
BEGIN
    PRINT 'SIMULACION: no se hizo ningun backup. Pon @Simular = 0 para ejecutarlo.';
    RETURN;
END

-- 4) Backup + verificacion
EXEC (@sql);

DECLARE @verif NVARCHAR(MAX) =
    N'RESTORE VERIFYONLY FROM DISK = N''' + @Archivo + N''' WITH CHECKSUM;';
EXEC (@verif);

PRINT 'Backup terminado y verificado: ' + @Archivo;
GO

/* ---------------------------------------------------------------------
   PARA RESTAURAR (en otro servidor o base; NO lo ejecutes sobre la base original sin querer):

   RESTORE FILELISTONLY FROM DISK = N'\\172.16.0.13\Backups\bd_tigohogar_AAAAMMDD_HHMMSS.bak';  -- nombres logicos
   RESTORE DATABASE bd_tigohogar_copia
     FROM DISK = N'\\172.16.0.13\Backups\bd_tigohogar_AAAAMMDD_HHMMSS.bak'
     WITH MOVE N'nombre_logico_datos' TO N'D:\Datos\bd_tigohogar_copia.mdf',
          MOVE N'nombre_logico_log'   TO N'D:\Datos\bd_tigohogar_copia_log.ldf',
          STATS = 10;
   --------------------------------------------------------------------- */
