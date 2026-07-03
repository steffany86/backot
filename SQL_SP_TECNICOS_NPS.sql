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

    DECLARE @IdUsuarioTecnicoSesion INT = NULL;
    DECLARE @IdVendedorSesion INT = NULL;
    DECLARE @TablaUsuarioTecnico SYSNAME = NULL;
    DECLARE @Sql NVARCHAR(MAX);
    DECLARE @ColId NVARCHAR(128);
    DECLARE @ColIdUsuario NVARCHAR(128);
    DECLARE @ColIdUsuarioAlt NVARCHAR(128);
    DECLARE @ColIdTecnico NVARCHAR(128);
    DECLARE @ColIdTecnicoAlt NVARCHAR(128);
    DECLARE @ColIdVendedor NVARCHAR(128);
    DECLARE @ColNombre NVARCHAR(128);
    DECLARE @ColEliminado NVARCHAR(128);
    DECLARE @ExprIdUsuario NVARCHAR(MAX);
    DECLARE @ExprIdTecnico NVARCHAR(MAX);

    CREATE TABLE #GrupoSupNps (
        id_grupo INT NULL,
        id_usuario INT NULL
    );

    CREATE TABLE #VendedorNps (
        id_vendedor INT NULL,
        nombre NVARCHAR(250) NULL,
        nombreNps NVARCHAR(250) NULL
    );

    IF OBJECT_ID('dbo.tbl_GrupoSup', 'U') IS NOT NULL
    BEGIN
        INSERT INTO #GrupoSupNps (id_grupo, id_usuario)
        SELECT id_grupo, id_usuario
        FROM dbo.tbl_GrupoSup;
    END

    IF OBJECT_ID('dbo.tbl_Vendedor', 'U') IS NOT NULL
    BEGIN
        INSERT INTO #VendedorNps (id_vendedor, nombre, nombreNps)
        SELECT Id_Vendedor, Nombre, NombreNPS
        FROM dbo.tbl_Vendedor
        WHERE ISNULL(E_Eliminado, 0) = 0;
    END

    IF OBJECT_ID('dbo.tbl_UsuarioTecnico', 'U') IS NOT NULL SET @TablaUsuarioTecnico = 'dbo.tbl_UsuarioTecnico';
    ELSE IF OBJECT_ID('dbo.tbl_usuariotecnico', 'U') IS NOT NULL SET @TablaUsuarioTecnico = 'dbo.tbl_usuariotecnico';
    ELSE IF OBJECT_ID('dbo.tbl_usuaritecnico', 'U') IS NOT NULL SET @TablaUsuarioTecnico = 'dbo.tbl_usuaritecnico';

    CREATE TABLE #UsuarioTecnicoNps (
        id INT NULL,
        id_usuario INT NULL,
        id_tecnico INT NULL,
        id_vendedor INT NULL,
        nombre NVARCHAR(250) NULL
    );

    IF @TablaUsuarioTecnico IS NOT NULL
    BEGIN
        SET @ColId = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'id') IS NOT NULL THEN 'id'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Id') IS NOT NULL THEN 'Id'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'id_usuario_tecnico') IS NOT NULL THEN 'id_usuario_tecnico'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Id_Usuario_Tecnico') IS NOT NULL THEN 'Id_Usuario_Tecnico'
            ELSE NULL
        END;
        SET @ColIdUsuario = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'id_usuario') IS NOT NULL THEN 'id_usuario'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Id_Usuario') IS NOT NULL THEN 'Id_Usuario'
            ELSE NULL
        END;
        SET @ColIdUsuarioAlt = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'idusuario') IS NOT NULL THEN 'idusuario'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'IdUsuario') IS NOT NULL THEN 'IdUsuario'
            ELSE NULL
        END;
        SET @ColIdTecnico = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'id_tecnico') IS NOT NULL THEN 'id_tecnico'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Id_Tecnico') IS NOT NULL THEN 'Id_Tecnico'
            ELSE NULL
        END;
        SET @ColIdTecnicoAlt = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'idtecnico') IS NOT NULL THEN 'idtecnico'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'IdTecnico') IS NOT NULL THEN 'IdTecnico'
            ELSE NULL
        END;
        SET @ColIdVendedor = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'id_vendedor') IS NOT NULL THEN 'id_vendedor'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Id_Vendedor') IS NOT NULL THEN 'Id_Vendedor'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'idvendedor') IS NOT NULL THEN 'idvendedor'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'IdVendedor') IS NOT NULL THEN 'IdVendedor'
            ELSE NULL
        END;
        SET @ColNombre = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'nombre') IS NOT NULL THEN 'nombre'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'Nombre') IS NOT NULL THEN 'Nombre'
            ELSE NULL
        END;
        SET @ColEliminado = CASE
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'e_eliminado') IS NOT NULL THEN 'e_eliminado'
            WHEN COL_LENGTH(@TablaUsuarioTecnico, 'E_Eliminado') IS NOT NULL THEN 'E_Eliminado'
            ELSE NULL
        END;
        SET @ExprIdUsuario = CASE
            WHEN @ColIdUsuario IS NOT NULL AND @ColIdUsuarioAlt IS NOT NULL
                THEN 'COALESCE(CONVERT(INT, ' + QUOTENAME(@ColIdUsuario) + '), CONVERT(INT, ' + QUOTENAME(@ColIdUsuarioAlt) + '))'
            WHEN @ColIdUsuario IS NOT NULL
                THEN 'CONVERT(INT, ' + QUOTENAME(@ColIdUsuario) + ')'
            WHEN @ColIdUsuarioAlt IS NOT NULL
                THEN 'CONVERT(INT, ' + QUOTENAME(@ColIdUsuarioAlt) + ')'
            ELSE 'NULL'
        END;
        SET @ExprIdTecnico = CASE
            WHEN @ColIdTecnico IS NOT NULL AND @ColIdTecnicoAlt IS NOT NULL
                THEN 'COALESCE(CONVERT(INT, ' + QUOTENAME(@ColIdTecnico) + '), CONVERT(INT, ' + QUOTENAME(@ColIdTecnicoAlt) + '))'
            WHEN @ColIdTecnico IS NOT NULL
                THEN 'CONVERT(INT, ' + QUOTENAME(@ColIdTecnico) + ')'
            WHEN @ColIdTecnicoAlt IS NOT NULL
                THEN 'CONVERT(INT, ' + QUOTENAME(@ColIdTecnicoAlt) + ')'
            ELSE 'NULL'
        END;

        SET @Sql = N'
            INSERT INTO #UsuarioTecnicoNps (id, id_usuario, id_tecnico, id_vendedor, nombre)
            SELECT
                ' + CASE WHEN @ColId IS NULL THEN 'NULL' ELSE 'CONVERT(INT, ' + QUOTENAME(@ColId) + ')' END + N',
                ' + @ExprIdUsuario + N',
                ' + @ExprIdTecnico + N',
                ' + CASE WHEN @ColIdVendedor IS NULL THEN 'NULL' ELSE 'CONVERT(INT, ' + QUOTENAME(@ColIdVendedor) + ')' END + N',
                ' + CASE WHEN @ColNombre IS NULL THEN 'NULL' ELSE 'CONVERT(NVARCHAR(250), ' + QUOTENAME(@ColNombre) + ')' END + N'
            FROM ' + @TablaUsuarioTecnico + N'
            WHERE ' + CASE WHEN @ColEliminado IS NULL THEN '1 = 1' ELSE 'ISNULL(' + QUOTENAME(@ColEliminado) + ', 0) = 0' END + N';';

        EXEC sp_executesql @Sql;

        SELECT TOP 1
            @IdUsuarioTecnicoSesion = id,
            @IdVendedorSesion = id_vendedor
        FROM #UsuarioTecnicoNps
        WHERE id_usuario = @IdUsuarioSesion
           OR id_tecnico = @IdUsuarioSesion
           OR id = @IdUsuarioSesion
           OR id_vendedor = @IdUsuarioSesion
        ORDER BY id DESC;
    END

    ;WITH grupos_sesion AS (
        SELECT DISTINCT g.id_grupo
        FROM dbo.tbl_Grupo g
        LEFT JOIN #GrupoSupNps gs
               ON gs.id_grupo = g.id_grupo
        LEFT JOIN dbo.tbl_DetalleGrupo dg_sesion
               ON dg_sesion.id_grupo = g.id_grupo
        LEFT JOIN dbo.tbl_GrupoBackup gb
               ON gb.id_grupo = g.id_grupo
              AND ISNULL(gb.e_activo, 0) = 1
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND (
                gs.id_usuario = ISNULL(@IdSupervisor, @IdUsuarioSesion)
             OR gs.id_usuario = @IdUsuarioSesion
             OR dg_sesion.id_usuario_tecnico IN (
                    @IdUsuarioSesion,
                    ISNULL(@IdUsuarioTecnicoSesion, -1),
                    ISNULL(@IdVendedorSesion, -1)
                )
             OR gb.id_usuario_tecnico_temporal IN (
                    @IdUsuarioSesion,
                    ISNULL(@IdUsuarioTecnicoSesion, -1),
                    ISNULL(@IdVendedorSesion, -1)
                )
          )
    ),
    tecnicos_grupo AS (
        SELECT DISTINCT
            g.id_grupo AS idGrupo,
            g.nombre AS grupo,
            gs.id_usuario AS idSupervisor,
            dg.id_usuario_tecnico AS idUsuarioTecnico,
            COALESCE(ut.id_vendedor, dg.id_usuario_tecnico) AS idTecnico,
            COALESCE(
                NULLIF(LTRIM(RTRIM(v.Nombre)), ''),
                NULLIF(LTRIM(RTRIM(ut.nombre)), ''),
                'Tecnico ' + CONVERT(NVARCHAR(20), COALESCE(ut.id_vendedor, dg.id_usuario_tecnico))
            ) AS tecnico,
            NULLIF(LTRIM(RTRIM(v.nombreNps)), '') AS nombreNps
        FROM grupos_sesion s
        INNER JOIN dbo.tbl_Grupo g
                ON g.id_grupo = s.id_grupo
        LEFT JOIN #GrupoSupNps gs
               ON gs.id_grupo = g.id_grupo
        INNER JOIN dbo.tbl_DetalleGrupo dg
                ON dg.id_grupo = g.id_grupo
        LEFT JOIN #UsuarioTecnicoNps ut
               ON ut.id = dg.id_usuario_tecnico
        LEFT JOIN #VendedorNps v
               ON v.id_vendedor = COALESCE(ut.id_vendedor, dg.id_usuario_tecnico)
        WHERE dg.id_usuario_tecnico IS NOT NULL
          AND dg.id_usuario_tecnico > 0
    )
    SELECT
        idTecnico,
        tecnico,
        nombreNps,
        tecnico AS tecnicoNombreOperativo,
        idUsuarioTecnico,
        idGrupo,
        grupo,
        idSupervisor
    FROM tecnicos_grupo
    WHERE idTecnico IS NOT NULL
      AND idTecnico > 0
    ORDER BY tecnico;
END
GO
