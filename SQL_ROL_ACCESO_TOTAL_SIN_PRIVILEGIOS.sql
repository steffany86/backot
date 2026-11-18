-- Rol con acceso a todos los menus EXCEPTO "privilegios".
-- Ejecutar en la base de ordenes (la que tiene tbl_Rol / tbl_tablamenu / tbl_RolMenu).
-- Requiere que existan los SP de SP_NUEVOS_PRIVILEGIOS.sql.
-- Cambiar @IdRol por el rol que se quiere configurar (el rol debe existir en tbl_Rol).

DECLARE @IdRol INT = 10;

IF NOT EXISTS (SELECT 1 FROM dbo.tbl_Rol WHERE Id_Rol = @IdRol AND ISNULL(E_Eliminado, 0) = 0)
BEGIN
    RAISERROR('El rol no existe o esta inactivo. Crearlo primero en tbl_Rol.', 16, 1);
    RETURN;
END

-- 1) Revisar que menus se van a excluir (todo lo que tenga "privilegio" en nombre/sidebar/pagina).
SELECT m.Id, m.nombre, m.nombre_sidebar, m.pagina_asociada
FROM dbo.tbl_tablamenu m
WHERE ISNULL(m.e_eliminado, 0) = 0
  AND (m.nombre LIKE '%privilegio%'
    OR ISNULL(m.nombre_sidebar, '') LIKE '%privilegio%'
    OR ISNULL(m.pagina_asociada, '') LIKE '%privilegio%');

-- 2) Asignar todos los menus activos menos los anteriores.
DECLARE @ids NVARCHAR(MAX) = (
    SELECT STUFF((
        SELECT ',' + CAST(m.Id AS NVARCHAR(10))
        FROM dbo.tbl_tablamenu m
        WHERE ISNULL(m.e_eliminado, 0) = 0
          AND m.nombre NOT LIKE '%privilegio%'
          AND ISNULL(m.nombre_sidebar, '') NOT LIKE '%privilegio%'
          AND ISNULL(m.pagina_asociada, '') NOT LIKE '%privilegio%'
        FOR XML PATH('')
    ), 1, 1, '')
);

EXEC dbo.spx_GuardarPrivilegiosRol @IdRol = @IdRol, @MenuIdsCsv = @ids;

-- 3) Verificacion: Asignado = 0 deberia ser solo "privilegios".
EXEC dbo.spx_ObtenerPrivilegiosRolDetalle @IdRol = @IdRol;
