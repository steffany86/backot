-- Stored procedures para el grid de /jornadas/historico (SupervisionRepository.java).
--
-- IMPORTANTE - topologia de bases de datos:
--   1) spx_Sup_ListarTecnicosEsperadosJornada  -> ejecutar en BDControlOrdenes (central)
--   2) spx_Sup_ResolverUsuariosPorTecnico       -> ejecutar en la BD "tigohogar" de CADA sucursal
--      (la app resuelve la conexion por sucursal via dbConnectionManager/resolveJdbcTemplateBySucursalNombre,
--       asi que este SP debe existir en cada base fisica de sucursal que tenga tbl_UsuarioTecnico)
--   3) spx_Sup_ListarHistoricoJornadas          -> ejecutar en la BD "tigohogar" fija (tigohogarJdbcTemplate),
--      que es la que contiene tbl_InicioJornadaAlturas para todas las sucursales
--
-- Compatible con SQL Server 2008 (no usa STRING_SPLIT ni TRY_CAST, ambas de versiones
-- posteriores). El split de listas de IDs se hace con la funcion dbo.fn_Sup_SplitInts
-- basada en XML, que debe crearse en cada base donde corran los SP 2 y 3.

-- =====================================================================
-- 0) Funcion auxiliar de split (crear en las mismas bases que los SP 2 y 3)
-- =====================================================================
IF OBJECT_ID('dbo.fn_Sup_SplitInts', 'TF') IS NOT NULL
    DROP FUNCTION dbo.fn_Sup_SplitInts;
GO

CREATE FUNCTION dbo.fn_Sup_SplitInts (@Csv NVARCHAR(MAX))
RETURNS @Result TABLE (Value INT)
AS
BEGIN
    DECLARE @Xml XML;
    IF @Csv IS NULL OR LTRIM(RTRIM(@Csv)) = ''
        RETURN;

    SET @Xml = CAST('<i>' + REPLACE(@Csv, ',', '</i><i>') + '</i>' AS XML);

    INSERT INTO @Result (Value)
    SELECT CAST(LTRIM(RTRIM(T.c.value('.', 'NVARCHAR(50)'))) AS INT)
    FROM @Xml.nodes('/i') AS T(c)
    WHERE ISNUMERIC(LTRIM(RTRIM(T.c.value('.', 'NVARCHAR(50)')))) = 1;

    RETURN;
END
GO

-- =====================================================================
-- 1) Tecnicos esperados de la jornada (BDControlOrdenes)
-- =====================================================================
IF OBJECT_ID('dbo.spx_Sup_ListarTecnicosEsperadosJornada', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_Sup_ListarTecnicosEsperadosJornada AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_Sup_ListarTecnicosEsperadosJornada
    @Sucursal     NVARCHAR(200) = NULL,
    @IdSupervisor INT           = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @SucursalNorm NVARCHAR(200) = NULLIF(LTRIM(RTRIM(@Sucursal)), '');
    DECLARE @AuxColumn SYSNAME;
    DECLARE @Sql NVARCHAR(MAX);

    -- La tabla historicamente tuvo la columna en camelCase o snake_case segun el entorno;
    -- se detecta una sola vez en vez del reintento try/catch que hacia el repositorio Java.
    IF COL_LENGTH('dbo.tbl_ConformacionCuadrillaDiario', 'id_tecnicoAuxiliar') IS NOT NULL
        SET @AuxColumn = N'id_tecnicoAuxiliar';
    ELSE
        SET @AuxColumn = N'id_tecnico_auxiliar';

    SET @Sql = N'
    WITH tecnicos AS (
        SELECT c.sucursal, c.grupo, c.idUsuarioSupervisor, c.supervisorACargo,
               CAST(c.id_tecnico AS INT) AS idTecnico, c.tecnico AS tecnico, c.fecha, c.fechaRegistro, c.id
        FROM dbo.tbl_ConformacionCuadrillaDiario c
        WHERE ISNULL(c.e_eliminado, 0) = 0
          AND (@SucursalNorm IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(c.sucursal, ''''))), ''_'', ''''), ''-'', ''''), '' '', ''''))
                                      = LOWER(REPLACE(REPLACE(REPLACE(@SucursalNorm, ''_'', ''''), ''-'', ''''), '' '', '''')))
          AND (@IdSupervisor IS NULL OR CAST(c.idUsuarioSupervisor AS INT) = @IdSupervisor)
          AND c.id_tecnico IS NOT NULL AND c.id_tecnico > 0
        UNION ALL
        SELECT c.sucursal, c.grupo, c.idUsuarioSupervisor, c.supervisorACargo,
               CAST(c.' + @AuxColumn + N' AS INT) AS idTecnico, c.auxiliar AS tecnico, c.fecha, c.fechaRegistro, c.id
        FROM dbo.tbl_ConformacionCuadrillaDiario c
        WHERE ISNULL(c.e_eliminado, 0) = 0
          AND (@SucursalNorm IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(c.sucursal, ''''))), ''_'', ''''), ''-'', ''''), '' '', ''''))
                                      = LOWER(REPLACE(REPLACE(REPLACE(@SucursalNorm, ''_'', ''''), ''-'', ''''), '' '', '''')))
          AND (@IdSupervisor IS NULL OR CAST(c.idUsuarioSupervisor AS INT) = @IdSupervisor)
          AND c.' + @AuxColumn + N' IS NOT NULL AND c.' + @AuxColumn + N' > 0
    ), ranked AS (
        SELECT *, ROW_NUMBER() OVER (
            PARTITION BY idTecnico
            ORDER BY ISNULL(fecha, ''19000101'') DESC, ISNULL(fechaRegistro, ''19000101'') DESC, id DESC
        ) AS rn
        FROM tecnicos
    )
    SELECT CAST(idTecnico AS INT) AS idTecnico, CAST(idTecnico AS INT) AS id_tecnico,
           tecnico AS tecnicoNombre, tecnico, sucursal, grupo,
           CAST(idUsuarioSupervisor AS INT) AS idSupervisor, supervisorACargo AS supervisorNombre
    FROM ranked WHERE rn = 1
    ORDER BY sucursal, grupo, tecnico;';

    EXEC sp_executesql @Sql,
        N'@SucursalNorm NVARCHAR(200), @IdSupervisor INT',
        @SucursalNorm = @SucursalNorm, @IdSupervisor = @IdSupervisor;
END
GO

-- =====================================================================
-- 2) Resolver usuarios asociados a cada tecnico (BD "tigohogar" por sucursal)
-- =====================================================================
IF OBJECT_ID('dbo.spx_Sup_ResolverUsuariosPorTecnico', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_Sup_ResolverUsuariosPorTecnico AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_Sup_ResolverUsuariosPorTecnico
    @IdsTecnico NVARCHAR(MAX)  -- CSV de id_tecnico (Id_Vendedor), ej: '101,205,309'
AS
BEGIN
    SET NOCOUNT ON;

    -- tbl_ruta puede ligar al vendedor por id_vendedor o por id_tecnico segun la instalacion;
    -- se detecta la columna una sola vez, igual que el fallback de tbl_ConformacionCuadrillaDiario.
    DECLARE @RutaVendedorColumn SYSNAME;
    DECLARE @Sql NVARCHAR(MAX);

    IF COL_LENGTH('dbo.tbl_ruta', 'id_vendedor') IS NOT NULL
        SET @RutaVendedorColumn = N'id_vendedor';
    ELSE IF COL_LENGTH('dbo.tbl_ruta', 'id_tecnico') IS NOT NULL
        SET @RutaVendedorColumn = N'id_tecnico';
    ELSE
        SET @RutaVendedorColumn = NULL;

    IF @RutaVendedorColumn IS NULL OR OBJECT_ID('dbo.tbl_ruta', 'U') IS NULL
    BEGIN
        -- No se encontro tbl_ruta o no se pudo determinar su columna de vendedor:
        -- se devuelve tieneRutaActiva = 1 (desconocido) para no ocultar tecnicos por error.
        SELECT ut.Id_Vendedor, ut.id_Usuario, CAST(1 AS BIT) AS tieneRutaActiva
        FROM dbo.tbl_UsuarioTecnico ut
        WHERE ISNULL(ut.e_eliminado, 0) = 0
          AND ut.Id_Vendedor IN (SELECT Value FROM dbo.fn_Sup_SplitInts(@IdsTecnico))
        ORDER BY ut.id DESC;
        RETURN;
    END

    SET @Sql = N'
    SELECT ut.Id_Vendedor, ut.id_Usuario,
           CAST(CASE WHEN EXISTS (
               SELECT 1 FROM dbo.tbl_ruta r
               WHERE r.' + @RutaVendedorColumn + N' = ut.Id_Vendedor
                 AND ISNULL(r.e_eliminado, 0) = 0
           ) THEN 1 ELSE 0 END AS BIT) AS tieneRutaActiva
    FROM dbo.tbl_UsuarioTecnico ut
    WHERE ISNULL(ut.e_eliminado, 0) = 0
      AND ut.Id_Vendedor IN (SELECT Value FROM dbo.fn_Sup_SplitInts(@IdsTecnico))
    ORDER BY ut.id DESC;';

    EXEC sp_executesql @Sql, N'@IdsTecnico NVARCHAR(MAX)', @IdsTecnico = @IdsTecnico;
END
GO

-- =====================================================================
-- 3) Historico de inicios/cierres de jornada (BD "tigohogar" fija)
-- =====================================================================
IF OBJECT_ID('dbo.spx_Sup_ListarHistoricoJornadas', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_Sup_ListarHistoricoJornadas AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_Sup_ListarHistoricoJornadas
    @FechaDesde  DATE,
    @FechaHasta  DATE,
    @Sucursal    NVARCHAR(200) = NULL,
    @IdsTecnico  NVARCHAR(MAX) = NULL  -- CSV de id_tecnico (id de usuario), opcional
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @SucursalNorm NVARCHAR(200) = NULLIF(LTRIM(RTRIM(@Sucursal)), '');
    DECLARE @HastaExclusivo DATETIME = DATEADD(DAY, 1, CAST(@FechaHasta AS DATETIME));

    -- A diferencia del repositorio Java (que recorria dia por dia y acumulaba),
    -- este SP resuelve todo el rango @FechaDesde..@FechaHasta en una sola consulta.
    SELECT ij.id_inicio, ij.id_tecnico, ij.id_auxiliar, ij.id_encargado,
           ij.fecha_registro, ij.fecha_cierre, ij.pendiente, ij.e_eliminado, ij.no_marco_cierre,
           ij.id_usuario_supervisor_grupo, ij.id_sucursal, ij.sucursal,
           ij.nombre_tecnico, ij.tecnico_nombre, ij.EstoyTrabajandoSolo AS estoy_trabajando_solo,
           ij.firma_inicio, ij.firma_cierre, ij.supervisor_aprobo_inicio, ij.fecha_aprobacion_inicio
    FROM dbo.tbl_InicioJornadaAlturas ij
    WHERE ij.fecha_registro >= @FechaDesde
      AND ij.fecha_registro < @HastaExclusivo
      AND ISNULL(ij.e_eliminado, 0) = 0
      AND (
            @SucursalNorm IS NULL
            OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(ij.sucursal, ''))), '_', ''), '-', ''), ' ', ''))
             = LOWER(REPLACE(REPLACE(REPLACE(@SucursalNorm, '_', ''), '-', ''), ' ', ''))
          )
      AND (
            @IdsTecnico IS NULL
            OR ij.id_tecnico IN (SELECT Value FROM dbo.fn_Sup_SplitInts(@IdsTecnico))
          )
    ORDER BY ij.fecha_registro ASC, ij.id_inicio ASC;
END
GO
