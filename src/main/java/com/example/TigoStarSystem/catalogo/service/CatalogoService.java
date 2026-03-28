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
