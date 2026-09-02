package com.example.TigoStarSystem.pedidotecnico.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.catalogo.repository.CatalogoRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import com.example.TigoStarSystem.pedidotecnico.dto.PedidoTecnicoCrearRequest;
import com.example.TigoStarSystem.pedidotecnico.dto.PedidoTecnicoItemRequest;
import com.example.TigoStarSystem.pedidotecnico.repository.PedidoTecnicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoTecnicoService {
    private static final String MATERIAL_MARKER_START = "[[PEDIDO_MATERIALES:";
    private static final String TECNICO_MARKER_START = "[[PEDIDO_TECNICO:";
    private static final String MATERIAL_MARKER_END = "]]";
    private final PedidoTecnicoRepository repository;
    private final CatalogoRepository catalogoRepository;
    private final OtRepository otRepository;
    private final AuthService authService;

    public PedidoTecnicoService(PedidoTecnicoRepository repository, CatalogoRepository catalogoRepository, OtRepository otRepository, AuthService authService) {
        this.repository = repository;
        this.catalogoRepository = catalogoRepository;
        this.otRepository = otRepository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String token, Integer idSucursal) {
        AuthLoginResponse user = requireUser(token);
        Integer sucursal = resolveSucursal(user, idSucursal);
        List<Map<String, Object>> rows = repository.listar(requireDataSource(sucursal));
        List<Map<String, Object>> pedidos = normalizar(rows);
        completarNombresMateriales(pedidos, sucursal);
        if (puedeVerTodos(user)) {
            return pedidos;
        }
        Set<Integer> idsTecnico = resolveIdsTecnico(user, sucursal);
        String nombre = normalize(user.getNombre());
        List<Map<String, Object>> filtrados = new ArrayList<>();
        for (Map<String, Object> pedido : pedidos) {
            Integer idUsuario = toInteger(findValue(pedido, "idUsuario", "Id_Usuario", "IdUsuario"));
            Integer idTecnico = toInteger(findValue(pedido, "idTecnico", "Id_Tecnico", "IdTecnico"));
            String tecnico = normalize(asString(findValue(pedido, "tecnico", "Usuario", "usuario", "NombreUsuario", "Tecnico", "nombreTecnico")));
            if ((idUsuario != null && idUsuario.equals(user.getIdUsuario()))
                    || (idTecnico != null && idsTecnico.contains(idTecnico))
                    || (!nombre.isEmpty() && nombre.equals(tecnico))) {
                filtrados.add(pedido);
            }
        }
        return filtrados;
    }

    public Map<String, Object> crear(String token, PedidoTecnicoCrearRequest request, Integer idSucursal) {
        AuthLoginResponse user = requireUser(token);
        if (puedeVerTodos(user) && !esTecnico(user)) {
            // Almacen puede ver todo, pero el alta esta pensada como pedido del tecnico.
            // Admin/sistemas pueden probar el flujo si tienen usuario tecnico mapeado.
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe agregar al menos un material.");
        }
        Integer sucursal = resolveSucursal(user, idSucursal);
        RutaTecnico rutaTecnico = resolveRutaTecnico(user, sucursal);
        Integer idTecnico = rutaTecnico.idVendedor;
        if (idTecnico == null || idTecnico <= 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "TECNICO_NO_ENCONTRADO",
                    "No se pudo resolver el id tecnico del usuario de la sesion."
            );
        }
        Integer idRuta = rutaTecnico.idRuta;
        Map<String, Integer> productosPorNombre = productosPorNombre(sucursal);
        List<PedidoTecnicoItemRequest> items = new ArrayList<>();
        for (PedidoTecnicoItemRequest item : request.getItems()) {
            String material = item == null ? "" : clean(item.getMaterial());
            Integer idProducto = item == null ? null : item.getIdProducto();
            BigDecimal cantidad = item == null ? null : item.getCantidad();
            if (material.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "material es requerido.");
            }
            idProducto = resolveIdProducto(idProducto, material, productosPorNombre);
            if (idProducto == null || idProducto <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe seleccionar un material existente del catalogo.");
            }
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "cantidad debe ser mayor a 0.");
            }
            PedidoTecnicoItemRequest normalized = new PedidoTecnicoItemRequest();
            normalized.setIdProducto(idProducto);
            normalized.setMaterial(material);
            normalized.setCantidad(cantidad);
            items.add(normalized);
        }
        Integer idPedido;
        try {
            idPedido = repository.crear(
                    requireDataSource(sucursal),
                    user.getIdUsuario(),
                    idTecnico,
                    idRuta,
                    user.getNombre(),
                    sucursal,
                    clean(request.getObservacion()),
                    items
            );
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PEDIDO_TECNICO_ERROR",
                    "No se pudo registrar el pedido de material. Detalle: " + rootMessage(ex)
            );
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idPedido", idPedido);
        out.put("idUsuario", user.getIdUsuario());
        out.put("idTecnico", idTecnico);
        out.put("idRuta", idRuta);
        out.put("tecnico", user.getNombre());
        out.put("idSucursal", sucursal);
        out.put("items", items);
        return out;
    }

    private DataSource requireDataSource(Integer idSucursal) {
        DataSource dataSource = otRepository.dataSource(idSucursal);
        if (dataSource == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DB_CONNECTION_ERROR", "No se pudo resolver datasource para la sucursal.");
        }
        return dataSource;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.trim().isEmpty() ? current.getClass().getSimpleName() : message;
    }

    private String limpiarObservacionRespaldo(String observacion) {
        String text = limpiarMarcador(firstNonBlank(observacion, ""), TECNICO_MARKER_START);
        return limpiarMarcador(text, MATERIAL_MARKER_START);
    }

    private String limpiarMarcador(String observacion, String markerStart) {
        String text = firstNonBlank(observacion, "");
        int start = text.indexOf(markerStart);
        if (start < 0) {
            return text;
        }
        int end = text.indexOf(MATERIAL_MARKER_END, start);
        if (end < 0) {
            return text.substring(0, start).trim();
        }
        return (text.substring(0, start) + text.substring(end + MATERIAL_MARKER_END.length())).trim();
    }

    private String parseTecnicoRespaldo(String observacion) {
        String payload = markerPayload(observacion, TECNICO_MARKER_START);
        if (payload.isEmpty()) {
            return "";
        }
        try {
            return clean(new String(Base64.getDecoder().decode(payload), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private List<Map<String, Object>> parseMaterialesRespaldo(String observacion) {
        List<Map<String, Object>> out = new ArrayList<>();
        String payload = markerPayload(observacion, MATERIAL_MARKER_START);
        if (payload.isEmpty()) {
            return out;
        }
        for (String token : payload.split(";")) {
            String[] parts = token.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                String material = new String(Base64.getDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
                BigDecimal cantidad = toBigDecimal(parts[1]);
                if (!clean(material).isEmpty() || cantidad.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("material", clean(material));
                    item.put("cantidad", cantidad);
                    out.add(item);
                }
            } catch (IllegalArgumentException ignored) {
                // Respaldo invalido; se ignora y se usa lo que venga del detalle.
            }
        }
        return out;
    }

    private String markerPayload(String observacion, String markerStart) {
        String text = firstNonBlank(observacion, "");
        int start = text.indexOf(markerStart);
        if (start < 0) {
            return "";
        }
        int end = text.indexOf(MATERIAL_MARKER_END, start);
        if (end < 0) {
            return "";
        }
        return text.substring(start + markerStart.length(), end);
    }

    private List<Map<String, Object>> normalizar(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byPedido = new LinkedHashMap<>();
        if (rows == null) {
            return new ArrayList<>();
        }
        for (Map<String, Object> row : rows) {
            String id = firstNonBlank(
                    asString(findValue(row, "PedidoIdAlias", "Id_PedidoTecnico", "IdPedidoTecnico", "Id_Pedido", "IdPedido")),
                    asString(findValue(row, "Id", "id"))
            );
            if (id == null || id.isEmpty()) {
                id = String.valueOf(byPedido.size() + 1);
            }
            final String idFinal = id;
            Map<String, Object> pedido = byPedido.computeIfAbsent(idFinal, ignored -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("idPedido", toInteger(idFinal));
                created.put("fecha", findValue(row, "PedidoFechaAlias", "Fecha_Registro", "FechaRegistro", "fecha_registro", "Fecha", "FechaPedido", "fecha"));
                created.put("idUsuario", toInteger(findValue(row, "Id_Usuario", "IdUsuario", "idUsuario", "id_usuario")));
                created.put("idTecnico", toInteger(findValue(row, "Id_Tecnico", "IdTecnico", "idTecnico", "id_tecnico")));
                created.put("idRuta", toInteger(findValue(row, "Id_Ruta", "IdRuta", "idRuta", "id_ruta", "Ruta", "ruta")));
                created.put("idPedidoVendedor", toInteger(findValue(row, "Id_PedidoVendedor", "IdPedidoVendedor", "idPedidoVendedor", "id_pedido_vendedor", "Id_Vendedor", "IdVendedor", "idVendedor", "id_vendedor")));
                created.put("tecnico", firstNonBlank(asString(findValue(row, "Usuario", "usuario", "NombreUsuario", "Nombre_Usuario", "Tecnico", "NombreTecnico", "Nombre_Tecnico", "tecnico")), ""));
                created.put("estado", firstNonBlank(asString(findValue(row, "Estado", "estado")), "PENDIENTE"));
                String observacionRaw = firstNonBlank(asString(findValue(row, "Observacion", "observacion", "Detalle")), "");
                created.put("observacion", limpiarObservacionRespaldo(observacionRaw));
                created.put("__materialesRespaldo", parseMaterialesRespaldo(observacionRaw));
                created.put("__tecnicoRespaldo", parseTecnicoRespaldo(observacionRaw));
                created.put("items", new ArrayList<Map<String, Object>>());
                return created;
            });
            String material = firstNonBlank(asString(findValue(
                    row,
                    "MaterialPedidoAlias",
                    "Material",
                    "NombreMaterial",
                    "Nombre_Material",
                    "MaterialPedido",
                    "Material_Pedido",
                    "Producto",
                    "NombreProducto",
                    "Nombre_Producto",
                    "Nombre",
                    "Descripcion",
                    "DescripcionMaterial",
                    "Descripcion_Material",
                    "Item",
                    "Articulo"
            )), "");
            BigDecimal cantidad = toBigDecimal(findValue(row, "CantidadPedidoAlias", "Cantidad", "cantidad", "Cant", "cant"));
            if (!material.isEmpty() || cantidad.compareTo(BigDecimal.ZERO) > 0) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) pedido.get("items");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("idProducto", toInteger(findValue(row, "ProductoPedidoAlias", "Id_Producto", "IdProducto", "idProducto", "id_producto")));
                item.put("material", material);
                item.put("cantidad", cantidad);
                items.add(item);
            }
        }
        for (Map<String, Object> pedido : byPedido.values()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> detalle = (List<Map<String, Object>>) pedido.get("items");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> respaldo = (List<Map<String, Object>>) pedido.get("__materialesRespaldo");
            boolean usarRespaldo = detalle == null || detalle.isEmpty();
            if (!usarRespaldo) {
                for (Map<String, Object> item : detalle) {
                    if (firstNonBlank(asString(item.get("material")), "").isEmpty()) {
                        usarRespaldo = true;
                        break;
                    }
                }
            }
            if (usarRespaldo && respaldo != null && !respaldo.isEmpty()) {
                pedido.put("items", respaldo);
            }
            String tecnico = firstNonBlank(asString(pedido.get("tecnico")), "");
            if (tecnico.isEmpty()) {
                pedido.put("tecnico", firstNonBlank(asString(pedido.get("__tecnicoRespaldo")), ""));
            }
            pedido.remove("__tecnicoRespaldo");
            pedido.remove("__materialesRespaldo");
        }
        return new ArrayList<>(byPedido.values());
    }

    private AuthLoginResponse requireUser(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse user = me == null ? null : me.getUsuario();
        if (user == null || user.getIdUsuario() == null || user.getIdUsuario() <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesion invalida.");
        }
        return user;
    }

    private Integer resolveSucursal(AuthLoginResponse user, Integer idSucursal) {
        Integer sucursal = idSucursal != null && idSucursal > 0 ? idSucursal : user.getIdSucursal();
        if (sucursal == null || sucursal <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSucursal es requerido.");
        }
        return sucursal;
    }

    private Set<Integer> resolveIdsTecnico(AuthLoginResponse user, Integer idSucursal) {
        Set<Integer> out = new HashSet<>();
        try {
            out.addAll(otRepository.obtenerIdsVendedorPorIdUsuario(user.getIdUsuario(), idSucursal));
        } catch (RuntimeException ignored) {
            // Si no hay mapeo, queda filtrado por idUsuario/nombre.
        }
        return out;
    }

    private RutaTecnico resolveRutaTecnico(AuthLoginResponse user, Integer idSucursal) {
        List<Map<String, Object>> rutas = catalogoRepository.listarRutasPorTecnico(user.getIdUsuario(), idSucursal);
        if (rutas != null) {
            for (Map<String, Object> ruta : rutas) {
                Integer idRuta = toInteger(findValue(ruta, "idRuta", "Id_Ruta", "id_ruta", "IdRuta", "ruta", "Ruta"));
                Integer idVendedor = toInteger(findValue(ruta, "idVendedor", "id_vendedor", "Id_Vendedor", "IdVendedor", "idTecnico", "id_tecnico", "Id_Tecnico"));
                if (idRuta != null && idRuta > 0) {
                    if (idVendedor == null || idVendedor <= 0) {
                        Set<Integer> idsTecnico = resolveIdsTecnico(user, idSucursal);
                        idVendedor = idsTecnico.isEmpty() ? null : idsTecnico.iterator().next();
                    }
                    return new RutaTecnico(idRuta, idVendedor);
                }
            }
        }
        throw new ApiException(
                HttpStatus.NOT_FOUND,
                "RUTA_TECNICO_NO_ENCONTRADA",
                "No se encontro ruta activa para el tecnico de la sesion."
        );
    }

    private Map<String, Integer> productosPorNombre(Integer idSucursal) {
        Map<String, Integer> out = new LinkedHashMap<>();
        List<Map<String, Object>> productos = catalogoRepository.listarProductosSinFungibleWeb(idSucursal);
        if (productos == null) {
            return out;
        }
        for (Map<String, Object> producto : productos) {
            Integer idProducto = toInteger(findValue(producto, "idProducto", "Id_Producto", "id_producto", "IdProducto"));
            String nombre = firstNonBlank(asString(findValue(producto, "producto", "Producto", "nombre", "Nombre", "descripcion", "Descripcion")), "");
            if (idProducto != null && idProducto > 0 && !nombre.isEmpty()) {
                out.putIfAbsent(normalize(nombre), idProducto);
            }
        }
        return out;
    }

    private void completarNombresMateriales(List<Map<String, Object>> pedidos, Integer idSucursal) {
        if (pedidos == null || pedidos.isEmpty()) {
            return;
        }
        Map<Integer, String> productos = productosPorId(idSucursal);
        if (productos.isEmpty()) {
            return;
        }
        for (Map<String, Object> pedido : pedidos) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) pedido.get("items");
            if (items == null) {
                continue;
            }
            for (Map<String, Object> item : items) {
                Integer idProducto = toInteger(findValue(item, "idProducto", "Id_Producto", "id_producto", "IdProducto", "ProductoPedidoAlias"));
                String material = firstNonBlank(asString(findValue(item, "material", "Material")), "");
                if (idProducto != null && idProducto > 0 && material.isEmpty()) {
                    item.put("material", firstNonBlank(productos.get(idProducto), ""));
                }
            }
        }
    }

    private Map<Integer, String> productosPorId(Integer idSucursal) {
        Map<Integer, String> out = new LinkedHashMap<>();
        List<Map<String, Object>> productos = catalogoRepository.listarProductosSinFungibleWeb(idSucursal);
        if (productos == null) {
            return out;
        }
        for (Map<String, Object> producto : productos) {
            Integer idProducto = toInteger(findValue(producto, "idProducto", "Id_Producto", "id_producto", "IdProducto"));
            String nombre = firstNonBlank(asString(findValue(producto, "producto", "Producto", "nombre", "Nombre", "descripcion", "Descripcion")), "");
            if (idProducto != null && idProducto > 0 && !nombre.isEmpty()) {
                out.putIfAbsent(idProducto, nombre);
            }
        }
        return out;
    }

    private Integer resolveIdProducto(Integer idProducto, String material, Map<String, Integer> productosPorNombre) {
        if (idProducto != null && idProducto > 0) {
            return idProducto;
        }
        if (productosPorNombre == null) {
            return null;
        }
        return productosPorNombre.get(normalize(material));
    }

    private boolean puedeVerTodos(AuthLoginResponse user) {
        String rol = normalize(user.getRol());
        return rol.contains("almacen")
                || rol.equals("sistemas")
                || rol.equals("admin")
                || rol.equals("administrador")
                || rol.contains("backoffice")
                || rol.contains("back office");
    }

    private boolean esTecnico(AuthLoginResponse user) {
        return "tecnico".equals(normalize(user.getRol())) || Integer.valueOf(8).equals(user.getIdRol());
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) return null;
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

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(String.valueOf(value).trim().replace(",", "."));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first.trim() : second;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private static class RutaTecnico {
        private final Integer idRuta;
        private final Integer idVendedor;

        private RutaTecnico(Integer idRuta, Integer idVendedor) {
            this.idRuta = idRuta;
            this.idVendedor = idVendedor;
        }
    }
}
