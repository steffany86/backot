package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.dto.OtCrearRequest;
import com.example.TigoStarSystem.ot.dto.OtCrearResponse;
import com.example.TigoStarSystem.ot.dto.OtCargoUsuarioItemRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarDatosRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaResponse;
import com.example.TigoStarSystem.ot.dto.OtDetalleMaterialRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarDetalleAgendaRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarDetalleAgendaResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarCargoUsuarioRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistroAgendaValidacionResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaResponse;
import com.example.TigoStarSystem.ot.dto.OtRealizadaRequest;
import com.example.TigoStarSystem.ot.dto.OtValidarVentaDetalleResponse;
import com.example.TigoStarSystem.ot.repository.ListaOtRepository;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class OtService {
    private static final Logger logger = LoggerFactory.getLogger(OtService.class);
    private final OtRepository otRepository;
    private final ListaOtRepository listaOtRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Inicializa el servicio principal de Ordenes de Trabajo.
     */
    public OtService(
            OtRepository otRepository,
            ListaOtRepository listaOtRepository,
            SucursalRepository sucursalRepository) {
        this.otRepository = otRepository;
        this.listaOtRepository = listaOtRepository;
        this.sucursalRepository = sucursalRepository;
    }

    /**
     * Lista OT por fecha y sucursal.
     */
    public List<Map<String, Object>> listarPorFecha(LocalDate fecha, Integer idSucursal) {
        logger.info("Listar OT por fecha={}", fecha);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenesPorFecha(fecha, idSucursal);
        logger.debug("Listar OT por fecha: filas={}", rows == null ? 0 : rows.size());
        return rows;
    }

    /**
     * Lista OT por rango de fechas y sucursal.
     */
    public List<Map<String, Object>> listarPorRango(LocalDate inicio, LocalDate fin, Integer idSucursal) {
        logger.info("Listar OT por rango inicio={}, fin={}", inicio, fin);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenesPorRango(inicio, fin, idSucursal);
        logger.debug("Listar OT por rango: filas={}", rows == null ? 0 : rows.size());
        return rows;
    }

    /**
     * Lista ventas finalizadas (tbl_venta) para tecnico autenticado.
     */
    public List<Map<String, Object>> listarFinalizadasPorTecnico(LocalDate fecha, Integer idUsuario, Integer idSucursal) {
        LocalDate fechaFiltro = fecha == null ? LocalDate.now() : fecha;
        if (idUsuario == null || idUsuario <= 0) {
            logger.warn("Finalizadas: idUsuario no valido para filtrar tecnico. fecha={}", fechaFiltro);
            return Collections.emptyList();
        }

        List<Integer> idsVendedor = otRepository.obtenerIdsVendedorPorIdUsuario(idUsuario, idSucursal);
        if (idsVendedor == null || idsVendedor.isEmpty()) {
            logger.warn("Finalizadas: no se encontro relacion id_usuario -> id_vendedor. idUsuario={}, fecha={}", idUsuario, fechaFiltro);
            return Collections.emptyList();
        }

        List<Map<String, Object>> rows = otRepository.obtenerVentasFinalizadasPorFechaYVendedores(fechaFiltro, idsVendedor, idSucursal);
        logger.debug(
                "Finalizadas: fecha={}, idUsuario={}, idsVendedor={}, filas={}",
                fechaFiltro,
                idUsuario,
                idsVendedor,
                rows == null ? 0 : rows.size()
        );
        return rows;
    }

    /**
     * Obtiene una OT por id de venta.
     */
    public Map<String, Object> obtenerPorId(Long idVenta, Integer idSucursal) {
        logger.info("Obtener OT por idVenta={}", idVenta);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenTrabajoPorIdVenta(idVenta, idSucursal);
        if (rows.isEmpty()) {
            throw notFound("Orden de trabajo no encontrada para id: " + idVenta);
        }
        return rows.get(0);
    }

    /**
     * Obtiene una OT por numero de orden.
     */
    public Map<String, Object> obtenerPorNumero(String numeroOrden, Integer idSucursal) {
        logger.info("Obtener OT por numero={}", numeroOrden);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenTrabajoPorNumero(numeroOrden, idSucursal);
        if (rows.isEmpty()) {
            throw notFound("Orden de trabajo no encontrada para numero: " + numeroOrden);
        }
        return rows.get(0);
    }

    /**
     * Obtiene detalle de materiales instalados de una venta.
     */
    public List<Map<String, Object>> obtenerDetalleInstalado(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleInstalado(idVenta, idSucursal);
    }

    /**
     * Obtiene detalle de materiales retirados de una venta.
     */
    public List<Map<String, Object>> obtenerDetalleRetirado(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleRetirado(idVenta, idSucursal);
    }

    /**
     * Obtiene detalle de excedentes de una venta.
     */
    public List<Map<String, Object>> obtenerDetalleExcedente(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleExcedente(idVenta, idSucursal);
    }

    /**
     * Obtiene detalle de cargo usuario para una venta.
     */
    public List<Map<String, Object>> obtenerDetalleCargoUsuario(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleCargoUsuario(idVenta, idSucursal);
    }

    /**
     * Obtiene el registro completo de una venta (cabecera + detalles) por Id_Venta.
     */
    public Map<String, Object> obtenerRegistroCompletoPorIdVenta(Long idVenta, Integer idSucursal) {
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idVenta es requerido.");
        }

        List<Map<String, Object>> cabeceraRows = otRepository.obtenerOrdenTrabajoPorIdVenta(idVenta, idSucursal);
        if (cabeceraRows == null || cabeceraRows.isEmpty()) {
            throw notFound("No se encontro registro en tbl_venta para idVenta: " + idVenta);
        }

        Map<String, Object> cabecera = new LinkedHashMap<>(cabeceraRows.get(0));
        List<Map<String, Object>> estadoRows = otRepository.obtenerEstadoCierrePorIdVenta(idVenta, idSucursal);
        if (estadoRows != null && !estadoRows.isEmpty()) {
            Map<String, Object> estadoRow = estadoRows.get(0);
            Integer idEstadoCierre = toInteger(findValue(estadoRow, "IdEstadoCierre", "idestadocierre", "Id_Estado", "id_estado"));
            String estadoCierre = asString(findValue(estadoRow, "EstadoCierre", "estadocierre", "Nombre", "nombre"));
            if (idEstadoCierre != null) {
                cabecera.put("idEstadoCierre", idEstadoCierre);
            }
            if (estadoCierre != null && !estadoCierre.trim().isEmpty()) {
                cabecera.put("estadoCierre", estadoCierre.trim());
            } else if (idEstadoCierre != null) {
                cabecera.put("estadoCierre", "ID " + idEstadoCierre);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cabecera", cabecera);
        out.put("instalados", otRepository.obtenerDetalleInstalado(idVenta, idSucursal));
        out.put("retirados", otRepository.obtenerDetalleRetirado(idVenta, idSucursal));
        out.put("cargoUsuario", otRepository.obtenerDetalleCargoUsuario(idVenta, idSucursal));
        return out;
    }

    /**
     * Registra detalle de cargo usuario para una OT.
     */
    @Transactional
    public int registrarCargoUsuario(OtRegistrarCargoUsuarioRequest request, Integer idSucursal) {
        if (request == null || request.getNumeroOrden() == null || request.getNumeroOrden().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "numeroOrden es requerido.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe agregar al menos un producto de cargo usuario.");
        }

        Map<String, Object> ventaRow = otRepository.obtenerOrdenTrabajoPorNumeroUnica(request.getNumeroOrden().trim(), idSucursal);
        if (ventaRow == null) {
            throw notFound("Orden de trabajo no encontrada para numero: " + request.getNumeroOrden());
        }

        Long idVenta = toLong(findValue(ventaRow, "Id_Venta", "idVenta", "id_venta"));
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "VALIDATION_ERROR", "No se pudo resolver la venta asociada a la OT.");
        }
        Integer idRuta = toInteger(findValue(ventaRow, "Id_Ruta", "id_ruta", "idruta"));
        Integer idUsuario = toInteger(findValue(ventaRow, "Id_Usuario", "id_usuario", "idusuario"));
        LocalDate fechaEjecucion = toLocalDate(findValue(ventaRow, "Fecha_Ejecucion", "fecha_ejecucion", "fecha"));
        LocalDate fechaTrabajo = fechaEjecucion == null ? LocalDate.now() : fechaEjecucion;
        if (idRuta == null || idRuta <= 0 || idUsuario == null || idUsuario <= 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VALIDATION_ERROR",
                    "No se pudo resolver ruta/usuario de la venta para actualizar estados de cargo usuario."
            );
        }

        int guardados = 0;
        for (OtCargoUsuarioItemRequest item : request.getItems()) {
            if (item == null) {
                continue;
            }
            Integer idProducto = item.getIdProducto();
            if (idProducto == null || idProducto <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cada item debe tener idProducto.");
            }
            String serie = safeTrim(item.getSerie());
            String chipId = safeTrim(item.getChipId());
            Integer cantidad = item.getCantidad() == null ? 0 : item.getCantidad();
            if (cantidad <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La cantidad debe ser mayor a cero.");
            }

            List<Map<String, Object>> duplicados = otRepository.obtenerCargoUsuarioExistente(serie, chipId);
            if (!duplicados.isEmpty()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "VALIDATION_ERROR",
                        "Ya existe un cargo usuario registrado con la misma serie o ChipID."
                );
            }

            otRepository.insertarCodigoVentaCargoUsuario(
                    idVenta,
                    idProducto,
                    serie,
                    chipId,
                    cantidad,
                    safeTrim(item.getExiste()),
                    idSucursal
            );
            if (!serie.isEmpty() || !chipId.isEmpty()) {
                otRepository.ejecutarRegModProducto(
                        serie,
                        chipId,
                        idRuta,
                        idProducto,
                        41,
                        idVenta,
                        idUsuario,
                        0,
                        fechaTrabajo,
                        idSucursal
                );
            }
            guardados += 1;
        }

        return guardados;
    }

    /**
     * Ejecuta el SP spx_ObtenerSaldoRuta para consultar saldo por ruta y fecha.
     */
    public List<Map<String, Object>> obtenerSaldoRuta(Integer idRuta, LocalDate fecha, Integer idSucursal) {
        validarMayorCero(idRuta, "idRuta");
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        try {
            return otRepository.obtenerSaldoRuta(idRuta, fecha, idSucursal);
        } catch (DataAccessException ex) {
            logger.warn("spx_ObtenerSaldoRuta fallo para idRuta={} fecha={} sucursal={}. Usando saldo basico de respaldo.", idRuta, fecha, idSucursal, ex);
            try {
                return otRepository.obtenerSaldoRutaBasico(idRuta, idSucursal);
            } catch (DataAccessException fallbackEx) {
                throw construirErrorSaldoRuta(idRuta, fecha, idSucursal, ex, fallbackEx);
            }
        } catch (RuntimeException ex) {
            logger.warn("spx_ObtenerSaldoRuta fallo para idRuta={} fecha={} sucursal={}. Usando saldo basico de respaldo.", idRuta, fecha, idSucursal, ex);
            try {
                return otRepository.obtenerSaldoRutaBasico(idRuta, idSucursal);
            } catch (RuntimeException fallbackEx) {
                throw construirErrorSaldoRuta(idRuta, fecha, idSucursal, ex, fallbackEx);
            }
        }
    }

    /**
     * Registra una nueva OT y devuelve ids principales.
     */
    public OtCrearResponse crearOt(OtCrearRequest request, Integer idSucursal) {
        Map<String, Object> result = otRepository.registrarOt(
                request.getIdUsuario(),
                request.getIdRuta(),
                request.getIdTipoServicio(),
                request.getCodigoCliente(),
                request.getIdEstado(),
                request.getObservacion(),
                request.getTieneObservacion(),
                request.getIdSucursal(),
                request.getNombreCliente(),
                idSucursal
        );
        Integer idVenta = toInteger(findValue(result, "idventa", "id_venta"));
        Integer ordenTrabajo = toInteger(findValue(result, "ordentrabajo", "orden_trabajo"));
        return new OtCrearResponse(idVenta, ordenTrabajo);
    }

    /**
     * Marca OT como realizada usando numero de orden.
     */
    public int registrarOtRealizada(OtRealizadaRequest request, Integer idSucursal) {
        return otRepository.modificarOtRealizada(
                request.getObservacion(),
                request.getIdEstado(),
                request.getNumeroOrden(),
                idSucursal
        );
    }

    /**
     * Registra una fila en tbl_venta mediante SP para flujo OT web.
     */
    public OtRegistrarVentaResponse registrarVentaParaRegistroOtWb(
            OtRegistrarVentaRequest request,
            Integer idSucursalSesion) {
        validarRegistroVentaRequest(request);
        Integer idSucursalFinal = request.getIdSucursal() != null ? request.getIdSucursal() : idSucursalSesion;
        if (idSucursalFinal == null || idSucursalFinal <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSucursal es requerido.");
        }
        Integer idSucursalResolucion = idSucursalSesion != null ? idSucursalSesion : idSucursalFinal;
        Integer idVendedorResuelto = resolverIdVendedorRegistroVenta(
                request.getIdUsuario(),
                request.getIdVendedor(),
                idSucursalResolucion
        );

        try {
            Map<String, Object> result = otRepository.registrarVentaParaRegistroOtWb(
                    request.getIdUsuario(),
                    idVendedorResuelto,
                    request.getIdGrupo(),
                    request.getIdTipoServicio(),
                    request.getOrdenTrabajo(),
                    request.getObservacion(),
                    request.getTotal(),
                    request.getIdUsuarioE(),
                    request.getEEliminado(),
                    request.getNombre(),
                    request.getOrigen(),
                    request.getIdEstado(),
                    idSucursalFinal,
                    request.getCodigoCliente(),
                    request.getTieneObservacion(),
                    request.getLatitud(),
                    request.getLongitud(),
                    idSucursalSesion
            );

            return new OtRegistrarVentaResponse(
                    toInteger(findValue(result, "Id_Venta", "id_venta", "idventa")),
                    toInteger(findValue(result, "OrdenTrabajo", "orden_trabajo", "ot")),
                    toInteger(findValue(result, "CodigoCliente", "codigo_cliente", "cliente_nro")),
                    toInteger(findValue(result, "Id_Sucursal", "id_sucursal", "idsucursal")),
                    asString(findValue(result, "Origen", "origen")),
                    toBigDecimal(findValue(result, "Latitud", "latitud")),
                    toBigDecimal(findValue(result, "Longitud", "longitud"))
            );
        } catch (DataAccessException ex) {
            throw traducirErrorRegistroVenta(ex, request);
        }
    }

    /**
     * Modifica datos basicos de OT; usa id path como fallback de numero de orden.
     */
    public int modificarDatosOt(Long idVentaPath, OtModificarDatosRequest request, Integer idSucursal) {
        String numeroOrden = request.getNumeroOrden();
        if (numeroOrden == null || numeroOrden.trim().isEmpty()) {
            // TODO: Confirmar si el {id} del endpoint corresponde al NroOrden o al Id_Venta.
            numeroOrden = String.valueOf(idVentaPath);
        }
        return otRepository.modificarOtRealizada(
                request.getObservacion(),
                request.getIdEstado(),
                numeroOrden,
                idSucursal
        );
    }

    /**
     * Modifica fecha de OT validando reglas de cuadre y ruta.
     */
    public OtModificarFechaResponse modificarFecha(Long idVenta, OtModificarFechaRequest request, Integer idSucursal) {
        List<Map<String, Object>> validacionModificacion =
                otRepository.sePuedeModificarOrdenTrabajo(
                        request.getFechaVieja(),
                        request.getFechaNueva(),
                        request.getIdRuta(),
                        idSucursal
                );
        List<Map<String, Object>> validacionCuadre =
                otRepository.validarCuadreRuta(request.getIdRuta(), request.getFechaNueva(), idSucursal);

        boolean puedeModificar = resultadoValido(validacionModificacion);
        boolean cuadreValido = resultadoValido(validacionCuadre);

        if (!puedeModificar || !cuadreValido) {
            Map<String, Object> details = new HashMap<>();
            details.put("validacionModificacion", validacionModificacion);
            details.put("validacionCuadre", validacionCuadre);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VALIDACION_CUADRE_FALLIDA",
                    "La OT no puede modificar su fecha segun las validaciones de ruta/cuadre.",
                    details
            );
        }

        int updated = otRepository.modificarOrdenTrabajoFecha(
                request.getIdUsuario(),
                request.getFechaNueva(),
                idVenta,
                idSucursal
        );
        return new OtModificarFechaResponse(updated, validacionCuadre, validacionModificacion);
    }

    /**
     * Anula solo el cargo usuario asociado a una OT.
     */
    public int anularSoloCu(Long idVenta, Integer idUsuario, Integer idSucursal) {
        if (idUsuario == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idUsuario es requerido para anular solo CU."
            );
        }
        return otRepository.eliminarCodigoUsuarioVenta(idVenta, idUsuario, idSucursal);
    }

    /**
     * Ejecuta el SP de cabecera de venta para registro OT web controlando errores conocidos.
     */
    public List<Map<String, Object>> obtenerCabeceraVentaParaRegistroOtWb(
            Integer clienteNro,
            Integer ot,
            String tor,
            String grupo,
            String tecnicoNombre,
            Integer idSucursal) {
        validarCabeceraVentaParams(clienteNro, ot, tor, grupo, tecnicoNombre);
        try {
            List<Map<String, Object>> rows = otRepository.obtenerCabeceraVentaParaRegistroOtWb(
                    clienteNro,
                    ot,
                    tor.trim(),
                    grupo.trim(),
                    tecnicoNombre.trim(),
                    idSucursal
            );
            return normalizarCabeceraVentaRows(rows);
        } catch (DataAccessException ex) {
            throw traducirErrorCabeceraVenta(ex, clienteNro, ot, tor, grupo, tecnicoNombre);
        }
    }

    /**
     * Ejecuta el SP spx_ValidarVentaYDetallewb para validar existencia de venta y detalle.
     */
    public OtValidarVentaDetalleResponse validarVentaYDetalleWb(
            String fecha,
            Integer nroOT,
            Integer numeroCliente,
            Integer idSucursal) {
        return validarVentaYDetalleWb(fecha, nroOT, numeroCliente, idSucursal, false);
    }

    public OtValidarVentaDetalleResponse validarVentaYDetalleWb(
            String fecha,
            Integer nroOT,
            Integer numeroCliente,
            Integer idSucursal,
            boolean incluirManual) {
        return validarVentaYDetalleWb(fecha, nroOT, numeroCliente, idSucursal, incluirManual, false);
    }

    public OtValidarVentaDetalleResponse validarVentaYDetalleWb(
            String fecha,
            Integer nroOT,
            Integer numeroCliente,
            Integer idSucursal,
            boolean incluirManual,
            boolean desdeAgenda) {
        LocalDate fechaParsed = parseFechaFlexible(fecha);
        validarMayorCero(nroOT, "nroOT");
        validarMayorCero(numeroCliente, "numeroCliente");

        try {
            boolean agendaMatch = incluirManual && existeRegistroEnAgenda(fechaParsed, nroOT, numeroCliente, idSucursal);
            if (incluirManual && (desdeAgenda || agendaMatch)) {
                try {
                    int actualizados = otRepository.promoverOrigenManualAOtWeb(
                            fechaParsed,
                            nroOT,
                            numeroCliente,
                            idSucursal
                    );
                    if (actualizados > 0) {
                        logger.info(
                                "Ventas manuales promovidas a OT_WEB por cruce agenda. fecha={}, nroOT={}, cliente={}, filas={}, desdeAgenda={}, agendaMatch={}",
                                fechaParsed,
                                nroOT,
                                numeroCliente,
                                actualizados,
                                desdeAgenda,
                                agendaMatch
                        );
                    }
                } catch (DataAccessException ex) {
                    logger.warn(
                            "No se pudo promover origen MANUAL->OT_WEB por cruce agenda (fecha={}, nroOT={}, cliente={}, desdeAgenda={}, agendaMatch={})",
                            fechaParsed,
                            nroOT,
                            numeroCliente,
                            desdeAgenda,
                            agendaMatch,
                            ex
                    );
                }
            }

            Map<String, Object> row = otRepository.validarVentaYDetalleWb(
                    fechaParsed,
                    nroOT,
                    numeroCliente,
                    idSucursal
            );
            Boolean existeVenta = toBoolean(findValue(row, "ExisteVenta", "existeventa"));
            Integer cantidadVentas = toInteger(findValue(row, "CantidadVentas", "cantidadventas"));
            if (existeVenta == null && cantidadVentas != null) {
                existeVenta = cantidadVentas > 0;
            }

            Boolean tieneDetalleEnCodigoVenta = toBoolean(findValue(row, "TieneDetalleEnCodigoVenta", "tienedetalleencodigoventa"));
            Integer cantidadDetalles = toInteger(findValue(row, "CantidadDetalles", "cantidaddetalles"));
            if (tieneDetalleEnCodigoVenta == null && cantidadDetalles != null) {
                tieneDetalleEnCodigoVenta = cantidadDetalles > 0;
            }

            Boolean addMaterialOCargoUsuario = toBoolean(
                    findValue(row,
                            "AddMaterial_o_CargoUsuario",
                            "addmaterial_o_cargousuario",
                            "addMaterialOCargoUsuario",
                            "addmaterialocargousuario")
            );

            Boolean habilitarCargarMaterial = toBoolean(
                    findValue(row,
                            "HabilitarCargarMaterial",
                            "habilitarcargarmaterial",
                            "puedeCargarMaterial",
                            "puedecargarmaterial")
            );
            if (habilitarCargarMaterial == null) {
                if (addMaterialOCargoUsuario != null) {
                    habilitarCargarMaterial =
                            Boolean.TRUE.equals(addMaterialOCargoUsuario) &&
                                    !Boolean.TRUE.equals(tieneDetalleEnCodigoVenta);
                } else {
                    habilitarCargarMaterial =
                            Boolean.TRUE.equals(existeVenta) &&
                                    !Boolean.TRUE.equals(tieneDetalleEnCodigoVenta);
                }
            }

            Map<String, Object> ventaRegistrada = otRepository.obtenerUltimaVentaPorOrdenYCliente(
                    nroOT,
                    numeroCliente,
                    idSucursal
            );
            if (incluirManual && esVentaManual(ventaRegistrada)) {
                Long idVentaManual = toLong(findValue(ventaRegistrada, "idVenta", "Id_Venta", "id_venta"));
                int totalDetallesManual = otRepository.contarDetallesPorIdVenta(idVentaManual, idSucursal);
                boolean tieneDetalleManual = totalDetallesManual > 0;

                tieneDetalleEnCodigoVenta = tieneDetalleManual;
                cantidadDetalles = totalDetallesManual;
                addMaterialOCargoUsuario = !tieneDetalleManual;
                habilitarCargarMaterial = !tieneDetalleManual;

                if (existeVenta == null) {
                    existeVenta = true;
                }
                if (cantidadVentas == null || cantidadVentas <= 0) {
                    cantidadVentas = 1;
                }
            }

            return new OtValidarVentaDetalleResponse(
                    toLocalDate(findValue(row, "Fecha", "fecha")),
                    toInteger(findValue(row, "NroOT", "nroot")),
                    toInteger(findValue(row, "NumeroCliente", "numerocliente", "codigoCliente")),
                    existeVenta,
                    cantidadVentas,
                    tieneDetalleEnCodigoVenta,
                    cantidadDetalles,
                    addMaterialOCargoUsuario,
                    habilitarCargarMaterial
            );
        } catch (DataAccessException ex) {
            throw traducirErrorValidarVentaDetalle(ex, fecha, nroOT, numeroCliente);
        }
    }

    private boolean esVentaManual(Map<String, Object> venta) {
        if (venta == null) {
            return false;
        }
        String origen = asString(findValue(venta, "origen", "Origen"));
        if (origen == null || origen.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeText(origen).replace(" ", "");
        return "manual".equals(normalized);
    }

    private boolean existeRegistroEnAgenda(
            LocalDate fecha,
            Integer nroOT,
            Integer numeroCliente,
            Integer idSucursal) {
        if (fecha == null || nroOT == null || nroOT <= 0 || numeroCliente == null || numeroCliente <= 0) {
            return false;
        }
        try {
            List<Map<String, Object>> rows = listaOtRepository.listarPorFecha(fecha, null, idSucursal);
            if (rows == null || rows.isEmpty()) {
                return false;
            }
            for (Map<String, Object> row : rows) {
                Integer ot = toInteger(findValue(row,
                        "OT", "ot",
                        "OrdenTrabajo", "ordenTrabajo",
                        "NroOT", "nroOT",
                        "Codigo", "codigo"));
                Integer cliente = toInteger(findValue(row,
                        "CodigoCliente", "codigoCliente",
                        "Cliente_Nro", "cliente_nro",
                        "NumeroCliente", "numeroCliente",
                        "CODIGO", "Codigo", "codigo"));
                if (ot == null || cliente == null) {
                    continue;
                }
                if (ot.equals(nroOT) && cliente.equals(numeroCliente)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            logger.warn(
                    "No se pudo verificar cruce con agenda. fecha={}, nroOT={}, cliente={}",
                    fecha,
                    nroOT,
                    numeroCliente,
                    ex
            );
        }
        return false;
    }

    /**
     * Ejecuta las validaciones previas al registro de OT en agenda:
     * cierre de almacen/PR_PD y movimientos pendientes.
     */
    public OtRegistroAgendaValidacionResponse validarRegistroAgenda(LocalDate fecha, Integer idSucursal) {
        LocalDate fechaEvaluada = fecha == null ? LocalDate.now() : fecha;
        reconciliarVentasManualConAgenda(fechaEvaluada, idSucursal);

        List<Map<String, Object>> cierreRows = otRepository.existeCierreAlmacenHoy(fechaEvaluada, idSucursal);
        Integer cierreCodigo = obtenerCodigoResultado(cierreRows);
        LocalDate fechaCierre = obtenerFechaResultado(cierreRows, fechaEvaluada);
        String cierreMensaje = construirMensajeCierreAlmacen(cierreCodigo, fechaCierre, cierreRows);
        boolean cierreBloqueado = cierreCodigo != null && cierreCodigo != 0;

        List<Map<String, Object>> cierrePrPdRows = otRepository.existeCierreAlmacenHoyPrPd(fechaEvaluada, idSucursal);
        Integer cierrePrPdCodigo = obtenerCodigoResultado(cierrePrPdRows);
        LocalDate fechaPrPd = obtenerFechaResultado(cierrePrPdRows, fechaEvaluada);
        String cierrePrPdMensaje = construirMensajeCierrePrPd(cierrePrPdCodigo, fechaPrPd);
        boolean cierrePrPdBloqueado = cierrePrPdCodigo != null && cierrePrPdCodigo != 0;

        if (cierreBloqueado) {
            return new OtRegistroAgendaValidacionResponse(
                    true,
                    "CIERRE_ALMACEN",
                    cierreMensaje,
                    fechaEvaluada,
                    false,
                    true,
                    cierreCodigo,
                    cierreMensaje,
                    cierrePrPdBloqueado,
                    cierrePrPdCodigo,
                    cierrePrPdMensaje,
                    false,
                    null,
                    Collections.emptyList()
            );
        }

        if (cierrePrPdBloqueado) {
            return new OtRegistroAgendaValidacionResponse(
                    true,
                    "CIERRE_ALMACEN_PR_PD",
                    cierrePrPdMensaje,
                    fechaEvaluada,
                    false,
                    false,
                    cierreCodigo,
                    cierreMensaje,
                    true,
                    cierrePrPdCodigo,
                    cierrePrPdMensaje,
                    false,
                    null,
                    Collections.emptyList()
            );
        }

        List<Map<String, Object>> movimientosRows = otRepository.validaMovimientos(fechaEvaluada, idSucursal);
        List<String> movimientosDetalle = construirDetallesMovimientos(movimientosRows);
        boolean movimientosBloqueados = !movimientosDetalle.isEmpty();
        String movimientosMensaje = movimientosBloqueados
                ? "Hay movimientos pendientes y/o registros antes y/o despues : Fecha "
                + fechaEvaluada.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + "\n"
                + String.join("\n", movimientosDetalle)
                : null;

        return new OtRegistroAgendaValidacionResponse(
                movimientosBloqueados,
                movimientosBloqueados ? "MOVIMIENTOS_PENDIENTES" : "OK",
                movimientosBloqueados ? movimientosMensaje : "Validacion ejecutada sin bloqueos.",
                fechaEvaluada,
                true,
                false,
                cierreCodigo,
                cierreMensaje,
                false,
                cierrePrPdCodigo,
                cierrePrPdMensaje,
                movimientosBloqueados,
                movimientosMensaje,
                movimientosDetalle
        );
    }

    private void reconciliarVentasManualConAgenda(LocalDate fecha, Integer idSucursal) {
        if (fecha == null) {
            return;
        }
        try {
            List<Map<String, Object>> agendaRows = listaOtRepository.listarPorFecha(fecha, null, idSucursal);
            if (agendaRows == null || agendaRows.isEmpty()) {
                return;
            }
            Set<String> agendaKeys = new HashSet<>();
            for (Map<String, Object> row : agendaRows) {
                Integer ordenTrabajo = toInteger(findValue(row,
                        "OT", "ot",
                        "OrdenTrabajo", "ordenTrabajo",
                        "NroOT", "nroOT"));
                Integer codigoCliente = toInteger(findValue(row,
                        "CODIGO", "codigo", "Codigo",
                        "CodigoCliente", "codigoCliente",
                        "NumeroCliente", "numeroCliente",
                        "Cliente_Nro", "cliente_nro"));
                if (ordenTrabajo == null || ordenTrabajo <= 0 || codigoCliente == null || codigoCliente <= 0) {
                    continue;
                }
                agendaKeys.add(ordenTrabajo + "|" + codigoCliente);
            }
            if (agendaKeys.isEmpty()) {
                return;
            }
            listaOtRepository.promoverVentasManualAOtWebPorAgenda(
                    fecha,
                    agendaKeys,
                    Collections.emptyList(),
                    null,
                    idSucursal
            );
        } catch (Exception ex) {
            logger.warn(
                    "No se pudo reconciliar ventas Manual->OT_WEB durante validarRegistroAgenda. fecha={}, sucursal={}",
                    fecha,
                    idSucursal,
                    ex
            );
        }
    }

    @Transactional
    public OtRegistrarDetalleAgendaResponse registrarDetalleAgenda(
            OtRegistrarDetalleAgendaRequest request,
            Integer idSucursal) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El cuerpo de la solicitud es requerido.");
        }
        String numeroOrden = request.getNumeroOrden() == null ? "" : request.getNumeroOrden().trim();
        if (numeroOrden.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "numeroOrden es requerido.");
        }
        if (request.getMateriales() == null || request.getMateriales().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe enviar al menos un material.");
        }

        Map<String, Object> venta = obtenerPorNumero(numeroOrden, idSucursal);
        Long idVenta = toLong(findValue(venta, "Id_Venta", "id_venta", "idventa", "id"));
        Integer idRuta = toInteger(findValue(venta, "Id_Ruta", "id_ruta", "idruta"));
        Integer idUsuario = toInteger(findValue(venta, "Id_Usuario", "id_usuario", "idusuario"));
        Integer idVendedor = toInteger(findValue(venta, "Id_Vendedor", "id_vendedor", "idvendedor"));
        Integer codigoCliente = toInteger(findValue(venta, "CodigoCliente", "codigo_cliente", "cliente_nro", "clientenro"));
        Integer ordenTrabajo = toInteger(findValue(venta, "OrdenTrabajo", "orden_trabajo", "ot"));
        LocalDate fechaEjecucion = toLocalDate(findValue(venta, "Fecha_Ejecucion", "fecha_ejecucion", "fecha"));

        if (idVenta == null || idVenta <= 0 || idRuta == null || idRuta <= 0 || idUsuario == null || idUsuario <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "VENTA_INVALIDA", "La venta encontrada no tiene datos suficientes para registrar detalle.");
        }

        LocalDate fechaTrabajo = fechaEjecucion == null ? LocalDate.now() : fechaEjecucion;
        OtRegistroAgendaValidacionResponse bloqueo = validarRegistroAgenda(fechaTrabajo, idSucursal);
        if (bloqueo.isBloqueado()) {
            throw new ApiException(HttpStatus.CONFLICT, "REGISTRO_BLOQUEADO", bloqueo.getMensaje());
        }

        List<Map<String, Object>> cuadreRows = otRepository.validarCuadreRuta(idRuta, fechaTrabajo, idSucursal);
        Boolean existeCuadre = coerceFirstBoolean(cuadreRows);
        if (Boolean.TRUE.equals(existeCuadre)) {
            throw new ApiException(HttpStatus.CONFLICT, "CUADRE_REGISTRADO", "No se puede registrar el detalle porque la ruta ya realizo cuadre.");
        }

        if (codigoCliente != null && ordenTrabajo != null) {
            OtValidarVentaDetalleResponse ventaDetalle = validarVentaYDetalleWb(
                    fechaTrabajo.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    ordenTrabajo,
                    codigoCliente,
                    idSucursal,
                    true
            );
            boolean existeVenta = Boolean.TRUE.equals(ventaDetalle.getExisteVenta())
                    || (ventaDetalle.getCantidadVentas() != null && ventaDetalle.getCantidadVentas() > 0);
            int cantidadDetalles = ventaDetalle.getCantidadDetalles() == null ? 0 : ventaDetalle.getCantidadDetalles();
            if (cantidadDetalles <= 0 && Boolean.TRUE.equals(ventaDetalle.getTieneDetalleEnCodigoVenta())) {
                cantidadDetalles = 1;
            }
            if (!Boolean.TRUE.equals(ventaDetalle.getHabilitarCargarMaterial())) {
                if (cantidadDetalles > 0) {
                    throw new ApiException(HttpStatus.CONFLICT, "DETALLE_YA_REGISTRADO", "La OT ya tiene detalle registrado en codigo venta.");
                }
                // Compatibilidad operativa:
                // si la venta ya existe (OT_WEB o reconciliada desde Manual) y aun no tiene detalle,
                // permitir la carga para evitar bloqueo de cierre por estado.
                if (!existeVenta) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "ESTADO_NO_PERMITE_CARGAR_MATERIAL",
                            "El estado actual de la OT no permite cargar material."
                    );
                }
            }
            if (cantidadDetalles > 0) {
                throw new ApiException(HttpStatus.CONFLICT, "DETALLE_YA_REGISTRADO", "La OT ya tiene detalle registrado en codigo venta.");
            }
        }

        validarMaterialesDetalle(request.getMateriales(), idRuta, idSucursal);

        int inserted = 0;
        List<OtDetalleMaterialRequest> devoluciones = new ArrayList<>();
        for (OtDetalleMaterialRequest material : request.getMateriales()) {
            otRepository.insertarCodigoVenta(
                    idVenta,
                    material.getIdProducto(),
                    material.getIdTipoMaterial(),
                    clean(material.getSerie()),
                    clean(material.getChipId()),
                    material.getCantidad(),
                    idSucursal
            );
            inserted++;
            otRepository.ejecutarRegModProducto(
                    clean(material.getSerie()),
                    clean(material.getChipId()),
                    idRuta,
                    material.getIdProducto(),
                    3,
                    idVenta,
                    idUsuario,
                    material.getIdTipoMaterial(),
                    fechaTrabajo,
                    idSucursal
            );
            if (material.getIdTipoMaterial() != null && (material.getIdTipoMaterial() == 2 || material.getIdTipoMaterial() == 5)) {
                devoluciones.add(material);
            }
        }

        int devolucionCount = 0;
        if (!devoluciones.isEmpty() && idVendedor != null && codigoCliente != null && ordenTrabajo != null) {
            Integer idDevolucion = otRepository.insertarDevolucion(
                    idUsuario,
                    idRuta,
                    idVendedor,
                    ordenTrabajo + " - " + codigoCliente,
                    fechaTrabajo,
                    request.getObservacion(),
                    idVenta,
                    idSucursal
            );
            if (idDevolucion != null) {
                for (OtDetalleMaterialRequest material : devoluciones) {
                    otRepository.insertarDetalleDevolucion(
                            idDevolucion,
                            material.getIdProducto(),
                            material.getIdTipoMaterial(),
                            clean(material.getSerie()),
                            clean(material.getChipId()),
                            material.getCantidad(),
                            material.getEntregado(),
                            idSucursal
                    );
                    otRepository.ejecutarRegModProducto(
                            clean(material.getSerie()),
                            clean(material.getChipId()),
                            idRuta,
                            material.getIdProducto(),
                            Boolean.TRUE.equals(material.getEntregado()) ? 4 : 35,
                            idDevolucion.longValue(),
                            idUsuario,
                            material.getIdTipoMaterial(),
                            fechaTrabajo,
                            idSucursal
                    );
                    devolucionCount++;
                }
            }
        }

        if (request.getIdEstado() != null && request.getIdEstado() > 0) {
            otRepository.modificarOtRealizada(
                    request.getObservacion() == null ? "" : request.getObservacion().trim(),
                    request.getIdEstado(),
                    numeroOrden,
                    idSucursal
            );
        }

        return new OtRegistrarDetalleAgendaResponse(idVenta, ordenTrabajo, inserted, devolucionCount);
    }

    /**
     * Marca anulacion OT + CU como no implementada hasta definir SP final.
     */
    public void anularConCu(Long idVenta, Integer idUsuario) {
        Map<String, Object> details = new HashMap<>();
        details.put("idVenta", idVenta);
        details.put("idUsuario", idUsuario);
        details.put("procedimientosRequeridos",
                java.util.Collections.singletonList("TODO: SP de anulacion OT + CU (rollback estados)."));
        throw new ApiException(
                HttpStatus.NOT_IMPLEMENTED,
                "MISSING_STORED_PROCEDURE",
                "No existe un SP definido para anular OT + CU y revertir estados.",
                details
        );
    }

    /**
     * Filtra listado de OT por pendiente y/o por tecnico autenticado.
     */
    public List<Map<String, Object>> filtrarListado(
            List<Map<String, Object>> rows,
            Integer idUsuario,
            String rol,
            Boolean pendiente,
            String tecnicoNombre,
            Integer idSucursal) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        boolean filtrarPendientes = pendiente == null || pendiente;
        boolean filtrarUsuario = esTecnico(rol) && idUsuario != null;
        String tecnicoNombreNorm = normalizeText(tecnicoNombre);
        Set<Integer> idsVendedorTecnico = Collections.emptySet();
        if (filtrarUsuario) {
            try {
                idsVendedorTecnico = new HashSet<>(otRepository.obtenerIdsVendedorPorIdUsuario(idUsuario, idSucursal));
            } catch (DataAccessException ex) {
                logger.warn("No se pudo resolver ids vendedor para idUsuario={}. Se aplica filtro directo por ids en fila.", idUsuario, ex);
            }
        }

        if (!filtrarPendientes && !filtrarUsuario) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (filtrarPendientes && !esPendiente(row)) {
                continue;
            }
            if (filtrarUsuario && !perteneceUsuario(row, idUsuario, idsVendedorTecnico, tecnicoNombreNorm)) {
                continue;
            }
            result.add(row);
        }

        logger.debug(
                "Filtrado OT: total={}, result={}, pendiente={}, filtrarUsuario={}, idUsuario={}, rol={}, idsVendedor={}",
                rows.size(),
                result.size(),
                filtrarPendientes,
                filtrarUsuario,
                idUsuario,
                rol,
                idsVendedorTecnico
        );
        return result;
    }

    /**
     * Determina si el rol corresponde a un tecnico.
     */
    private boolean esTecnico(String rol) {
        if (rol == null) {
            return false;
        }
        String normalized = normalizeText(rol);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.contains("tecnico") || normalized.equals("tec") || normalized.contains("tech");
    }

    /**
     * Determina si una OT esta pendiente a partir de flags/estado textual.
     */
    private boolean esPendiente(Map<String, Object> row) {
        Object flag = findValue(row,
                "otrealizada", "ot_realizada", "realizada", "realizado",
                "e_realizada", "e_realizado", "otrealizado", "ot_realizado");
        Boolean doneFlag = toBoolean(flag);
        if (doneFlag != null) {
            return !doneFlag;
        }

        Object estadoText = findValue(row,
                "estado", "estado_ot", "estadoot", "estado_orden", "estadoorden",
                "estadotrabajo", "estado_trabajo", "estado_venta", "estadoventa");
        if (estadoText instanceof String) {
            String normalized = normalizeText((String) estadoText);
            String compact = normalized.replace(" ", "");
            if (contieneEstadoFinal(normalized) || contieneEstadoFinal(compact)) {
                return false;
            }
            if (contieneEstadoPendiente(normalized) || contieneEstadoPendiente(compact)) {
                return true;
            }
            return true;
        }

        Object idEstado = findValue(row,
                "id_estado", "idestado", "id_estado_ot", "idestadot",
                "id_estadoorden", "idestadotrabajo");
        if (idEstado != null) {
            logger.debug("Estado numerico sin mapeo, se mantiene como pendiente. keys={}", row.keySet());
            return true;
        }

        return true;
    }

    /**
     * Verifica si la fila de OT pertenece al usuario indicado.
     */
    private boolean perteneceUsuario(
            Map<String, Object> row,
            Integer idUsuario,
            Set<Integer> idsVendedorTecnico,
            String tecnicoNombreNorm) {
        if (coincideConAlguno(row, idUsuario,
                "idusuario", "id_usuario", "iduser", "usuarioid",
                "id_tecnico", "idtecnico", "tecnicoid",
                "id_usuario_asignado", "idasignado", "id_asignado",
                "idpersonal", "id_personal", "idempleado", "id_empleado")) {
            return true;
        }
        if (idsVendedorTecnico == null || idsVendedorTecnico.isEmpty()) {
            return false;
        }
        for (Integer idVendedor : idsVendedorTecnico) {
            if (coincideConAlguno(row, idVendedor, "id_vendedor", "idvendedor", "id_tecnico", "idtecnico")) {
                return true;
            }
        }

        if (tecnicoNombreNorm == null || tecnicoNombreNorm.isEmpty()) {
            return false;
        }

        String tecnicoFilaNorm = normalizeText(asString(findValue(row,
                "tecnico", "Tecnico",
                "tecnico_nombre", "tecnicoNombre", "nombre_tecnico", "NombreTecnico",
                "nombreusuario", "nombre_usuario", "nombreUsuario", "NombreUsuario",
                "vendedor", "Vendedor", "nombrevendedor", "nombre_vendedor", "NombreVendedor",
                "usuario", "Usuario", "nombre", "Nombre"
        )));
        if (tecnicoFilaNorm == null || tecnicoFilaNorm.isEmpty()) {
            return false;
        }
        return tecnicoFilaNorm.equals(tecnicoNombreNorm)
                || tecnicoFilaNorm.contains(tecnicoNombreNorm)
                || tecnicoNombreNorm.contains(tecnicoFilaNorm);
    }

    /**
     * Verifica si alguna columna candidata coincide con el id esperado.
     */
    private boolean coincideConAlguno(Map<String, Object> row, Integer esperado, String... candidates) {
        if (esperado == null) {
            return false;
        }
        for (String candidate : candidates) {
            Integer actual = toInteger(findValue(row, candidate));
            if (actual != null && actual.equals(esperado)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detecta textos de estado finalizado/cerrado.
     */
    private boolean contieneEstadoFinal(String normalized) {
        return contiene(normalized,
                "realizada", "realizado",
                "cerrada", "cerrado",
                "finalizada", "finalizado",
                "anulada", "anulado",
                "cancelada", "cancelado",
                "completada", "completado",
                "entregada", "entregado");
    }

    /**
     * Detecta textos de estado pendiente/en proceso.
     */
    private boolean contieneEstadoPendiente(String normalized) {
        return contiene(normalized,
                "pendiente",
                "abierto",
                "asignado",
                "enproceso",
                "en proceso",
                "programada",
                "programado");
    }

    /**
     * Evalua si el texto contiene alguno de los tokens dados.
     */
    private boolean contiene(String value, String... tokens) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convierte valores dinamicos a boolean soportando formatos numericos/texto.
     */
    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String normalized = normalizeText(value.toString());
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.equals("si") || normalized.equals("true") || normalized.equals("1") || normalized.equals("s")) {
            return true;
        }
        if (normalized.equals("no") || normalized.equals("false") || normalized.equals("0") || normalized.equals("n")) {
            return false;
        }
        return null;
    }

    /**
     * Convierte valor dinamico a Integer de forma segura.
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Busca el primer valor disponible por aliases de columna.
     */
    private Object findValue(Map<String, Object> row, String... candidates) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = normalizeKey(entry.getKey());
            normalized.put(key, entry.getValue());
        }
        for (String candidate : candidates) {
            String key = normalizeKey(candidate);
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
        }
        return null;
    }

    /**
     * Normaliza nombre de columna para comparacion uniforme.
     */
    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza texto (sin tildes, en minusculas) para comparaciones.
     */
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Interpreta resultado de SPs de validacion con heuristica flexible.
     */
    private boolean resultadoValido(List<Map<String, Object>> rows) {
        // TODO: Ajustar esta logica cuando se conozcan columnas exactas de los SPs de validacion.
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        Map<String, Object> first = rows.get(0);
        for (Object value : first.values()) {
            if (value instanceof Boolean) {
                if (((Boolean) value)) {
                    return true;
                }
            }
            if (value instanceof Number) {
                if (((Number) value).intValue() == 1) {
                    return true;
                }
            }
            if (value instanceof String) {
                String normalized = ((String) value).trim().toUpperCase();
                if (normalized.equals("OK") || normalized.equals("SI") || normalized.equals("TRUE") || normalized.equals("1")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fabrica error 404 estandar para entidades OT no encontradas.
     */
    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private void validarRegistroVentaRequest(OtRegistrarVentaRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El cuerpo de la solicitud es requerido.");
        }
        validarMayorCero(request.getIdUsuario(), "idUsuario");
        validarMayorCero(request.getIdGrupo(), "idGrupo");
        validarMayorCero(request.getIdTipoServicio(), "idTipoServicio");
        validarMayorCero(request.getOrdenTrabajo(), "ordenTrabajo");
        validarMayorCero(request.getIdEstado(), "idEstado");
        validarMayorCero(request.getCodigoCliente(), "codigoCliente");
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "nombre es requerido.");
        }
        if (request.getOrigen() == null || request.getOrigen().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "origen es requerido.");
        }
        if (request.getLatitud() != null
                && (request.getLatitud().compareTo(new BigDecimal("-90")) < 0
                || request.getLatitud().compareTo(new BigDecimal("90")) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "latitud fuera de rango (-90 a 90).");
        }
        if (request.getLongitud() != null
                && (request.getLongitud().compareTo(new BigDecimal("-180")) < 0
                || request.getLongitud().compareTo(new BigDecimal("180")) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "longitud fuera de rango (-180 a 180).");
        }
    }

    private Integer resolverIdVendedorRegistroVenta(
            Integer idUsuario,
            Integer idVendedorRequest,
            Integer idSucursal) {
        List<Integer> idsVendedor = otRepository.obtenerIdsVendedorPorIdUsuario(idUsuario, idSucursal);
        if (idsVendedor == null || idsVendedor.isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            details.put("idUsuario", idUsuario);
            details.put("idVendedorRequest", idVendedorRequest);
            details.put("idSucursal", idSucursal);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "No se pudo resolver id_vendedor para el id_usuario en tbl_usuariotecnico.",
                    details
            );
        }
        if (idsVendedor.size() > 1) {
            Map<String, Object> details = new HashMap<>();
            details.put("idUsuario", idUsuario);
            details.put("idsVendedor", idsVendedor);
            details.put("idSucursal", idSucursal);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VALIDATION_ERROR",
                    "El id_usuario tiene mas de un id_vendedor asociado en tbl_usuariotecnico.",
                    details
            );
        }
        return idsVendedor.get(0);
    }

    private void validarMayorCero(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field + " es requerido y debe ser mayor a 0.");
        }
    }

    private ApiException traducirErrorRegistroVenta(DataAccessException ex, OtRegistrarVentaRequest request) {
        Map<String, Object> details = new HashMap<>();
        Throwable root = ex.getMostSpecificCause();
        details.put("storedProcedure", "spx_RegistrarVentaParaRegistroOTwb");
        details.put("rootCause", root == null ? ex.getMessage() : root.getMessage());
        details.put("ordenTrabajo", request.getOrdenTrabajo());
        details.put("codigoCliente", request.getCodigoCliente());

        if (ex instanceof QueryTimeoutException || ex instanceof CannotAcquireLockException) {
            return new ApiException(HttpStatus.GATEWAY_TIMEOUT, "SP_TIMEOUT", "El procedimiento excedio el tiempo de espera.", details);
        }
        if (ex instanceof CannotGetJdbcConnectionException) {
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DB_CONNECTION_ERROR", "No se pudo conectar a la base de datos.", details);
        }

        SQLException sqlEx = findSqlException(ex);
        if (sqlEx != null && sqlEx.getErrorCode() == 2812) {
            return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SP_NOT_FOUND", "No se encontro el procedimiento almacenado spx_RegistrarVentaParaRegistroOTwb.", details);
        }
        if (ex instanceof BadSqlGrammarException) {
            return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SP_SQL_ERROR", "Error SQL al ejecutar el procedimiento almacenado.", details);
        }
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SP_EXECUTION_ERROR", "No se pudo ejecutar el procedimiento almacenado.", details);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void validarMaterialesDetalle(List<OtDetalleMaterialRequest> materiales, Integer idRuta, Integer idSucursal) {
        List<String> repetidos = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        Map<Integer, ProductoDigitos> digitosPorProducto = cargarDigitosPorProducto(materiales, idSucursal);
        for (int i = 0; i < materiales.size(); i++) {
            OtDetalleMaterialRequest material = materiales.get(i);
            if (material.getIdProducto() == null || material.getIdProducto() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cada material debe incluir idProducto valido.");
            }
            if (material.getIdTipoMaterial() == null || material.getIdTipoMaterial() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cada material debe incluir idTipoMaterial valido.");
            }
            if (material.getCantidad() == null || material.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cada material debe incluir cantidad mayor a 0.");
            }
            String serie = clean(material.getSerie());
            String chipId = clean(material.getChipId());
            boolean requiereIdentificacion = material.getRequiereIdentificacion() == null
                    ? true
                    : material.getRequiereIdentificacion();
            ProductoDigitos caracteristicas = digitosPorProducto.get(material.getIdProducto());
            boolean omitirIdentificacion = requiereOmitirIdentificacion(caracteristicas);
            if (requiereIdentificacion && !omitirIdentificacion && serie.isEmpty() && chipId.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cada material debe incluir serie o chipId.");
            }
            if (!serie.isEmpty()) {
                String keySerie = "S|" + serie.toLowerCase(Locale.ROOT);
                if (!seen.add(keySerie)) {
                    repetidos.add("Serie repetida: " + serie);
                }
            }
            if (!chipId.isEmpty()) {
                String keyChip = "C|" + chipId.toLowerCase(Locale.ROOT);
                if (!seen.add(keyChip)) {
                    repetidos.add("ChipId repetido: " + chipId);
                }
            }

            if (!serie.isEmpty() && !chipId.isEmpty()) {
                Map<String, Object> unicidad = otRepository.validarSerieChipIdUnicos(serie, chipId);
                if (!Boolean.TRUE.equals(unicidad.get("sePuede"))) {
                    String observacionUnicidad = asString(unicidad.get("observacion"));
                    boolean serieExiste = Boolean.TRUE.equals(unicidad.get("serieExiste"));
                    boolean chipExiste = Boolean.TRUE.equals(unicidad.get("chipExiste"));
                    boolean puedeRegistrarRetiradoSinSaldo =
                            esMaterialRetirado(material)
                                    && !serieExiste
                                    && !chipExiste
                                    && mensajeSerieInexistente(observacionUnicidad);
                    if (puedeRegistrarRetiradoSinSaldo) {
                        logger.debug(
                                "Material retirado {} se registra aunque serie/chip no existan en saldo: {}",
                                serie,
                                observacionUnicidad
                        );
                    } else {
                        throw new ApiException(
                                HttpStatus.CONFLICT,
                                "SERIE_CHIP_INVALIDOS",
                                observacionUnicidad == null
                                        ? "La serie y el ChipID no corresponden al mismo registro."
                                        : observacionUnicidad
                        );
                    }
                }
            }

            List<Map<String, Object>> estadoRows = otRepository.validarEstadoSerie(
                    serie,
                    chipId,
                    material.getIdProducto(),
                    material.getIdTipoMaterial(),
                    idRuta,
                    idSucursal
            );
            if (isNoSePuedeRegistrar(estadoRows)) {
                String detalle = asString(valueByIndex(estadoRows.get(0), 1));
                if (!permiteRegistrarProductoRetiradoInexistente(material, detalle)) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "ESTADO_SERIE_INVALIDO",
                            (serie.isEmpty() ? chipId : serie) + " - " + (detalle == null ? "NoSePuedeRegistrar" : detalle)
                    );
                }
                logger.debug("El material retirado {} se registra aun cuando no existe en el saldo: {}",
                        serie.isEmpty() ? chipId : serie,
                        detalle);
            }
        }
        if (!repetidos.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "SERIE_REPETIDA", String.join(" | ", repetidos));
        }
    }

    private Map<Integer, ProductoDigitos> cargarDigitosPorProducto(List<OtDetalleMaterialRequest> materiales, Integer idSucursal) {
        Map<Integer, ProductoDigitos> out = new HashMap<>();
        if (materiales == null || materiales.isEmpty()) {
            return out;
        }
        for (OtDetalleMaterialRequest material : materiales) {
            if (material == null) {
                continue;
            }
            Integer idProducto = material.getIdProducto();
            if (idProducto == null || idProducto <= 0 || out.containsKey(idProducto)) {
                continue;
            }
            Map<String, Object> row = otRepository.obtenerDigitosProducto(idProducto, idSucursal);
            if (row == null || row.isEmpty()) {
                logger.debug("No se encontraron digitos para producto {} (sucursal {}).", idProducto, idSucursal);
                out.put(idProducto, null);
                continue;
            }
            ProductoDigitos digitos = new ProductoDigitos(
                    toInteger(findValue(row, "DigitosImei", "digitosimei")),
                    toInteger(findValue(row, "DigitosChipId", "digitoschipid"))
            );
            out.put(idProducto, digitos);
            logger.debug("Producto {} - Digitos Imei={}, ChipId={} (sucursal {}).",
                    idProducto,
                    digitos.digitosImei,
                    digitos.digitosChipId,
                    idSucursal);
        }
        return out;
    }

    private boolean requiereOmitirIdentificacion(ProductoDigitos caracteristicas) {
        return caracteristicas != null && !caracteristicas.requiereIdentificacion();
    }

    private boolean isNoSePuedeRegistrar(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        String result = asString(valueByIndex(rows.get(0), 0));
        return result != null && result.trim().equalsIgnoreCase("NoSePuedeRegistrar");
    }

    private boolean permiteRegistrarProductoRetiradoInexistente(OtDetalleMaterialRequest material, String detalle) {
        if (!esMaterialRetirado(material) || detalle == null) {
            return false;
        }
        return mensajeSerieInexistente(detalle);
    }

    private boolean esMaterialRetirado(OtDetalleMaterialRequest material) {
        if (material == null) {
            return false;
        }
        Integer tipo = material.getIdTipoMaterial();
        return Integer.valueOf(2).equals(tipo) || Integer.valueOf(5).equals(tipo);
    }

    private boolean mensajeSerieInexistente(String detalle) {
        String normalized = detalle.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("no se pudo validar")
                || normalized.contains("no existe")
                || normalized.contains("no se encontro")
                || normalized.contains("no se pudo encontrar")
                || normalized.contains("sin saldo")
                || normalized.contains("no existe saldo");
    }

    private Boolean coerceFirstBoolean(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Map<String, Object> first = rows.get(0);
        for (Object value : first.values()) {
            Boolean parsed = toBoolean(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer obtenerCodigoResultado(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return toInteger(valueByIndex(rows.get(0), 0));
    }

    private LocalDate obtenerFechaResultado(List<Map<String, Object>> rows, LocalDate fallback) {
        if (rows == null || rows.isEmpty()) {
            return fallback;
        }
        LocalDate fecha = toLocalDate(valueByIndex(rows.get(0), 1));
        return fecha == null ? fallback : fecha;
    }

    private Object valueByIndex(Map<String, Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return null;
        }
        int current = 0;
        for (Object value : row.values()) {
            if (current == index) {
                return value;
            }
            current++;
        }
        return null;
    }

    private String construirMensajeCierreAlmacen(
            Integer codigo,
            LocalDate fechaResultado,
            List<Map<String, Object>> rows) {
        if (codigo == null || codigo == 0) {
            return null;
        }
        String mensajeExtra = null;
        if (rows != null && !rows.isEmpty()) {
            mensajeExtra = asString(valueByIndex(rows.get(0), 2));
        }
        if (codigo == -1) {
            String mensaje = "Verificar Fecha Servidor. Fecha_Registro " + fechaResultado;
            if (mensajeExtra != null && !mensajeExtra.trim().isEmpty()) {
                mensaje += "\n" + mensajeExtra.trim();
            }
            return mensaje;
        }
        if (codigo == 1) {
            return "Ya hay un registro de Cierre de Almacen.";
        }
        return "Validacion de cierre de almacen bloqueada.";
    }

    private String construirMensajeCierrePrPd(Integer codigo, LocalDate fechaResultado) {
        if (codigo == null || codigo == 0) {
            return null;
        }
        if (codigo == -1) {
            return "Verificar Fecha Servidor. Fecha_Registro " + fechaResultado;
        }
        if (codigo == 1) {
            return "Ya hay un registro de Cierre de Almacen.";
        }
        return "Validacion de cierre de almacen PR/PD bloqueada.";
    }

    private List<String> construirDetallesMovimientos(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> detalles = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String parte1 = asString(valueByIndex(row, 0));
            String parte2 = asString(valueByIndex(row, 1));
            if (parte1 == null && parte2 == null) {
                continue;
            }
            if (parte2 == null || parte2.trim().isEmpty()) {
                detalles.add(parte1 == null ? "" : parte1.trim());
                continue;
            }
            if (parte1 == null || parte1.trim().isEmpty()) {
                detalles.add(parte2.trim());
                continue;
            }
            detalles.add(parte1.trim() + " - " + parte2.trim());
        }
        return detalles;
    }

    private LocalDate parseFechaFlexible(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "fecha es requerida.");
        }
        String value = fecha.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.BASIC_ISO_DATE
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar con el siguiente formato.
            }
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "fecha invalida. Use yyyy-MM-dd, dd/MM/yyyy o yyyyMMdd."
        );
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.BASIC_ISO_DATE
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar con el siguiente formato.
            }
        }
        return null;
    }

    private void validarCabeceraVentaParams(
            Integer clienteNro,
            Integer ot,
            String tor,
            String grupo,
            String tecnicoNombre) {
        if (clienteNro == null || clienteNro <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "clienteNro es requerido y debe ser mayor a 0.");
        }
        if (ot == null || ot <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ot es requerido y debe ser mayor a 0.");
        }
        if (tor == null || tor.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tor es requerido.");
        }
        if (grupo == null || grupo.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "grupo es requerido.");
        }
        if (tecnicoNombre == null || tecnicoNombre.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tecnicoNombre es requerido.");
        }
    }

    private ApiException traducirErrorCabeceraVenta(
            DataAccessException ex,
            Integer clienteNro,
            Integer ot,
            String tor,
            String grupo,
            String tecnicoNombre) {
        Map<String, Object> details = new HashMap<>();
        Throwable root = ex.getMostSpecificCause();
        String rootMessage = root == null ? ex.getMessage() : root.getMessage();
        details.put("storedProcedure", "spx_ObtenerCaberaVentaParaRegistroOTwb");
        details.put("clienteNro", clienteNro);
        details.put("ot", ot);
        details.put("tor", tor);
        details.put("grupo", grupo);
        details.put("tecnicoNombre", tecnicoNombre);
        details.put("rootCause", rootMessage);

        if (ex instanceof QueryTimeoutException || ex instanceof CannotAcquireLockException) {
            return new ApiException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "SP_TIMEOUT",
                    "El procedimiento excedio el tiempo de espera.",
                    details
            );
        }
        if (ex instanceof CannotGetJdbcConnectionException) {
            return new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "DB_CONNECTION_ERROR",
                    "No se pudo conectar a la base de datos.",
                    details
            );
        }

        SQLException sqlEx = findSqlException(ex);
        if (sqlEx != null && sqlEx.getErrorCode() == 2812) {
            return new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SP_NOT_FOUND",
                    "No se encontro el procedimiento almacenado spx_ObtenerCaberaVentaParaRegistroOTwb.",
                    details
            );
        }
        if (ex instanceof BadSqlGrammarException) {
            return new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SP_SQL_ERROR",
                    "Error SQL al ejecutar el procedimiento almacenado.",
                    details
            );
        }

        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SP_EXECUTION_ERROR",
                "No se pudo ejecutar el procedimiento almacenado.",
                details
        );
    }

    private ApiException traducirErrorValidarVentaDetalle(
            DataAccessException ex,
            String fecha,
            Integer nroOT,
            Integer numeroCliente) {
        Map<String, Object> details = new HashMap<>();
        Throwable root = ex.getMostSpecificCause();
        details.put("storedProcedure", "spx_ValidarVentaYDetallewb");
        details.put("fecha", fecha);
        details.put("nroOT", nroOT);
        details.put("numeroCliente", numeroCliente);
        details.put("rootCause", root == null ? ex.getMessage() : root.getMessage());

        if (ex instanceof QueryTimeoutException || ex instanceof CannotAcquireLockException) {
            return new ApiException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "SP_TIMEOUT",
                    "El procedimiento excedio el tiempo de espera.",
                    details
            );
        }
        if (ex instanceof CannotGetJdbcConnectionException) {
            return new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "DB_CONNECTION_ERROR",
                    "No se pudo conectar a la base de datos.",
                    details
            );
        }

        SQLException sqlEx = findSqlException(ex);
        if (sqlEx != null && sqlEx.getErrorCode() == 2812) {
            return new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SP_NOT_FOUND",
                    "No se encontro el procedimiento almacenado spx_ValidarVentaYDetallewb.",
                    details
            );
        }
        if (ex instanceof BadSqlGrammarException) {
            return new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SP_SQL_ERROR",
                    "Error SQL al ejecutar el procedimiento almacenado.",
                    details
            );
        }
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SP_EXECUTION_ERROR",
                "No se pudo ejecutar el procedimiento almacenado.",
                details
        );
    }

    private SQLException findSqlException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException) {
                return (SQLException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private static final class ProductoDigitos {
        private final Integer digitosImei;
        private final Integer digitosChipId;

        private ProductoDigitos(Integer digitosImei, Integer digitosChipId) {
            this.digitosImei = digitosImei;
            this.digitosChipId = digitosChipId;
        }

        private boolean requiereSerie() {
            return digitosImei != null && digitosImei > 0;
        }

        private boolean requiereChip() {
            return digitosChipId != null && digitosChipId > 0;
        }

        private boolean requiereIdentificacion() {
            return requiereSerie() || requiereChip();
        }
    }

    private List<Map<String, Object>> normalizarCabeceraVentaRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        Map<Integer, String> sucursalesPorId = cargarSucursalesPorId();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            if (row != null) {
                normalized.putAll(row);
            }

            Object idGrupo = findValue(row, "IdGrupo", "idGrupo", "id_grupo");
            Object idRuta = findValue(row, "IdRuta", "idRuta", "id_ruta");
            if (idRuta == null && idGrupo != null) {
                normalized.put("idRuta", idGrupo);
                normalized.put("IdRuta", idGrupo);
            }
            if (idGrupo == null && idRuta != null) {
                normalized.put("idGrupo", idRuta);
                normalized.put("IdGrupo", idRuta);
            }

            Object nombreGrupo = findValue(row, "NombreGrupo", "nombreGrupo", "nombre_grupo");
            Object nombreRuta = findValue(row, "NombreRuta", "nombreRuta", "nombre_ruta");
            if (nombreRuta == null && nombreGrupo != null) {
                normalized.put("NombreRuta", nombreGrupo);
                normalized.put("nombreRuta", nombreGrupo);
            }
            if (nombreGrupo == null && nombreRuta != null) {
                normalized.put("NombreGrupo", nombreRuta);
                normalized.put("nombreGrupo", nombreRuta);
            }

            Integer idSucursal = toInteger(findValue(row,
                    "IdSucursal",
                    "idSucursal",
                    "Id_Sucursal",
                    "id_sucursal",
                    "idsucursal"));
            if (idSucursal != null) {
                normalized.put("idSucursal", idSucursal);
                normalized.put("IdSucursal", idSucursal);
                normalized.put("id_sucursal", idSucursal);
                normalized.put("idsucursal", idSucursal);
            }

            String nombreSucursal = trimToNull(asString(findValue(row,
                    "Sucursal",
                    "sucursal",
                    "NombreSucursal",
                    "nombreSucursal",
                    "nombre_sucursal")));
            if (nombreSucursal == null && idSucursal != null) {
                nombreSucursal = trimToNull(sucursalesPorId.get(idSucursal));
            }
            if (nombreSucursal != null) {
                normalized.put("Sucursal", nombreSucursal);
                normalized.put("sucursal", nombreSucursal);
                normalized.put("NombreSucursal", nombreSucursal);
                normalized.put("nombreSucursal", nombreSucursal);
                normalized.put("nombre_sucursal", nombreSucursal);
            }

            out.add(normalized);
        }
        return out;
    }

    private Map<Integer, String> cargarSucursalesPorId() {
        try {
            List<Map<String, Object>> rows = sucursalRepository.obtenerSucursales();
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Integer, String> out = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Integer id = toInteger(findValue(row, "IdSucursal", "idSucursal", "Id_Sucursal", "id_sucursal", "idsucursal"));
                String nombre = trimToNull(asString(findValue(row, "Sucursal", "sucursal", "NombreSucursal", "nombreSucursal")));
                if (id != null && nombre != null) {
                    out.put(id, nombre);
                }
            }
            return out;
        } catch (Exception ex) {
            logger.warn("No se pudo resolver catalogo de sucursales para normalizar cabecera OT.", ex);
            return Collections.emptyMap();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException construirErrorSaldoRuta(
            Integer idRuta,
            LocalDate fecha,
            Integer idSucursal,
            Exception primaryEx,
            Exception fallbackEx) {
        Map<String, Object> details = new HashMap<>();
        details.put("idRuta", idRuta);
        details.put("fecha", fecha);
        details.put("idSucursal", idSucursal);
        details.put("primaryCause", rootMessage(primaryEx));
        details.put("fallbackCause", rootMessage(fallbackEx));
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SALDO_RUTA_ERROR",
                "No se pudo obtener el saldo de ruta.",
                details
        );
    }

    private String rootMessage(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
