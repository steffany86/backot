CREATE PROCEDURE dbo.SP_NPS_LISTAR_TECNICOS_POR_SUPERVISOR
  @IdSucursal INT,
  @IdSupervisor INT
AS
BEGIN
  SET NOCOUNT ON;
  SELECT DISTINCT
    cc.id_tecnico AS idTecnico,
    ISNULL(ut.Nombre, cc.tecnico) AS tecnico
  FROM dbo.tbl_ConformacionCuadrillaDiario cc
  LEFT JOIN dbo.tbl_UsuarioTecnico map ON map.id_Vendedor = cc.id_tecnico AND ISNULL(map.e_eliminado,0)=0
  LEFT JOIN dbo.tbl_Usuario ut ON ut.Id_Usuario = map.id_Usuario
  WHERE ISNULL(cc.e_eliminado, 0) = 0
    AND (@IdSupervisor = 0 OR cc.idUsuarioSupervisor = @IdSupervisor)
  ORDER BY tecnico;
END

