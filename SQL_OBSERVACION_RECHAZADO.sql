-- La columna que guarda el motivo de rechazo del inicio de jornada ya existe
-- en bd_tigohogar como dbo.tbl_InicioJornadaAlturas.observacionrechazado
-- (sin guion bajo). Este script queda solo como resguardo idempotente por si
-- se corre en un entorno donde todavia no existe; no crea una columna duplicada.
--
-- Usada por SupervisionRepository.actualizarObservacionRechazado (motivo que
-- el supervisor escribe al rechazar) y por TecnicoInicioJornadaRepository
-- (para mostrarselo al tecnico en su siguiente intento de registro del dia).

IF COL_LENGTH('dbo.tbl_InicioJornadaAlturas', 'observacionrechazado') IS NULL
BEGIN
    ALTER TABLE dbo.tbl_InicioJornadaAlturas
    ADD observacionrechazado NVARCHAR(500) NULL;
END
GO
