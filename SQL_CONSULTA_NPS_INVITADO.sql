-- ========================================================================
-- Consulta de "Detalle de Invitaciones Vigentes" (pantalla /nps, modo nps_invitado)
-- Equivalente a NpsRepository.obtenerDashboardInvitado (base central).
-- Cambia las variables del bloque PARAMETROS; NULL = sin filtro.
-- ========================================================================

-- PARAMETROS ---------------------------------------------------------------
DECLARE @IdSucursal        INT           = 20;               -- 20 = SANTA CRUZ
DECLARE @FechaInicio       DATE          = '2026-09-29';
DECLARE @FechaFin          DATE          = '2026-10-02';
DECLARE @TecnicoNombre     NVARCHAR(200) = NULL;             -- ej. 'CRISTHIAN ALBERTO CAMAQUE RIVERA'
DECLARE @IdSupervisor      INT           = NULL;
DECLARE @SupervisorNombre  NVARCHAR(200) = NULL;
-- --------------------------------------------------------------------------

DECLARE @SucursalNombre NVARCHAR(120) = CASE @IdSucursal
    WHEN 9  THEN 'SANTA CRUZ' WHEN 20 THEN 'SANTA CRUZ' WHEN 4  THEN 'SUCRE'
    WHEN 7  THEN 'TARIJA'     WHEN 2  THEN 'YACUIBA'    WHEN 15 THEN 'RIBERALTA'
    WHEN 19 THEN 'MONTERO'    WHEN 5  THEN 'CAMIRI'     WHEN 10 THEN 'CHIQUITANIA'
    WHEN 16 THEN 'COBIJA'     WHEN 12 THEN 'IVIRGARZAMA' WHEN 6 THEN 'PUERTO SUAREZ'
    WHEN 11 THEN 'SAN IGNACIO' WHEN 17 THEN 'TRINIDAD'  WHEN 14 THEN 'YAPACANI'
    ELSE NULL END;
DECLARE @SucursalNombreNorm NVARCHAR(120) =
    REPLACE(REPLACE(UPPER(ISNULL(@SucursalNombre, '')), ' ', ''), '_', '');

WITH base AS (
    SELECT i.*,
           rel.supervisorACargo    AS _supervisorACargo,
           rel.idUsuarioSupervisor AS _idUsuarioSupervisor,
           rel.sucursal            AS _sucursal,
           ROW_NUMBER() OVER (
               PARTITION BY i.external_transaction_id
               ORDER BY CASE WHEN ISDATE(i.fecha_carga) = 1
                             THEN CONVERT(DATETIME, i.fecha_carga, 103)
                             ELSE CONVERT(DATETIME, '1900-01-01', 120) END DESC,
                        i.id_NPS_INVITACIONES_MAKIRO DESC) AS _rn
    FROM dbo.tbl_NPS_INVITACIONES_MAKIRO i
    OUTER APPLY (
        SELECT TOP 1 cc.supervisorACargo, cc.idUsuarioSupervisor, cc.sucursal
        FROM dbo.tbl_ConformacionCuadrillaDiario cc
        WHERE ISNULL(cc.e_eliminado, 0) = 0
          AND UPPER(LTRIM(RTRIM(cc.tecnico))) = UPPER(LTRIM(RTRIM(i.tecnico)))
        ORDER BY cc.fecha DESC, cc.id DESC
    ) rel
    WHERE (@FechaInicio IS NULL OR (ISDATE(i.fecha_creacion) = 1 AND CONVERT(DATE, i.fecha_creacion, 103) >= @FechaInicio))
      AND (@FechaFin    IS NULL OR (ISDATE(i.fecha_creacion) = 1 AND CONVERT(DATE, i.fecha_creacion, 103) <= @FechaFin))
      AND (@TecnicoNombre IS NULL OR UPPER(LTRIM(RTRIM(i.tecnico))) = UPPER(LTRIM(RTRIM(@TecnicoNombre))))
      AND (@IdSupervisor IS NULL OR rel.idUsuarioSupervisor = @IdSupervisor)
      AND (@SupervisorNombre IS NULL OR UPPER(LTRIM(RTRIM(rel.supervisorACargo))) = UPPER(LTRIM(RTRIM(@SupervisorNombre))))
      AND (@IdSucursal IS NULL OR @SucursalNombre IS NULL OR ISNULL(rel.sucursal, '') = ''
           OR REPLACE(REPLACE(UPPER(ISNULL(rel.sucursal, '')), ' ', ''), '_', '') LIKE '%' + @SucursalNombreNorm + '%')
)
SELECT
    i.tecnico                         AS Tecnico,
    i.fecha_creacion                  AS Fecha,
    i.id_cliente                      AS IdCliente,
    i.tecnologia                      AS Tecnologia,
    i.tipo_transaccion                AS TipoTransaccion,
    i.telefono_encuesta               AS Telefono,
    CASE WHEN ISDATE(i.fecha_creacion) = 1
         THEN DATEDIFF(DAY, CONVERT(DATE, i.fecha_creacion, 103), CONVERT(DATE, GETDATE()))
         ELSE NULL END                AS DiasEnvio,
    -- columnas adicionales que usa la pantalla para los graficos
    ISNULL(i._sucursal, '-')          AS Ciudad,
    i._supervisorACargo               AS Supervisor,
    i.contratista                     AS Contratista,
    i.dealer                          AS Dealer,
    i.respuesta                       AS Respuesta,
    i.fecha_respuesta                 AS FechaRespuesta,
    i.mes_respuesta                   AS MesRespuesta,
    i.external_transaction_id         AS IdTransaccion,
    i.fecha_carga                     AS FechaCarga
FROM base i
WHERE i._rn = 1;
