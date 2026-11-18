-- ========================================================================
-- CREAR spx_ObtenerLlamadasDeAtencion
-- ========================================================================
-- Lista las llamadas de atencion (tbl_LlamadaAtencion) para la pantalla
-- /supervisor/llamada-atencion. Todos los filtros son opcionales (NULL = sin
-- filtro). Excluye registros eliminados (e_eliminado = 1).
-- ========================================================================

USE [BD_TigoHogar]
GO

CREATE OR ALTER PROC dbo.spx_ObtenerLlamadasDeAtencion
    @limite       INT           = 200,
    @id_tecnico   NVARCHAR(30)  = NULL,
    @fecha_desde  DATE          = NULL,
    @fecha_hasta  DATE          = NULL,
    @id_sucursal  INT           = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SET @id_tecnico = NULLIF(LTRIM(RTRIM(@id_tecnico)), '');

    SELECT TOP (@limite)
        la.Id                                        AS idLlamadaAtencion,
        CAST(la.CodigoEmpleado AS NVARCHAR(30))      AS idTecnico,
        CAST(la.CodigoEmpleado AS NVARCHAR(30))      AS codEmpleado,
        la.NombreEmpleado                            AS tecnico,
        la.NombreEmpleado                            AS tecnicoNombre,
        la.Tabla                                     AS tabla,
        la.Id_UsuarioSupervisor                      AS idUsuarioSupervisor,
        la.Id_TipoComunicacion                       AS idTipoComunicacion,
        tc.TipoComunicacion                          AS tipoComunicacion,
        la.Fecha_Registro                            AS fechaRegistro,
        la.Motivo                                    AS motivo,
        la.Descripcion                               AS descripcion,
        la.ComentarioColaborador                     AS comentarioColaborador,
        la.Acuerdos                                  AS acuerdos,
        la.Testigo                                   AS testigo,
        la.FechaSeguimiento                          AS fechaSeguimiento,
        la.FirmaTecnico                              AS firmaTecnico,
        la.FirmaTestigo                              AS firmaTestigo,
        la.Id_Sucursal                               AS idSucursal,
        la.Sucursal                                  AS sucursal
    FROM dbo.tbl_LlamadaAtencion la
    LEFT JOIN dbo.tbl_TipoComunicacion tc
           ON tc.Id_TipoComunicacion = la.Id_TipoComunicacion
    WHERE (@id_tecnico IS NULL OR CAST(la.CodigoEmpleado AS NVARCHAR(30)) = @id_tecnico)
      AND (@fecha_desde IS NULL OR CAST(la.Fecha_Registro AS DATE) >= @fecha_desde)
      AND (@fecha_hasta IS NULL OR CAST(la.Fecha_Registro AS DATE) <= @fecha_hasta)
      AND (@id_sucursal IS NULL OR ISNULL(la.Id_Sucursal, -1) = @id_sucursal)
      AND ISNULL(la.e_eliminado, 0) = 0
    ORDER BY la.Fecha_Registro DESC, la.Id DESC;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ObtenerLlamadasDeAtencion creado/actualizado.';
PRINT '========================================';
