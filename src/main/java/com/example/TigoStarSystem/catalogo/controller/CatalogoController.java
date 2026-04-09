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

    @GetMapping("/productos/TraerTodosLosProductos_SinFungibleWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosSinFungibleWeb() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosSinFungibleWeb(),
                "Listado de productos sin fungible web."));
    }

    @GetMapping("/productos/TraerTodosLosProductos_x_IdRutaWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosPorRuta(
            @RequestParam("rutaId") Integer rutaId) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosPorRuta(rutaId),
                "Listado de productos por ruta."));
    }

    @GetMapping("/productos/TraerTodosLosProductosPCargoUsuarioWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosCargoUsuarioWeb() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosCargoUsuarioWeb(),
                "Listado de productos para cargo usuario."));
    }

    @GetMapping("/cargo-usuario/buscar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> buscarSerialCargoUsuario(
            @RequestParam(value = "serial", required = false) String serial,
            @RequestParam(value = "chipId", required = false) String chipId,
            @RequestParam("tipoCodigo") Integer tipoCodigo) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.buscarSerialCargoUsuario(serial, chipId, tipoCodigo),
                "Busqueda cargo usuario."
        ));
    }

    @GetMapping("/chip-id/spx_TraerChipID2")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> traerChipIdPorSerie(
            @RequestParam("serie") String serie) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.traerChipIdPorSerie(serie),
                "ChipID obtenido por serie."));
    }

    @GetMapping("/validar-serie-chip")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validarSerieChipUnico(
            @RequestParam("serie") String serie,
            @RequestParam("chipId") String chipId) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieChipUnico(serie, chipId),
                "Validacion de serie y ChipID."));
    }

    @GetMapping("/spx_TraerDatoSerieChipIdCU_OT")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarSerieSaldo(
            @RequestParam("serie") String serie,
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam("tipoMaterial") Integer tipoMaterial,
            @RequestParam("idRuta") Integer idRuta) {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieSaldo(serie, idProducto, tipoMaterial, idRuta),
                "Validacion de serie contra saldo."));
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
