-- Backup SP Tarija antes de sync 20260612_112948
USE [BDSistemaAntenaPMTarija];
GO

/* [sp_obtenerlistaordenestrabajo] */
CREATE proc dbo.sp_ObtenerListaOrdenesTrabajo(@Fecha_Registro datetime)
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
	Order by V.Id_Ruta, V.Id_Venta
End
GO

/* [sp_obtenerlistaordenestrabajorfechas] */
CREATE proc dbo.sp_ObtenerListaOrdenesTrabajoRFechas(@Fecha_RegistroInicio datetime,@Fecha_RegistroFin datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, 
	dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo, T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, 
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

/* [spr_traervendedores_x_formtecnico] */
create proc [dbo].[spr_TraerVendedores_x_FormTecnico]
as
begin
	select v.*, ts.id_Tipo_Solicitante, ts.Nombre TipoSolicitante
	from tbl_Vendedor v inner join tbl_TipoSolicitante ts on ts.id_Tipo_Solicitante = v.id_tiposolicitante
	where v.E_Eliminado = 0 and makiro=1
	order by Nombre
end
GO

/* [spx_existecierrealmacenhoy] */


CREATE proc [dbo].[spx_ExisteCierreAlmacenHoy](@FechaRegistro datetime)
as
begin
	declare @cuantostablamov_pendientes int
	declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
	insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fechaRegistro

	declare @textopendientes nvarchar(max)
	set @textopendientes =  (SELECT STUFF(
								(SELECT ', ' + movimiento
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

/* [spx_modificarordentrabajofecha] */

create proc spx_ModificarOrdenTrabajoFecha(@Id_UsuarioModifico int, @fecha datetime,@Id_Venta int)
as
begin
	declare @Id_Devolucion int
	set @Id_Devolucion = 0
	set @Id_Devolucion= (select Id_devolucion from tbl_Devolucion WHERE Id_Venta=@Id_Venta)
		if(@Id_Devolucion>0)
		begin	
		select @Id_Devolucion
			update tbl_Devolucion set Fecha=@fecha where Id_Venta=@Id_Venta
		end
		
		update tbl_Venta set Fecha_Ejecucion=@fecha,Id_UsuarioE=@Id_UsuarioModifico where Id_Venta =@Id_Venta 
end

GO

/* [spx_obtenerrutaxidtecnico] */
create proc spx_ObtenerRutaXIdTecnico(@Id_Tecnico int)
as
select * from tbl_ruta  where id_vendedor =@Id_Tecnico and e_eliminado=0
GO

/* [spx_obtenersaldoruta] */

create proc [dbo].[spx_ObtenerSaldoRuta](@Id_Ruta int,@Fecha datetime)
as
begin
	declare @tablaSaldo table(
		Id_Producto int,
		Nombre nvarchar(max),
		Saldo decimal(18,2),
		Venta decimal(18,2),
		Sobrante decimal(18,2),		
		Precio decimal(18,2),
		TotalVendidos decimal(18,2)
		)
			
		insert into @tablaSaldo
		select st.Id_Producto,pr.Nombre,st.Cantidad ,0,0,pr.PrecioVenta,0
		from tbl_SaldoTarjetas st inner join tbl_producto pr on pr.Id_Producto = st.Id_Producto
		where Id_Ruta=@Id_Ruta and st.E_Eliminado=0


	declare @Venta table(
		Id_Producto int,
		Id_Ruta int,
		Venta decimal(18,2),
		Nombre nvarchar(250)
	)	
	insert into @Venta
	 exec sp_TraerVentaDiaRuta @Id_Ruta ,@Fecha
	 
 		update @tablaSaldo
		set Venta = v.Venta
		from @tablaSaldo c inner join @venta v on v.id_Producto = c.id_producto
		
		update @tablaSaldo
		set TotalVendidos = Venta*Precio
		--from @tablaSaldo c inner join @venta v on v.id_Producto = c.id_producto
		
		update @tablaSaldo
		set Sobrante = Saldo - venta 
		
			
	select * from @tablaSaldo order by Sobrante asc
end


GO
