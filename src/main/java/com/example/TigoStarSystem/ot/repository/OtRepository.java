package com.example.TigoStarSystem.ot.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class OtRepository {
    private final JdbcTemplate jdbcTemplate;
    private final OtDbSupport dbSupport;

    public OtRepository(
            JdbcTemplate jdbcTemplate,
            SucursalRepository sucursalRepository,
            @Value("${spring.datasource.driver-class-name}") String dbDriver,
            @Value("${spring.datasource.url}") String mainDatasourceUrl,
            @Value("${app.sucre.datasource.url:}") String sucreDatasourceUrl,
            @Value("${auth.login.sucre.database:SucrePrueba}") String sucreDatabase,
            @Value("${app.sucre.datasource.username:${spring.datasource.username}}") String sucreUsername,
            @Value("${app.sucre.datasource.password:${spring.datasource.password}}") String sucrePassword,
            @Value("${app.datasource.params:encrypt=false;trustServerCertificate=true}") String dbParams) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbSupport = new OtDbSupport(
                sucursalRepository,
                dbDriver,
                mainDatasourceUrl,
                sucreDatasourceUrl,
                sucreDatabase,
                sucreUsername,
                sucrePassword,
                dbParams
        );
    }

    public List<Map<String, Object>> obtenerOrdenesPorFecha(LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerListaOrdenesTrabajo ?",
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> obtenerOrdenesPorRango(LocalDate inicio, LocalDate fin, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerListaOrdenesTrabajoRFechas ?, ?",
                sqlDate(inicio),
                sqlDate(fin)
        );
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoPorNumero(String numeroOrden, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerOrdenTrabajo_X_Numero ?",
                numeroOrden
        );
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoPorIdVenta(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerOrdenTrabajo_X_Id_Venta ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoCompletaPorId(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ObtenerOrdeTrabajoCompletaXID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleInstalado(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerDetalleVenta_Instalado_X_ID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleRetirado(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerDetalleVenta_Retirado_X_ID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleExcedente(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerDetalleVenta_Excedente_X_ID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleCargoUsuario(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerDetalleVenta_CargoUsuario_X_ID ?",
                idVenta
        );
    }

    public int modificarOtRealizada(String observacion, Integer idEstado, String numeroOrden, Integer idSucursal) {
        return template(idSucursal).update(
                "EXEC sp_ModificarOT_OTRealizada ?, ?, ?",
                observacion,
                idEstado,
                numeroOrden
        );
    }

    public List<Map<String, Object>> validarCuadreRuta(Integer idRuta, LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ValidarCuadreRuta ?, ?",
                idRuta,
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> existeCierreAlmacenHoy(LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ExisteCierreAlmacenHoy ?",
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> existeCierreAlmacenHoyPrPd(LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ExisteCierreAlmacenHoyPR_PD ?",
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> validaMovimientos(LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ValidaMovimientos ?",
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> validarEstadoSerie(
            String serie,
            String chipId,
            Integer idProducto,
            Integer idTipoMaterial,
            Integer idRuta,
            Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_VerificarEstadoSerie ?, ?, ?, 3, ?, ?",
                serie,
                chipId,
                idProducto,
                idTipoMaterial,
                idRuta
        );
    }

    public Integer insertarCodigoVenta(
            Long idVenta,
            Integer idProducto,
            Integer idTipoMaterial,
            String codInicio,
            String chipId,
            BigDecimal cantidad,
            Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        target.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.tbl_CodigoVenta " +
                            "(Id_Venta, Id_Producto, Id_TipoMaterial, Cod_Inicio, ChipID, Cantidad, Precio, TotalParcial, E_Eliminado) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, idVenta);
            ps.setInt(2, idProducto);
            ps.setInt(3, idTipoMaterial);
            ps.setString(4, codInicio);
            ps.setString(5, chipId);
            ps.setBigDecimal(6, cantidad);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.intValue();
    }

    public Integer insertarDevolucion(
            Integer idUsuario,
            Integer idRuta,
            Integer idVendedor,
            String nroOrdenTrabajo,
            LocalDate fecha,
            String observacion,
            Long idVenta,
            Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        target.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.tbl_Devolucion " +
                            "(Id_Usuario, Id_Ruta, Id_Vendedor, Id_TipoDevolucion, NroOrdenTrabajo, Fecha, Observacion, E_Eliminado, Estado, Fecha_Registro, Archivo, nombreArchivo, Id_Venta) " +
                            "VALUES (?, ?, ?, 2, ?, ?, ?, 0, 0, ?, ?, '', ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, idUsuario);
            ps.setInt(2, idRuta);
            ps.setInt(3, idVendedor);
            ps.setString(4, nroOrdenTrabajo);
            ps.setDate(5, sqlDate(fecha));
            ps.setString(6, observacion);
            ps.setDate(7, sqlDate(fecha));
            ps.setString(8, "");
            ps.setLong(9, idVenta);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.intValue();
    }

    public Integer insertarDetalleDevolucion(
            Integer idDevolucion,
            Integer idProducto,
            String codInicio,
            String chipId,
            BigDecimal cantidad,
            Boolean entregado,
            Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        target.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.tbl_DetalleDevolucion " +
                            "(Id_Devolucion, Id_Producto, Cod_Inicio, ChipID, Cantidad, E_Eliminado, Entregado, PendienteRecojo) " +
                            "VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            boolean entregadoValue = entregado != null && entregado;
            ps.setInt(1, idDevolucion);
            ps.setInt(2, idProducto);
            ps.setString(3, codInicio);
            ps.setString(4, chipId);
            ps.setBigDecimal(5, cantidad);
            ps.setBoolean(6, entregadoValue);
            ps.setBoolean(7, !entregadoValue);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.intValue();
    }

    public int ejecutarRegModProducto(
            String serie,
            String chipId,
            Integer idRuta,
            Integer idProducto,
            Integer accion,
            Long idReferencia,
            Integer idUsuario,
            Integer idTipoMaterial,
            LocalDate fecha,
            Integer idSucursal) {
        return template(idSucursal).update(
                "EXEC spx_RegMod_Productos ?, ?, ?, ?, ?, ?, ?, ?, ?",
                serie,
                chipId,
                idRuta,
                idProducto,
                accion,
                idReferencia,
                idUsuario,
                idTipoMaterial,
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> sePuedeModificarOrdenTrabajo(
            LocalDate fechaVieja,
            LocalDate fechaNueva,
            Integer idRuta,
            Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_SePuedeModificarOrdenTrabajo ?, ?, ?",
                sqlDate(fechaVieja),
                sqlDate(fechaNueva),
                idRuta
        );
    }

    public int modificarOrdenTrabajoFecha(Integer idUsuario, LocalDate fechaNueva, Long idVenta, Integer idSucursal) {
        return template(idSucursal).update(
                "EXEC spx_ModificarOrdenTrabajoFecha ?, ?, ?",
                idUsuario,
                sqlDate(fechaNueva),
                idVenta
        );
    }

    public int eliminarCodigoUsuarioVenta(Long idVenta, Integer idUsuario, Integer idSucursal) {
        return template(idSucursal).update(
                "EXEC spx_EliminarCodigoUsuario_Venta ?, ?",
                idVenta,
                idUsuario
        );
    }

    public List<Map<String, Object>> obtenerCabeceraVentaParaRegistroOtWb(
            Integer clienteNro,
            Integer ot,
            String tor,
            String grupo,
            String tecnicoNombre,
            Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC dbo.spx_ObtenerCaberaVentaParaRegistroOTwb ?, ?, ?, ?, ?",
                clienteNro,
                ot,
                tor,
                grupo,
                tecnicoNombre
        );
    }

    public Map<String, Object> validarVentaYDetalleWb(
            LocalDate fecha,
            Integer nroOT,
            Integer numeroCliente,
            Integer idSucursal) {
        return template(idSucursal).queryForMap(
                "EXEC dbo.spx_ValidarVentaYDetallewb ?, ?, ?",
                sqlDate(fecha),
                nroOT,
                numeroCliente
        );
    }

    public Map<String, Object> registrarOt(
            Integer idUsuario,
            Integer idRuta,
            Integer idTipoServicio,
            Integer codigoCliente,
            Integer idEstado,
            String observacion,
            Boolean tieneObservacion,
            Integer idSucursal,
            String nombreCliente,
            Integer idSucursalSesion
    ) {
        return template(idSucursalSesion).queryForMap(
                "EXEC spx_RegistrarOrdenTrabajo ?, ?, ?, ?, ?, ?, ?, ?, ?",
                idUsuario,
                idRuta,
                idTipoServicio,
                codigoCliente,
                idEstado,
                observacion,
                tieneObservacion == null ? Boolean.FALSE : tieneObservacion,
                idSucursal,
                nombreCliente
        );
    }

    public Map<String, Object> registrarVentaParaRegistroOtWb(
            Integer idUsuario,
            Integer idVendedor,
            Integer idGrupo,
            Integer idTipoServicio,
            Integer ordenTrabajo,
            String observacion,
            java.math.BigDecimal total,
            Integer idUsuarioE,
            Boolean eEliminado,
            String nombre,
            String origen,
            Integer idEstado,
            Integer idSucursal,
            Integer codigoCliente,
            Boolean tieneObservacion,
            java.math.BigDecimal latitud,
            java.math.BigDecimal longitud,
            Integer idSucursalSesion
    ) {
        return template(idSucursalSesion).queryForMap(
                "EXEC dbo.spx_RegistrarVentaParaRegistroOTwb ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?",
                idUsuario,
                idVendedor,
                idGrupo,
                idTipoServicio,
                ordenTrabajo,
                observacion,
                total,
                idUsuarioE,
                eEliminado,
                nombre,
                origen,
                idEstado,
                idSucursal,
                codigoCliente,
                tieneObservacion,
                latitud,
                longitud
        );
    }

    private JdbcTemplate template(Integer idSucursal) {
        return dbSupport.resolveTemplate(idSucursal, jdbcTemplate);
    }

    private Date sqlDate(LocalDate date) {
        return Date.valueOf(date);
    }
}
