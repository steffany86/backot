USE BDControlOrdenes;
GO

SET NOCOUNT ON;

DECLARE @IdRolAlmacenero INT;
DECLARE @IdMenu INT;

SELECT @IdRolAlmacenero = Id_Rol
FROM dbo.tbl_rol
WHERE UPPER(LTRIM(RTRIM(Nombre))) = 'ALMACENERO'
  AND ISNULL(E_Eliminado, 0) = 0;

IF @IdRolAlmacenero IS NULL
BEGIN
    RAISERROR('No existe el rol Almacenero activo en dbo.tbl_rol.', 16, 1);
    RETURN;
END;

SELECT @IdMenu = Id
FROM dbo.tbl_tablamenu
WHERE pagina_asociada = 'CruceOrdenesAgendaMakiroPage'
  AND ISNULL(e_eliminado, 0) = 0;

IF @IdMenu IS NULL
BEGIN
    INSERT INTO dbo.tbl_tablamenu
        (nombre, orden, padre, e_eliminado, fecharegistro, id_Usuario, pagina_asociada, nombre_sidebar, Direccion)
    VALUES
        ('tsm_Cruce_Agenda_Makiro', 50, 0, 0, GETDATE(), 1, 'CruceOrdenesAgendaMakiroPage', 'Cruce Agenda Makiro', '/almacen/cruce-agenda-makiro');

    SET @IdMenu = SCOPE_IDENTITY();

    UPDATE dbo.tbl_tablamenu
    SET padre = @IdMenu
    WHERE Id = @IdMenu;
END
ELSE
BEGIN
    UPDATE dbo.tbl_tablamenu
    SET nombre = 'tsm_Cruce_Agenda_Makiro',
        nombre_sidebar = 'Cruce Agenda Makiro',
        Direccion = '/almacen/cruce-agenda-makiro',
        padre = CASE WHEN ISNULL(padre, 0) = 0 THEN Id ELSE padre END
    WHERE Id = @IdMenu;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.tbl_RolMenu
    WHERE Id_Rol = @IdRolAlmacenero
      AND Id_Menu = @IdMenu
)
BEGIN
    UPDATE dbo.tbl_RolMenu
    SET E_Eliminado = 0
    WHERE Id_Rol = @IdRolAlmacenero
      AND Id_Menu = @IdMenu;
END
ELSE
BEGIN
    INSERT INTO dbo.tbl_RolMenu (Id_Menu, Id_Rol, E_Eliminado)
    VALUES (@IdMenu, @IdRolAlmacenero, 0);
END;

SELECT
    @IdRolAlmacenero AS IdRolAlmacenero,
    @IdMenu AS IdMenuCruceAgendaMakiro,
    'OK' AS Estado;
