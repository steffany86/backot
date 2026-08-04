USE [BD_TigoHogar];
GO

IF COL_LENGTH('dbo.tbl_InicioJornadaAlturas', 'firma_inicio') IS NULL
BEGIN
    ALTER TABLE dbo.tbl_InicioJornadaAlturas
    ADD firma_inicio NVARCHAR(MAX) NULL;
END;
GO

IF COL_LENGTH('dbo.tbl_InicioJornadaAlturas', 'firma_cierre') IS NULL
BEGIN
    ALTER TABLE dbo.tbl_InicioJornadaAlturas
    ADD firma_cierre NVARCHAR(MAX) NULL;
END;
GO
