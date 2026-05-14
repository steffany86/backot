SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

/*
  SP: spx_ListarTecnicosSupervisorConformacionCuadrilla
  Objetivo: listar tecnicos/auxiliares asociados a un supervisor
            segun la conformacion de cuadrillas.
  Uso de prueba: EXEC dbo.spx_ListarTecnicosSupervisorConformacionCuadrilla 87
*/
CREATE OR ALTER PROCEDURE dbo.spx_ListarTecnicosSupervisorConformacionCuadrilla
    @IdSupervisor INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdSupervisor IS NULL OR @IdSupervisor <= 0
    BEGIN
        SELECT CAST(NULL AS INT) AS idTecnico, CAST(NULL AS NVARCHAR(200)) AS tecnico
        WHERE 1 = 0;
        RETURN;
    END;

    DECLARE @sql NVARCHAR(MAX) = N'';

    IF OBJECT_ID(N'dbo.tbl_ConformacionCuadrillaDiario', N'U') IS NOT NULL
    BEGIN
        SET @sql = @sql + N'
        SELECT id_usuarioSupervisor AS idSupervisor, id_tecnico AS idTecnico
        FROM dbo.tbl_ConformacionCuadrillaDiario
        UNION ALL
        SELECT id_usuarioSupervisor AS idSupervisor, id_tecnicoAuxiliar AS idTecnico
        FROM dbo.tbl_ConformacionCuadrillaDiario
        ';
    END;

    IF OBJECT_ID(N'dbo.tbl_ConformacionCuadrillaDiarioWeb', N'U') IS NOT NULL
    BEGIN
        SET @sql = @sql + CASE WHEN LEN(@sql) > 0 THEN N' UNION ALL ' ELSE N'' END + N'
        SELECT id_usuarioSupervisor AS idSupervisor, id_tecnico AS idTecnico
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb
        UNION ALL
        SELECT id_usuarioSupervisor AS idSupervisor, id_tecnicoAuxiliar AS idTecnico
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb
        ';
    END;

    IF OBJECT_ID(N'dbo.conformacion_cuadrillas', N'U') IS NOT NULL
    BEGIN
        SET @sql = @sql + CASE WHEN LEN(@sql) > 0 THEN N' UNION ALL ' ELSE N'' END + N'
        SELECT id_supervisor AS idSupervisor, id_tecnico_principal AS idTecnico
        FROM dbo.conformacion_cuadrillas
        UNION ALL
        SELECT id_supervisor AS idSupervisor, id_tecnico_auxiliar AS idTecnico
        FROM dbo.conformacion_cuadrillas
        ';
    END;

    IF LEN(@sql) = 0
    BEGIN
        SELECT CAST(NULL AS INT) AS idTecnico, CAST(NULL AS NVARCHAR(200)) AS tecnico
        WHERE 1 = 0;
        RETURN;
    END;

    SET @sql = N'
    ;WITH base AS (
      ' + @sql + N'
    ),
    filtrada AS (
      SELECT DISTINCT idTecnico
      FROM base
      WHERE idSupervisor = @IdSupervisor
        AND idTecnico IS NOT NULL
    )
    SELECT f.idTecnico,
           COALESCE(NULLIF(LTRIM(RTRIM(u.Nombre)), ''''), CONCAT(''Tecnico '', f.idTecnico)) AS tecnico
    FROM filtrada f
    LEFT JOIN dbo.tbl_Usuario u
      ON u.Id_Usuario = f.idTecnico
    ORDER BY tecnico, f.idTecnico;';

    EXEC sp_executesql @sql, N'@IdSupervisor INT', @IdSupervisor = @IdSupervisor;
END
GO
