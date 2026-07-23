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
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            List<Map<String, Object>> detalle = normalizarDetalle(saldoRows, retiros);
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
        List<Map<String, Object>> detalle = normalizarDetalle(saldoRows, retiros);
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

    /**
     * Ejecuta la validacion de cuadre para una ruta y fecha.
     */
    public List<Map<String, Object>> validarCuadreRuta(Integer idRuta, LocalDate fecha) {
        return cuadreRepository.validarCuadreRuta(idRuta, fecha);
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

    private List<Map<String, Object>> normalizarDetalle(List<Map<String, Object>> rows, List<Map<String, Object>> retiros) {
        Map<Integer, Double> retiradoPorProducto = agruparRetirosPorProducto(retiros);
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Integer idProducto = toPositiveInteger(findValue(row, "idProducto", "Id_Producto", "id_producto"));
            String producto = firstNonBlank(
                    valueAsString(findValue(row, "producto", "Producto", "nombre", "Nombre")),
                    idProducto == null ? "Producto" : "Producto " + idProducto
            );
            double saldo = toDouble(findValue(row, "saldo", "Saldo", "SaldoDia", "SaldoDiaHoy", "Cantidad", "cantidad", "existencia", "Disponible"));
            double vendido = toDouble(findValue(row, "venta", "Venta", "Vendido", "ItemsVendidos", "itemsVendidos"));
            double retirado = retiradoPorProducto.containsKey(idProducto)
                    ? retiradoPorProducto.get(idProducto)
                    : toDouble(findValue(row, "retirado", "Retirado", "ItemsRetirados", "itemsRetirados", "NoEntregado", "noEntregado"));
            Object sobranteRaw = findValue(row, "sobrante", "Sobrante", "ItemsSobrantes", "itemsSobrantes");
            double sobrante = sobranteRaw == null ? Math.max(0, saldo - vendido - retirado) : toDouble(sobranteRaw);
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
