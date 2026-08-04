package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.catalogo.repository.CatalogoRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.repository.CuadreRepository;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class CuadreService {
    private final CuadreRepository cuadreRepository;
    private final CatalogoRepository catalogoRepository;
    private final OtRepository otRepository;
    private final AuthService authService;

    /**
     * Inicializa el servicio de validacion de cuadre.
     */
    public CuadreService(
            CuadreRepository cuadreRepository,
            CatalogoRepository catalogoRepository,
            OtRepository otRepository,
            AuthService authService
    ) {
        this.cuadreRepository = cuadreRepository;
        this.catalogoRepository = catalogoRepository;
        this.otRepository = otRepository;
        this.authService = authService;
    }

    public Map<String, Object> obtenerCuadreTecnicoActual(String token, LocalDate fecha, Integer idSucursal) {
        AuthLoginResponse tecnico = requireTecnico(token);
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        Integer sucursalFinal = idSucursal != null && idSucursal > 0 ? idSucursal : tecnico.getIdSucursal();

        List<Map<String, Object>> rutas = catalogoRepository.listarRutasPorTecnico(tecnico.getIdUsuario(), sucursalFinal);
        if (rutas == null || rutas.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "RUTA_TECNICO_NO_ENCONTRADA",
                    "No se encontro ruta activa para el tecnico de la sesion."
            );
        }

        boolean cierreAlmacenRegistrado = coerceCount(otRepository.existeCierreAlmacenHoy(fechaFinal, sucursalFinal)) > 0;
        boolean cierrePrPdRegistrado = coerceCount(otRepository.existeCierreAlmacenHoyPrPd(fechaFinal, sucursalFinal)) > 0;
        List<Map<String, Object>> rutasCuadre = new ArrayList<>();
        for (Map<String, Object> ruta : rutas) {
            Integer idRuta = toPositiveInteger(findValue(ruta, "idRuta", "Id_Ruta", "id_ruta", "ruta"));
            if (idRuta == null) {
                continue;
            }
            List<Map<String, Object>> saldoRows = obtenerSaldoRutaConFallback(idRuta, fechaFinal, sucursalFinal);
            List<Map<String, Object>> retiros = obtenerRetirosConFallback(idRuta, sucursalFinal);
            List<Map<String, Object>> detalle = normalizarDetalle(saldoRows, retiros, sucursalFinal);
            Map<String, Object> resumen = resumir(detalle);
            boolean cuadreRegistrado = coerceCount(otRepository.validarCuadreRuta(idRuta, fechaFinal, sucursalFinal)) > 0;
            boolean registroDisponible = !cuadreRegistrado && !cierreAlmacenRegistrado && !cierrePrPdRegistrado;
            String bloqueoRegistro = null;
            if (cuadreRegistrado) {
                bloqueoRegistro = "El cuadre ya fue registrado para esta fecha.";
            } else if (cierreAlmacenRegistrado) {
                bloqueoRegistro = "Ya existe cierre de almacen para esta fecha.";
            } else if (cierrePrPdRegistrado) {
                bloqueoRegistro = "Ya existe cierre PR/PD para esta fecha.";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idRuta", idRuta);
            item.put("ruta", firstNonBlank(
                    valueAsString(findValue(ruta, "nombre", "Nombre", "ruta", "Ruta")),
                    valueAsString(idRuta)
            ));
            item.put("idVendedor", findValue(ruta, "idVendedor", "id_vendedor", "Id_Vendedor", "idTecnico", "id_tecnico"));
            item.put("cuadreRegistrado", cuadreRegistrado);
            item.put("cierreAlmacenRegistrado", cierreAlmacenRegistrado);
            item.put("cierrePrPdRegistrado", cierrePrPdRegistrado);
            item.put("registroDisponible", registroDisponible);
            item.put("bloqueoRegistro", bloqueoRegistro);
            item.put("resumen", resumen);
            item.put("detalle", detalle);
            rutasCuadre.add(item);
        }

        if (rutasCuadre.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "RUTA_TECNICO_NO_ENCONTRADA",
                    "No se pudo resolver una ruta valida para el tecnico de la sesion."
            );
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fecha", fechaFinal.toString());
        out.put("idTecnico", tecnico.getIdUsuario());
        out.put("tecnico", tecnico.getNombre());
        out.put("idSucursal", sucursalFinal);
        out.put("rutas", rutasCuadre);
        out.put("resumen", resumirRutas(rutasCuadre));
        return out;
    }

    public Map<String, Object> registrarCuadreTecnicoActual(
            String token,
            LocalDate fecha,
            Integer idRuta,
            String observacion,
            Integer idSucursal) {
        AuthLoginResponse tecnico = requireTecnico(token);
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        Integer sucursalFinal = idSucursal != null && idSucursal > 0 ? idSucursal : tecnico.getIdSucursal();
        if (idRuta == null || idRuta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idRuta es requerido.");
        }

        Map<String, Object> ruta = resolverRutaTecnico(tecnico, idRuta, sucursalFinal);
        Integer idVendedor = toPositiveInteger(findValue(ruta, "idVendedor", "id_vendedor", "Id_Vendedor", "idTecnico", "id_tecnico"));
        if (idVendedor == null) {
            idVendedor = tecnico.getIdUsuario();
        }

        validarRegistroPermitido(idRuta, fechaFinal, sucursalFinal);

        List<Map<String, Object>> saldoRows = obtenerSaldoRutaConFallback(idRuta, fechaFinal, sucursalFinal);
        List<Map<String, Object>> retiros = obtenerRetirosConFallback(idRuta, sucursalFinal);
        List<Map<String, Object>> detalle = normalizarDetalle(saldoRows, retiros, sucursalFinal);
        validarDetalleCuadre(idRuta, fechaFinal, detalle, sucursalFinal);

        Integer idCuadre = otRepository.registrarCuadreTecnico(
                idRuta,
                idVendedor,
                tecnico.getIdUsuario(),
                fechaFinal,
                observacion == null ? "" : observacion.trim(),
                detalle,
                retiros,
                sucursalFinal
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idCuadre", idCuadre);
        out.put("fecha", fechaFinal.toString());
        out.put("idRuta", idRuta);
        out.put("ruta", firstNonBlank(
                valueAsString(findValue(ruta, "nombre", "Nombre", "ruta", "Ruta")),
                valueAsString(idRuta)
        ));
        out.put("idTecnico", tecnico.getIdUsuario());
        out.put("tecnico", tecnico.getNombre());
        out.put("resumen", resumir(detalle));
        out.put("detalle", detalle);
        return out;
    }

    public Map<String, Object> previewCuadreAutomaticoSistemas(String token, LocalDate fecha, Integer idSucursal) {
        AuthLoginResponse usuario = requireSistemas(token);
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        Integer sucursalFinal = resolverSucursalSistemas(usuario, idSucursal);
        List<Map<String, Object>> rutas = otRepository.obtenerRutasNoCuadradas(fechaFinal, sucursalFinal);
        List<Map<String, Object>> resultados = new ArrayList<>();
        if (rutas != null) {
            for (Map<String, Object> ruta : rutas) {
                resultados.add(crearPreviewRutaAutomatico(ruta, fechaFinal));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fecha", fechaFinal.toString());
        out.put("idSucursal", sucursalFinal);
        out.put("usuario", usuario.getNombre());
        out.put("rutas", resultados);
        out.put("resumen", resumirResultadosAutomaticos(resultados));
        return out;
    }

    public Map<String, Object> ejecutarCuadreAutomaticoSistemas(String token, LocalDate fecha, Integer idSucursal) {
        return ejecutarCuadreAutomaticoSistemas(token, fecha, idSucursal, null);
    }

    public Map<String, Object> ejecutarCuadreAutomaticoSistemas(
            String token,
            LocalDate fecha,
            Integer idSucursal,
            Consumer<Map<String, Object>> progress
    ) {
        publishProgress(progress, "progress", "running", "Validando usuario", "Validando usuario sistemas y parametros de sucursal.", null);
        AuthLoginResponse usuario = requireSistemas(token);
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        Integer sucursalFinal = resolverSucursalSistemas(usuario, idSucursal);
        publishProgress(progress, "progress", "running", "Resolviendo usuario", "Resolviendo usuario que quedara registrado en el cuadre.", null);
        Integer idUsuarioRegistro = resolverUsuarioRegistroAutomatico(usuario, sucursalFinal);

        publishProgress(progress, "progress", "running", "Consultando rutas", "Consultando rutas pendientes de cuadre automatico.", null);
        List<Map<String, Object>> rutas = otRepository.obtenerRutasNoCuadradas(fechaFinal, sucursalFinal);
        List<Map<String, Object>> resultados = new ArrayList<>();
        int totalRutas = rutas == null ? 0 : rutas.size();
        Map<String, Object> rutasExtra = new LinkedHashMap<>();
        rutasExtra.put("totalRutas", totalRutas);
        publishProgress(progress, "progress", "running", "Rutas encontradas", "Se encontraron " + totalRutas + " rutas pendientes.", rutasExtra);
        if (rutas != null) {
            int index = 0;
            for (Map<String, Object> ruta : rutas) {
                index++;
                Map<String, Object> resultado = crearResultadoRutaAutomatico(
                        ruta,
                        fechaFinal,
                        sucursalFinal,
                        true,
                        idUsuarioRegistro,
                        progress,
                        index,
                        totalRutas
                );
                resultados.add(resultado);
            }
        }
        publishProgress(progress, "progress", "running", "Validando cierre", "Verificando si quedaron rutas pendientes antes del cierre automatico.", null);
        List<Map<String, Object>> cierres = ejecutarCierresAutomaticosSiCorresponde(fechaFinal, sucursalFinal, idUsuarioRegistro, progress);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fecha", fechaFinal.toString());
        out.put("idSucursal", sucursalFinal);
        out.put("idUsuarioRegistro", idUsuarioRegistro);
        out.put("rutas", resultados);
        out.put("cierres", cierres);
        out.put("resumen", resumirResultadosAutomaticos(resultados));
        publishProgress(progress, "progress", "success", "Resumen final", "Resumen de cuadre automatico preparado.", out);
        return out;
    }

    /**
     * Ejecuta la validacion de cuadre para una ruta y fecha.
     */
    public List<Map<String, Object>> validarCuadreRuta(Integer idRuta, LocalDate fecha) {
        return cuadreRepository.validarCuadreRuta(idRuta, fecha);
    }

    private Map<String, Object> crearResultadoRutaAutomatico(
            Map<String, Object> ruta,
            LocalDate fecha,
            Integer idSucursal,
            boolean registrar,
            Integer idUsuarioRegistro
    ) {
        return crearResultadoRutaAutomatico(ruta, fecha, idSucursal, registrar, idUsuarioRegistro, null, null, null);
    }

    private Map<String, Object> crearResultadoRutaAutomatico(
            Map<String, Object> ruta,
            LocalDate fecha,
            Integer idSucursal,
            boolean registrar,
            Integer idUsuarioRegistro,
            Consumer<Map<String, Object>> progress,
            Integer rutaIndex,
            Integer totalRutas
    ) {
        Integer idRuta = toPositiveInteger(findValue(ruta, "Id_Ruta", "idRuta", "id_ruta"));
        Integer idVendedor = toPositiveInteger(findValue(ruta, "Id_Vendedor", "idVendedor", "id_vendedor"));
        String nombreRuta = firstNonBlank(valueAsString(findValue(ruta, "NombreRuta", "ruta", "Ruta", "nombre")), valueAsString(idRuta));
        String nombreVendedor = valueAsString(findValue(ruta, "NombreVendedor", "vendedor", "Vendedor"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idRuta", idRuta);
        out.put("ruta", nombreRuta);
        out.put("idVendedor", idVendedor);
        out.put("vendedor", nombreVendedor);
        out.put("fecha", fecha.toString());
        out.put("estado", registrar ? "Pendiente" : "Preview");
        out.put("mensaje", "");
        out.put("idCuadre", null);

        try {
            publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Validando ruta", "Validando datos base de la ruta.");
            if (idRuta == null || idRuta <= 0) {
                throw new ApiException(HttpStatus.CONFLICT, "RUTA_INVALIDA", "Error: Ruta Almacen.");
            }
            if (idVendedor == null || idVendedor <= 0) {
                throw new ApiException(HttpStatus.CONFLICT, "VENDEDOR_INVALIDO", "No se pudo resolver vendedor de la ruta.");
            }
            publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Revisando bloqueos", "Validando cuadre previo, cierres y movimientos pendientes.");
            validarRegistroPermitido(idRuta, fecha, idSucursal);
            publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Calculando saldo", "Consultando saldo de ruta y ventas usadas durante el dia.");
            publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Consultando retiros", "Consultando productos retirados o no entregados de la ruta.");
            List<Map<String, Object>> retiros = obtenerRetirosConFallback(idRuta, idSucursal);
            List<Map<String, Object>> detalle = prepararDetalleCuadreAutomaticoLegacy(idRuta, fecha, retiros, idSucursal);
            int cantidadOt = coerceCount(otRepository.obtenerCantidadOtDiaRuta(idRuta, fecha, idSucursal));
            publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Validando detalle", "Validando sobrantes, saldos y ventas registradas.");
            validarDetalleCuadre(idRuta, fecha, detalle, idSucursal);
            out.put("cantidadOt", cantidadOt);
            out.put("resumen", resumir(detalle));
            if (registrar) {
                publishRutaProgress(progress, rutaIndex, totalRutas, idRuta, nombreRuta, "Registrando cuadre", "Registrando cabecera, detalle, retiros y actualizacion de saldo.");
                List<Map<String, Object>> detalleRegistro = detalleRegistroAutomaticoLegacy(detalle);
                Integer idCuadre = otRepository.registrarCuadreTecnico(
                        idRuta,
                        idVendedor,
                        idUsuarioRegistro,
                        fecha,
                        "Cuadre automatico",
                        detalleRegistro,
                        retiros,
                        idSucursal
                );
                out.put("idCuadre", idCuadre);
                out.put("estado", "Registrado");
                out.put("mensaje", "Cuadre registrado correctamente.");
                Map<String, Object> extra = rutaProgressExtra(rutaIndex, totalRutas, idRuta, nombreRuta);
                extra.put("resultadoRuta", out);
                publishProgress(progress, "route", "success", "Ruta registrada", "Ruta " + nombreRuta + " registrada correctamente.", extra);
            } else {
                out.put("estado", "Listo");
                out.put("mensaje", "Ruta lista para cuadre automatico.");
            }
        } catch (ApiException ex) {
            out.put("estado", "Omitido");
            out.put("mensaje", ex.getMessage());
            out.put("codigo", ex.getCode());
            Map<String, Object> extra = rutaProgressExtra(rutaIndex, totalRutas, idRuta, nombreRuta);
            extra.put("codigo", ex.getCode());
            extra.put("resultadoRuta", out);
            publishProgress(progress, "route", "warning", "Ruta omitida", "Ruta " + nombreRuta + " omitida: " + ex.getMessage(), extra);
        } catch (Exception ex) {
            out.put("estado", "Error");
            out.put("mensaje", ex.getMessage());
            Map<String, Object> extra = rutaProgressExtra(rutaIndex, totalRutas, idRuta, nombreRuta);
            extra.put("resultadoRuta", out);
            publishProgress(progress, "route", "error", "Error en ruta", "Ruta " + nombreRuta + " con error: " + ex.getMessage(), extra);
        }
        return out;
    }

    private Map<String, Object> crearPreviewRutaAutomatico(Map<String, Object> ruta, LocalDate fecha) {
        Integer idRuta = toPositiveInteger(findValue(ruta, "Id_Ruta", "idRuta", "id_ruta"));
        Integer idVendedor = toPositiveInteger(findValue(ruta, "Id_Vendedor", "idVendedor", "id_vendedor"));
        String nombreRuta = firstNonBlank(valueAsString(findValue(ruta, "NombreRuta", "ruta", "Ruta", "nombre")), valueAsString(idRuta));
        String nombreVendedor = valueAsString(findValue(ruta, "NombreVendedor", "vendedor", "Vendedor"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idRuta", idRuta);
        out.put("ruta", nombreRuta);
        out.put("idVendedor", idVendedor);
        out.put("vendedor", nombreVendedor);
        out.put("fecha", fecha.toString());
        out.put("estado", "Pendiente");
        out.put("mensaje", "Pendiente de ejecutar validaciones.");
        out.put("idCuadre", null);
        out.put("cantidadOt", null);
        return out;
    }

    private Map<String, Object> resumirResultadosAutomaticos(List<Map<String, Object>> resultados) {
        int registrados = 0;
        int listos = 0;
        int omitidos = 0;
        int errores = 0;
        for (Map<String, Object> row : resultados) {
            String estado = normalize(valueAsString(row.get("estado")));
            if ("registrado".equals(estado)) {
                registrados++;
            } else if ("listo".equals(estado)) {
                listos++;
            } else if ("omitido".equals(estado)) {
                omitidos++;
            } else if ("error".equals(estado)) {
                errores++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", resultados.size());
        out.put("registrados", registrados);
        out.put("listos", listos);
        out.put("omitidos", omitidos);
        out.put("errores", errores);
        return out;
    }

    private void publishRutaProgress(
            Consumer<Map<String, Object>> progress,
            Integer rutaIndex,
            Integer totalRutas,
            Integer idRuta,
            String ruta,
            String step,
            String message
    ) {
        Map<String, Object> extra = rutaProgressExtra(rutaIndex, totalRutas, idRuta, ruta);
        publishProgress(progress, "route", "running", step, message, extra);
    }

    private Map<String, Object> rutaProgressExtra(Integer rutaIndex, Integer totalRutas, Integer idRuta, String ruta) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("rutaIndex", rutaIndex);
        extra.put("totalRutas", totalRutas);
        extra.put("idRuta", idRuta);
        extra.put("ruta", ruta);
        return extra;
    }

    private void publishProgress(
            Consumer<Map<String, Object>> progress,
            String type,
            String status,
            String step,
            String message,
            Map<String, Object> extra
    ) {
        if (progress == null) {
            return;
        }
        progress.accept(CuadreAutomaticoJobService.event(type, status, step, message, extra));
    }

    private AuthLoginResponse requireSistemas(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        String rol = normalize(usuario == null ? null : usuario.getRol());
        String login = normalize(usuario == null ? null : usuario.getLoggin());
        if (!"sistemas".equals(rol) && !"sistemas".equals(login)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_SISTEMAS_ONLY", "Solo sistemas puede ejecutar cuadre automatico.");
        }
        return usuario;
    }

    private Integer resolverSucursalSistemas(AuthLoginResponse usuario, Integer idSucursal) {
        if (idSucursal != null && idSucursal > 0) {
            return idSucursal;
        }
        if (usuario != null && usuario.getIdSucursal() != null && usuario.getIdSucursal() > 0) {
            return usuario.getIdSucursal();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSucursal es requerido.");
    }

    private Integer resolverUsuarioRegistroAutomatico(AuthLoginResponse usuario, Integer idSucursal) {
        if (usuario != null && usuario.getIdUsuario() != null && usuario.getIdUsuario() > 0) {
            return usuario.getIdUsuario();
        }
        Integer sistemas = otRepository.obtenerIdUsuarioPorLogin("sistemas", idSucursal);
        if (sistemas != null && sistemas > 0) {
            return sistemas;
        }
        Integer stefany = otRepository.obtenerIdUsuarioPorLogin("stefany", idSucursal);
        if (stefany != null && stefany > 0) {
            return stefany;
        }
        throw new ApiException(
                HttpStatus.CONFLICT,
                "USUARIO_REGISTRO_NO_ENCONTRADO",
                "No se encontro usuario de BD para registrar el cuadre automatico."
        );
    }

    private List<Map<String, Object>> obtenerSaldoRutaConFallback(Integer idRuta, LocalDate fecha, Integer idSucursal) {
        try {
            return otRepository.obtenerSaldoRuta(idRuta, fecha, idSucursal);
        } catch (DataAccessException ex) {
            return otRepository.obtenerSaldoRutaBasico(idRuta, idSucursal);
        }
    }

    private List<Map<String, Object>> obtenerRetirosConFallback(Integer idRuta, Integer idSucursal) {
        try {
            return otRepository.obtenerProductosNoEntregadosRuta(idRuta, idSucursal);
        } catch (RuntimeException ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> prepararDetalleCuadreAutomaticoLegacy(
            Integer idRuta,
            LocalDate fecha,
            List<Map<String, Object>> retiros,
            Integer idSucursal
    ) {
        List<Map<String, Object>> saldos = otRepository.obtenerSaldoTarjetasRutaLegacy(idRuta, idSucursal);
        if (saldos == null || saldos.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "CUADRE_SIN_PEDIDO", "Registre un Pedido.");
        }

        Map<Integer, Double> retiradoPorProducto = agruparRetirosPorProducto(retiros);
        Map<Integer, Map<String, Object>> productos = resolverProductosCuadre(saldos, idSucursal);
        List<Map<String, Object>> detalle = new ArrayList<>();
        for (Map<String, Object> saldoRow : saldos) {
            Integer idProducto = toPositiveInteger(findValue(saldoRow, "Id_Producto", "idProducto", "id_producto"));
            if (idProducto == null) {
                continue;
            }
            Map<String, Object> producto = productos.get(idProducto);
            if (producto == null) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "PRODUCTO_NO_ENCONTRADO",
                        "No se encontro el producto " + idProducto + "."
                );
            }
            double saldo = toDouble(findValue(saldoRow, "Cantidad", "cantidad", "saldo", "Saldo"));
            double precio = toDouble(findValue(producto, "precio", "Precio", "PrecioVenta", "precioVenta"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idProducto", idProducto);
            item.put("producto", firstNonBlank(valueAsString(findValue(producto, "producto", "Producto", "Nombre", "nombre")), "Producto " + idProducto));
            item.put("saldo", saldo);
            item.put("vendido", 0d);
            item.put("retirado", retiradoPorProducto.getOrDefault(idProducto, 0d));
            item.put("sobrante", saldo);
            item.put("precio", precio);
            item.put("totalVendido", 0d);
            detalle.add(item);
        }

        aplicarVentasCuadreAutomaticoLegacy(detalle, idRuta, fecha, idSucursal);
        return detalle;
    }

    private void aplicarVentasCuadreAutomaticoLegacy(
            List<Map<String, Object>> detalle,
            Integer idRuta,
            LocalDate fecha,
            Integer idSucursal
    ) {
        List<Map<String, Object>> ventas = otRepository.obtenerVentaDiaRuta(idRuta, fecha, idSucursal);
        if (ventas == null || ventas.isEmpty()) {
            return;
        }
        for (Map<String, Object> venta : ventas) {
            Integer idProducto = toPositiveInteger(findValue(venta, "Id_Producto", "idProducto", "id_producto"));
            if (idProducto == null) {
                continue;
            }
            Map<String, Object> item = buscarDetalleProducto(detalle, idProducto);
            if (item == null) {
                String nombre = firstNonBlank(valueAsString(findValue(venta, "Nombre", "nombre", "Producto", "producto")), "Producto " + idProducto);
                throw new ApiException(HttpStatus.CONFLICT, "CUADRE_SIN_SALDO_PRODUCTO", "No tiene saldo de : " + nombre);
            }
            double vendido = toDouble(findValue(venta, "Venta", "venta", "Vendido", "vendido"));
            double sobrante = toDouble(item.get("sobrante")) - vendido;
            double precio = toDouble(item.get("precio"));
            item.put("vendido", vendido);
            item.put("sobrante", sobrante);
            item.put("totalVendido", vendido * precio);
        }
    }

    private List<Map<String, Object>> detalleRegistroAutomaticoLegacy(List<Map<String, Object>> detalle) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (detalle == null) {
            return out;
        }
        for (Map<String, Object> item : detalle) {
            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("precio", 0d);
            row.put("totalVendido", 0d);
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> ejecutarCierresAutomaticosSiCorresponde(
            LocalDate fecha,
            Integer idSucursal,
            Integer idUsuarioRegistro,
            Consumer<Map<String, Object>> progress
    ) {
        List<Map<String, Object>> cierres = new ArrayList<>();
        List<Map<String, Object>> pendientes = otRepository.obtenerRutasNoCuadradas(fecha, idSucursal);
        if (pendientes != null && !pendientes.isEmpty()) {
            Map<String, Object> omitido = crearResultadoCierre("Cierre Automatico", fecha);
            omitido.put("estado", "Omitido");
            omitido.put("mensaje", "No se registro cierre automatico porque aun existen " + pendientes.size() + " ruta(s) pendiente(s) de cuadre.");
            cierres.add(omitido);
            publishProgress(progress, "route", "warning", "Cierre omitido", String.valueOf(omitido.get("mensaje")), omitido);
            return cierres;
        }

        cierres.add(ejecutarCierreAlmacenAutomatico(fecha, idSucursal, idUsuarioRegistro, progress));
        cierres.add(ejecutarCierreAlmacenPrPdAutomatico(fecha, idSucursal, idUsuarioRegistro, progress));
        return cierres;
    }

    private Map<String, Object> ejecutarCierreAlmacenAutomatico(
            LocalDate fecha,
            Integer idSucursal,
            Integer idUsuarioRegistro,
            Consumer<Map<String, Object>> progress
    ) {
        Map<String, Object> out = crearResultadoCierre("Cierre Almacen", fecha);
        try {
            publishProgress(progress, "route", "running", "Cierre Almacen", "Validando cierre de almacen.", out);
            String mensaje = validarCierreAlmacen(fecha, idSucursal);
            if (!mensaje.isEmpty()) {
                out.put("estado", "Omitido");
                out.put("mensaje", mensaje);
                publishProgress(progress, "route", "warning", "Cierre Almacen omitido", mensaje, out);
                return out;
            }
            List<Map<String, Object>> detalle = otRepository.obtenerDetalleCierreAlmacen(fecha, idSucursal);
            mensaje = validarDetalleCierre(detalle);
            if (!mensaje.isEmpty()) {
                out.put("estado", "Omitido");
                out.put("mensaje", mensaje);
                publishProgress(progress, "route", "warning", "Cierre Almacen omitido", mensaje, out);
                return out;
            }
            Integer idCierre = otRepository.registrarCierreAlmacen(fecha, idUsuarioRegistro, detalle, idSucursal);
            out.put("idRegistro", idCierre);
            out.put("estado", "Registrado");
            out.put("mensaje", "Cierre de almacen registrado correctamente.");
            publishProgress(progress, "route", "success", "Cierre Almacen registrado", String.valueOf(out.get("mensaje")), out);
        } catch (Exception ex) {
            out.put("estado", "Error");
            out.put("mensaje", ex.getMessage());
            publishProgress(progress, "route", "error", "Error Cierre Almacen", ex.getMessage(), out);
        }
        return out;
    }

    private Map<String, Object> ejecutarCierreAlmacenPrPdAutomatico(
            LocalDate fecha,
            Integer idSucursal,
            Integer idUsuarioRegistro,
            Consumer<Map<String, Object>> progress
    ) {
        Map<String, Object> out = crearResultadoCierre("Cierre Almacen PR_PD", fecha);
        try {
            publishProgress(progress, "route", "running", "Cierre Almacen PR_PD", "Validando cierre de almacen PR_PD.", out);
            String mensaje = validarCierreAlmacenPrPd(fecha, idSucursal);
            if (!mensaje.isEmpty()) {
                out.put("estado", "Omitido");
                out.put("mensaje", mensaje);
                publishProgress(progress, "route", "warning", "Cierre PR_PD omitido", mensaje, out);
                return out;
            }
            List<Map<String, Object>> detalle = otRepository.obtenerDetalleCierreAlmacenPrPd(fecha, idSucursal);
            mensaje = validarDetalleCierre(detalle);
            if (!mensaje.isEmpty()) {
                out.put("estado", "Omitido");
                out.put("mensaje", mensaje);
                publishProgress(progress, "route", "warning", "Cierre PR_PD omitido", mensaje, out);
                return out;
            }
            Integer idCierre = otRepository.registrarCierreAlmacenPrPd(fecha, idUsuarioRegistro, detalle, idSucursal);
            out.put("idRegistro", idCierre);
            out.put("estado", "Registrado");
            out.put("mensaje", "Cierre de almacen PR_PD registrado correctamente.");
            publishProgress(progress, "route", "success", "Cierre PR_PD registrado", String.valueOf(out.get("mensaje")), out);
        } catch (Exception ex) {
            out.put("estado", "Error");
            out.put("mensaje", ex.getMessage());
            publishProgress(progress, "route", "error", "Error Cierre PR_PD", ex.getMessage(), out);
        }
        return out;
    }

    private Map<String, Object> crearResultadoCierre(String tipo, LocalDate fecha) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tipo", tipo);
        out.put("fecha", fecha.toString());
        out.put("estado", "Pendiente");
        out.put("idRegistro", 0);
        out.put("mensaje", "");
        return out;
    }

    private String validarCierreAlmacen(LocalDate fecha, Integer idSucursal) {
        StringBuilder mensaje = new StringBuilder();
        if (coerceCount(otRepository.existeCierreAlmacenHoy(fecha, idSucursal)) > 0) {
            mensaje.append("No se puede registrar cierre de almacen. Ya existe cierre para la fecha.").append('\n');
        }
        appendSiNoSePuede(mensaje, "No se puede registrar cierre de almacen. ", otRepository.sePuedeHacerCierreAlmacen(fecha, idSucursal));
        appendMovimientos(mensaje, "Transacciones pendientes. ", otRepository.validaMovimientos(fecha, idSucursal));
        appendMovimientos(mensaje, "Transacciones pendientes. ", otRepository.validaMovimientosSCierre(fecha, idSucursal));
        appendRutasPendientesCierre(mensaje, fecha, idSucursal);
        return mensaje.toString().trim();
    }

    private String validarCierreAlmacenPrPd(LocalDate fecha, Integer idSucursal) {
        StringBuilder mensaje = new StringBuilder();
        appendSiNoSePuede(mensaje,
                "No se puede registrar cierre de almacen PR_PD. Se tiene que registrar primero el cierre de Almacen Productos Nuevos. ",
                otRepository.sePuedeHacerCierreAlmacenPrPd(fecha, idSucursal));
        appendMovimientos(mensaje, "Transacciones pendientes. ", otRepository.validaMovimientosSCierre(fecha, idSucursal));
        appendRutasPendientesCierre(mensaje, fecha, idSucursal);
        if (coerceCount(otRepository.existeCierreAlmacenHoyPrPd(fecha, idSucursal)) > 0) {
            mensaje.append("No se puede registrar el cierre PR_PD verificar.").append('\n');
        }
        return mensaje.toString().trim();
    }

    private void appendSiNoSePuede(StringBuilder mensaje, String prefijo, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean bloqueado = false;
        StringBuilder detalle = new StringBuilder();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            for (Object value : row.values()) {
                String text = valueAsString(value);
                if (text == null) {
                    continue;
                }
                if ("nosepuede".equals(normalize(text).replace(" ", ""))) {
                    bloqueado = true;
                } else {
                    if (detalle.length() > 0) {
                        detalle.append(" | ");
                    }
                    detalle.append(text);
                }
            }
        }
        if (bloqueado) {
            mensaje.append(prefijo);
            if (detalle.length() > 0) {
                mensaje.append(detalle);
            }
            mensaje.append('\n');
        }
    }

    private void appendMovimientos(StringBuilder mensaje, String prefijo, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if (rows.size() == 1 && coerceCount(rows) <= 0) {
            return;
        }
        mensaje.append(prefijo);
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            StringBuilder detalle = new StringBuilder();
            for (Object value : row.values()) {
                String text = valueAsString(value);
                if (text == null) {
                    continue;
                }
                if (detalle.length() > 0) {
                    detalle.append(" | ");
                }
                detalle.append(text);
            }
            if (detalle.length() > 0) {
                mensaje.append(detalle).append("; ");
            }
        }
        mensaje.append('\n');
    }

    private void appendRutasPendientesCierre(StringBuilder mensaje, LocalDate fecha, Integer idSucursal) {
        List<Map<String, Object>> pendientes = otRepository.rutasPendientesCuadreCierre(fecha, idSucursal);
        if (pendientes != null && !pendientes.isEmpty()) {
            mensaje.append("Falta que registren cuadre ").append(pendientes.size()).append(" ruta(s).").append('\n');
        }
    }

    private String validarDetalleCierre(List<Map<String, Object>> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return "Detalle: No existe ningun dato.";
        }
        for (Map<String, Object> row : detalle) {
            if (toPositiveInteger(valueAt(row, 0)) == null) {
                return "Detalle: Existe producto invalido para cierre.";
            }
            int size = row == null ? 0 : row.size();
            if (size >= 2 && (toDouble(valueAt(row, size - 1)) < 0 || toDouble(valueAt(row, size - 2)) < 0)) {
                return "Detalle: No puede existir saldo Negativo.";
            }
        }
        return "";
    }

    private Object valueAt(Map<String, Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return null;
        }
        int i = 0;
        for (Object value : row.values()) {
            if (i == index) {
                return value;
            }
            i++;
        }
        return null;
    }

    private Map<String, Object> buscarDetalleProducto(List<Map<String, Object>> detalle, Integer idProducto) {
        if (detalle == null || idProducto == null) {
            return null;
        }
        for (Map<String, Object> item : detalle) {
            Integer current = toPositiveInteger(item.get("idProducto"));
            if (idProducto.equals(current)) {
                return item;
            }
        }
        return null;
    }

    private Map<Integer, Map<String, Object>> resolverProductosCuadre(List<Map<String, Object>> rows, Integer idSucursal) {
        Set<Integer> idsProducto = new LinkedHashSet<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Integer idProducto = toPositiveInteger(findValue(row, "idProducto", "Id_Producto", "id_producto"));
                if (idProducto != null) {
                    idsProducto.add(idProducto);
                }
            }
        }
        if (idsProducto.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return otRepository.obtenerProductosCuadre(new ArrayList<>(idsProducto), idSucursal);
    }

    private List<Map<String, Object>> normalizarDetalle(List<Map<String, Object>> rows, List<Map<String, Object>> retiros, Integer idSucursal) {
        Map<Integer, Double> retiradoPorProducto = agruparRetirosPorProducto(retiros);
        Map<Integer, String> nombresProducto = resolverNombresProducto(rows, idSucursal);
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Integer idProducto = toPositiveInteger(findValue(row, "idProducto", "Id_Producto", "id_producto"));
            if (idProducto != null && nombresProducto != null && !nombresProducto.containsKey(idProducto)) {
                continue;
            }
            String productoCatalogo = idProducto == null || nombresProducto == null ? null : nombresProducto.get(idProducto);
            String productoRow = valueAsString(findValue(row, "producto", "Producto", "nombre", "Nombre", "nombreProducto", "NombreProducto", "Nombre_Producto"));
            if (esNombreProductoGenerico(productoRow, idProducto) && productoCatalogo != null) {
                productoRow = null;
            }
            String producto = firstNonBlank(
                    productoRow,
                    firstNonBlank(productoCatalogo, idProducto == null ? "Producto" : "Producto " + idProducto)
            );
            double saldo = toDouble(findValue(row, "saldo", "Saldo", "SaldoDia", "SaldoDiaHoy", "Cantidad", "cantidad", "existencia", "Disponible"));
            double vendido = toDouble(findValue(
                    row,
                    "venta", "Venta", "Vendido", "ItemsVendidos", "itemsVendidos",
                    "UsadoHoy", "usadoHoy", "SaldoUsadoHoy", "saldoUsadoHoy"
            ));
            double retirado = retiradoPorProducto.containsKey(idProducto)
                    ? retiradoPorProducto.get(idProducto)
                    : toDouble(findValue(row, "retirado", "Retirado", "ItemsRetirados", "itemsRetirados", "NoEntregado", "noEntregado"));
            Object sobranteRaw = findValue(row, "sobrante", "Sobrante", "ItemsSobrantes", "itemsSobrantes");
            double sobrante = sobranteRaw == null ? saldo - vendido : toDouble(sobranteRaw);
            double precio = toDouble(findValue(row, "precio", "Precio", "PrecioVenta", "precioVenta"));
            double totalVendido = toDouble(findValue(row, "totalVendido", "TotalVendido", "TotalVendidos", "totalVendidos"));
            if (totalVendido == 0 && precio != 0 && vendido != 0) {
                totalVendido = precio * vendido;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idProducto", idProducto);
            item.put("producto", producto);
            item.put("saldo", saldo);
            item.put("vendido", vendido);
            item.put("retirado", retirado);
            item.put("sobrante", sobrante);
            item.put("precio", precio);
            item.put("totalVendido", totalVendido);
            out.add(item);
        }
        return out;
    }

    private Map<Integer, String> resolverNombresProducto(List<Map<String, Object>> rows, Integer idSucursal) {
        Set<Integer> idsProducto = new LinkedHashSet<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Integer idProducto = toPositiveInteger(findValue(row, "idProducto", "Id_Producto", "id_producto"));
                if (idProducto != null) {
                    idsProducto.add(idProducto);
                }
            }
        }
        if (idsProducto.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return otRepository.obtenerNombresProducto(new ArrayList<>(idsProducto), idSucursal);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean esNombreProductoGenerico(String producto, Integer idProducto) {
        if (producto == null || producto.trim().isEmpty()) {
            return false;
        }
        String normalizado = normalize(producto).replace(" ", "");
        if ("producto".equals(normalizado)) {
            return true;
        }
        return idProducto != null && normalizado.equals("producto" + idProducto);
    }

    private Map<String, Object> resumir(List<Map<String, Object>> detalle) {
        double saldo = 0;
        double vendido = 0;
        double retirado = 0;
        double sobrante = 0;
        double totalVendido = 0;
        for (Map<String, Object> item : detalle) {
            saldo += toDouble(item.get("saldo"));
            vendido += toDouble(item.get("vendido"));
            retirado += toDouble(item.get("retirado"));
            sobrante += toDouble(item.get("sobrante"));
            totalVendido += toDouble(item.get("totalVendido"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", detalle.size());
        out.put("saldo", saldo);
        out.put("vendido", vendido);
        out.put("retirado", retirado);
        out.put("sobrante", sobrante);
        out.put("totalVendido", totalVendido);
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resumirRutas(List<Map<String, Object>> rutas) {
        List<Map<String, Object>> acumulado = new ArrayList<>();
        for (Map<String, Object> ruta : rutas) {
            Object detalle = ruta.get("detalle");
            if (detalle instanceof List) {
                acumulado.addAll((List<Map<String, Object>>) detalle);
            }
        }
        Map<String, Object> out = resumir(acumulado);
        out.put("rutas", rutas.size());
        return out;
    }

    private Map<String, Object> resolverRutaTecnico(AuthLoginResponse tecnico, Integer idRuta, Integer idSucursal) {
        List<Map<String, Object>> rutas = catalogoRepository.listarRutasPorTecnico(tecnico.getIdUsuario(), idSucursal);
        if (rutas == null || rutas.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "RUTA_TECNICO_NO_ENCONTRADA",
                    "No se encontro ruta activa para el tecnico de la sesion."
            );
        }
        for (Map<String, Object> ruta : rutas) {
            Integer current = toPositiveInteger(findValue(ruta, "idRuta", "Id_Ruta", "id_ruta", "ruta"));
            if (idRuta.equals(current)) {
                return ruta;
            }
        }
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "RUTA_NO_ASIGNADA_TECNICO",
                "La ruta seleccionada no pertenece al tecnico de la sesion."
        );
    }

    private void validarRegistroPermitido(Integer idRuta, LocalDate fecha, Integer idSucursal) {
        if (coerceCount(otRepository.validarCuadreRuta(idRuta, fecha, idSucursal)) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CUADRE_YA_REGISTRADO",
                    "El grupo ya realizo cuadre para la fecha seleccionada."
            );
        }
        if (coerceCount(otRepository.existeCierreAlmacenHoy(fecha, idSucursal)) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CIERRE_ALMACEN_EXISTE",
                    "No se puede registrar el cuadre porque existe cierre de almacen para la fecha."
            );
        }
        if (coerceCount(otRepository.existeCierreAlmacenHoyPrPd(fecha, idSucursal)) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CIERRE_ALMACEN_PRPD_EXISTE",
                    "No se puede registrar el cuadre porque existe cierre PR/PD para la fecha."
            );
        }
        List<Map<String, Object>> movimientos = otRepository.validaMovimientos(fecha, idSucursal);
        if (movimientos != null && !movimientos.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MOVIMIENTOS_CIERRE_PENDIENTES",
                    "Hay movimientos pendientes antes/despues del ultimo cierre. Verifique antes de registrar cuadre.",
                    java.util.Collections.singletonMap("movimientos", movimientos)
            );
        }
    }

    private void validarDetalleCuadre(Integer idRuta, LocalDate fecha, List<Map<String, Object>> detalle, Integer idSucursal) {
        if (detalle == null || detalle.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "CUADRE_SIN_DETALLE", "No existe detalle de saldo para registrar cuadre.");
        }
        Map<Integer, Double> ventas = agruparVentasPorProducto(otRepository.obtenerVentaDiaRuta(idRuta, fecha, idSucursal));
        for (Map<String, Object> item : detalle) {
            Integer idProducto = toPositiveInteger(item.get("idProducto"));
            double saldo = toDouble(item.get("saldo"));
            double vendido = toDouble(item.get("vendido"));
            double retirado = toDouble(item.get("retirado"));
            double sobrante = toDouble(item.get("sobrante"));
            if (sobrante < 0) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "SOBRANTE_NEGATIVO",
                        "No puede registrar cuadre con sobrante negativo."
                );
            }
            // Igual que frm_Cuadre: el sobrante representa saldo menos ventas.
            // Los retiros se registran en tbl_SaldoRetiro y no descuentan dos veces el saldo.
            if (Math.abs((sobrante + vendido) - saldo) > 0.01d) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "SALDO_NO_COINCIDE",
                        "Verificar cantidad, no coincide con el saldo."
                );
            }
            if (vendido > 0 && (idProducto == null || !ventas.containsKey(idProducto))) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "VENTA_NO_COINCIDE",
                        "La cantidad de items usados no coincide con las OT registradas."
                );
            }
            if (idProducto != null && ventas.containsKey(idProducto) && Math.abs(ventas.get(idProducto) - vendido) > 0.01d) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "VENTA_NO_COINCIDE",
                        "La cantidad de items usados no coincide con las OT registradas."
                );
            }
        }
        for (Map.Entry<Integer, Double> venta : ventas.entrySet()) {
            boolean productoEncontrado = false;
            for (Map<String, Object> item : detalle) {
                Integer idProducto = toPositiveInteger(item.get("idProducto"));
                if (venta.getKey().equals(idProducto)) {
                    productoEncontrado = true;
                    break;
                }
            }
            if (!productoEncontrado) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "VENTA_NO_COINCIDE",
                        "La cantidad de items usados no coincide con las OT registradas."
                );
            }
        }
    }

    private Map<Integer, Double> agruparRetirosPorProducto(List<Map<String, Object>> retiros) {
        Map<Integer, Double> out = new LinkedHashMap<>();
        if (retiros == null) {
            return out;
        }
        for (Map<String, Object> row : retiros) {
            Integer idProducto = toPositiveInteger(findValue(row, "Id_Producto", "idProducto", "id_producto"));
            if (idProducto == null) {
                continue;
            }
            double cantidad = toDouble(findValue(row, "Cantidad", "cantidad"));
            out.put(idProducto, out.getOrDefault(idProducto, 0d) + cantidad);
        }
        return out;
    }

    private Map<Integer, Double> agruparVentasPorProducto(List<Map<String, Object>> ventas) {
        Map<Integer, Double> out = new LinkedHashMap<>();
        if (ventas == null) {
            return out;
        }
        for (Map<String, Object> row : ventas) {
            Integer idProducto = toPositiveInteger(findValue(row, "Id_Producto", "idProducto", "id_producto"));
            if (idProducto == null) {
                continue;
            }
            double cantidad = toDouble(findValue(row, "Venta", "venta", "Vendido", "vendido"));
            out.put(idProducto, out.getOrDefault(idProducto, 0d) + cantidad);
        }
        return out;
    }

    private int coerceCount(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<String, Object> first = rows.get(0);
        if (first == null || first.isEmpty()) {
            return 0;
        }
        for (Object value : first.values()) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (Exception ignored) {
                // probar siguiente valor
            }
        }
        return 0;
    }

    private AuthLoginResponse requireTecnico(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        String rol = normalize(usuario == null ? null : usuario.getRol());
        boolean permitido = "tecnico".equals(rol) || (usuario != null && Integer.valueOf(8).equals(usuario.getIdRol()));
        if (!permitido) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_TECNICO_ONLY",
                    "Esta funcionalidad es solo para rol Tecnico."
            );
        }
        return usuario;
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (current.equals(normalizeKey(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private Integer toPositiveInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            int n = ((Number) value).intValue();
            return n > 0 ? n : null;
        }
        try {
            int n = Integer.parseInt(String.valueOf(value).trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }
}
