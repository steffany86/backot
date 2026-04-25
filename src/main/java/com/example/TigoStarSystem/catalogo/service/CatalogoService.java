package com.example.TigoStarSystem.catalogo.service;

import com.example.TigoStarSystem.catalogo.repository.CatalogoRepository;
import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogoService {
    private final CatalogoRepository catalogoRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Inicializa el servicio de catalogos.
     */
    public CatalogoService(CatalogoRepository catalogoRepository, SucursalRepository sucursalRepository) {
        this.catalogoRepository = catalogoRepository;
        this.sucursalRepository = sucursalRepository;
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
        Integer tecnico = idTecnico != null && idTecnico > 0 ? idTecnico : null;
        List<Map<String, Object>> rows = catalogoRepository.listarRutasPorTecnico(tecnico);
        return normalizarRutas(rows);
    }

    /**
     * Lista tipos de servicio.
     */
    public List<Map<String, Object>> listarTiposServicio() {
        return catalogoRepository.listarTiposServicio();
    }

    /**
     * Lista productos nomencladores (sufijo -> producto).
     */
    public List<Map<String, Object>> listarNomencladores() {
        return catalogoRepository.listarNomencladores();
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
     * Lista catalogo de productos sin fungible web.
     */
    public List<Map<String, Object>> listarProductosSinFungibleWeb() {
        return catalogoRepository.listarProductosSinFungibleWeb();
    }

    public List<Map<String, Object>> listarProductosPorRuta(Integer idRuta) {
        if (idRuta == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "rutaId es requerido."
            );
        }
        return catalogoRepository.listarProductosPorRuta(idRuta);
    }

    /**
     * Lista productos disponibles para cargo usuario.
     */
    public List<Map<String, Object>> listarProductosCargoUsuarioWeb() {
        return catalogoRepository.listarProductosCargoUsuarioWeb();
    }

    /**
     * Busca existencia de serial/chip para cargo usuario.
     */
    public List<Map<String, Object>> buscarSerialCargoUsuario(String serial, String chipId, Integer tipoCodigo) {
        if (tipoCodigo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tipoCodigo es requerido.");
        }
        return catalogoRepository.buscarSerialCargoUsuario(
                serial == null ? "" : serial.trim(),
                chipId == null ? "" : chipId.trim(),
                tipoCodigo
        );
    }

    /**
     * Obtiene chipId e idProducto desde la serie.
     */
    public List<Map<String, Object>> traerChipIdPorSerie(String serie) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        return catalogoRepository.traerChipIdPorSerie(serie.trim());
    }

    /**
     * Valida que serie y chipId correspondan al mismo registro.
     */
    public Map<String, Object> validarSerieChipUnico(String serie, String chipId) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        if (chipId == null || chipId.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "chipId es requerido.");
        }
        return catalogoRepository.validarSerieChipUnico(serie.trim(), chipId.trim());
    }

    /**
     * Valida la serie contra el saldo usando el procedimiento de OT.
     */
    public List<Map<String, Object>> validarSerieSaldo(String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        if (idProducto == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idProducto es requerido.");
        }
        if (tipoMaterial == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tipoMaterial es requerido.");
        }
        if (idRuta == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idRuta es requerido.");
        }
        return catalogoRepository.validarSerieSaldo(serie.trim(), idProducto, tipoMaterial, idRuta);
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCU(String serie) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        return catalogoRepository.traerDatoSerieChipIdCU(serie.trim());
    }

    public List<Map<String, Object>> traerDatoSerieChipIdCUCUNR2(String serie, String chipId) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "serie es requerida.");
        }
        if (chipId == null || chipId.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "chipId es requerido.");
        }
        return catalogoRepository.traerDatoSerieChipIdCUCUNR2(serie.trim(), chipId.trim());
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

    private List<Map<String, Object>> normalizarRutas(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            if (isRutaEliminada(row)) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            if (row != null) {
                normalized.putAll(row);
            }
            Object idRuta = readValue(row, "idRuta", "id_ruta", "Id_Ruta", "IdRuta", "id", "Id");
            Object nombreRuta = readValue(row, "ruta", "cuadrilla", "Nombre", "nombre", "grupo", "Grupo");
            Object idTecnico = readValue(row, "idTecnico", "id_tecnico", "Id_Tecnico", "id_vendedor", "Id_Vendedor");
            if (idRuta != null) {
                normalized.put("idRuta", idRuta);
                normalized.put("id_ruta", idRuta);
            }
            if (nombreRuta != null) {
                normalized.put("ruta", nombreRuta);
                normalized.put("cuadrilla", nombreRuta);
                normalized.put("grupo", nombreRuta);
            }
            if (idTecnico != null) {
                normalized.put("idTecnico", idTecnico);
                normalized.put("id_tecnico", idTecnico);
            }
            out.add(normalized);
        }
        return out;
    }

    private boolean isRutaEliminada(Map<String, Object> row) {
        Object raw = readValue(row, "e_eliminado", "E_Eliminado", "eeliminado", "eliminado");
        if (raw == null) return false;
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        String normalized = String.valueOf(raw).trim().toLowerCase();
        if (normalized.isEmpty()) return false;
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "si".equals(normalized)
                || "yes".equals(normalized);
    }

    private Object readValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }
}
