package com.example.TigoStarSystem.catalogo.service;

import com.example.TigoStarSystem.catalogo.repository.CatalogoRepository;
import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

@Service
public class CatalogoService {
    private final CatalogoRepository catalogoRepository;
    private final SucursalRepository sucursalRepository;
    private final OtRepository otRepository;

    /**
     * Inicializa el servicio de catalogos.
     */
    public CatalogoService(
            CatalogoRepository catalogoRepository,
            SucursalRepository sucursalRepository,
            OtRepository otRepository) {
        this.catalogoRepository = catalogoRepository;
        this.sucursalRepository = sucursalRepository;
        this.otRepository = otRepository;
    }

    /**
     * Lista tecnicos disponibles para formularios de OT/cuadrillas.
     */
    public List<Map<String, Object>> listarTecnicos() {
        return catalogoRepository.listarTecnicos();
    }

    /**
     * Lista rutas; si no llega tecnico, devuelve todas.
     */
    public List<Map<String, Object>> listarRutas(Integer idTecnico) {
        if (idTecnico == null) {
            return catalogoRepository.listarTodasRutas();
        }
        return catalogoRepository.listarRutasPorTecnico(idTecnico);
    }

    /**
     * Lista tipos de servicio.
     */
    public List<Map<String, Object>> listarTiposServicio() {
        return catalogoRepository.listarTiposServicio();
    }

    /**
     * Lista estados de OT.
     */
    public List<Map<String, Object>> listarEstados() {
        return catalogoRepository.listarEstados();
    }

    /**
     * Lista tipos de material segun tipo de servicio.
     */
    public List<Map<String, Object>> listarTipoMaterial(Integer idTipoServicio) {
        if (idTipoServicio == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "tipoServicioId es requerido."
            );
        }
        return catalogoRepository.listarTipoMaterial(idTipoServicio);
    }

    /**
     * Lista catalogo de productos.
     */
    public List<Map<String, Object>> listarProductos() {
        return catalogoRepository.listarProductos();
    }

    /**
     * Lista mascaras/configuraciones de productos.
     */
    public List<Map<String, Object>> listarProductosMascara() {
        return catalogoRepository.listarProductosMascara();
    }

    /**
     * Lista kits de decodificadores.
     */
    public List<Map<String, Object>> listarKitsDecodificadores() {
        return catalogoRepository.listarKitsDecodificadores();
    }

    /**
     * Lista sucursales registradas.
     */
    public List<Map<String, Object>> listarSucursales() {
        return sucursalRepository.obtenerSucursales();
    }

    public List<Map<String, Object>> buscarCargoUsuario(String serial, String chipId, Integer tipoCodigo) {
        String dato = trimToNull(serial);
        if (dato == null) {
            dato = trimToNull(chipId);
        }
        if (dato == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Debe enviar serial o chipId."
            );
        }

        List<Map<String, Object>> spRows;
        try {
            spRows = catalogoRepository.buscarCargoUsuarioSp(dato);
        } catch (Exception ex) {
            spRows = java.util.Collections.emptyList();
        }

        Map<String, Object> base = spRows.isEmpty()
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(spRows.get(0));

        Map<String, Object> producto = catalogoRepository.buscarProductoRegistradoPorDato(dato);
        Integer idProducto = toInteger(readValue(base, "Id_Producto", "id_producto", "IdProducto", "idProducto"));
        if (idProducto == null && producto != null) {
            idProducto = toInteger(readValue(producto, "Id_Producto", "id_producto", "IdProducto", "idProducto"));
        }

        String serialDb = trimToNull(asString(readValue(base, "Serial", "serial")));
        String chipDb = trimToNull(asString(readValue(base, "ChipID", "ChipId", "chipid", "chipId")));
        if (producto != null) {
            if (serialDb == null) {
                serialDb = trimToNull(asString(readValue(producto, "Serial", "serial")));
            }
            if (chipDb == null) {
                chipDb = trimToNull(asString(readValue(producto, "ChipID", "ChipId", "chipid", "chipId")));
            }
        }

        String nombreProducto = trimToNull(asString(readValue(base, "Nombre", "nombre", "Producto", "producto")));
        if (nombreProducto == null && idProducto != null) {
            nombreProducto = trimToNull(catalogoRepository.buscarNombreProductoPorId(idProducto));
        }

        String existeRaw = trimToNull(asString(readValue(base, "Existe", "existe")));
        String sePuedeRaw = trimToNull(asString(readValue(base, "SePuede", "sePuede")));
        String observacion = trimToNull(asString(readValue(base, "Observacion", "observacion", "Mensaje", "message")));

        boolean existe = producto != null || (existeRaw != null && existeRaw.equalsIgnoreCase("Existe"));
        boolean sePuede = sePuedeRaw != null
                ? sePuedeRaw.equalsIgnoreCase("SePuede")
                : !existe;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("Existe", existe ? "Existe" : "NoExiste");
        out.put("SePuede", sePuede ? "SePuede" : "NoSePuede");
        out.put("Observacion", observacion);
        out.put("Id_Producto", idProducto);
        out.put("Nombre", nombreProducto);
        out.put("Serial", serialDb);
        out.put("ChipID", chipDb);
        out.put("TipoCodigo", tipoCodigo);

        List<Map<String, Object>> response = new ArrayList<>();
        response.add(out);
        return response;
    }

    public List<Map<String, Object>> validarSerieSaldoOt(
            String serie,
            Integer idProducto,
            Integer idTipoMaterial,
            Integer idRuta,
            Integer idSucursal) {
        String serieTrim = trimToNull(serie);
        if (serieTrim == null) {
            return buildSerieSaldoResponse(false, "serie es requerida.", null, idProducto, null);
        }
        if (idProducto == null || idProducto <= 0) {
            return buildSerieSaldoResponse(false, "idProducto es requerido.", serieTrim, null, null);
        }
        if (idTipoMaterial == null || idTipoMaterial <= 0) {
            return buildSerieSaldoResponse(false, "tipoMaterial es requerido.", serieTrim, idProducto, null);
        }
        if (idRuta == null || idRuta <= 0) {
            return buildSerieSaldoResponse(false, "idRuta es requerido para validar saldo.", serieTrim, idProducto, null);
        }

        try {
            List<Map<String, Object>> rows = otRepository.validarEstadoSerie(
                    serieTrim,
                    "",
                    idProducto,
                    idTipoMaterial,
                    idRuta,
                    idSucursal
            );

            String estado = rows.isEmpty() ? null : asString(valueByIndex(rows.get(0), 0));
            String observacion = rows.isEmpty() ? "No se pudo validar la serie." : asString(valueByIndex(rows.get(0), 1));
            boolean sePuede = !isNoSePuedeRegistrar(estado);

            Map<String, Object> chipRow = otRepository.obtenerChipIdPorSerie(serieTrim, idSucursal);
            String chip = chipRow == null ? null : trimToNull(asString(readValue(chipRow, "ChipID", "ChipId", "chipid", "chipId")));
            Integer idProductoDb = chipRow == null ? null : toInteger(readValue(chipRow, "Id_Producto", "id_producto", "IdProducto", "idProducto"));

            return buildSerieSaldoResponse(
                    sePuede,
                    trimToNull(observacion),
                    serieTrim,
                    idProductoDb != null ? idProductoDb : idProducto,
                    chip
            );
        } catch (Exception ex) {
            return buildSerieSaldoResponse(false, "No se pudo validar la serie.", serieTrim, idProducto, null);
        }
    }

    public Map<String, Object> validarSerieChipUnico(String serie, String chipId) {
        String serieTrim = trimToNull(serie);
        String chipTrim = trimToNull(chipId);
        if (serieTrim == null || chipTrim == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie y chipId son requeridos.");
        }

        Map<String, Object> result = new LinkedHashMap<>(otRepository.validarSerieChipIdUnicos(serieTrim, chipTrim));
        boolean chipExiste = Boolean.TRUE.equals(result.get("chipExiste"));
        boolean mismoRegistro = Boolean.TRUE.equals(result.get("mismoRegistro"));
        result.put("sePuede", !chipExiste || mismoRegistro);
        return result;
    }

    public List<Map<String, Object>> obtenerChipIdPorSerie(String serie, Integer idSucursal) {
        String serieTrim = trimToNull(serie);
        if (serieTrim == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        Map<String, Object> row = otRepository.obtenerChipIdPorSerie(serieTrim, idSucursal);
        if (row == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> response = new ArrayList<>();
        response.add(row);
        return response;
    }

    private Object readValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) return null;
        for (String key : keys) {
            if (row.containsKey(key)) {
                Object value = row.get(key);
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    return value;
                }
            }
        }
        return null;
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

    private boolean isNoSePuedeRegistrar(String estado) {
        String normalized = trimToNull(estado);
        return normalized != null && normalized.equalsIgnoreCase("NoSePuedeRegistrar");
    }

    private List<Map<String, Object>> buildSerieSaldoResponse(
            boolean sePuede,
            String observacion,
            String serie,
            Integer idProducto,
            String chip) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("SePuede", sePuede ? "SePuede" : "NoSePuede");
        out.put("Observacion", trimToNull(observacion));
        out.put("Id_Producto", idProducto);
        out.put("Serial", trimToNull(serie));
        out.put("ChipID", trimToNull(chip));
        List<Map<String, Object>> response = new ArrayList<>();
        response.add(out);
        return response;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
