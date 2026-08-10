USE [BD_TigoHogar];
GO

ALTER PROCEDURE dbo.SP_Inicio_AprobarSupervisor
    @IdInicio INT,
    @IdSupervisor INT,
    @SupervisorAproboInicio NVARCHAR(200)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_InicioJornadaAlturas
    SET pendiente = 0,
        id_usuario_aprobo_inicio = @IdSupervisor,
        supervisor_aprobo_inicio = @SupervisorAproboInicio,
        fecha_aprobacion_inicio = GETDATE()
    WHERE id_inicio = @IdInicio
      AND id_encargado = @IdSupervisor
      AND ISNULL(pendiente, 0) = 1
      AND ISNULL(e_eliminado, 0) = 0;

    SELECT @@ROWCOUNT AS updated;
END;
GO

ALTER PROCEDURE dbo.SP_Inicio_AprobarPorIdHoy
    @IdInicio INT,
    @IdSupervisor INT,
    @SupervisorAproboInicio NVARCHAR(200)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_InicioJornadaAlturas
    SET pendiente = 0,
        id_usuario_aprobo_inicio = @IdSupervisor,
        supervisor_aprobo_inicio = @SupervisorAproboInicio,
        fecha_aprobacion_inicio = GETDATE()
    WHERE id_inicio = @IdInicio
      AND ISNULL(pendiente, 0) = 1
      AND ISNULL(e_eliminado, 0) = 0
      AND fecha_registro >= CONVERT(DATE, GETDATE())
      AND fecha_registro < DATEADD(DAY, 1, CONVERT(DATE, GETDATE()));

    SELECT @@ROWCOUNT AS updated;
END;
GO
