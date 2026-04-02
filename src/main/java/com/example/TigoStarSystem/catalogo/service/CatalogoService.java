package com.example.TigoStarSystem.catalogo.service;

import com.example.TigoStarSystem.catalogo.repository.CatalogoRepository;
import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
     * Busca chipId asociado a una serie usando spx_TraerChipID2.
     */
    public List<Map<String, Object>> traerChipIdSpxTraerChipID2(String serie) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "serie es requerida."
            );
        }
        return catalogoRepository.traerChipIdSpxTraerChipID2(serie);
    }

    /**
     * Ejecuta spx_TraerDatoSerieChipIdCU_OT para obtener información del producto/serie.
     */
    public List<Map<String, Object>> traerDatoSerieChipIdCuOt(
            String serie, Integer idProducto, Integer tipoMaterial, Integer idRuta) {
        if (serie == null || serie.trim().isEmpty() || idProducto == null || tipoMaterial == null || idRuta == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "serie, idProducto, tipoMaterial e idRuta son requeridos."
            );
        }
        return catalogoRepository.traerDatoSerieChipIdCuOt(serie, idProducto, tipoMaterial, idRuta);
    }

    /**
     * Valida que serie y ChipID correspondan al mismo registro y sean unicos.
     */
    public Map<String, Object> validarSerieChipIdUnicos(String serie, String chipId) {
        if (serie == null || serie.trim().isEmpty() || chipId == null || chipId.trim().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "serie y chipId son requeridos."
            );
        }
        return catalogoRepository.validarSerieChipIdUnicos(serie, chipId);
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
}
