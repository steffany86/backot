/* =====================================================================
   MIGRACION tbl_Supervision (origen) -> tbl_Supervision (destino)   PASO 1 de 3
   Se ejecuta en el DESTINO: 172.16.0.13, base BDControlOrdenes (SQL 2008).

   Origen : bd_tigohogar.dbo.tbl_Supervision, por el servidor vinculado SRV2012.
            (comprobar con: SELECT name FROM sys.servers; si se llama distinto, cambiar @Origen abajo)
   Que hace: copia a dbo.tbl_SupervisionTemporal (MISMA estructura del origen, fotos aun en base64)
             las supervisiones del origen con id > @DesdeId que todavia NO existen en el destino.
             Se puede repetir cuantas veces haga falta: cada corrida vacia la temporal y vuelve a cargar
             solo lo que falta.
   Luego   : PASO 2 = migrador de fotos sobre la temporal (ver SQL_V2_MIGRAR_3_PASAR_A_SUPERVISION.sql).

   Parametros:
     @DesdeId  : solo ids mayores a este (299 = se migra desde el 300 en adelante)
     @Limite   : cuantas filas copiar (5 para probar; 0 = todas)
     @Simular  : 1 = solo muestra que copiaria, no escribe nada
   ===================================================================== */
USE BDControlOrdenes;
GO
SET NOCOUNT ON;

DECLARE @DesdeId INT = 1396;
DECLARE @Limite  INT = 10;    -- 5 para probar; luego 50 por tanda
DECLARE @Simular BIT = 0;--0 simulacion --
DECLARE @Origen  NVARCHAR(200) = N'SRV2012.bd_tigohogar.dbo.tbl_Supervision';

-- 1) Tabla temporal con la estructura exacta del origen (solo la primera vez).
IF OBJECT_ID('dbo.tbl_SupervisionTemporal') IS NULL AND @Simular = 0
BEGIN
    EXEC (N'SELECT * INTO dbo.tbl_SupervisionTemporal FROM ' + @Origen + N' WHERE 1 = 0');
    PRINT 'Creada dbo.tbl_SupervisionTemporal';
END

-- 2) Ids del origen que faltan en el destino.
IF OBJECT_ID('tempdb..#ids') IS NOT NULL DROP TABLE #ids;
CREATE TABLE #ids (id INT NOT NULL PRIMARY KEY);
DECLARE @sqlIds NVARCHAR(MAX) = N'INSERT INTO #ids (id) SELECT id FROM ' + @Origen + N' WHERE id > ' + CAST(@DesdeId AS NVARCHAR(20));
EXEC (@sqlIds);
-- (EXEC crea su propio ambito, pero #ids es visible porque se creo antes)

DELETE i FROM #ids i WHERE EXISTS (SELECT 1 FROM dbo.tbl_Supervision d WHERE d.id = i.id);

SELECT COUNT(*) AS faltan_en_destino, MIN(id) AS minId, MAX(id) AS maxId FROM #ids;

IF @Simular = 1
BEGIN
    SELECT TOP (CASE WHEN @Limite > 0 THEN @Limite ELSE 2147483647 END) id AS se_copiaria
    FROM #ids ORDER BY id;
    PRINT 'SIMULACION: no se escribio nada. Pon @Simular = 0 para copiar.';
    RETURN;
END

-- 3) Vaciar la temporal y copiar fila por fila (evita transferir GB en una sola sentencia).
TRUNCATE TABLE dbo.tbl_SupervisionTemporal;

-- Lista de columnas de la temporal (asi no dependo de nombres escritos a mano).
DECLARE @cols NVARCHAR(MAX);
SELECT @cols = STUFF((
    SELECT ', [' + c.name + ']'
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID('dbo.tbl_SupervisionTemporal')
    ORDER BY c.column_id
    FOR XML PATH('')), 1, 2, '');

DECLARE @esIdentity BIT = ISNULL(COLUMNPROPERTY(OBJECT_ID('dbo.tbl_SupervisionTemporal'), 'id', 'IsIdentity'), 0);
DECLARE @id INT, @n INT = 0, @sql NVARCHAR(MAX);

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT TOP (CASE WHEN @Limite > 0 THEN @Limite ELSE 2147483647 END) id FROM #ids ORDER BY id;
OPEN cur;
FETCH NEXT FROM cur INTO @id;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'';
    IF @esIdentity = 1 SET @sql = @sql + N'SET IDENTITY_INSERT dbo.tbl_SupervisionTemporal ON; ';
    SET @sql = @sql + N'INSERT INTO dbo.tbl_SupervisionTemporal (' + @cols + N') SELECT ' + @cols
             + N' FROM ' + @Origen + N' WHERE id = ' + CAST(@id AS NVARCHAR(20)) + N'; ';
    IF @esIdentity = 1 SET @sql = @sql + N'SET IDENTITY_INSERT dbo.tbl_SupervisionTemporal OFF; ';
    EXEC (@sql);

    SET @n = @n + 1;
    IF @n % 25 = 0 RAISERROR('Copiadas %d filas', 0, 1, @n) WITH NOWAIT;
    FETCH NEXT FROM cur INTO @id;
END
CLOSE cur; DEALLOCATE cur;

PRINT 'Copiadas a dbo.tbl_SupervisionTemporal: ' + CAST(@n AS VARCHAR(10));
SELECT COUNT(*) AS en_temporal, MIN(id) AS minId, MAX(id) AS maxId FROM dbo.tbl_SupervisionTemporal;
GO
