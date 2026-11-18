-- =====================================================================
-- SUPERVISION V2 - actualiza el catalogo del cruce de materiales (tbl_SupMaterialCruce)
-- BDControlOrdenes (172.16.0.13). Se puede ejecutar mas de una vez.
-- Los nombres deben ser IGUALES a p.NombreProductoSupervisionDeCampo (el cruce con
-- spx_ObtenerMaterialesOT_PSupervision se hace por nombre, sin importar mayusculas ni tildes).
--   - Los materiales que cambian de nombre se renombran (se conserva el id y el historial).
--   - Los que faltan se insertan; los que ya no estan en la lista se marcan e_eliminado = 1.
-- NCHAR(209) = N con tilde (SE?AL) para que el archivo no dependa de la codificacion.
-- =====================================================================
USE BDControlOrdenes;
GO
SET NOCOUNT ON;
GO

-- 1) Renombrar los que cambiaron
UPDATE dbo.tbl_SupMaterialCruce SET material = N'CABLE COAXIAL SPB'     WHERE material = N'CABLE COAXIAL SPB/SPN';
UPDATE dbo.tbl_SupMaterialCruce SET material = N'CONECTORES INTERNOS'   WHERE material = N'TERMINALES EXTERNOS e INTERNOS';
UPDATE dbo.tbl_SupMaterialCruce SET material = N'HDMI'                  WHERE material = N'HDMI/ RCA';
UPDATE dbo.tbl_SupMaterialCruce SET material = N'SPLITER DE 2 ; 3'      WHERE material = N'SPLITER UOT/2; 3';
GO

-- 2) Lista final con su orden
DECLARE @lista TABLE (orden INT NOT NULL, material NVARCHAR(150) NOT NULL);
INSERT INTO @lista (orden, material) VALUES
    (1,  N'DECODIFICADORES'),
    (2,  N'MODEM HFC'),
    (3,  N'CABLE COAXIAL CP'),
    (4,  N'CABLE COAXIAL SPB'),
    (5,  N'CABLE COAXIAL SPN'),
    (6,  N'CONECTORES INTERNOS'),
    (7,  N'CONECTORES EXTERNOS'),
    (8,  N'FILTRO'),
    (9,  N'HDMI'),
    (10, N'RCA'),
    (11, N'QSPAN'),
    (12, N'SPLITER DE 2 ; 3'),
    (13, N'PRESINTO IDENTIFICATIVO( AZUL)'),
    (14, N'GRAPAS de CONCRETO'),
    (15, N'ATENUADOR DE SE' + NCHAR(209) + N'AL'),
    (16, N'CARGA TERMINAL');

-- Insertar los que faltan
INSERT INTO dbo.tbl_SupMaterialCruce (orden, material)
SELECT l.orden, l.material FROM @lista l
WHERE NOT EXISTS (SELECT 1 FROM dbo.tbl_SupMaterialCruce m WHERE m.material = l.material);

-- Reactivar y reordenar los de la lista
UPDATE m SET m.orden = l.orden, m.e_eliminado = 0
FROM dbo.tbl_SupMaterialCruce m
INNER JOIN @lista l ON l.material = m.material;

-- Dar de baja los que ya no estan en la lista
UPDATE m SET m.e_eliminado = 1
FROM dbo.tbl_SupMaterialCruce m
WHERE NOT EXISTS (SELECT 1 FROM @lista l WHERE l.material = m.material);
GO

SELECT id, orden, material, e_eliminado FROM dbo.tbl_SupMaterialCruce ORDER BY e_eliminado, orden;
GO
