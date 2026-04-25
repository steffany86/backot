package com.example.TigoStarSystem.ot.repository;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        JdbcTemplate target = template(idSucursal);
        Date fechaSql = sqlDate(fecha);
        try {
            return target.queryForList(
                    "EXEC sp_ObtenerListaOrdenesTrabajo_OTWEB ?",
                    fechaSql
            );
        } catch (DataAccessException ex) {
            return target.queryForList(
                    "EXEC sp_ObtenerListaOrdenesTrabajo ?",
                    fechaSql
            );
        }
    }

    public List<Map<String, Object>> obtenerOrdenesPorRango(LocalDate inicio, LocalDate fin, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerListaOrdenesTrabajoRFechas ?, ?",
                sqlDate(inicio),
                sqlDate(fin)
        );
    }

    public List<Map<String, Object>> obtenerVentasFinalizadasPorFechaYVendedores(
            LocalDate fecha,
            List<Integer> idsVendedor,
            Integer idSucursal) {
        if (idsVendedor == null || idsVendedor.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> idsValidos = new ArrayList<>();
        for (Integer id : idsVendedor) {
            if (id != null && id > 0 && !idsValidos.contains(id)) {
                idsValidos.add(id);
            }
        }
        if (idsValidos.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < idsValidos.size(); i++) {
            if (i > 0) {
                inClause.append(", ");
            }
            inClause.append("?");
        }

        String sql = "SELECT " +
                "v.Id_Venta AS idVenta, " +
                "v.OrdenTrabajo AS ordenTrabajo, " +
                "v.CodigoCliente AS codigoCliente, " +
                "v.Fecha_Ejecucion AS fechaEjecucion, " +
                "v.Origen AS origen, " +
                "v.Id_Vendedor AS idVendedor, " +
                "v.Id_TipoServicio AS idTipoServicio, " +
                "ts.Nombre AS tipoServicio, " +
                "v.Id_Estado AS idEstado, " +
                "e.Nombre AS estado " +
                "FROM dbo.tbl_Venta v " +
                "LEFT JOIN dbo.tbl_tiposervicio ts ON ts.Id_TipoServicio = v.Id_TipoServicio " +
                "LEFT JOIN dbo.tbl_estado e ON e.Id_Estado = v.Id_Estado " +
                "WHERE ISNULL(v.E_Eliminado, 0) = 0 " +
                "AND CONVERT(DATE, v.Fecha_Ejecucion) = ? " +
                "AND v.Id_Vendedor IN (" + inClause + ") " +
                "ORDER BY v.Id_Venta DESC";

        List<Object> params = new ArrayList<>();
        params.add(sqlDate(fecha));
        params.addAll(idsValidos);
        return template(idSucursal).queryForList(sql, params.toArray());
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoPorNumero(String numeroOrden, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerOrdenTrabajo_X_Numero ?",
                numeroOrden
        );
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoPorIdVenta(Long idVenta, Integer idSucursal) {
        return queryForListByIdVentaConSpAlternativos(
                idVenta,
                idSucursal,
                "sp_ObtenerCabezeraOrdenTrabajo_X_Numero",
                "sp_ObtenerOrdenTrabajo_X_Id_Venta"
        );
    }

    public List<Map<String, Object>> obtenerOrdenTrabajoCompletaPorId(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC spx_ObtenerOrdeTrabajoCompletaXID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleInstalado(Long idVenta, Integer idSucursal) {
        List<Map<String, Object>> rows = queryForListByIdVentaConSpAlternativos(
                idVenta,
                idSucursal,
                "sp_ObtenerInstalado_X_Numero",
                "sp_ObtenerDetalleVenta_Instalado_X_ID"
        );
        return enrichRowsConTipoMaterial(rows, idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerDetalleRetirado(Long idVenta, Integer idSucursal) {
        List<Map<String, Object>> rows = queryForListByIdVentaConSpAlternativos(
                idVenta,
                idSucursal,
                "sp_ObteneRetirado_X_Numero",
                "sp_ObtenerDetalleVenta_Retirado_X_ID"
        );
        return enrichRowsConTipoMaterial(rows, idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerDetalleExcedente(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC sp_ObtenerDetalleVenta_Excedente_X_ID ?",
                idVenta
        );
    }

    public List<Map<String, Object>> obtenerDetalleCargoUsuario(Long idVenta, Integer idSucursal) {
        List<Map<String, Object>> rows = queryForListByIdVentaConSpAlternativos(
                idVenta,
                idSucursal,
                "sp_ObtenerDetalleVenta_CargoUsuario_X_ID"
        );
        return enrichRowsConTipoMaterial(rows, idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerEstadoCierrePorIdVenta(Long idVenta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "SELECT TOP (1) " +
                        "v.Id_Estado AS IdEstadoCierre, " +
                        "e.Nombre AS EstadoCierre " +
                        "FROM dbo.tbl_Venta v " +
                        "LEFT JOIN dbo.tbl_estado e ON e.Id_Estado = v.Id_Estado " +
                        "WHERE v.Id_Venta = ? AND ISNULL(v.E_Eliminado, 0) = 0",
                idVenta
        );
    }

    public List<Integer> obtenerIdsVendedorPorIdUsuario(Integer idUsuario, Integer idSucursal) {
        if (idUsuario == null || idUsuario <= 0) {
            return Collections.emptyList();
        }

        JdbcTemplate target = template(idSucursal);
        String[] statements = new String[] {
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_tecnico = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idtecnico = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ?",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_usuario = ?",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idusuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.id_tecnico = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuariotecnico ut " +
                        "WHERE ut.idtecnico = ?",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Usuario = ? AND ISNULL(ut.E_Eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdUsuario = ? AND ISNULL(ut.E_Eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.Id_Tecnico = ? AND ISNULL(ut.E_Eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.Id_Vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.IdTecnico = ? AND ISNULL(ut.E_Eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.id_usuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.idusuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.id_tecnico = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "WHERE ut.idtecnico = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_tecnico = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idtecnico = ? AND ISNULL(ut.e_eliminado, 0) = 0",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ?",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_usuario = ?",
                "SELECT DISTINCT CAST(ut.idvendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idusuario = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.id_tecnico = ?",
                "SELECT DISTINCT CAST(ut.id_vendedor AS INT) AS id_vendedor " +
                        "FROM dbo.tbl_usuaritecnico ut " +
                        "WHERE ut.idtecnico = ?"
        };

        RuntimeException lastError = null;
        for (String sql : statements) {
            try {
                List<Map<String, Object>> rows = target.queryForList(sql, idUsuario);
                if (rows == null || rows.isEmpty()) {
                    continue;
                }
                LinkedHashSet<Integer> out = new LinkedHashSet<>();
                for (Map<String, Object> row : rows) {
                    Object value = row.get("id_vendedor");
                    if (value == null) {
                        value = row.get("Id_Vendedor");
                    }
                    if (value == null) {
                        value = row.get("idvendedor");
                    }
                    if (value == null) {
                        continue;
                    }
                    Integer parsed = parsePositiveInteger(value);
                    if (parsed != null) out.add(parsed);
                }
                if (!out.isEmpty()) return new ArrayList<>(out);
            } catch (DataAccessException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        return Collections.emptyList();
    }

    private Integer parsePositiveInteger(Object value) {
        if (value == null) return null;
        try {
            Integer parsed = value instanceof Number
                    ? ((Number) value).intValue()
                    : Integer.parseInt(String.valueOf(value).trim());
            return parsed != null && parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Map<String, Object> obtenerOrdenTrabajoPorNumeroUnica(String numeroOrden, Integer idSucursal) {
        List<Map<String, Object>> rows = obtenerOrdenTrabajoPorNumero(numeroOrden, idSucursal);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> obtenerUltimaVentaPorOrdenYCliente(Integer ordenTrabajo, Integer codigoCliente, Integer idSucursal) {
        if (ordenTrabajo == null || ordenTrabajo <= 0 || codigoCliente == null || codigoCliente <= 0) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = template(idSucursal).queryForList(
                    "SELECT TOP (1) " +
                            "v.Id_Venta AS idVenta, " +
                            "v.OrdenTrabajo AS ordenTrabajo, " +
                            "v.CodigoCliente AS codigoCliente, " +
                            "v.Origen AS origen, " +
                            "v.Id_Estado AS idEstado, " +
                            "v.Fecha_Ejecucion AS fechaEjecucion " +
                            "FROM dbo.tbl_Venta v " +
                            "WHERE ISNULL(v.E_Eliminado, 0) = 0 " +
                            "AND v.OrdenTrabajo = ? " +
                            "AND v.CodigoCliente = ? " +
                            "ORDER BY v.Id_Venta DESC",
                    ordenTrabajo,
                    codigoCliente
            );
            return rows == null || rows.isEmpty() ? null : rows.get(0);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public List<Map<String, Object>> obtenerCargoUsuarioExistente(String serie, String chipId) {
        boolean serieVacia = serie == null || serie.trim().isEmpty();
        boolean chipVacio = chipId == null || chipId.trim().isEmpty();
        if (serieVacia && chipVacio) {
            return java.util.Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT TOP 1 Id FROM dbo.tbl_CodigoVentaCargoUsuario " +
                        "WHERE E_Eliminado = 0 AND ((? <> '' AND Serial = ?) OR (? <> '' AND ChipId = ?))",
                serie == null ? "" : serie,
                serie == null ? "" : serie,
                chipId == null ? "" : chipId,
                chipId == null ? "" : chipId
        );
    }

    public List<Map<String, Object>> obtenerSaldoRuta(Integer idRuta, LocalDate fecha, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC dbo.spx_ObtenerSaldoRuta ?, ?",
                idRuta,
                sqlDate(fecha)
        );
    }

    public List<Map<String, Object>> obtenerSaldoRutaBasico(Integer idRuta, Integer idSucursal) {
        return template(idSucursal).queryForList(
                "EXEC dbo.spb_SaldoRutasCantidad_X_Ruta ?",
                idRuta
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

    public Map<String, Object> validarSerieChipIdUnicos(String serie, String chipId) {
        List<Map<String, Object>> rows = template(null).queryForList(
                "SELECT TOP 1 serial, chipid, id_producto, e_eliminado " +
                        "FROM dbo.tbl_productos " +
                        "WHERE (serial = ? OR chipid = ?) AND e_eliminado = 0",
                serie,
                chipId
        );
        if (rows.isEmpty()) {
            return buildSerieChipResult(false, false, false, serie, chipId);
        }

        Map<String, Object> row = rows.get(0);
        String serieDb = normalizeText(asString(row.get("serial")));
        String chipDb = normalizeText(asString(row.get("chipid")));
        String serieInput = normalizeText(serie);
        String chipInput = normalizeText(chipId);
        boolean serieExiste = !serieDb.isEmpty() && serieDb.equals(serieInput);
        boolean chipExiste = !chipDb.isEmpty() && chipDb.equals(chipInput);
        boolean mismoRegistro = serieExiste && chipExiste;
        return buildSerieChipResult(serieExiste, chipExiste, mismoRegistro, serie, chipId);
    }

    private Map<String, Object> buildSerieChipResult(
            boolean serieExiste,
            boolean chipExiste,
            boolean mismoRegistro,
            String serie,
            String chipId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("serie", serie);
        result.put("chipId", chipId);
        result.put("serieExiste", serieExiste);
        result.put("chipExiste", chipExiste);
        result.put("mismoRegistro", mismoRegistro);
        result.put("sePuede", mismoRegistro);
        if (mismoRegistro) {
            result.put("observacion", "Serie y ChipID coinciden correctamente.");
        } else if (!serieExiste && chipExiste) {
            result.put("observacion", "La serie no existe, pero el ChipID ya esta registrado.");
        } else if (serieExiste && !chipExiste) {
            result.put("observacion", "La serie ya existe, pero el ChipID no coincide o no existe.");
        } else if (serieExiste) {
            result.put("observacion", "La serie y el ChipID existen, pero no corresponden al mismo registro.");
        } else {
            result.put("observacion", "La serie y el ChipID no existen en saldo.");
        }
        return result;
    }

    public Map<String, Object> obtenerDigitosProducto(Integer idProducto, Integer idSucursal) {
        List<Map<String, Object>> rows = template(idSucursal).queryForList(
                "SELECT DigitosImei, DigitosChipId FROM dbo.tbl_Producto " +
                        "WHERE Id_Producto = ? AND E_Eliminado = 0",
                idProducto
        );
        if (rows.isEmpty() && idSucursal != null) {
            rows = template(null).queryForList(
                    "SELECT DigitosImei, DigitosChipId FROM dbo.tbl_Producto " +
                            "WHERE Id_Producto = ? AND E_Eliminado = 0",
                    idProducto
            );
        }
        return rows.isEmpty() ? null : rows.get(0);
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

    public Integer insertarCodigoVentaCargoUsuario(
            Long idVenta,
            Integer idProducto,
            String serie,
            String chipId,
            Integer cantidad,
            String existe,
            Integer idSucursal) {
        JdbcTemplate target = template(idSucursal);
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            target.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dbo.tbl_CodigoVentaCargoUsuario " +
                                "(Id_Venta, Id_Producto, Serial, ChipId, Cantidad, Existe, E_Eliminado) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 0)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, idVenta);
                ps.setInt(2, idProducto);
                ps.setString(3, serie);
                ps.setString(4, chipId);
                ps.setInt(5, cantidad);
                ps.setString(6, existe);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key == null ? null : key.intValue();
        } catch (org.springframework.jdbc.BadSqlGrammarException ex) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            target.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dbo.tbl_CodigoVentaCargoUsuario " +
                                "(Id_Venta, Id_Producto, Serial, ChipId, Cantidad, E_Eliminado) " +
                                "VALUES (?, ?, ?, ?, ?, 0)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, idVenta);
                ps.setInt(2, idProducto);
                ps.setString(3, serie);
                ps.setString(4, chipId);
                ps.setInt(5, cantidad);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return key == null ? null : key.intValue();
        }
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
            Integer idTipoMaterial,
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
                            "(Id_Devolucion, Id_Producto, Id_TipoMaterial, Cod_Inicio, ChipID, Cantidad, E_Eliminado, Entregado, PendienteRecojo) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            boolean entregadoValue = entregado != null && entregado;
            ps.setInt(1, idDevolucion);
            ps.setInt(2, idProducto);
            ps.setInt(3, idTipoMaterial == null ? 0 : idTipoMaterial);
            ps.setString(4, codInicio);
            ps.setString(5, chipId);
            ps.setBigDecimal(6, cantidad);
            ps.setBoolean(7, entregadoValue);
            ps.setBoolean(8, !entregadoValue);
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

    public int contarDetallesPorIdVenta(Long idVenta, Integer idSucursal) {
        if (idVenta == null || idVenta <= 0) {
            return 0;
        }
        JdbcTemplate target = template(idSucursal);
        String[] statements = new String[] {
                "SELECT " +
                        "(SELECT COUNT(1) FROM dbo.tbl_CodigoVenta cv WHERE cv.Id_Venta = ? AND ISNULL(cv.E_Eliminado, 0) = 0) + " +
                        "(SELECT COUNT(1) FROM dbo.tbl_CodigoVentaCargoUsuario cu WHERE cu.Id_Venta = ? AND ISNULL(cu.E_Eliminado, 0) = 0) AS total",
                "SELECT " +
                        "(SELECT COUNT(1) FROM dbo.tbl_codigoventa cv WHERE cv.id_venta = ? AND ISNULL(cv.e_eliminado, 0) = 0) + " +
                        "(SELECT COUNT(1) FROM dbo.tbl_codigoventacargousuario cu WHERE cu.id_venta = ? AND ISNULL(cu.e_eliminado, 0) = 0) AS total",
                "SELECT COUNT(1) AS total FROM dbo.tbl_CodigoVenta cv WHERE cv.Id_Venta = ? AND ISNULL(cv.E_Eliminado, 0) = 0",
                "SELECT COUNT(1) AS total FROM dbo.tbl_codigoventa cv WHERE cv.id_venta = ? AND ISNULL(cv.e_eliminado, 0) = 0"
        };

        for (String sql : statements) {
            try {
                Integer total;
                if (sql.contains("tbl_CodigoVentaCargoUsuario") || sql.contains("tbl_codigoventacargousuario")) {
                    total = target.queryForObject(sql, Integer.class, idVenta, idVenta);
                } else {
                    total = target.queryForObject(sql, Integer.class, idVenta);
                }
                if (total != null && total >= 0) {
                    return total;
                }
            } catch (DataAccessException ex) {
                // Intentar siguiente variante.
            }
        }
        return 0;
    }

    public int promoverOrigenManualAOtWeb(
            LocalDate fechaEjecucion,
            Integer ordenTrabajo,
            Integer codigoCliente,
            Integer idSucursal) {
        if (fechaEjecucion == null || ordenTrabajo == null || ordenTrabajo <= 0 || codigoCliente == null || codigoCliente <= 0) {
            return 0;
        }
        return template(idSucursal).update(
                "UPDATE dbo.tbl_Venta " +
                        "SET Origen = 'OT_WEB' " +
                        "WHERE CONVERT(DATE, Fecha_Ejecucion) = ? " +
                        "AND OrdenTrabajo = ? " +
                        "AND CodigoCliente = ? " +
                        "AND ISNULL(E_Eliminado, 0) = 0 " +
                        "AND UPPER(LTRIM(RTRIM(ISNULL(Origen, '')))) = 'MANUAL'",
                sqlDate(fechaEjecucion),
                ordenTrabajo,
                codigoCliente
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Ejecuta una lista de SP alternativos para detalle por Id_Venta.
     */
    private List<Map<String, Object>> queryForListByIdVentaConSpAlternativos(
            Long idVenta,
            Integer idSucursal,
            String... storedProcedures) {
        if (idVenta == null || idVenta <= 0) {
            return java.util.Collections.emptyList();
        }
        JdbcTemplate target = template(idSucursal);
        RuntimeException lastError = null;

        for (String storedProcedure : storedProcedures) {
            if (storedProcedure == null || storedProcedure.trim().isEmpty()) {
                continue;
            }
            try {
                return target.queryForList("EXEC dbo." + storedProcedure + " ?", idVenta);
            } catch (DataAccessException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        return java.util.Collections.emptyList();
    }

    private List<Map<String, Object>> enrichRowsConTipoMaterial(List<Map<String, Object>> rows, Long idVenta, Integer idSucursal) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }

        final Map<Integer, String> tipoMaterialById = loadTipoMaterialMap(idSucursal);
        final Map<String, String> tipoMaterialByDetalle = loadTipoMaterialByDetalleCodigoVenta(idVenta, idSucursal);
        final Map<Integer, String> tipoMaterialByProducto = loadTipoMaterialByProductoMap(idSucursal);
        List<Map<String, Object>> out = new ArrayList<>(rows.size());

        for (Map<String, Object> source : rows) {
            Map<String, Object> row = new LinkedHashMap<>(source);
            Integer idTipoMaterial = firstPositiveInteger(row,
                    "Id_TipoMaterial",
                    "id_tipo_material",
                    "idtipomaterial",
                    "IdTipoMaterial",
                    "idTipoMaterial",
                    "tipoMaterial",
                    "TipoMaterial"
            );
            Integer idProducto = firstPositiveInteger(row,
                    "Id_Producto",
                    "id_producto",
                    "idproducto",
                    "IdProducto",
                    "idProducto"
            );
            String detalleKey = buildDetalleMaterialKey(row);

            String nombre = null;
            if (idTipoMaterial != null && idTipoMaterial > 0) {
                nombre = tipoMaterialById.get(idTipoMaterial);
            }
            if ((nombre == null || nombre.trim().isEmpty()) && detalleKey != null && !detalleKey.isEmpty()) {
                nombre = tipoMaterialByDetalle.get(detalleKey);
            }
            if ((nombre == null || nombre.trim().isEmpty()) && idProducto != null && idProducto > 0) {
                nombre = tipoMaterialByProducto.get(idProducto);
            }

            if (nombre != null && !nombre.trim().isEmpty()) {
                row.put("TipoMaterial", nombre.trim());
            } else if (idTipoMaterial != null && idTipoMaterial > 0) {
                row.put("TipoMaterial", "ID " + idTipoMaterial);
            }

            out.add(row);
        }

        return out;
    }

    private String buildDetalleMaterialKey(Map<String, Object> row) {
        Integer idProducto = firstPositiveInteger(row,
                "Id_Producto",
                "id_producto",
                "idproducto",
                "IdProducto",
                "idProducto"
        );
        if (idProducto == null || idProducto <= 0) {
            return "";
        }

        String codInicio = normalizeText(firstStringIgnoreCase(row,
                "Cod_Inicio",
                "cod_inicio",
                "CodInicio",
                "codInicio",
                "Serial",
                "serial"
        ));
        String chipId = normalizeText(firstStringIgnoreCase(row,
                "ChipID",
                "chipid",
                "ChipId",
                "chipId"
        ));
        String cantidad = normalizeDecimalLike(firstStringIgnoreCase(row, "Cantidad", "cantidad"));
        return idProducto + "|" + codInicio + "|" + chipId + "|" + cantidad;
    }

    private String normalizeDecimalLike(String value) {
        if (value == null) return "";
        String source = value.trim();
        if (source.isEmpty()) return "";
        try {
            return new BigDecimal(source).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return source;
        }
    }

    private String firstStringIgnoreCase(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) continue;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() == null) continue;
                if (entry.getKey().equalsIgnoreCase(key)) {
                    Object value = entry.getValue();
                    return value == null ? "" : String.valueOf(value);
                }
            }
        }
        return "";
    }

    private Integer firstPositiveInteger(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (entry.getKey().equalsIgnoreCase(key)) {
                    Integer parsed = parsePositiveInteger(entry.getValue());
                    if (parsed != null && parsed > 0) {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }

    private Map<Integer, String> loadTipoMaterialMap(Integer idSucursal) {
        try {
            List<Map<String, Object>> rows = template(idSucursal).queryForList(
                    "SELECT CAST(Id_TipoMaterial AS INT) AS idTipoMaterial, " +
                            "LTRIM(RTRIM(ISNULL(Nombre, ''))) AS nombre " +
                            "FROM dbo.tbl_tipomaterial " +
                            "WHERE ISNULL(E_Eliminado, 0) = 0"
            );
            Map<Integer, String> out = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Integer id = firstPositiveInteger(row, "idTipoMaterial", "Id_TipoMaterial", "id_tipo_material");
                if (id == null || id <= 0) {
                    continue;
                }
                Object nombreRaw = row.get("nombre");
                String nombre = nombreRaw == null ? "" : String.valueOf(nombreRaw).trim();
                if (!nombre.isEmpty()) {
                    out.put(id, nombre);
                }
            }
            return out;
        } catch (DataAccessException ex) {
            return Collections.emptyMap();
        }
    }

    private Map<Integer, String> loadTipoMaterialByProductoMap(Integer idSucursal) {
        try {
            List<Map<String, Object>> rows = template(idSucursal).queryForList(
                    "SELECT CAST(p.Id_Producto AS INT) AS idProducto, " +
                            "LTRIM(RTRIM(ISNULL(p.TipoMaterial, ''))) AS tipoMaterial " +
                            "FROM dbo.tbl_producto p " +
                            "WHERE ISNULL(p.E_Eliminado, 0) = 0"
            );
            Map<Integer, String> out = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Integer idProducto = firstPositiveInteger(row, "idProducto", "Id_Producto", "id_producto");
                if (idProducto == null || idProducto <= 0) {
                    continue;
                }
                Object tipoMaterialRaw = row.get("tipoMaterial");
                String tipoMaterial = tipoMaterialRaw == null ? "" : String.valueOf(tipoMaterialRaw).trim();
                if (!tipoMaterial.isEmpty()) {
                    out.put(idProducto, tipoMaterial);
                }
            }
            return out;
        } catch (DataAccessException ex) {
            return Collections.emptyMap();
        }
    }

    private Map<String, String> loadTipoMaterialByDetalleCodigoVenta(Long idVenta, Integer idSucursal) {
        if (idVenta == null || idVenta <= 0) {
            return Collections.emptyMap();
        }
        try {
            List<Map<String, Object>> rows = template(idSucursal).queryForList(
                    "SELECT " +
                            "CAST(cv.Id_Producto AS INT) AS idProducto, " +
                            "LTRIM(RTRIM(ISNULL(cv.Cod_Inicio, ''))) AS codInicio, " +
                            "LTRIM(RTRIM(ISNULL(cv.ChipID, ''))) AS chipId, " +
                            "CAST(ISNULL(cv.Cantidad, 0) AS DECIMAL(18,4)) AS cantidad, " +
                            "LTRIM(RTRIM(ISNULL(tm.Nombre, ''))) AS tipoMaterial " +
                            "FROM dbo.tbl_codigoventa cv " +
                            "LEFT JOIN dbo.tbl_tipomaterial tm " +
                            "ON tm.Id_TipoMaterial = cv.Id_TipoMaterial " +
                            "AND ISNULL(tm.E_Eliminado, 0) = 0 " +
                            "WHERE cv.Id_Venta = ? " +
                            "AND ISNULL(cv.E_Eliminado, 0) = 0",
                    idVenta
            );
            Map<String, String> out = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Integer idProducto = firstPositiveInteger(row, "idProducto", "Id_Producto", "id_producto");
                if (idProducto == null || idProducto <= 0) continue;
                String codInicio = normalizeText(firstStringIgnoreCase(row, "codInicio", "Cod_Inicio", "cod_inicio"));
                String chipId = normalizeText(firstStringIgnoreCase(row, "chipId", "ChipID", "chipid"));
                String cantidad = normalizeDecimalLike(firstStringIgnoreCase(row, "cantidad", "Cantidad"));
                String key = idProducto + "|" + codInicio + "|" + chipId + "|" + cantidad;
                String tipoMaterial = firstStringIgnoreCase(row, "tipoMaterial", "TipoMaterial", "Nombre", "nombre").trim();
                if (!tipoMaterial.isEmpty() && !out.containsKey(key)) {
                    out.put(key, tipoMaterial);
                }
            }
            return out;
        } catch (DataAccessException ex) {
            return Collections.emptyMap();
        }
    }
}
