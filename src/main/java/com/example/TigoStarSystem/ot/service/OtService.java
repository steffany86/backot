package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.dto.OtCrearRequest;
import com.example.TigoStarSystem.ot.dto.OtCrearResponse;
import com.example.TigoStarSystem.ot.dto.OtModificarDatosRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaResponse;
import com.example.TigoStarSystem.ot.dto.OtRealizadaRequest;
import com.example.TigoStarSystem.ot.dto.OtValidarVentaDetalleResponse;
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

@Service
public class OtService {
    private static final Logger logger = LoggerFactory.getLogger(OtService.class);
    private final OtRepository otRepository;

    /**
     * Inicializa el servicio principal de Ordenes de Trabajo.
     */
    public OtService(OtRepository otRepository) {
        this.otRepository = otRepository;
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

        try {
            Map<String, Object> result = otRepository.registrarVentaParaRegistroOtWb(
                    request.getIdUsuario(),
                    request.getIdVendedor(),
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
        LocalDate fechaParsed = parseFechaFlexible(fecha);
        validarMayorCero(nroOT, "nroOT");
        validarMayorCero(numeroCliente, "numeroCliente");

        try {
            Map<String, Object> row = otRepository.validarVentaYDetalleWb(
                    fechaParsed,
                    nroOT,
                    numeroCliente,
                    idSucursal
            );
            return new OtValidarVentaDetalleResponse(
                    toLocalDate(findValue(row, "Fecha", "fecha")),
                    toInteger(findValue(row, "NroOT", "nroot")),
                    toInteger(findValue(row, "NumeroCliente", "numerocliente", "codigoCliente")),
                    toBoolean(findValue(row, "ExisteVenta", "existeventa")),
                    toInteger(findValue(row, "CantidadVentas", "cantidadventas")),
                    toBoolean(findValue(row, "TieneDetalleEnCodigoVenta", "tienedetalleencodigoventa")),
                    toInteger(findValue(row, "CantidadDetalles", "cantidaddetalles"))
            );
        } catch (DataAccessException ex) {
            throw traducirErrorValidarVentaDetalle(ex, fecha, nroOT, numeroCliente);
        }
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
            Boolean pendiente) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        boolean filtrarPendientes = pendiente == null || pendiente;
        boolean filtrarUsuario = esTecnico(rol) && idUsuario != null;

        if (!filtrarPendientes && !filtrarUsuario) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (filtrarPendientes && !esPendiente(row)) {
                continue;
            }
            if (filtrarUsuario && !perteneceUsuario(row, idUsuario)) {
                continue;
            }
            result.add(row);
        }

        logger.debug(
                "Filtrado OT: total={}, result={}, pendiente={}, filtrarUsuario={}, idUsuario={}, rol={}",
                rows.size(),
                result.size(),
                filtrarPendientes,
                filtrarUsuario,
                idUsuario,
                rol
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
    private boolean perteneceUsuario(Map<String, Object> row, Integer idUsuario) {
        Object value = findValue(row,
                "idusuario", "id_usuario", "iduser", "usuarioid",
                "id_tecnico", "idtecnico", "tecnicoid",
                "id_vendedor", "idvendedor", "id_usuario_asignado",
                "idasignado", "id_asignado", "idpersonal", "id_personal",
                "idempleado", "id_empleado");
        if (value == null) {
            logger.debug("No se encontro campo de usuario en OT. keys={}", row.keySet());
            return false;
        }
        Integer rowId = toInteger(value);
        if (rowId == null) {
            logger.debug("No se pudo convertir el idUsuario de OT. value={}", value);
            return false;
        }
        return rowId.equals(idUsuario);
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
        validarMayorCero(request.getIdVendedor(), "idVendedor");
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

    private List<Map<String, Object>> normalizarCabeceraVentaRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
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

            out.add(normalized);
        }
        return out;
    }
}
