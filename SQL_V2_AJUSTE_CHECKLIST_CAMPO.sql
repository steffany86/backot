-- =====================================================================
-- SUPERVISION V2 - ajuste checklist tipo 4 (Supervision de campo)
-- Conectorizacion(torque) y Validacion de material (aplica_revision = AMBAS) solo
-- se muestran en revision Interna/Externa (lo resuelve el front) y quedan al final,
-- despues de los items de Interna y Externa. Se puede ejecutar mas de una vez.
-- =====================================================================
USE BDControlOrdenes;
GO

UPDATE dbo.tbl_SupChecklistItem SET orden = 7  WHERE grupo = N'REVISION' AND aplica_revision = N'EXTERNA' AND item = N'Acometida asegurado';
UPDATE dbo.tbl_SupChecklistItem SET orden = 8  WHERE grupo = N'REVISION' AND aplica_revision = N'EXTERNA' AND item = N'Cruce de acometida';
UPDATE dbo.tbl_SupChecklistItem SET orden = 9  WHERE grupo = N'REVISION' AND aplica_revision = N'EXTERNA' AND item = N'Loop de ingreso';
UPDATE dbo.tbl_SupChecklistItem SET orden = 10 WHERE grupo = N'REVISION' AND aplica_revision = N'EXTERNA' AND item = N'Loop en el top';
UPDATE dbo.tbl_SupChecklistItem SET orden = 11 WHERE grupo = N'REVISION' AND aplica_revision = N'EXTERNA' AND item = N'Nomenclador identificador';
UPDATE dbo.tbl_SupChecklistItem SET orden = 12 WHERE grupo = N'REVISION' AND aplica_revision = N'AMBAS' AND item = N'Conectorizacion(torque)';
UPDATE dbo.tbl_SupChecklistItem SET orden = 13 WHERE grupo = N'REVISION' AND aplica_revision = N'AMBAS' AND item = N'Validacion de material';
GO

SELECT aplica_revision, orden, item FROM dbo.tbl_SupChecklistItem WHERE grupo = N'REVISION' AND e_eliminado = 0 ORDER BY orden;
