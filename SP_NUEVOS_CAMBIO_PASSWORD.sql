/*
    SP_NUEVOS_CAMBIO_PASSWORD.sql
    Ejecutar en cada BD de sucursal.

    Objetivo:
    1) Dejar la politica de cambio configurable en BD (sin hardcode en codigo).
    2) Permitir consultar si un usuario debe cambiar password.
    3) Cambiar password actualizando NecesitaCambio y UltimaModificacion.
    4) Permitir forzar cambio de password por usuario.
*/

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

/* =========================================================
   1) TABLA DE CONFIGURACION DE SEGURIDAD (GLOBAL)
   ========================================================= */
IF OBJECT_ID('dbo.tbl_configuracion_seguridad', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tbl_configuracion_seguridad
    (
        Id_Configuracion       int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Activo                 bit NOT NULL CONSTRAINT DF_tbl_cfgseg_Activo DEFAULT (1),
        RequierePorFlag        bit NOT NULL CONSTRAINT DF_tbl_cfgseg_ReqPorFlag DEFAULT (1), -- usa NecesitaCambio
        DiasVigenciaPassword   int NOT NULL CONSTRAINT DF_tbl_cfgseg_DiasVigencia DEFAULT (60), -- 0 desactiva control por dias
        FechaCreacion          datetime NOT NULL CONSTRAINT DF_tbl_cfgseg_FechaCreacion DEFAULT (GETDATE()),
        FechaModificacion      datetime NULL
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tbl_configuracion_seguridad
    WHERE Activo = 1
)
BEGIN
    INSERT INTO dbo.tbl_configuracion_seguridad
    (
        Activo,
        RequierePorFlag,
        DiasVigenciaPassword,
        FechaCreacion,
        FechaModificacion
    )
    VALUES
    (
        1,
        1,
        60,
        GETDATE(),
        GETDATE()
    );
END
GO

/* =========================================================
   2) SP: ACTUALIZAR POLITICA DE CAMBIO DE PASSWORD
      (asi cambias valores sin tocar codigo ni SP)
   ========================================================= */
IF OBJECT_ID('dbo.spx_ConfigurarPoliticaPassword', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ConfigurarPoliticaPassword AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_ConfigurarPoliticaPassword
    @RequierePorFlag bit,
    @DiasVigenciaPassword int
AS
BEGIN
    SET NOCOUNT ON;

    IF @DiasVigenciaPassword < 0
    BEGIN
        RAISERROR('DiasVigenciaPassword no puede ser negativo.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_configuracion_seguridad
    SET Activo = 0
    WHERE Activo = 1;

    INSERT INTO dbo.tbl_configuracion_seguridad
    (
        Activo,
        RequierePorFlag,
        DiasVigenciaPassword,
        FechaCreacion,
        FechaModificacion
    )
    VALUES
    (
        1,
        @RequierePorFlag,
        @DiasVigenciaPassword,
        GETDATE(),
        GETDATE()
    );

    SELECT
        RequierePorFlag,
        DiasVigenciaPassword
    FROM dbo.tbl_configuracion_seguridad
    WHERE Activo = 1;
END
GO

/* =========================================================
   3) SP: CONSULTAR SI USUARIO DEBE CAMBIAR PASSWORD
   ========================================================= */
IF OBJECT_ID('dbo.spx_UsuarioDebeCambiarPassword', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_UsuarioDebeCambiarPassword AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_UsuarioDebeCambiarPassword
    @Login nvarchar(50)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @RequierePorFlag bit = 1;
    DECLARE @DiasVigenciaPassword int = 60;

    SELECT TOP 1
        @RequierePorFlag = RequierePorFlag,
        @DiasVigenciaPassword = DiasVigenciaPassword
    FROM dbo.tbl_configuracion_seguridad
    WHERE Activo = 1
    ORDER BY Id_Configuracion DESC;

    ;WITH U AS
    (
        SELECT TOP 1
            u.Id_Usuario,
            u.Loggin,
            u.Nombre,
            CAST(ISNULL(u.NecesitaCambio, 0) AS bit) AS NecesitaCambio,
            u.UltimaModificacion,
            CASE
                WHEN u.UltimaModificacion IS NULL THEN NULL
                ELSE DATEDIFF(DAY, u.UltimaModificacion, GETDATE())
            END AS DiasDesdeUltimaModificacion
        FROM dbo.tbl_usuario u
        WHERE u.E_Eliminado = 0
          AND u.Loggin = @Login
    )
    SELECT
        U.Id_Usuario,
        U.Loggin,
        U.Nombre,
        U.NecesitaCambio,
        U.UltimaModificacion,
        U.DiasDesdeUltimaModificacion,
        @RequierePorFlag AS RequierePorFlag,
        @DiasVigenciaPassword AS DiasVigenciaPassword,
        CAST(
            CASE
                WHEN @RequierePorFlag = 1 AND U.NecesitaCambio = 1 THEN 1
                WHEN @DiasVigenciaPassword > 0
                     AND ISNULL(U.DiasDesdeUltimaModificacion, 999999) > @DiasVigenciaPassword THEN 1
                ELSE 0
            END
            AS bit
        ) AS DebeCambiar
    FROM U;
END
GO

/* =========================================================
   4) SP: CAMBIAR PASSWORD
      - valida password actual
      - actualiza Password, NecesitaCambio, UltimaModificacion
   ========================================================= */
IF OBJECT_ID('dbo.spx_CambiarPasswordUsuario', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_CambiarPasswordUsuario AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_CambiarPasswordUsuario
    @Login nvarchar(50),
    @PasswordHashActual varchar(100),
    @PasswordHashNueva varchar(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF ISNULL(LTRIM(RTRIM(@Login)), '') = ''
    BEGIN
        RAISERROR('Login requerido.', 16, 1);
        RETURN;
    END

    IF ISNULL(LTRIM(RTRIM(@PasswordHashNueva)), '') = ''
    BEGIN
        RAISERROR('Password nueva requerida.', 16, 1);
        RETURN;
    END

    IF @PasswordHashActual = @PasswordHashNueva
    BEGIN
        RAISERROR('La nueva password no puede ser igual a la actual.', 16, 1);
        RETURN;
    END

    DECLARE @IdUsuario int;

    SELECT TOP 1
        @IdUsuario = u.Id_Usuario
    FROM dbo.tbl_usuario u
    WHERE u.E_Eliminado = 0
      AND u.Loggin = @Login
      AND u.Password = @PasswordHashActual;

    IF @IdUsuario IS NULL
    BEGIN
        RAISERROR('Usuario no existe o password actual incorrecta.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_usuario
    SET Password = @PasswordHashNueva,
        NecesitaCambio = 0,
        UltimaModificacion = GETDATE()
    WHERE Id_Usuario = @IdUsuario;

    SELECT
        CAST(1 AS bit) AS Exito,
        'Password actualizada correctamente.' AS Mensaje,
        @IdUsuario AS Id_Usuario;
END
GO

IF OBJECT_ID('dbo.spx_CambiarPasswordUsuarioPorId', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_CambiarPasswordUsuarioPorId AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_CambiarPasswordUsuarioPorId
    @Id_Usuario int,
    @PasswordHashActual varchar(100),
    @PasswordHashNueva varchar(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF @Id_Usuario IS NULL OR @Id_Usuario <= 0
    BEGIN
        RAISERROR('Id_Usuario requerido.', 16, 1);
        RETURN;
    END

    IF ISNULL(LTRIM(RTRIM(@PasswordHashNueva)), '') = ''
    BEGIN
        RAISERROR('Password nueva requerida.', 16, 1);
        RETURN;
    END

    IF @PasswordHashActual = @PasswordHashNueva
    BEGIN
        RAISERROR('La nueva password no puede ser igual a la actual.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_usuario u
        WHERE u.E_Eliminado = 0
          AND u.Id_Usuario = @Id_Usuario
          AND u.Password = @PasswordHashActual
    )
    BEGIN
        RAISERROR('Usuario no existe o password actual incorrecta.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_usuario
    SET Password = @PasswordHashNueva,
        NecesitaCambio = 0,
        UltimaModificacion = GETDATE()
    WHERE Id_Usuario = @Id_Usuario;

    SELECT
        CAST(1 AS bit) AS Exito,
        'Password actualizada correctamente.' AS Mensaje,
        @Id_Usuario AS Id_Usuario;
END
GO

/* =========================================================
   5) SP: FORZAR/QUITAR CAMBIO DE PASSWORD (ADMIN)
   ========================================================= */
IF OBJECT_ID('dbo.spx_ForzarCambioPasswordUsuario', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ForzarCambioPasswordUsuario AS SELECT 1 AS placeholder;');
END
GO

ALTER PROC dbo.spx_ForzarCambioPasswordUsuario
    @Login nvarchar(50),
    @NecesitaCambio bit
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_usuario
    SET NecesitaCambio = @NecesitaCambio,
        UltimaModificacion = CASE WHEN @NecesitaCambio = 1 THEN ISNULL(UltimaModificacion, GETDATE()) ELSE GETDATE() END
    WHERE E_Eliminado = 0
      AND Loggin = @Login;

    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Usuario no encontrado.', 16, 1);
        RETURN;
    END

    SELECT
        CAST(1 AS bit) AS Exito,
        'Actualizacion realizada.' AS Mensaje,
        @Login AS Loggin,
        @NecesitaCambio AS NecesitaCambio;
END
GO

/* =========================================================
   EJEMPLOS RAPIDOS DE USO
   =========================================================
   -- Cambiar politica a 45 dias:
   EXEC dbo.spx_ConfigurarPoliticaPassword @RequierePorFlag = 1, @DiasVigenciaPassword = 45;

   -- Validar si usuario debe cambiar:
   EXEC dbo.spx_UsuarioDebeCambiarPassword @Login = 'stefany';

   -- Cambiar password:
   EXEC dbo.spx_CambiarPasswordUsuario
        @Login = 'stefany',
        @PasswordHashActual = 'HASH_ACTUAL_MD5_BASE64',
        @PasswordHashNueva = 'HASH_NUEVA_MD5_BASE64';

   -- Forzar cambio:
   EXEC dbo.spx_ForzarCambioPasswordUsuario @Login = 'stefany', @NecesitaCambio = 1;
*/
