IF DB_ID('BDControlOrdenes') IS NULL
BEGIN
    RAISERROR('BDControlOrdenes no existe en este servidor.', 16, 1);
    RETURN;
END
GO

USE BDControlOrdenes
GO

SET NOCOUNT ON;

DECLARE @now DATETIME = GETDATE();
DECLARE @idMenu INT;

SELECT TOP 1 @idMenu = Id
FROM dbo.tbl_tablamenu
WHERE pagina_asociada = N'NodoZonaPage'
   OR LOWER(LTRIM(RTRIM(nombre))) = N'tsm_nodo_zona'
ORDER BY Id;

IF @idMenu IS NULL
BEGIN
    INSERT INTO dbo.tbl_tablamenu (
        nombre,
        nombre_sidebar,
        pagina_asociada,
        Direccion,
        [orden],
        padre,
        e_eliminado,
        fecharegistro,
        id_usuario
    )
    VALUES (
        N'tsm_nodo_zona',
        N'Nodo Zona',
        N'NodoZonaPage',
        N'/backoffice/nodo-zona',
        90,
        1,
        0,
        @now,
        1
    );

    SET @idMenu = SCOPE_IDENTITY();
END
ELSE
BEGIN
    UPDATE dbo.tbl_tablamenu
    SET nombre = N'tsm_nodo_zona',
        nombre_sidebar = N'Nodo Zona',
        pagina_asociada = N'NodoZonaPage',
        Direccion = N'/backoffice/nodo-zona',
        [orden] = 90,
        padre = ISNULL(padre, 1),
        e_eliminado = 0
    WHERE Id = @idMenu;
END

IF OBJECT_ID('dbo.tbl_MenuPaginaAsociada', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_MenuPaginaAsociada
        WHERE Id_Menu = @idMenu
          AND LOWER(LTRIM(RTRIM(Pagina_Asociada))) = LOWER(N'NodoZonaPage')
    )
    BEGIN
        UPDATE dbo.tbl_MenuPaginaAsociada
        SET E_Eliminado = 0,
            FechaRegistro = ISNULL(FechaRegistro, @now),
            Id_Usuario = ISNULL(Id_Usuario, 1)
        WHERE Id_Menu = @idMenu
          AND LOWER(LTRIM(RTRIM(Pagina_Asociada))) = LOWER(N'NodoZonaPage');
    END
    ELSE
    BEGIN
        INSERT INTO dbo.tbl_MenuPaginaAsociada (Id_Menu, Pagina_Asociada, E_Eliminado, FechaRegistro, Id_Usuario)
        VALUES (@idMenu, N'NodoZonaPage', 0, @now, 1);
    END
END

IF OBJECT_ID('dbo.tbl_RolMenu', 'U') IS NOT NULL
BEGIN
    DECLARE @roles TABLE (Id_Rol INT NOT NULL PRIMARY KEY);

    INSERT INTO @roles (Id_Rol)
    SELECT Id_Rol
    FROM dbo.tbl_Rol
    WHERE ISNULL(E_Eliminado, 0) = 0
      AND LOWER(REPLACE(REPLACE(LTRIM(RTRIM(Nombre)), ' ', ''), '_', '')) IN (
          N'backoffice',
          N'backofficev',
          N'sistemas',
          N'admin',
          N'administrador'
      );

    UPDATE rm
    SET E_Eliminado = 0
    FROM dbo.tbl_RolMenu rm
    INNER JOIN @roles r ON r.Id_Rol = rm.Id_Rol
    WHERE rm.Id_Menu = @idMenu;

    INSERT INTO dbo.tbl_RolMenu (Id_Menu, Id_Rol, E_Eliminado)
    SELECT @idMenu, r.Id_Rol, 0
    FROM @roles r
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_RolMenu rm
        WHERE rm.Id_Menu = @idMenu
          AND rm.Id_Rol = r.Id_Rol
    );
END

SELECT
    @idMenu AS id_menu_nodo_zona,
    tm.nombre,
    tm.nombre_sidebar,
    tm.pagina_asociada,
    tm.Direccion
FROM dbo.tbl_tablamenu tm
WHERE tm.Id = @idMenu;
GO
