-- =====================================================================
-- SUPERVISION V2 - firmas, checklists y cruce de materiales
-- BDControlOrdenes (172.16.0.13, SQL Server 2008). Se puede ejecutar mas de una vez
-- (solo crea lo que falta y solo siembra los catalogos si estan vacios).
-- Fuente: tools/Supervision.xlsx
--
-- Regla de observacion: si la respuesta es la NEGATIVA del item (No / Incorrecto / Menor,
-- Mayor o Sin Senal en potencia) la observacion es OBLIGATORIA; en las demas respuestas
-- se puede escribir pero no es obligatoria. La columna respuestas_obligan_obs guarda
-- cuales respuestas obligan (separadas por |); la validacion la hace la aplicacion.
-- =====================================================================
USE BDControlOrdenes;
GO
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- 1) Firmas en tbl_Supervision (se guarda la RUTA del archivo, como las fotos)
--    Tecnico: tipos 1, 2 y 5  |  Auxiliar (opcional): tipos 1 y 2  |  Cliente: tipo 4 Interna e Interna/Externa
-- ---------------------------------------------------------------------
IF COL_LENGTH('dbo.tbl_Supervision', 'FirmaTecnico') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD FirmaTecnico NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.tbl_Supervision', 'FirmaAuxiliar') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD FirmaAuxiliar NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.tbl_Supervision', 'FirmaCliente') IS NULL
    ALTER TABLE dbo.tbl_Supervision ADD FirmaCliente NVARCHAR(500) NULL;
GO

-- ---------------------------------------------------------------------
-- 2) Catalogo de items del checklist
--    grupo: REVISION (supervision de campo) | EPP | VEHICULO | HERRAMIENTAS
--    aplica_revision (solo REVISION): INTERNA | EXTERNA | AMBAS (AMBAS = solo cuando la revision es Interna/Externa)
--    tipo_respuesta: SI_NO | CORRECTO_INCORRECTO | POTENCIA | CRUCE_MATERIAL
-- ---------------------------------------------------------------------
IF OBJECT_ID('dbo.tbl_SupChecklistItem', 'U') IS NULL
CREATE TABLE dbo.tbl_SupChecklistItem (
    id                      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tbl_SupChecklistItem PRIMARY KEY,
    grupo                   NVARCHAR(30)  NOT NULL,
    aplica_revision         NVARCHAR(20)  NULL,
    orden                   INT           NOT NULL CONSTRAINT DF_tbl_SupChecklistItem_orden DEFAULT (0),
    item                    NVARCHAR(200) NOT NULL,
    tipo_respuesta          NVARCHAR(25)  NOT NULL,
    opciones                NVARCHAR(200) NULL,
    respuestas_obligan_obs  NVARCHAR(200) NULL,
    e_eliminado             BIT           NOT NULL CONSTRAINT DF_tbl_SupChecklistItem_elim DEFAULT (0)
);
GO

-- ---------------------------------------------------------------------
-- 3) Respuestas del checklist por supervision
-- ---------------------------------------------------------------------
IF OBJECT_ID('dbo.tbl_SupervisionChecklist', 'U') IS NULL
CREATE TABLE dbo.tbl_SupervisionChecklist (
    id              INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tbl_SupervisionChecklist PRIMARY KEY,
    id_supervision  INT           NOT NULL,
    id_item         INT           NOT NULL,
    respuesta       NVARCHAR(30)  NULL,
    observacion     NVARCHAR(500) NULL,
    fecha_registro  DATETIME      NOT NULL CONSTRAINT DF_tbl_SupervisionChecklist_fecha DEFAULT (GETDATE()),
    e_eliminado     BIT           NOT NULL CONSTRAINT DF_tbl_SupervisionChecklist_elim DEFAULT (0),
    CONSTRAINT FK_tbl_SupervisionChecklist_Item FOREIGN KEY (id_item) REFERENCES dbo.tbl_SupChecklistItem (id)
);
GO
-- Una respuesta por item y supervision
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_tbl_SupervisionChecklist_sup_item' AND object_id = OBJECT_ID('dbo.tbl_SupervisionChecklist'))
    CREATE UNIQUE INDEX UX_tbl_SupervisionChecklist_sup_item ON dbo.tbl_SupervisionChecklist (id_supervision, id_item);
GO
-- FK hacia tbl_Supervision(id): solo si esa columna es llave primaria (o unica)
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_tbl_SupervisionChecklist_Supervision')
   AND EXISTS (SELECT 1 FROM sys.indexes i
               JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
               JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
               WHERE i.object_id = OBJECT_ID('dbo.tbl_Supervision') AND c.name = 'id' AND (i.is_primary_key = 1 OR i.is_unique = 1))
    ALTER TABLE dbo.tbl_SupervisionChecklist ADD CONSTRAINT FK_tbl_SupervisionChecklist_Supervision
        FOREIGN KEY (id_supervision) REFERENCES dbo.tbl_Supervision (id);
ELSE IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_tbl_SupervisionChecklist_Supervision')
    PRINT 'AVISO: tbl_Supervision.id no es llave primaria; no se creo la FK de tbl_SupervisionChecklist a tbl_Supervision.';
GO

-- ---------------------------------------------------------------------
-- 4) Cruce de materiales: catalogo y detalle por supervision
-- ---------------------------------------------------------------------
IF OBJECT_ID('dbo.tbl_SupMaterialCruce', 'U') IS NULL
CREATE TABLE dbo.tbl_SupMaterialCruce (
    id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tbl_SupMaterialCruce PRIMARY KEY,
    orden        INT           NOT NULL CONSTRAINT DF_tbl_SupMaterialCruce_orden DEFAULT (0),
    material     NVARCHAR(150) NOT NULL,
    e_eliminado  BIT           NOT NULL CONSTRAINT DF_tbl_SupMaterialCruce_elim DEFAULT (0)
);
GO

IF OBJECT_ID('dbo.tbl_SupervisionCruceMaterial', 'U') IS NULL
CREATE TABLE dbo.tbl_SupervisionCruceMaterial (
    id              INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tbl_SupervisionCruceMaterial PRIMARY KEY,
    id_supervision  INT           NOT NULL,
    id_material     INT           NOT NULL,
    cant_sistema    INT           NULL,   -- cantidad segun el sistema (OT + nro de cliente)
    cant_fisico     INT           NULL,   -- cantidad que digita el supervisor
    diferencia      AS (ISNULL(cant_fisico, 0) - ISNULL(cant_sistema, 0)),
    observacion     NVARCHAR(500) NULL,   -- obligatoria cuando hay diferencia (lo valida la aplicacion)
    fecha_registro  DATETIME      NOT NULL CONSTRAINT DF_tbl_SupervisionCruceMaterial_fecha DEFAULT (GETDATE()),
    e_eliminado     BIT           NOT NULL CONSTRAINT DF_tbl_SupervisionCruceMaterial_elim DEFAULT (0),
    CONSTRAINT FK_tbl_SupervisionCruceMaterial_Material FOREIGN KEY (id_material) REFERENCES dbo.tbl_SupMaterialCruce (id)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_tbl_SupervisionCruceMaterial_sup_mat' AND object_id = OBJECT_ID('dbo.tbl_SupervisionCruceMaterial'))
    CREATE UNIQUE INDEX UX_tbl_SupervisionCruceMaterial_sup_mat ON dbo.tbl_SupervisionCruceMaterial (id_supervision, id_material);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_tbl_SupervisionCruceMaterial_Supervision')
   AND EXISTS (SELECT 1 FROM sys.indexes i
               JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
               JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
               WHERE i.object_id = OBJECT_ID('dbo.tbl_Supervision') AND c.name = 'id' AND (i.is_primary_key = 1 OR i.is_unique = 1))
    ALTER TABLE dbo.tbl_SupervisionCruceMaterial ADD CONSTRAINT FK_tbl_SupervisionCruceMaterial_Supervision
        FOREIGN KEY (id_supervision) REFERENCES dbo.tbl_Supervision (id);
GO

-- ---------------------------------------------------------------------
-- 5) Datos iniciales (solo si los catalogos estan vacios)
--    NCHAR(241) = n con tilde (Se?al, Pelda?os) para que el archivo no dependa de la codificacion.
-- ---------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.tbl_SupChecklistItem)
BEGIN
    DECLARE @sinSenal NVARCHAR(30);
    SET @sinSenal = N'Sin Se' + NCHAR(241) + N'al';

    -- Supervision de campo, revision INTERNA
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, aplica_revision, orden, item, tipo_respuesta, opciones, respuestas_obligan_obs) VALUES
        (N'REVISION', N'INTERNA', 1, N'Engrampado',                       N'CORRECTO_INCORRECTO', NULL, N'Incorrecto'),
        (N'REVISION', N'INTERNA', 2, N'Nivel de Potencia equipos',        N'POTENCIA', N'Menor|Normal|Mayor|' + @sinSenal, N'Menor|Mayor|' + @sinSenal),
        (N'REVISION', N'INTERNA', 3, N'Distribucion Cableado',            N'CORRECTO_INCORRECTO', NULL, N'Incorrecto'),
        (N'REVISION', N'INTERNA', 4, N'Servicio funcionando',             N'SI_NO', NULL, N'No'),
        (N'REVISION', N'INTERNA', 5, N'Configuracion wifi',               N'SI_NO', NULL, N'No'),
        (N'REVISION', N'INTERNA', 6, N'Validacion de banda 2.4 G y 5G',   N'SI_NO', NULL, N'No');

    -- Supervision de campo, revision EXTERNA
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, aplica_revision, orden, item, tipo_respuesta, opciones, respuestas_obligan_obs) VALUES
        (N'REVISION', N'EXTERNA', 7,  N'Acometida asegurado',            N'SI_NO', NULL, N'No'),
        (N'REVISION', N'EXTERNA', 8,  N'Cruce de acometida',             N'SI_NO', NULL, N'No'),
        (N'REVISION', N'EXTERNA', 9,  N'Loop de ingreso',                N'SI_NO', NULL, N'No'),
        (N'REVISION', N'EXTERNA', 10, N'Loop en el top',                 N'SI_NO', NULL, N'No'),
        (N'REVISION', N'EXTERNA', 11, N'Nomenclador identificador',      N'SI_NO', NULL, N'No');

    -- Supervision de campo: items solo de la revision Interna/Externa (AMBAS)
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, aplica_revision, orden, item, tipo_respuesta, opciones, respuestas_obligan_obs) VALUES
        (N'REVISION', N'AMBAS', 12, N'Conectorizacion(torque)',          N'CORRECTO_INCORRECTO', NULL, N'Incorrecto'),
        (N'REVISION', N'AMBAS', 13, N'Validacion de material',           N'CRUCE_MATERIAL', NULL, NULL);

    -- Interceptacion de ruta: EPP
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, orden, item, tipo_respuesta, respuestas_obligan_obs) VALUES
        (N'EPP', 1,  N'Arnes De Cuerpo Completo',                  N'SI_NO', N'No'),
        (N'EPP', 2,  N'Cabo De Vida En Cinta',                     N'SI_NO', N'No'),
        (N'EPP', 3,  N'Cordino De 6 Metros (Guindo)',              N'SI_NO', N'No'),
        (N'EPP', 4,  N'Guantes De TRABAJO',                        N'SI_NO', N'No'),
        (N'EPP', 5,  N'Lentes De Seguridad - NEGROS',              N'SI_NO', N'No'),
        (N'EPP', 6,  N'Lentes De Seguridad - Transparente',        N'SI_NO', N'No'),
        (N'EPP', 7,  N'Casco De Seguridad Trabajo En Altura',      N'SI_NO', N'No'),
        (N'EPP', 8,  N'Botas Dielectricos',                        N'SI_NO', N'No'),
        (N'EPP', 9,  N'Camisa Manga Larga Con Linea Reflectiva',   N'SI_NO', N'No'),
        (N'EPP', 10, N'Chaleco',                                   N'SI_NO', N'No'),
        (N'EPP', 11, N'Pantalon Con Linea Reflectiva',             N'SI_NO', N'No');

    -- Interceptacion de ruta: Vehiculo
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, orden, item, tipo_respuesta, respuestas_obligan_obs) VALUES
        (N'VEHICULO', 1, N'Botiquin',                N'SI_NO', N'No'),
        (N'VEHICULO', 2, N'Triangulo',               N'SI_NO', N'No'),
        (N'VEHICULO', 3, N'Extintor Para Vehiculo',  N'SI_NO', N'No'),
        (N'VEHICULO', 4, N'Gato Para Vehiculo',      N'SI_NO', N'No'),
        (N'VEHICULO', 5, N'Cable Alargador 20 Mts',  N'SI_NO', N'No'),
        (N'VEHICULO', 6, N'Cono De Parqueo',         N'SI_NO', N'No');

    -- Interceptacion de ruta: Herramientas del tecnico
    INSERT INTO dbo.tbl_SupChecklistItem (grupo, orden, item, tipo_respuesta, respuestas_obligan_obs) VALUES
        (N'HERRAMIENTAS', 1,  N'Aprisionador De Conectores "F" RG-6 Y RG-11',     N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 2,  N'Pelacable Coaxial - RG-6 Y RG-11',                 N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 3,  N'Cuerda de aseguramiento en poste (15 metros)',     N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 4,  N'Cuerda de aseguramiento en red (1 metro)',         N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 5,  N'Llave Torque Hfc',                                 N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 6,  N'Alicate De Corte De 7"',                           N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 7,  N'Alicate De Fuerza De 8"',                          N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 8,  N'Busca Polo',                                       N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 9,  N'Desarmador Combinado De 9" (Estrella Y Plano)',    N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 10, N'Caja De Herramientas De 20"',                      N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 11, N'Enchufe Y Regleta',                                N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 12, N'Martillos TRAMONTINA',                             N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 13, N'Pasacable Metalico (30 Mts)',                      N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 14, N'Taladro INDUSTRIAL De 1/2"',                       N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 15, N'Escalera Tijera De 8 Pelda' + NCHAR(241) + N'os',  N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 16, N'Escaleras  24 Pelda' + NCHAR(241) + N'os',         N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 17, N'Gancho En Aluminio Con V -Para Escalera Larga',    N'SI_NO', N'No'),
        (N'HERRAMIENTAS', 18, N'Medidor De Se' + NCHAR(241) + N'al HFC',           N'SI_NO', N'No');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.tbl_SupMaterialCruce)
    INSERT INTO dbo.tbl_SupMaterialCruce (orden, material) VALUES
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
GO

-- ---------------------------------------------------------------------
-- 6) Resumen
-- ---------------------------------------------------------------------
SELECT grupo, aplica_revision, COUNT(*) AS items FROM dbo.tbl_SupChecklistItem GROUP BY grupo, aplica_revision ORDER BY grupo, aplica_revision;
SELECT COUNT(*) AS materiales_cruce FROM dbo.tbl_SupMaterialCruce;
PRINT 'Listo: firmas, checklists y cruce de materiales de Supervision V2.';
GO
