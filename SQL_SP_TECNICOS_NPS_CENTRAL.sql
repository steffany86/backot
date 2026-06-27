IF OBJECT_ID('dbo.SP_tecnicos_NPS', 'P') IS NULL
    EXEC('CREATE PROCEDURE dbo.SP_tecnicos_NPS AS BEGIN SET NOCOUNT ON; SELECT 1 AS stub; END');
GO

ALTER PROCEDURE dbo.SP_tecnicos_NPS
    @IdUsuarioSesion INT,
    @IdSucursal INT = NULL,
    @IdSupervisor INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Supervisor INT = COALESCE(NULLIF(@IdSupervisor, 0), @IdUsuarioSesion);
    DECLARE @SucursalNombre NVARCHAR(120) = CASE @IdSucursal
        WHEN 9 THEN 'SANTA CRUZ'
        WHEN 20 THEN 'SANTA CRUZ'
        WHEN 4 THEN 'SUCRE'
        WHEN 7 THEN 'TARIJA'
        WHEN 2 THEN 'YACUIBA'
        WHEN 15 THEN 'RIBERALTA'
        WHEN 19 THEN 'MONTERO'
        WHEN 5 THEN 'CAMIRI'
        WHEN 10 THEN 'CHIQUITANIA'
        WHEN 16 THEN 'COBIJA'
        WHEN 12 THEN 'IVIRGARZAMA'
        WHEN 6 THEN 'PUERTO SUAREZ'
        WHEN 11 THEN 'SAN IGNACIO'
        WHEN 17 THEN 'TRINIDAD'
        WHEN 14 THEN 'YAPACANI'
        ELSE NULL
    END;
    DECLARE @SucursalNorm NVARCHAR(120) =
        LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(@SucursalNombre, ''))), '_', ''), '-', ''), ' ', ''));

    ;WITH base AS (
        SELECT
            LTRIM(RTRIM(ISNULL(cc.grupo, ''))) AS grupo,
            CAST(cc.id_tecnico AS INT) AS idTecnico,
            LTRIM(RTRIM(ISNULL(cc.tecnico, ''))) AS tecnico,
            CAST(cc.idUsuarioSupervisor AS INT) AS idSupervisor,
            LTRIM(RTRIM(ISNULL(cc.supervisorACargo, ''))) AS supervisor,
            cc.sucursal,
            cc.fecha,
            cc.fechaRegistro,
            cc.id,
            ROW_NUMBER() OVER (
                PARTITION BY LTRIM(RTRIM(ISNULL(cc.grupo, ''))), CAST(cc.id_tecnico AS INT)
                ORDER BY cc.fecha DESC, cc.fechaRegistro DESC, cc.id DESC
            ) AS rnTecnicoGrupo
        FROM dbo.tbl_ConformacionCuadrillaDiario cc
        WHERE ISNULL(cc.e_eliminado, 0) = 0
          AND LTRIM(RTRIM(ISNULL(cc.grupo, ''))) <> ''
          AND cc.id_tecnico IS NOT NULL
          AND cc.id_tecnico > 0
          AND (
                @SucursalNombre IS NULL
             OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(cc.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = @SucursalNorm
          )
    ),
    asignados AS (
        SELECT
            b.*,
            ROW_NUMBER() OVER (
                PARTITION BY b.grupo
                ORDER BY b.fecha DESC, b.fechaRegistro DESC, b.id DESC
            ) AS rnGrupo
        FROM base b
        WHERE b.rnTecnicoGrupo = 1
          AND b.idSupervisor = @Supervisor
    )
    SELECT
        a.idTecnico,
        COALESCE(NULLIF(a.tecnico, ''), 'Tecnico ' + CONVERT(NVARCHAR(20), a.idTecnico)) AS tecnico,
        a.idTecnico AS idUsuarioTecnico,
        DENSE_RANK() OVER (ORDER BY a.grupo) AS idGrupo,
        a.grupo,
        a.idSupervisor
    FROM asignados a
    WHERE a.rnGrupo = 1
    ORDER BY a.grupo, tecnico;
END
GO
