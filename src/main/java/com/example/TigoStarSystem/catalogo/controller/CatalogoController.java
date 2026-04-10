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

    @GetMapping("/cargo-usuario/buscar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> buscarCargoUsuario(
            @RequestParam(value = "serial", required = false) String serial,
            @RequestParam(value = "chipId", required = false) String chipId,
            @RequestParam(value = "tipoCodigo", required = false) Integer tipoCodigo) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.buscarCargoUsuario(serial, chipId, tipoCodigo),
                "Validacion de serie/chip para cargo usuario."
        ));
    }

    @GetMapping({"/spx_TraerDatoSerieChipIdCU_OT", "/validar-serie-saldo"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarSerieSaldoOt(
            @RequestParam("serie") String serie,
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam(value = "tipoMaterial", required = false) Integer tipoMaterial,
            @RequestParam(value = "idTipoMaterial", required = false) Integer idTipoMaterial,
            @RequestParam(value = "idRuta", required = false) Integer idRuta,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        Integer idTipoMaterialFinal = tipoMaterial != null ? tipoMaterial : idTipoMaterial;
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieSaldoOt(serie, idProducto, idTipoMaterialFinal, idRuta, idSucursal),
                "Validacion de serie/chip para OT."
        ));
    }

    @GetMapping({"/chip-id/spx_TraerChipID2", "/chip-id"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerChipIdPorSerie(
            @RequestParam("serie") String serie,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.obtenerChipIdPorSerie(serie, idSucursal),
                "ChipID obtenido por serie."
        ));
    }

    @GetMapping("/validar-serie-chip")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validarSerieChip(
            @RequestParam("serie") String serie,
            @RequestParam("chipId") String chipId) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieChipUnico(serie, chipId),
                "Validacion de unicidad serie/chipid."
        ));
    }
}
