-- Quita el menu "Privilegios" a un rol sin tocar el resto de sus menus.
-- Ejecutar en la base de ordenes (tbl_tablamenu / tbl_RolMenu).

DECLARE @IdRol INT = 10;

-- 1) Ver que menus coinciden y si el rol los tiene asignados (revisar antes de continuar).
SELECT m.Id, m.nombre, m.nombre_sidebar, m.pagina_asociada, rm.Id_RolMenu, rm.E_Eliminado
FROM dbo.tbl_tablamenu m
LEFT JOIN dbo.tbl_RolMenu rm ON rm.Id_Menu = m.Id AND rm.Id_Rol = @IdRol
WHERE ISNULL(m.e_eliminado, 0) = 0
  AND (m.nombre LIKE '%privilegio%'
    OR ISNULL(m.nombre_sidebar, '') LIKE '%privilegio%'
    OR ISNULL(m.pagina_asociada, '') LIKE '%privilegio%');

-- 2) Desasignar (baja logica, igual que hace spx_GuardarPrivilegiosRol).
UPDATE rm
SET rm.E_Eliminado = 1
FROM dbo.tbl_RolMenu rm
INNER JOIN dbo.tbl_tablamenu m ON m.Id = rm.Id_Menu
WHERE rm.Id_Rol = @IdRol
  AND ISNULL(rm.E_Eliminado, 0) = 0
  AND (m.nombre LIKE '%privilegio%'
    OR ISNULL(m.nombre_sidebar, '') LIKE '%privilegio%'
    OR ISNULL(m.pagina_asociada, '') LIKE '%privilegio%');

SELECT @@ROWCOUNT AS menus_quitados;

-- 3) Verificacion: "Privilegios" debe salir con Asignado = 0.
EXEC dbo.spx_ObtenerPrivilegiosRolDetalle @IdRol = @IdRol;
