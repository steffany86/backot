USE BD_TigoHogar
GO

IF COL_LENGTH('dbo.tbl_InicioJornadaAlturas', 'estoy_trabajando_solo') IS NULL
BEGIN
    ALTER TABLE dbo.tbl_InicioJornadaAlturas
    ADD estoy_trabajando_solo BIT NULL;
END
GO
