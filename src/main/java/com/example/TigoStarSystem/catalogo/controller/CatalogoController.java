package com.example.TigoStarSystem.catalogo.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.catalogo.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/catalogos")
public class CatalogoController {
    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/tecnicos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTecnicos() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTecnicos(),
                "Listado de tecnicos."));
    }

    @GetMapping("/rutas")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarRutas(
            @RequestParam(value = "tecnicoId", required = false) Integer tecnicoId) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarRutas(tecnicoId),
                "Listado de rutas."));
    }

    @GetMapping("/tipo-servicio")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTipoServicio() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTiposServicio(),
                "Listado de tipos de servicio."));
    }

    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarEstados() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarEstados(),
                "Listado de estados."));
    }

    @GetMapping("/tipo-material")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTipoMaterial(
            @RequestParam("tipoServicioId") Integer tipoServicioId) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTipoMaterial(tipoServicioId),
                "Listado de tipos de material."));
    }

    @GetMapping("/productos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductos() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductos(),
                "Listado de productos."));
    }

    @GetMapping("/productos/mascara")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosMascara() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosMascara(),
                "Listado de mascaras de productos."));
    }

    @GetMapping("/kits-decodificadores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarKitsDecodificadores() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarKitsDecodificadores(),
                "Listado de kits decodificadores."));
    }

    @GetMapping("/sucursales")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarSucursales() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarSucursales(),
                "Listado de sucursales."));
    }
}
