-- Privilegios en BDControlOrdenes usando la nueva tabla dbo.tbl_tablamenu
-- Incluye: menu base minimo + SP de lectura/guardado por rol

IF DB_ID('BDControlOrdenes') IS NULL
BEGIN
    RAISERROR('BDControlOrdenes no existe en este servidor.', 16, 1);
    RETURN;
END
GO

USE BDControlOrdenes
GO

IF OBJECT_ID('dbo.tbl_tablamenu', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tbl_tablamenu (
        Id INT IDENTITY(1,1) NOT NULL,
        nombre NVARCHAR(150) NULL,
        [orden] INT NULL,
        padre INT NULL,
        e_eliminado BIT NULL,
        fecharegistro DATETIME NULL,
        id_Usuario INT NULL,
        CONSTRAINT PK_tbl_tablamenu PRIMARY KEY (Id)
    );
END
GO

IF OBJECT_ID('dbo.tbl_RolMenu', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tbl_RolMenu (
        Id_RolMenu INT IDENTITY(1,1) NOT NULL,
        Id_Menu INT NULL,
        Id_Rol INT NULL,
        E_Eliminado BIT NULL,
        CONSTRAINT PK_tbl_RolMenu PRIMARY KEY (Id_RolMenu)
    );
END
GO

-- Menus minimos actuales
DECLARE @ahora DATETIME = GETDATE();

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tbl_tablamenu
    WHERE LOWER(LTRIM(RTRIM(nombre))) = 'tsm_conformacioncuadrillas'
)
BEGIN
    INSERT INTO dbo.tbl_tablamenu (nombre, [orden], padre, e_eliminado, fecharegistro, id_Usuario)
    VALUES (N'tsm_ConformacionCuadrillas', 1, 1, 0, @ahora, 1);
END

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tbl_tablamenu
    WHERE LOWER(LTRIM(RTRIM(nombre))) = 'tsm_listaagenda'
)
BEGIN
    INSERT INTO dbo.tbl_tablamenu (nombre, [orden], padre, e_eliminado, fecharegistro, id_Usuario)
    VALUES (N'tsm_ListaAgenda', 2, 1, 0, @ahora, 1);
END

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tbl_tablamenu
    WHERE LOWER(LTRIM(RTRIM(nombre))) = 'tsm_privilegios'
)
BEGIN
    INSERT INTO dbo.tbl_tablamenu (nombre, [orden], padre, e_eliminado, fecharegistro, id_Usuario)
    VALUES (N'tsm_privilegios', 3, 1, 0, @ahora, 1);
END

IF NOT EXISTS (
    SELECT 1
    FROM dbo.tbl_tablamenu
    WHERE LOWER(LTRIM(RTRIM(nombre))) = 'prueba'
)
BEGIN
    INSERT INTO dbo.tbl_tablamenu (nombre, [orden], padre, e_eliminado, fecharegistro, id_Usuario)
    VALUES (N'prueba', 4, 1, 0, @ahora, 1);
END
GO

IF OBJECT_ID('dbo.spx_ObtenerPrivilegiosRoles', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ObtenerPrivilegiosRoles AS SELECT 1 AS placeholder;');
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC dbo.spx_ObtenerPrivilegiosRoles
AS
BEGIN
    SET NOCOUNT ON;

    SELECT r.Id_Rol,
           r.Nombre AS Rol
    FROM dbo.tbl_Rol r
    WHERE ISNULL(r.E_Eliminado, 0) = 0
    ORDER BY r.Nombre;
END
GO

IF OBJECT_ID('dbo.spx_ObtenerPrivilegiosRolDetalle', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_ObtenerPrivilegiosRolDetalle AS SELECT 1 AS placeholder;');
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC dbo.spx_ObtenerPrivilegiosRolDetalle
    @IdRol INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdRol IS NULL
    BEGIN
        RAISERROR('IdRol es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Rol r
        WHERE r.Id_Rol = @IdRol
          AND ISNULL(r.E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Rol no encontrado o inactivo.', 16, 1);
        RETURN;
    END

    SELECT m.Id AS Id_Menu,
           m.nombre AS Nombre,
           m.[orden] AS Nivel,
           m.padre AS Padre,
           CASE
               WHEN rm.Id_RolMenu IS NULL THEN CAST(0 AS bit)
               ELSE CAST(1 AS bit)
           END AS Asignado
    FROM dbo.tbl_tablamenu m
    LEFT JOIN dbo.tbl_RolMenu rm
           ON rm.Id_Menu = m.Id
          AND rm.Id_Rol = @IdRol
          AND ISNULL(rm.E_Eliminado, 0) = 0
    WHERE ISNULL(m.e_eliminado, 0) = 0
    ORDER BY m.padre, m.[orden], m.Id;
END
GO

IF OBJECT_ID('dbo.spx_GuardarPrivilegiosRol', 'P') IS NULL
BEGIN
    EXEC('CREATE PROC dbo.spx_GuardarPrivilegiosRol AS SELECT 1 AS placeholder;');
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC dbo.spx_GuardarPrivilegiosRol
    @IdRol INT,
    @MenuIdsCsv NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @IdRol IS NULL
    BEGIN
        RAISERROR('IdRol es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Rol r
        WHERE r.Id_Rol = @IdRol
          AND ISNULL(r.E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Rol no encontrado o inactivo.', 16, 1);
        RETURN;
    END

    DECLARE @MenuIds TABLE (
        Id_Menu INT NOT NULL PRIMARY KEY
    );

    SET @MenuIdsCsv = ISNULL(@MenuIdsCsv, '');
    SET @MenuIdsCsv = REPLACE(@MenuIdsCsv, ' ', '');

    IF LEN(LTRIM(RTRIM(@MenuIdsCsv))) > 0
    BEGIN
        DECLARE @xml XML;
        DECLARE @sanitized NVARCHAR(MAX);
        SET @sanitized = @MenuIdsCsv;

        WHILE CHARINDEX(',,', @sanitized) > 0
        BEGIN
            SET @sanitized = REPLACE(@sanitized, ',,', ',');
        END

        IF LEFT(@sanitized, 1) = ','
        BEGIN
            SET @sanitized = SUBSTRING(@sanitized, 2, LEN(@sanitized) - 1);
        END

        IF RIGHT(@sanitized, 1) = ','
        BEGIN
            SET @sanitized = LEFT(@sanitized, LEN(@sanitized) - 1);
        END

        IF LEN(@sanitized) > 0
        BEGIN
            SET @xml = CAST('<x>' + REPLACE(@sanitized, ',', '</x><x>') + '</x>' AS XML);

            INSERT INTO @MenuIds (Id_Menu)
            SELECT DISTINCT CAST(T.c.value('.', 'nvarchar(30)') AS INT)
            FROM @xml.nodes('/x') AS T(c)
            WHERE ISNUMERIC(T.c.value('.', 'nvarchar(30)')) = 1
              AND CAST(T.c.value('.', 'nvarchar(30)') AS INT) > 0;
        END
    END

    IF EXISTS (
        SELECT 1
        FROM @MenuIds i
        LEFT JOIN dbo.tbl_tablamenu m
               ON m.Id = i.Id_Menu
              AND ISNULL(m.e_eliminado, 0) = 0
        WHERE m.Id IS NULL
    )
    BEGIN
        RAISERROR('MenuIds contiene elementos inexistentes o inactivos.', 16, 1);
        RETURN;
    END

    BEGIN TRY
        BEGIN TRANSACTION;

        UPDATE dbo.tbl_RolMenu
        SET E_Eliminado = 1
        WHERE Id_Rol = @IdRol
          AND ISNULL(E_Eliminado, 0) = 0;

        UPDATE rm
        SET rm.E_Eliminado = 0
        FROM dbo.tbl_RolMenu rm
        INNER JOIN @MenuIds i ON i.Id_Menu = rm.Id_Menu
        WHERE rm.Id_Rol = @IdRol;

        INSERT INTO dbo.tbl_RolMenu (Id_Menu, Id_Rol, E_Eliminado)
        SELECT i.Id_Menu, @IdRol, 0
        FROM @MenuIds i
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.tbl_RolMenu rm
            WHERE rm.Id_Rol = @IdRol
              AND rm.Id_Menu = i.Id_Menu
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
        END
        DECLARE @ErrMsg NVARCHAR(4000);
        SET @ErrMsg = ERROR_MESSAGE();
        RAISERROR(@ErrMsg, 16, 1);
        RETURN;
    END CATCH

    EXEC dbo.spx_ObtenerPrivilegiosRolDetalle @IdRol;
END
GO
