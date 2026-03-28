package com.example.TigoStarSystem.ot.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
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
