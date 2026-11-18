/* =====================================================================
   MIGRACION tbl_Supervision (origen) -> tbl_Supervision (destino)   PASO 3 de 3
   Se ejecuta en el DESTINO: 172.16.0.13, base BDControlOrdenes (SQL 2008).

   Antes: PASO 1 (SQL_V2_MIGRAR_1_COPIAR_A_TEMPORAL.sql) y PASO 2 = migrador de fotos sobre la temporal:

     MigradorFotosSupervision.exe --base BDControlOrdenes --tabla tbl_SupervisionTemporal --columna-clave id --salida D:/BDControlOrdenesImagenes/Supervision
         (primero SIN --aplicar para simular; luego con --aplicar). Se ejecuta en la maquina del 172.16.0.13
         para que las fotos queden en D:\BDControlOrdenesImagenes\Supervision (la carpeta que usa la aplicacion).

   Que hace: pasa de dbo.tbl_SupervisionTemporal a dbo.tbl_Supervision, conservando el id:
       Codigo (origen)  ->  CodigoCliente (destino)
       Id_Supervision (origen)  ->  no pasa
       cargo_creado_por, FirmaTecnico, FirmaAuxiliar, FirmaCliente, Id_LlamadaDeAtencion,
       id_supervisor_asignado, nombre_supervisor_asignado  ->  NULL
   Seguridad: aborta si alguna foto sigue en base64 (el migrador de fotos no se corrio) y no repite ids.
   ===================================================================== */
USE BDControlOrdenes;
GO
SET NOCOUNT ON;

DECLARE @Simular BIT = 0;   -- 1 = solo muestra; 0 = inserta

IF OBJECT_ID('dbo.tbl_SupervisionTemporal') IS NULL
BEGIN
    RAISERROR('No existe dbo.tbl_SupervisionTemporal. Corre el PASO 1.', 16, 1);
    RETURN;
END

-- Columnas que debe tener la temporal (si el origen cambio de nombre alguna, avisa en vez de fallar a medias).
DECLARE @faltantes NVARCHAR(MAX) = N'';
SELECT @faltantes = @faltantes + n + N' '
FROM (SELECT 'id' n UNION ALL SELECT 'Id_Supervisor' UNION ALL SELECT 'nombre_Id_Supervisor'
      UNION ALL SELECT 'Id_TecnicoPrincipal' UNION ALL SELECT 'nombre_Id_TecnicoPrincipal'
      UNION ALL SELECT 'Id_TecnicoAuxiliar' UNION ALL SELECT 'Id_TipoSupervision'
      UNION ALL SELECT 'nombre_id_TipoSupervision' UNION ALL SELECT 'Id_TipoTrabajo'
      UNION ALL SELECT 'nombre_id_Tipotrabajo' UNION ALL SELECT 'Id_TipoPenalizacion'
      UNION ALL SELECT 'nombre_id_TipoPenalizacion' UNION ALL SELECT 'Supervision_Por'
      UNION ALL SELECT 'Tecnologia' UNION ALL SELECT 'Codigo' UNION ALL SELECT 'OrdenTrabajo'
      UNION ALL SELECT 'FechaSupervision' UNION ALL SELECT 'TipoRevision' UNION ALL SELECT 'Observacion'
      UNION ALL SELECT 'DescripcionAdicionalObservacion' UNION ALL SELECT 'Ubicacion'
      UNION ALL SELECT 'estado_sup' UNION ALL SELECT 'creado_por' UNION ALL SELECT 'sucursal'
      UNION ALL SELECT 'origen' UNION ALL SELECT 'FotoBoletaSupervision' UNION ALL SELECT 'FotoCanalesPilotos'
      UNION ALL SELECT 'FotoNivelesDocsis' UNION ALL SELECT 'FotoMedicionRuido' UNION ALL SELECT 'FotoBarridoCanales'
      UNION ALL SELECT 'FotoObservacion1' UNION ALL SELECT 'FotoObservacion2' UNION ALL SELECT 'FotoObservacion3'
      UNION ALL SELECT 'FotoObservacion4' UNION ALL SELECT 'FechaRegistro' UNION ALL SELECT 'E_Eliminado') x
WHERE COL_LENGTH('dbo.tbl_SupervisionTemporal', n) IS NULL;
IF LEN(@faltantes) > 0
BEGIN
    DECLARE @m NVARCHAR(500) = N'La temporal no tiene estas columnas: ' + @faltantes;
    RAISERROR(@m, 16, 1);
    RETURN;
END

-- Filas a pasar: las que aun no estan en el destino.
IF OBJECT_ID('tempdb..#pasar') IS NOT NULL DROP TABLE #pasar;
SELECT t.id INTO #pasar
FROM dbo.tbl_SupervisionTemporal t
WHERE NOT EXISTS (SELECT 1 FROM dbo.tbl_Supervision d WHERE d.id = t.id);

SELECT COUNT(*) AS a_pasar, MIN(id) AS minId, MAX(id) AS maxId FROM #pasar;

-- Todas las fotos deben estar ya como ruta de archivo (corto), no base64.
DECLARE @conBase64 INT;
SELECT @conBase64 = COUNT(*)
FROM dbo.tbl_SupervisionTemporal t JOIN #pasar p ON p.id = t.id
WHERE DATALENGTH(t.FotoBoletaSupervision) > 1000 OR DATALENGTH(t.FotoCanalesPilotos) > 1000
   OR DATALENGTH(t.FotoNivelesDocsis) > 1000 OR DATALENGTH(t.FotoMedicionRuido) > 1000
   OR DATALENGTH(t.FotoBarridoCanales) > 1000 OR DATALENGTH(t.FotoObservacion1) > 1000
   OR DATALENGTH(t.FotoObservacion2) > 1000 OR DATALENGTH(t.FotoObservacion3) > 1000
   OR DATALENGTH(t.FotoObservacion4) > 1000;
IF @conBase64 > 0
BEGIN
    DECLARE @m2 NVARCHAR(300) = N'Hay ' + CAST(@conBase64 AS NVARCHAR(10)) + N' filas con fotos aun en base64. Corre el PASO 2 (migrador de fotos con --aplicar).';
    RAISERROR(@m2, 16, 1);
    RETURN;
END

IF @Simular = 1
BEGIN
    SELECT TOP 20 t.id, t.Codigo AS sera_CodigoCliente, t.estado_sup, t.FotoNivelesDocsis, t.FotoObservacion1
    FROM dbo.tbl_SupervisionTemporal t JOIN #pasar p ON p.id = t.id ORDER BY t.id;
    PRINT 'SIMULACION: no se inserto nada. Pon @Simular = 0 para insertar.';
    RETURN;
END

DECLARE @esIdentity BIT = ISNULL(COLUMNPROPERTY(OBJECT_ID('dbo.tbl_Supervision'), 'id', 'IsIdentity'), 0);

BEGIN TRY
    BEGIN TRANSACTION;

    IF @esIdentity = 1 SET IDENTITY_INSERT dbo.tbl_Supervision ON;

    INSERT INTO dbo.tbl_Supervision (
        id, Id_Supervisor, nombre_Id_Supervisor, Id_TecnicoPrincipal, nombre_Id_TecnicoPrincipal,
        Id_TecnicoAuxiliar, Id_TipoSupervision, nombre_id_TipoSupervision, Id_TipoTrabajo, nombre_id_Tipotrabajo,
        Id_TipoPenalizacion, nombre_id_TipoPenalizacion, Supervision_Por, Tecnologia, CodigoCliente, OrdenTrabajo,
        FechaSupervision, TipoRevision, Observacion, DescripcionAdicionalObservacion, Ubicacion,
        estado_sup, creado_por, sucursal, origen,
        FotoBoletaSupervision, FotoCanalesPilotos, FotoNivelesDocsis, FotoMedicionRuido, FotoBarridoCanales,
        FotoObservacion1, FotoObservacion2, FotoObservacion3, FotoObservacion4,
        FechaRegistro, E_Eliminado)
    -- (cargo_creado_por, firmas, Id_LlamadaDeAtencion y supervisor asignado no se listan: quedan en NULL)
    SELECT
        t.id, t.Id_Supervisor, t.nombre_Id_Supervisor, t.Id_TecnicoPrincipal, t.nombre_Id_TecnicoPrincipal,
        t.Id_TecnicoAuxiliar, t.Id_TipoSupervision, t.nombre_id_TipoSupervision, t.Id_TipoTrabajo, t.nombre_id_Tipotrabajo,
        t.Id_TipoPenalizacion, t.nombre_id_TipoPenalizacion, t.Supervision_Por, t.Tecnologia, t.Codigo, t.OrdenTrabajo,
        t.FechaSupervision, t.TipoRevision, t.Observacion, t.DescripcionAdicionalObservacion, t.Ubicacion,
        t.estado_sup, t.creado_por, t.sucursal, t.origen,
        t.FotoBoletaSupervision, t.FotoCanalesPilotos, t.FotoNivelesDocsis, t.FotoMedicionRuido, t.FotoBarridoCanales,
        t.FotoObservacion1, t.FotoObservacion2, t.FotoObservacion3, t.FotoObservacion4,
        t.FechaRegistro, t.E_Eliminado
    FROM dbo.tbl_SupervisionTemporal t
    JOIN #pasar p ON p.id = t.id
    ORDER BY t.id;

    DECLARE @insertadas INT = @@ROWCOUNT;

    IF @esIdentity = 1 SET IDENTITY_INSERT dbo.tbl_Supervision OFF;

    COMMIT TRANSACTION;
    PRINT 'Insertadas en dbo.tbl_Supervision: ' + CAST(@insertadas AS VARCHAR(10));
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    IF @esIdentity = 1 SET IDENTITY_INSERT dbo.tbl_Supervision OFF;
    DECLARE @err NVARCHAR(2000) = ERROR_MESSAGE();
    RAISERROR(@err, 16, 1);
END CATCH

-- Verificacion
SELECT COUNT(*) AS total_destino, MAX(id) AS maxId FROM dbo.tbl_Supervision;
GO


--SELECT COUNT(*) AS total, MAX(id) AS maxId FROM dbo.tbl_Supervision;
--select * from tbl_Supervision


--SELECT
--  SUM(CASE WHEN id <= 200 THEN 1 ELSE 0 END)               AS hasta_200,
--  SUM(CASE WHEN id BETWEEN 201 AND 205 THEN 1 ELSE 0 END)  AS de_201_a_205,
--  SUM(CASE WHEN id BETWEEN 206 AND 299 THEN 1 ELSE 0 END)  AS de_206_a_299,
--  SUM(CASE WHEN id >= 300 THEN 1 ELSE 0 END)               AS desde_300
--FROM dbo.tbl_Supervision;
