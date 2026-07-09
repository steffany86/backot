/*
  Cambia el auxiliar de supervision para que sea nombre escrito y opcional.
  Ejecutar en la base donde existe dbo.tbl_Supervision.
*/

IF OBJECT_ID(N'dbo.tbl_Supervision', N'U') IS NULL
BEGIN
    THROW 50001, 'No existe dbo.tbl_Supervision en esta base.', 1;
END;

IF COL_LENGTH('dbo.tbl_Supervision', 'Id_TecnicoAuxiliar') IS NOT NULL
BEGIN
    ALTER TABLE dbo.tbl_Supervision
    ALTER COLUMN Id_TecnicoAuxiliar NVARCHAR(150) NULL;

    UPDATE dbo.tbl_Supervision
    SET Id_TecnicoAuxiliar = UPPER(LTRIM(RTRIM(Id_TecnicoAuxiliar)))
    WHERE Id_TecnicoAuxiliar IS NOT NULL;
END;

IF COL_LENGTH('dbo.tbl_Supervision', 'id_tecnico_auxiliar') IS NOT NULL
BEGIN
    ALTER TABLE dbo.tbl_Supervision
    ALTER COLUMN id_tecnico_auxiliar NVARCHAR(150) NULL;

    UPDATE dbo.tbl_Supervision
    SET id_tecnico_auxiliar = UPPER(LTRIM(RTRIM(id_tecnico_auxiliar)))
    WHERE id_tecnico_auxiliar IS NOT NULL;
END;

PRINT 'Auxiliar de supervision configurado como texto opcional.';
