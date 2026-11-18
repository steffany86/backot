-- ========================================================================
-- CREAR spx_ObtenerNpsInvitaciones
-- ========================================================================
-- Lista las invitaciones NPS vigentes (tbl_NPS_INVITACIONES_MAKIRO) para la
-- pantalla /nps en modo nps_invitado ("Detalle de Invitaciones Vigentes").
-- Reemplaza el SELECT que estaba en NpsRepository.obtenerDashboardInvitado.
-- Las fechas NO son parametros: @fecha_fin = fecha del ultimo registro (max
-- fecha_creacion) y @fecha_inicio = @fecha_fin - 4 dias. Los demas filtros son
-- opcionales (NULL = sin filtro). Devuelve una fila por
-- external_transaction_id (la de fecha_carga mas reciente).
-- ========================================================================

USE [BDControlOrdenes]
GO

CREATE OR ALTER PROC dbo.spx_ObtenerNpsInvitaciones
    @id_sucursal       INT           = NULL,
    @tecnico_nombre    NVARCHAR(200) = NULL,
    @id_supervisor     INT           = NULL,
    @supervisor_nombre NVARCHAR(200) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @fecha_fin DATE = (
        SELECT MAX(CONVERT(DATE, fecha_creacion, 103))
        FROM dbo.tbl_NPS_INVITACIONES_MAKIRO
        WHERE ISDATE(fecha_creacion) = 1
    );
    DECLARE @fecha_inicio DATE = DATEADD(DAY, -4, @fecha_fin);

    DECLARE @SucursalNombre NVARCHAR(120) = CASE @id_sucursal
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
        WHERE (ISDATE(i.fecha_creacion) = 1 AND CONVERT(DATE, i.fecha_creacion, 103) >= @fecha_inicio)
          AND (ISDATE(i.fecha_creacion) = 1 AND CONVERT(DATE, i.fecha_creacion, 103) <= @fecha_fin)
          AND (@tecnico_nombre IS NULL OR UPPER(LTRIM(RTRIM(i.tecnico))) = UPPER(LTRIM(RTRIM(@tecnico_nombre))))
          AND (@id_supervisor IS NULL OR rel.idUsuarioSupervisor = @id_supervisor)
          AND (@supervisor_nombre IS NULL OR UPPER(LTRIM(RTRIM(rel.supervisorACargo))) = UPPER(LTRIM(RTRIM(@supervisor_nombre))))
          AND (@id_sucursal IS NULL OR @SucursalNombre IS NULL OR ISNULL(rel.sucursal, '') = ''
               OR REPLACE(REPLACE(UPPER(ISNULL(rel.sucursal, '')), ' ', ''), '_', '') LIKE '%' + @SucursalNombreNorm + '%')
    )
    SELECT
        i.fecha_creacion AS fecha_creacion,
        i.external_transaction_id AS id_transaccion,
        i.external_transaction_id AS nro_orden,
        i.external_transaction_id AS surveyid_for_internal_use,
        ISNULL(i.fecha_respuesta, i.fecha_creacion) AS fecha_de_respuesta,
        NULL AS tipo_de_alerta, NULL AS tipo_de_encuesta,
        i.id_cliente AS nombre_cliente, i.id_cliente AS id_cliente,
        NULL AS unit, NULL AS email, i.tipo_transaccion AS tipo_de_transaccion, i.fecha_transaccion AS fecha_de_transaccion,
        NULL AS journey, NULL AS flag_b2b, NULL AS field_serv_subtipo_tran_global,
        i.respuesta AS ltr, i.respuesta AS likelihood_to_recommend_come, NULL AS ces, NULL AS csat, NULL AS fcr_comment_export, NULL AS fcr,
        NULL AS cumplimiento_de_agenda, i.tecnologia AS tecnologia, NULL AS ta_topicos_ltr,
        CASE WHEN ISNUMERIC(NULLIF(LTRIM(RTRIM(i.respuesta)), '')) = 1 AND CONVERT(INT, NULLIF(LTRIM(RTRIM(i.respuesta)), '')) >= 9 THEN 'PROMOTOR'
             WHEN ISNUMERIC(NULLIF(LTRIM(RTRIM(i.respuesta)), '')) = 1 AND CONVERT(INT, NULLIF(LTRIM(RTRIM(i.respuesta)), '')) >= 7 THEN 'PASIVO'
             WHEN ISNUMERIC(NULLIF(LTRIM(RTRIM(i.respuesta)), '')) = 1 AND CONVERT(INT, NULLIF(LTRIM(RTRIM(i.respuesta)), '')) >= 0 THEN 'DETRACTOR'
             ELSE 'SIN_RESPUESTA' END AS nps_tipo,
        NULL AS satisfaccion_precio_reparacion, NULL AS imagen_personal, NULL AS amabilidad_tecnico, NULL AS conocimiento_tecnico,
        NULL AS imagen_personal_2, NULL AS amabilidad_tecnico_2, NULL AS conocimiento_tecnico_2,
        NULL AS departamento, ISNULL(i._sucursal, '-') AS ciudad, i._supervisorACargo AS supervisor_1,
        i.id_cliente AS clientenro, i.external_transaction_id AS ordennro, i.id_cliente AS cliente_nombre_completo,
        NULL AS cliente_estado, NULL AS departamento_siga, NULL AS territorio, ISNULL(i._sucursal, '-') AS ciudad_siga,
        NULL AS poblacion_cliente, NULL AS tecnologia_siga,
        NULL AS orden_tipo_cod, NULL AS orden_estado, NULL AS orden_fecha_registro, NULL AS orden_hora_registro,
        NULL AS orden_mes_finalizacion, NULL AS diafin, NULL AS orden_fecha_finalizacion, NULL AS orden_hora_finalizacion,
        NULL AS zona_grupo, NULL AS zona_tap,
        i.tecnico AS dealer_tecnico_nombre, NULL AS tecnicoid, i.tecnico AS tecnico_nombre, i.dealer AS dealer,
        i.fecha_carga AS fecha_carga, i.id_NPS_INVITACIONES_MAKIRO AS id_NPS_RESPUESTAS_MAKIRO,
        i.telefono_encuesta AS telefono_encuesta, i.contratista AS contratista, i.respuesta AS respuesta,
        i.fecha_respuesta AS fecha_respuesta, i.mes_respuesta AS mes_respuesta,
        CASE WHEN ISDATE(i.fecha_creacion) = 1
             THEN DATEDIFF(DAY, CONVERT(DATE, i.fecha_creacion, 103), CONVERT(DATE, GETDATE()))
             ELSE NULL END AS dias_envio
    FROM base i
    WHERE i._rn = 1;
END
GO

PRINT '========================================';
PRINT 'SCRIPT COMPLETADO EXITOSAMENTE';
PRINT 'spx_ObtenerNpsInvitaciones creado/actualizado.';
PRINT '========================================';
