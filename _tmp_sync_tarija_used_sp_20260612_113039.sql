-- Sync SP Tarija desde Santa Cruz 20260612_113039
USE [BDSistemaAntenaPMTarija];
GO

/* ALTER [sp_obtenerlistaordenestrabajo] */
ALTER proc [dbo].[sp_ObtenerListaOrdenesTrabajo](@Fecha_Registro datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo,
	RutaPDF,
	 T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, V.Observacion , r.Id_Ruta 
	,	case when  v.TieneObservacion = 0 then 'No' else 'Si' end TieneObservacion,
	 case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) = dbo.dateonly(@Fecha_Registro) and V.E_eliminado = 0
	Order by V.Id_Ruta, V.Id_Venta
End
GO

/* CREATE [sp_obtenerlistaordenestrabajo_otweb] */
CREATE proc [dbo].[sp_ObtenerListaOrdenesTrabajo_OTWEB](@Fecha_Registro datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo, T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, V.Observacion , r.Id_Ruta 
	,	case when  v.TieneObservacion = 0 then 'No' else 'Si' end TieneObservacion,
	 case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) = dbo.dateonly(@Fecha_Registro) and V.E_eliminado = 0
	 AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
	Order by V.Id_Ruta, V.Id_Venta
End
GO

/* ALTER [sp_obtenerlistaordenestrabajorfechas] */
ALTER proc [dbo].[sp_ObtenerListaOrdenesTrabajoRFechas](@Fecha_RegistroInicio datetime,@Fecha_RegistroFin datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, 
	dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo,
	v.RutaPDF,
	 T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, 
	V.Observacion
	, case when v.tieneObservacion =0 then 'No' else 'Si' end TieneObservacion,
	case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) between dbo.dateonly(@Fecha_RegistroInicio)  and dbo.dateonly(@Fecha_RegistroFin)
	and V.E_eliminado = 0
	Order by V.Id_Ruta,V.Fecha_Ejecucion desc, V.Id_Venta
End
GO

/* ALTER [spx_existecierrealmacenhoy] */
--SELECT 'una linea' + CHAR(13) + CHAR(10) + 'otra linea'
--spx_ExisteCierreAlmacenHoy '18/05/2021'
ALTER proc [dbo].[spx_ExisteCierreAlmacenHoy](@FechaRegistro datetime)
as
begin
	declare @cuantostablamov_pendientes int
	declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
	insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fechaRegistro

	declare @textopendientes nvarchar(max)
	set @textopendientes =  (SELECT STUFF(
								(SELECT +', ' +CHAR(10)+ movimiento
								FROM @tablamov_pendientes        
								FOR XML PATH ('')),
								1,2, ''))
	declare @diferenciaDias int 
	set @diferenciaDias=(select DATEDIFF(DAY,@FechaRegistro,GETDATE()))
		if(@diferenciaDias>=0)	--es de dias pasados verificar que no hayan trabajos pendientes
		begin		
			set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)
				select -1 cantidad,@FechaRegistro as FechaVerificacion ,('Hay transacciones pendientes: ' +@textopendientes )Observacion		
			else 
				select 0 cantidad,@FechaRegistro as FechaVerificacion,''Observacion		
		end	
		
		if(@diferenciaDias<0)
		begin			
			select -1 cantidad,@FechaRegistro as FechaVerificacion ,'> a la fecha del Servidor' Observacion		
		end
end
GO

/* CREATE [spx_listarotfinalizadas] */
CREATE PROC [dbo].[spx_ListarOtFinalizadas]
    @Fecha DATETIME,
    @IdUsuario INT = NULL,
    @IdsVendedorCsv NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @Fecha IS NULL
    BEGIN
        RAISERROR('Fecha es requerida.', 16, 1);
        RETURN;
    END

    DECLARE @Vendedores TABLE (Id_Vendedor INT PRIMARY KEY);

    IF @IdsVendedorCsv IS NOT NULL AND LTRIM(RTRIM(@IdsVendedorCsv)) <> ''
    BEGIN
        DECLARE @xml XML;
        DECLARE @csv NVARCHAR(MAX);
        SET @csv = REPLACE(LTRIM(RTRIM(@IdsVendedorCsv)), ' ', '');
        SET @xml = CAST('<x><i>' + REPLACE(@csv, ',', '</i><i>') + '</i></x>' AS XML);

        INSERT INTO @Vendedores (Id_Vendedor)
        SELECT DISTINCT CAST(T.N.value('.', 'NVARCHAR(50)') AS INT)
        FROM @xml.nodes('/x/i') AS T(N)
        WHERE ISNUMERIC(T.N.value('.', 'NVARCHAR(50)')) = 1
          AND CAST(T.N.value('.', 'NVARCHAR(50)') AS INT) > 0;
    END

    IF @IdUsuario IS NOT NULL AND @IdUsuario > 0
       AND NOT EXISTS (SELECT 1 FROM @Vendedores WHERE Id_Vendedor = @IdUsuario)
    BEGIN
        INSERT INTO @Vendedores (Id_Vendedor) VALUES (@IdUsuario);
    END

    SELECT
        v.Id_Venta AS idVenta,
        v.OrdenTrabajo AS ordenTrabajo,
        v.CodigoCliente AS codigoCliente,
        v.Fecha_Ejecucion AS fechaEjecucion,
        v.Origen AS origen,
        v.Id_Vendedor AS idVendedor,
        v.Id_TipoServicio AS idTipoServicio,
        ts.Nombre AS tipoServicio,
        v.Id_Estado AS idEstado,
        e.Nombre AS estado
    FROM dbo.tbl_Venta v
    LEFT JOIN dbo.tbl_tiposervicio ts ON ts.Id_TipoServicio = v.Id_TipoServicio
    LEFT JOIN dbo.tbl_estado e ON e.Id_Estado = v.Id_Estado
    WHERE ISNULL(v.E_Eliminado, 0) = 0 and origen<>''
      AND CONVERT(DATE, v.Fecha_Ejecucion) = @Fecha
      AND EXISTS (
            SELECT 1
            FROM @Vendedores x
            WHERE x.Id_Vendedor = v.Id_Vendedor
      )
    ORDER BY v.Id_Venta DESC;
END
GO

/* CREATE [spx_obtenerauxiliaresconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerAuxiliaresConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        v.Id_Vendedor AS id_tecnicoAuxiliar,
        v.Nombre AS auxiliar,
        v.CuentaSF AS cuenta_sf,
        v.SalesForce AS salesforce,
        v.Habilidad AS habilidad,
        v.Vehiculo AS vehiculo,
        v.*
    FROM dbo.tbl_Vendedor v
    WHERE v.E_Eliminado = 0
    and v.Id_Vendedor>0 and v.id_tiposolicitante=1
    and id_vendedor not in (
		select id_vendedor from tbl_ruta where e_eliminado=0 and id_vendedor>0
    )
    ORDER BY v.Nombre;
END
GO

/* CREATE [spx_obtenerconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerConformacionCuadrillaWeb]
    @Fecha DATE = NULL,
    @Sucursal NVARCHAR(100) = NULL,
    @Limite INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @FechaConsulta DATE = ISNULL(@Fecha, CAST(GETDATE() AS DATE));
    DECLARE @SucursalNormalizada NVARCHAR(100) = NULLIF(LTRIM(RTRIM(@Sucursal)), '');

    ;WITH VersionActual AS (
        SELECT TOP 1 LTRIM(RTRIM(v.sucursal)) AS sucursal
        FROM dbo.tbl_version v
        WHERE v.sucursal IS NOT NULL
          AND LTRIM(RTRIM(v.sucursal)) <> ''
    ),
    BaseCuadrillas AS (
        SELECT
            CAST(r.Id_Ruta AS BIGINT) AS id_ruta,
            @FechaConsulta AS fecha,
            CAST('PENDIENTE' AS NVARCHAR(20)) AS estado,
            CAST(
                CASE
                    WHEN UPPER(LTRIM(RTRIM(ISNULL(r.Tipo, '')))) IN ('TITULAR', 'BACKUP')
                        THEN UPPER(LTRIM(RTRIM(r.Tipo)))
                    ELSE 'TITULAR'
                END
                AS NVARCHAR(20)
            ) AS actividad,
            v.Id_Vendedor AS id_tecnico,
            v.CuentaSF AS cuenta_sf,
            v.SalesForce AS salesforce,
            v.Habilidad AS habilidad,
            v.Vehiculo AS vehiculo,
            r.Nombre AS grupo,
            COALESCE(NULLIF(LTRIM(RTRIM(r.BodegaTigo)), ''), NULLIF(LTRIM(RTRIM(r.almacenTigo)), '')) AS almacen,
            COALESCE(NULLIF(LTRIM(RTRIM(r.almacenTigo)), ''), NULLIF(LTRIM(RTRIM(r.BodegaTigo)), '')) AS grupoDigitacion,
            v.Nombre AS tecnico,
            va.sucursal AS sucursal,
            GETDATE() AS fechaRegistro,
            CONVERT(BIT, ISNULL(r.E_Eliminado, 0)) AS e_eliminado
        FROM dbo.tbl_Ruta r
        INNER JOIN dbo.tbl_Vendedor v
            ON v.Id_Vendedor = r.Id_Vendedor
        CROSS JOIN VersionActual va
        WHERE v.E_Eliminado = 0
    ),
    GuardadasGrupo AS (
        SELECT
            g.*,
            ROW_NUMBER() OVER (
                PARTITION BY
                    g.id_tecnico,
                    UPPER(LTRIM(RTRIM(ISNULL(g.grupo, ''))))
                ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
            ) AS rn
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND g.fecha = @FechaConsulta
          AND (
                @SucursalNormalizada IS NULL
                OR UPPER(LTRIM(RTRIM(ISNULL(g.sucursal, '')))) = UPPER(@SucursalNormalizada)
              )
    ),
    GuardadasTecnico AS (
        SELECT
            g.*,
            ROW_NUMBER() OVER (
                PARTITION BY g.id_tecnico
                ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
            ) AS rn
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND g.fecha = @FechaConsulta
          AND (
                @SucursalNormalizada IS NULL
                OR UPPER(LTRIM(RTRIM(ISNULL(g.sucursal, '')))) = UPPER(@SucursalNormalizada)
              )
    )
    SELECT TOP (CASE WHEN @Limite IS NULL OR @Limite <= 0 THEN 2147483647 ELSE @Limite END)
        b.id_ruta AS id,
        b.id_ruta AS id_ruta,
        COALESCE(ge.fecha, gt.fecha, b.fecha) AS fecha,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.estado)), ''), NULLIF(LTRIM(RTRIM(gt.estado)), ''), b.estado) AS estado,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.actividad)), ''), NULLIF(LTRIM(RTRIM(gt.actividad)), ''), b.actividad) AS actividad,
        b.id_tecnico,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.cuenta_sf)), ''), NULLIF(LTRIM(RTRIM(gt.cuenta_sf)), ''), b.cuenta_sf) AS cuenta_sf,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.salesforce)), ''), NULLIF(LTRIM(RTRIM(gt.salesforce)), ''), b.salesforce) AS salesforce,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.habilidad)), ''), NULLIF(LTRIM(RTRIM(gt.habilidad)), ''), b.habilidad) AS habilidad,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.vehiculo)), ''), NULLIF(LTRIM(RTRIM(gt.vehiculo)), ''), b.vehiculo) AS vehiculo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.grupo)), ''), NULLIF(LTRIM(RTRIM(gt.grupo)), ''), b.grupo) AS grupo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.almacen)), ''), NULLIF(LTRIM(RTRIM(gt.almacen)), ''), b.almacen) AS almacen,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.grupoDigitacion)), ''), NULLIF(LTRIM(RTRIM(gt.grupoDigitacion)), ''), b.grupoDigitacion) AS grupoDigitacion,
        COALESCE(ge.idUsuarioDigitador, gt.idUsuarioDigitador) AS idUsuarioDigitador,
        COALESCE(ge.digitador, gt.digitador) AS digitador,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.tecnico)), ''), NULLIF(LTRIM(RTRIM(gt.tecnico)), ''), b.tecnico) AS tecnico,
        COALESCE(ge.id_tecnicoAuxiliar, gt.id_tecnicoAuxiliar) AS id_tecnicoAuxiliar,
        COALESCE(ge.auxiliar, gt.auxiliar) AS auxiliar,
        COALESCE(ge.idUsuarioSupervisor, gt.idUsuarioSupervisor) AS idUsuarioSupervisor,
        COALESCE(ge.supervisorACargo, gt.supervisorACargo) AS supervisorACargo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.sucursal)), ''), NULLIF(LTRIM(RTRIM(gt.sucursal)), ''), b.sucursal) AS sucursal,
        COALESCE(ge.observacion, gt.observacion) AS observacion,
        COALESCE(ge.idUsuarioRegistra, gt.idUsuarioRegistra) AS idUsuarioRegistra,
        COALESCE(ge.fechaRegistro, gt.fechaRegistro, b.fechaRegistro) AS fechaRegistro,
        COALESCE(ge.e_eliminado, gt.e_eliminado, b.e_eliminado) AS e_eliminado
    FROM BaseCuadrillas b
    LEFT JOIN GuardadasGrupo ge
        ON ge.id_tecnico = b.id_tecnico
       AND UPPER(LTRIM(RTRIM(ISNULL(ge.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
       AND ge.rn = 1
    LEFT JOIN GuardadasTecnico gt
        ON gt.id_tecnico = b.id_tecnico
       AND gt.rn = 1
    WHERE @SucursalNormalizada IS NULL
       OR UPPER(LTRIM(RTRIM(ISNULL(COALESCE(ge.sucursal, gt.sucursal, b.sucursal), '')))) = UPPER(@SucursalNormalizada)
    ORDER BY
        COALESCE(ge.e_eliminado, gt.e_eliminado, b.e_eliminado),
        COALESCE(ge.grupo, gt.grupo, b.grupo),
        COALESCE(ge.tecnico, gt.tecnico, b.tecnico),
        b.id_ruta;
END
GO

/* CREATE [spx_obtenerconformacioncuadrillawebporid] */
CREATE PROC [dbo].[spx_ObtenerConformacionCuadrillaWebPorId]
    @Id BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH VersionActual AS (
        SELECT TOP 1 LTRIM(RTRIM(v.sucursal)) AS sucursal
        FROM dbo.tbl_version v
        WHERE v.sucursal IS NOT NULL
          AND LTRIM(RTRIM(v.sucursal)) <> ''
    ),
    BaseRow AS (
        SELECT TOP 1
            CAST(r.Id_Ruta AS BIGINT) AS id_ruta,
            CAST(GETDATE() AS DATE) AS fecha,
            CAST('PENDIENTE' AS NVARCHAR(20)) AS estado,
            CAST(
                CASE
                    WHEN UPPER(LTRIM(RTRIM(ISNULL(r.Tipo, '')))) IN ('TITULAR', 'BACKUP')
                        THEN UPPER(LTRIM(RTRIM(r.Tipo)))
                    ELSE 'TITULAR'
                END
                AS NVARCHAR(20)
            ) AS actividad,
            v.Id_Vendedor AS id_tecnico,
            v.CuentaSF AS cuenta_sf,
            v.SalesForce AS salesforce,
            v.Habilidad AS habilidad,
            v.Vehiculo AS vehiculo,
            r.Nombre AS grupo,
            COALESCE(NULLIF(LTRIM(RTRIM(r.BodegaTigo)), ''), NULLIF(LTRIM(RTRIM(r.almacenTigo)), '')) AS almacen,
            COALESCE(NULLIF(LTRIM(RTRIM(r.almacenTigo)), ''), NULLIF(LTRIM(RTRIM(r.BodegaTigo)), '')) AS grupoDigitacion,
            v.Nombre AS tecnico,
            va.sucursal AS sucursal,
            GETDATE() AS fechaRegistro,
            CONVERT(BIT, ISNULL(r.E_Eliminado, 0)) AS e_eliminado
        FROM dbo.tbl_Ruta r
        INNER JOIN dbo.tbl_Vendedor v
            ON v.Id_Vendedor = r.Id_Vendedor
        CROSS JOIN VersionActual va
        WHERE r.Id_Ruta = @Id
          AND v.E_Eliminado = 0
    ),
    GuardadaGrupo AS (
        SELECT TOP 1 g.*
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        INNER JOIN BaseRow b
            ON b.id_tecnico = g.id_tecnico
           AND UPPER(LTRIM(RTRIM(ISNULL(g.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
        WHERE ISNULL(g.e_eliminado, 0) = 0
        ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
    ),
    GuardadaTecnico AS (
        SELECT TOP 1 g.*
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        INNER JOIN BaseRow b
            ON b.id_tecnico = g.id_tecnico
        WHERE ISNULL(g.e_eliminado, 0) = 0
        ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
    )
    SELECT TOP 1
        b.id_ruta AS id,
        b.id_ruta,
        COALESCE(gg.fecha, gt.fecha, b.fecha) AS fecha,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.estado)), ''), NULLIF(LTRIM(RTRIM(gt.estado)), ''), b.estado) AS estado,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.actividad)), ''), NULLIF(LTRIM(RTRIM(gt.actividad)), ''), b.actividad) AS actividad,
        b.id_tecnico,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.cuenta_sf)), ''), NULLIF(LTRIM(RTRIM(gt.cuenta_sf)), ''), b.cuenta_sf) AS cuenta_sf,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.salesforce)), ''), NULLIF(LTRIM(RTRIM(gt.salesforce)), ''), b.salesforce) AS salesforce,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.habilidad)), ''), NULLIF(LTRIM(RTRIM(gt.habilidad)), ''), b.habilidad) AS habilidad,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.vehiculo)), ''), NULLIF(LTRIM(RTRIM(gt.vehiculo)), ''), b.vehiculo) AS vehiculo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.grupo)), ''), NULLIF(LTRIM(RTRIM(gt.grupo)), ''), b.grupo) AS grupo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.almacen)), ''), NULLIF(LTRIM(RTRIM(gt.almacen)), ''), b.almacen) AS almacen,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.grupoDigitacion)), ''), NULLIF(LTRIM(RTRIM(gt.grupoDigitacion)), ''), b.grupoDigitacion) AS grupoDigitacion,
        COALESCE(gg.idUsuarioDigitador, gt.idUsuarioDigitador) AS idUsuarioDigitador,
        COALESCE(gg.digitador, gt.digitador) AS digitador,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.tecnico)), ''), NULLIF(LTRIM(RTRIM(gt.tecnico)), ''), b.tecnico) AS tecnico,
        COALESCE(gg.id_tecnicoAuxiliar, gt.id_tecnicoAuxiliar) AS id_tecnicoAuxiliar,
        COALESCE(gg.auxiliar, gt.auxiliar) AS auxiliar,
        COALESCE(gg.idUsuarioSupervisor, gt.idUsuarioSupervisor) AS idUsuarioSupervisor,
        COALESCE(gg.supervisorACargo, gt.supervisorACargo) AS supervisorACargo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.sucursal)), ''), NULLIF(LTRIM(RTRIM(gt.sucursal)), ''), b.sucursal) AS sucursal,
        COALESCE(gg.observacion, gt.observacion) AS observacion,
        COALESCE(gg.idUsuarioRegistra, gt.idUsuarioRegistra) AS idUsuarioRegistra,
        COALESCE(gg.fechaRegistro, gt.fechaRegistro, b.fechaRegistro) AS fechaRegistro,
        COALESCE(gg.e_eliminado, gt.e_eliminado, b.e_eliminado) AS e_eliminado
    FROM BaseRow b
    LEFT JOIN GuardadaGrupo gg
        ON gg.id_tecnico = b.id_tecnico
       AND UPPER(LTRIM(RTRIM(ISNULL(gg.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
    LEFT JOIN GuardadaTecnico gt
        ON gt.id_tecnico = b.id_tecnico;
END
GO

/* ALTER [spx_obtenerrutaxidtecnico] */
--select * from tbl_usuariotecnico
--[spx_ObtenerRutaXIdTecnico] 309
ALTER proc [dbo].[spx_ObtenerRutaXIdTecnico](@Id_Tecnico int)
as
--select * from tbl_ruta  where id_vendedor =@Id_Tecnico and e_eliminado=0

BEGIN
    SET NOCOUNT ON;

    IF OBJECT_ID('dbo.tbl_Ruta', 'U') IS NULL
    BEGIN
        RAISERROR('No existe la tabla dbo.tbl_Ruta.', 16, 1);
        RETURN;
    END

    DECLARE @sql NVARCHAR(MAX);

    SET @sql = N'
    SELECT
        r.Id_Ruta AS id_ruta,
        r.Nombre AS cuadrilla,
        r.Nombre AS ruta,
        r.Id_Vendedor AS id_tecnico,'
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'Tipo') IS NOT NULL
            THEN N' r.Tipo AS tipo,'
            ELSE N' CAST(NULL AS NVARCHAR(50)) AS tipo,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'visible') IS NOT NULL
            THEN N' r.visible AS visible,'
            ELSE N' CAST(NULL AS BIT) AS visible,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'BodegaTigo') IS NOT NULL
            THEN N' r.BodegaTigo AS bodega_tigo,'
            ELSE N' CAST(NULL AS NVARCHAR(250)) AS bodega_tigo,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'almacenTigo') IS NOT NULL
            THEN N' r.almacenTigo AS almacen_tigo,'
            ELSE N' CAST(NULL AS NVARCHAR(250)) AS almacen_tigo,'
          END
        + N'
        r.Id_Ruta,
        r.Id_Vendedor,
        v.Nombre AS NombreTecnico,
        v.CuentaSF,
        v.SalesForce
    FROM dbo.tbl_Ruta r
    INNER JOIN dbo.tbl_vendedor v ON r.id_vendedor = v.id_vendedor
    WHERE ISNULL(r.E_Eliminado, 0) = 0
      AND (v.grupodigitacion IS NOT NULL AND v.grupodigitacion <> '''')
      AND (@Id_Tecnico IS NULL OR r.Id_Vendedor = @Id_Tecnico)
    ORDER BY r.Nombre;';

    EXEC sp_executesql @sql, N'@Id_Tecnico INT', @Id_Tecnico = @Id_Tecnico;
END
GO

/* CREATE [spx_obtenertecnicosconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerTecnicosConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        v.Id_Vendedor AS id_tecnico,
        v.Nombre AS tecnico,
        v.CuentaSF AS cuenta_sf,
        v.SalesForce AS salesforce,
        v.Habilidad AS habilidad,
        v.Vehiculo AS vehiculo,
        r.Id_Ruta AS id_ruta,
        r.Nombre AS grupo,
        r.BodegaTigo AS almacen,
        r.almacenTigo AS grupoDigitacion,
        v.*
    FROM dbo.tbl_Vendedor v inner join tbl_Ruta r on r.id_vendedor=v.id_vendedor 
    
    WHERE v.E_Eliminado = 0 and v.id_vendedor >0 and r.e_eliminado=0 and (v.cuentasf is not null and v.cuentasf <>'')
    and (v.salesforce is not null and v.salesforce <>'')
    ORDER BY v.Nombre;
END
GO

/* CREATE [spx_registrarventapararegistrootwb] */
CREATE PROCEDURE [dbo].[spx_RegistrarVentaParaRegistroOTwb]
    @Id_Usuario INT,
    @Id_Vendedor INT,
    @Id_Grupo INT,
    @Id_TipoServicio INT,
    @OrdenTrabajo INT,
    @Observacion NVARCHAR(MAX) = NULL,
    @Total DECIMAL(18,2) = 0,
    @Id_UsuarioE INT = NULL,
    @E_Eliminado BIT = 0,
    @Nombre NVARCHAR(250) = NULL,
    @Origen NVARCHAR(100),
    @Id_Estado INT,
    @Id_Sucursal INT,
    @CodigoCliente INT,
    @TieneObservacion BIT = 0,
    @Latitud DECIMAL(9,6) = NULL,
    @Longitud DECIMAL(9,6) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF @Origen IS NULL OR LTRIM(RTRIM(@Origen)) = ''
        BEGIN
            RAISERROR('Origen es requerido.',16,1);
            RETURN;
        END

        IF COL_LENGTH('dbo.tbl_venta', 'Origen') IS NULL
        BEGIN
            RAISERROR('La columna Origen no existe en dbo.tbl_venta.',16,1);
            RETURN;
        END

        BEGIN TRANSACTION;

        IF EXISTS (
            SELECT 1
            FROM dbo.tbl_venta WITH (UPDLOCK, HOLDLOCK)
            WHERE OrdenTrabajo = @OrdenTrabajo
        )
        BEGIN
            RAISERROR('Ya existe una OT registrada con el mismo numero de orden.',16,1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        INSERT INTO dbo.tbl_venta (
            Id_Usuario,
            Id_Vendedor,
            Id_Ruta,
            Id_TipoServicio,
            Fecha_Ejecucion,
            Fecha_Registro,
            OrdenTrabajo,
            Observacion,
            Total,
            Id_UsuarioE,
            E_Eliminado,
            Nombre,
            Origen,
            Id_Estado,
            Id_Sucursal,
            CodigoCliente,
            TieneObservacion,
            Latitud,
            Longitud
        )
        VALUES (
            @Id_Usuario,
            @Id_Vendedor,
            @Id_Grupo,
            @Id_TipoServicio,
            GETDATE(),
            GETDATE(),
            @OrdenTrabajo,
            @Observacion,
            ISNULL(@Total,0),
            @Id_UsuarioE,
            ISNULL(@E_Eliminado,0),
            @Nombre,
            @Origen,
            @Id_Estado,
            @Id_Sucursal,
            @CodigoCliente,
            ISNULL(@TieneObservacion,0),
            @Latitud,
            @Longitud
        );

        DECLARE @Id_Venta INT;
        SET @Id_Venta = CAST(SCOPE_IDENTITY() AS INT);

        COMMIT TRANSACTION;

        SELECT
            @Id_Venta AS Id_Venta,
            @OrdenTrabajo AS OrdenTrabajo,
            @CodigoCliente AS CodigoCliente,
            @Id_Sucursal AS Id_Sucursal,
            @Origen AS Origen,
            @Latitud AS Latitud,
            @Longitud AS Longitud;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        DECLARE @ErrMsg NVARCHAR(4000);
        SET @ErrMsg = ERROR_MESSAGE();
        RAISERROR(@ErrMsg,16,1);
    END CATCH
END
GO

/* CREATE [spx_validarventaydetallewb] */
CREATE PROC [dbo].[spx_ValidarVentaYDetallewb]
    @Fecha DATETIME,
    @NroOT INT,
    @NumeroCliente INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CantidadVentas INT = 0;
    DECLARE @CantidadDetallesVenta INT = 0;
    DECLARE @CantidadDetallesCargoUsuario INT = 0;
    DECLARE @CantidadDetalles INT = 0;
    DECLARE @IdEstado INT = NULL;
    DECLARE @AddMaterial_o_CargoUsuario INT = 0;
    DECLARE @HabilitarCargarMaterial INT = 0;
	DECLARE @TieneDetalle INT = 0;

    SELECT @CantidadVentas = COUNT(1)
    FROM dbo.tbl_Venta v
    WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
      AND v.OrdenTrabajo = @NroOT
      AND v.CodigoCliente = @NumeroCliente
--      AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
      AND ISNULL(v.E_Eliminado, 0) = 0;

    IF (@CantidadVentas > 0)
    BEGIN
        SELECT TOP (1)
            @IdEstado = v.Id_Estado,
            @TieneDetalle = v.TieneDetalle
        FROM dbo.tbl_Venta v
        WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
          AND v.OrdenTrabajo = @NroOT
          AND v.CodigoCliente = @NumeroCliente
          --AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
          AND ISNULL(v.E_Eliminado, 0) = 0;

        SELECT @CantidadDetallesVenta = COUNT(1)
        FROM dbo.tbl_CodigoVenta cv
        INNER JOIN dbo.tbl_Venta v
            ON v.Id_Venta = cv.Id_Venta
        WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
          AND v.OrdenTrabajo = @NroOT
          AND v.CodigoCliente = @NumeroCliente
          --AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
          AND ISNULL(v.E_Eliminado, 0) = 0
          AND ISNULL(cv.E_Eliminado, 0) = 0;

        SELECT @CantidadDetallesCargoUsuario = COUNT(1)
        FROM dbo.tbl_CodigoVentaCargoUsuario cvu
        INNER JOIN dbo.tbl_Venta v
            ON v.Id_Venta = cvu.Id_Venta
        WHERE CONVERT(DATE, v.Fecha_Ejecucion) = CONVERT(DATE, @Fecha)
          AND v.OrdenTrabajo = @NroOT
          AND v.CodigoCliente = @NumeroCliente
          --AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
          AND ISNULL(v.E_Eliminado, 0) = 0
          AND ISNULL(cvu.E_Eliminado, 0) = 0;
    END

    IF (@IdEstado IS NOT NULL)
    BEGIN
        SELECT TOP (1)
            @AddMaterial_o_CargoUsuario = CASE WHEN ISNULL(e.AddMaterial_o_CargoUsuario, 0) = 1 THEN 1 ELSE 0 END
        FROM dbo.tbl_estado e
        WHERE e.Id_Estado = @IdEstado
          AND ISNULL(e.E_Eliminado, 0) = 0;
    END

    SET @CantidadDetalles = ISNULL(@CantidadDetallesVenta, 0) + ISNULL(@CantidadDetallesCargoUsuario, 0);
    SET @HabilitarCargarMaterial = CASE
        WHEN @AddMaterial_o_CargoUsuario = 1
             AND @CantidadVentas > 0
             AND @CantidadDetalles = 0
        THEN 1
        ELSE 0
    END;

    SELECT
        CONVERT(DATE, @Fecha) AS Fecha,
        @NroOT AS NroOT,
        @NumeroCliente AS NumeroCliente,
        CASE WHEN @CantidadVentas > 0 THEN 1 ELSE 0 END AS ExisteVenta,
        @CantidadVentas AS CantidadVentas,
        CASE WHEN @CantidadDetalles > 0 THEN 1 ELSE 0 END AS TieneDetalleEnCodigoVenta,
        @CantidadDetalles AS CantidadDetalles,
        case when @IdEstado is null then 0 else @IdEstado end AS IdEstado,
  --      @tienedetalle TieneDetalle,
        @AddMaterial_o_CargoUsuario AS AddMaterial_o_CargoUsuario,
        case when @IdEstado=1 and @AddMaterial_o_CargoUsuario=1  then @tienedetalle else 0 end  AS TieneDetalle;
END
GO

/* CREATE [traertodoslosproductos_sinfungibleweb] */
CREATE proc [dbo].[TraerTodosLosProductos_SinFungibleWeb]
as
select * from tbl_producto  where E_Eliminado=0 and tipomaterial='MATERIAL'
order by nombre
GO
