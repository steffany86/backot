/* =====================================================================
   LIBERAR ESPACIO EN EL ORIGEN: poner en NULL las fotos base64 de bd_tigohogar.tbl_Supervision
   SOLO cuando la foto ya esta migrada y el archivo EXISTE en el disco del destino.

   Se ejecuta en el DESTINO (172.16.0.13, BDControlOrdenes, SQL 2008), usando el servidor
   vinculado SRV2012 (necesita permiso de UPDATE sobre el origen por ese vinculo).

   Por cada foto del origen (base64, > 1000 bytes) comprueba:
     1) que el destino tenga la ruta de esa foto para el mismo id,
     2) que el archivo exista en @Raiz (xp_fileexist, corre en el servidor destino),
   y solo entonces pone esa columna en NULL en el origen. No borra filas.

   Antes: backup verificado del origen (ya hecho: D:\RespaldoSQL\bd_tigohogar_20261116_100226.bak)
          y haber corrido la ultima pasada de migracion (pasos 1-2-3) si hubo registros nuevos.
   Uso: @Simular = 1 (solo cuenta), luego @Limite = 20 con @Simular = 0 para probar, luego @Limite = 0.
   Despues hay que compactar el origen (ver NOTA al final).
   ===================================================================== */
USE BDControlOrdenes;
GO
SET NOCOUNT ON;

DECLARE @Simular BIT = 1;
DECLARE @Limite  INT = 20;      -- cuantas fotos limpiar como maximo (0 = todas)
DECLARE @Raiz    NVARCHAR(260) = N'D:\BDControlOrdenesImagenes\Supervision\';
DECLARE @Origen  NVARCHAR(200) = N'SRV2012.bd_tigohogar.dbo.tbl_Supervision';

IF OBJECT_ID('tempdb..#cols') IS NOT NULL DROP TABLE #cols;
IF OBJECT_ID('tempdb..#lens') IS NOT NULL DROP TABLE #lens;
CREATE TABLE #cols (c NVARCHAR(100));
INSERT #cols VALUES ('FotoBoletaSupervision'),('FotoCanalesPilotos'),('FotoNivelesDocsis'),('FotoMedicionRuido'),
  ('FotoBarridoCanales'),('FotoObservacion1'),('FotoObservacion2'),('FotoObservacion3'),('FotoObservacion4');
CREATE TABLE #lens (id INT, c NVARCHAR(100), ruta NVARCHAR(400) NULL, existe INT NULL);

-- 1) Fotos del origen que siguen en base64 y tienen ruta en el destino
DECLARE @col NVARCHAR(100), @sql NVARCHAR(MAX);
DECLARE cc CURSOR LOCAL FAST_FORWARD FOR SELECT c FROM #cols;
OPEN cc; FETCH NEXT FROM cc INTO @col;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'INSERT #lens (id, c, ruta) SELECT o.id, N''' + @col + N''', d.' + QUOTENAME(@col) + N'
      FROM ' + @Origen + N' o JOIN dbo.tbl_Supervision d ON d.id = o.id
      WHERE DATALENGTH(o.' + QUOTENAME(@col) + N') > 1000
        AND d.' + QUOTENAME(@col) + N' IS NOT NULL AND DATALENGTH(d.' + QUOTENAME(@col) + N') BETWEEN 5 AND 400';
    EXEC (@sql);
    FETCH NEXT FROM cc INTO @col;
END
CLOSE cc; DEALLOCATE cc;

-- 2) Comprobar que el archivo existe
DECLARE @id INT, @ruta NVARCHAR(400), @f NVARCHAR(700);
DECLARE @chk TABLE (a INT, b INT, c INT);
DECLARE cf CURSOR LOCAL FAST_FORWARD FOR SELECT id, c, ruta FROM #lens;
OPEN cf; FETCH NEXT FROM cf INTO @id, @col, @ruta;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @f = @Raiz + REPLACE(@ruta, '/', '\');
    DELETE @chk;
    INSERT @chk EXEC master..xp_fileexist @f;
    UPDATE #lens SET existe = (SELECT TOP 1 a FROM @chk) WHERE id = @id AND c = @col;
    FETCH NEXT FROM cf INTO @id, @col, @ruta;
END
CLOSE cf; DEALLOCATE cf;

SELECT COUNT(*) AS fotos_base64_con_ruta,
       SUM(CASE WHEN existe = 1 THEN 1 ELSE 0 END) AS archivo_existe,
       SUM(CASE WHEN ISNULL(existe,0) <> 1 THEN 1 ELSE 0 END) AS archivo_NO_existe_no_se_toca
FROM #lens;

SELECT TOP 20 id, c AS columna, ruta FROM #lens WHERE ISNULL(existe,0) <> 1 ORDER BY id;

IF @Simular = 1
BEGIN
    PRINT 'SIMULACION: no se cambio nada en el origen. Pon @Simular = 0 para limpiar.';
    RETURN;
END

-- 3) Poner NULL en el origen (solo las que tienen archivo)
DECLARE @n INT = 0;
DECLARE cu CURSOR LOCAL FAST_FORWARD FOR
    SELECT TOP (CASE WHEN @Limite > 0 THEN @Limite ELSE 2147483647 END) id, c FROM #lens WHERE existe = 1 ORDER BY id;
OPEN cu; FETCH NEXT FROM cu INTO @id, @col;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'UPDATE ' + @Origen + N' SET ' + QUOTENAME(@col) + N' = NULL WHERE id = ' + CAST(@id AS NVARCHAR(20))
             + N' AND DATALENGTH(' + QUOTENAME(@col) + N') > 1000';
    EXEC (@sql);
    SET @n = @n + 1;
    IF @n % 100 = 0 RAISERROR('Limpiadas %d fotos', 0, 1, @n) WITH NOWAIT;
    FETCH NEXT FROM cu INTO @id, @col;
END
CLOSE cu; DEALLOCATE cu;
PRINT 'Fotos puestas en NULL en el origen: ' + CAST(@n AS VARCHAR(10));
GO

/* NOTA: poner NULL NO reduce el archivo .mdf; solo deja espacio libre dentro. Para recuperarlo,
   EN EL ORIGEN (SQL 2022, bd_tigohogar), con poca actividad y espacio libre para el proceso:
     -- ver cuanto espacio libre interno hay
     EXEC sp_spaceused;
     -- compactar los datos grandes de la tabla
     ALTER INDEX ALL ON dbo.tbl_Supervision REORGANIZE WITH (LOB_COMPACTION = ON);
     -- (o, si hay espacio: ALTER TABLE dbo.tbl_Supervision REBUILD;)
     -- y por ultimo devolver el espacio al disco
     DBCC SHRINKFILE (N'nombre_logico_datos', <tamano_objetivo_MB>);
   El nombre logico: SELECT name, size*8/1024 AS MB FROM sys.database_files; */
