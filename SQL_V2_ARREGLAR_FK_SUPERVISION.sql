/* =====================================================================
   ARREGLO: las llaves foraneas del checklist y del cruce de materiales apuntan a
   dbo.tbl_Supervisionv3 (la tabla vieja renombrada) en vez de dbo.tbl_Supervision.
   Sintoma: al guardar una supervision con checklist, error
   "INSERT en conflicto con FOREIGN KEY FK_tbl_SupervisionChecklist_Supervision ... tabla dbo.tbl_Supervisionv3".

   Ejecutar en 172.16.0.13, base BDControlOrdenes (SQL 2008).
   Que hace (todo dentro de una transaccion: o queda completo o no cambia nada):
     1) Comprueba que tbl_Supervision.id no tenga nulos ni repetidos.
     2) Le agrega llave primaria a tbl_Supervision.id (la tabla nueva no tiene; una FK la necesita).
        Nombre PK_tbl_Supervision_nueva, porque PK_tbl_Supervision ya lo usa la tabla v3.
     3) Borra las dos FK que apuntan a v3 y las vuelve a crear apuntando a dbo.tbl_Supervision.
   NO toca datos ni el identity (ver nota al final).

   Hacerlo con poca actividad (nadie guardando supervisiones en ese momento).
   @Simular = 1 solo comprueba; 0 aplica.
   ===================================================================== */
USE BDControlOrdenes;
GO
SET NOCOUNT ON;

DECLARE @Simular BIT = 1;

-- 1) Comprobaciones previas
DECLARE @filas INT, @distintos INT, @nulos INT;
SELECT @filas = COUNT(*), @distintos = COUNT(DISTINCT id), @nulos = SUM(CASE WHEN id IS NULL THEN 1 ELSE 0 END)
FROM dbo.tbl_Supervision;
SELECT @filas AS filas, @distintos AS ids_distintos, ISNULL(@nulos, 0) AS ids_nulos;

IF @filas <> @distintos OR ISNULL(@nulos, 0) > 0
BEGIN
    RAISERROR('tbl_Supervision.id tiene repetidos o nulos: no se puede crear la llave primaria. Avisa antes de seguir.', 16, 1);
    RETURN;
END

DECLARE @huerf_chk INT, @huerf_cruce INT;
SELECT @huerf_chk = COUNT(*) FROM dbo.tbl_SupervisionChecklist WHERE id_supervision NOT IN (SELECT id FROM dbo.tbl_Supervision);
SELECT @huerf_cruce = COUNT(*) FROM dbo.tbl_SupervisionCruceMaterial WHERE id_supervision NOT IN (SELECT id FROM dbo.tbl_Supervision);
SELECT @huerf_chk AS checklist_sin_padre, @huerf_cruce AS cruce_sin_padre;
IF @huerf_chk > 0 OR @huerf_cruce > 0
BEGIN
    RAISERROR('Hay filas de checklist o cruce cuya supervision no existe en tbl_Supervision. Revisalas antes de recrear las FK.', 16, 1);
    RETURN;
END

IF @Simular = 1
BEGIN
    PRINT 'SIMULACION: las comprobaciones pasaron; no se cambio nada. Pon @Simular = 0 para aplicar.';
    RETURN;
END

-- 2) y 3) Cambios, en una transaccion
BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.tbl_Supervision') AND is_primary_key = 1)
        ALTER TABLE dbo.tbl_Supervision ADD CONSTRAINT PK_tbl_Supervision_nueva PRIMARY KEY CLUSTERED (id);

    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_tbl_SupervisionChecklist_Supervision')
        ALTER TABLE dbo.tbl_SupervisionChecklist DROP CONSTRAINT FK_tbl_SupervisionChecklist_Supervision;
    ALTER TABLE dbo.tbl_SupervisionChecklist ADD CONSTRAINT FK_tbl_SupervisionChecklist_Supervision
        FOREIGN KEY (id_supervision) REFERENCES dbo.tbl_Supervision (id);

    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_tbl_SupervisionCruceMaterial_Supervision')
        ALTER TABLE dbo.tbl_SupervisionCruceMaterial DROP CONSTRAINT FK_tbl_SupervisionCruceMaterial_Supervision;
    ALTER TABLE dbo.tbl_SupervisionCruceMaterial ADD CONSTRAINT FK_tbl_SupervisionCruceMaterial_Supervision
        FOREIGN KEY (id_supervision) REFERENCES dbo.tbl_Supervision (id);

    COMMIT TRANSACTION;
    PRINT 'Listo: las FK ahora apuntan a dbo.tbl_Supervision.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @err NVARCHAR(2000) = ERROR_MESSAGE();
    RAISERROR(@err, 16, 1);
END CATCH

-- Verificacion: las dos FK deben apuntar a tbl_Supervision
SELECT fk.name AS llave, OBJECT_NAME(fk.parent_object_id) AS tabla_hija, OBJECT_NAME(fk.referenced_object_id) AS apunta_a
FROM sys.foreign_keys fk WHERE fk.name LIKE 'FK_tbl_Supervision%';
GO

/* NOTA (identity): segun tus consultas, tbl_Supervision.id NO es identity (la v3 si lo es).
   Este script no lo cambia porque agregar IDENTITY a una columna existente exige reconstruir la tabla.
   Los registros nuevos de Sup_v2 siguen generando su id como hasta ahora (las pruebas 1409 a 1413 se guardaron).
   Antes de tocar eso hay que revisar como los procedimientos asignan el id:
     SELECT OBJECT_DEFINITION(OBJECT_ID('dbo.spx_RegistrarSupervisionManual'));  */
