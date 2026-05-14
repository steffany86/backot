package com.example.TigoStarSystem.catalogo.controller;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.catalogo.service.CatalogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/catalogos")
public class CatalogoController {
    private static final Logger logger = LoggerFactory.getLogger(CatalogoController.class);
    private final CatalogoService catalogoService;
    private final AuthService authService;

    public CatalogoController(CatalogoService catalogoService, AuthService authService) {
        this.catalogoService = catalogoService;
        this.authService = authService;
    }

    @GetMapping("/tecnicos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTecnicos(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTecnicos(idSucursal),
                "Listado de tecnicos."));
    }

    @GetMapping("/rutas")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarRutas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "tecnicoId", required = false) Integer tecnicoId,
            @RequestParam(value = "idTecnico", required = false) Integer idTecnico) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        Integer tecnico = tecnicoId != null ? tecnicoId : idTecnico;
        logger.info("GET /catalogos/rutas idSucursal={} tecnicoId={}", idSucursal, tecnico);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarRutas(tecnico, idSucursal),
                "Listado de rutas."));
    }

    @GetMapping("/tipo-servicio")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTipoServicio(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTiposServicio(idSucursal),
                "Listado de tipos de servicio."));
    }

    @GetMapping("/nomencladores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarNomencladores(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarNomencladores(idSucursal),
                "Listado de productos nomencladores."));
    }

    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarEstados(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarEstados(idSucursal),
                "Listado de estados."));
    }

    @GetMapping("/ramales")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarRamales(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarRamales(idSucursal),
                "Listado de ramales."));
    }

    @GetMapping("/tipo-tecnologia")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposTecnologia(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("idRuta") Integer idRuta) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTiposTecnologia(idRuta, idSucursal),
                "Listado de tipos de tecnologia."));
    }

    @GetMapping("/tipo-material")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTipoMaterial(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("tipoServicioId") Integer tipoServicioId) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarTipoMaterial(tipoServicioId, idSucursal),
                "Listado de tipos de material."));
    }

    @GetMapping("/productos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductos(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductos(idSucursal),
                "Listado de productos."));
    }

    @GetMapping("/productos/TraerTodosLosProductos_SinFungibleWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosSinFungibleWeb(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosSinFungibleWeb(idSucursal),
                "Listado de productos sin fungible web."));
    }

    @GetMapping("/productos/TraerTodosLosProductos_x_IdRutaWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosPorRuta(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("rutaId") Integer rutaId) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        logger.info("GET /catalogos/productos/TraerTodosLosProductos_x_IdRutaWeb idSucursal={} rutaId={}", idSucursal, rutaId);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosPorRuta(rutaId, idSucursal),
                "Listado de productos por ruta."));
    }

    @GetMapping("/productos/TraerTodosLosProductosPCargoUsuarioWeb")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosCargoUsuarioWeb(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosCargoUsuarioWeb(idSucursal),
                "Listado de productos para cargo usuario."));
    }

    @GetMapping("/cargo-usuario/buscar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> buscarSerialCargoUsuario(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "serial", required = false) String serial,
            @RequestParam(value = "chipId", required = false) String chipId,
            @RequestParam("tipoCodigo") Integer tipoCodigo) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.buscarSerialCargoUsuario(serial, chipId, tipoCodigo, idSucursal),
                "Busqueda cargo usuario."
        ));
    }

    @GetMapping("/chip-id/spx_TraerChipID2")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> traerChipIdPorSerie(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("serie") String serie) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.traerChipIdPorSerie(serie, idSucursal),
                "ChipID obtenido por serie."));
    }

    @GetMapping("/validar-serie-chip")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validarSerieChipUnico(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("serie") String serie,
            @RequestParam("chipId") String chipId) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieChipUnico(serie, chipId, idSucursal),
                "Validacion de serie y ChipID."));
    }

    @GetMapping("/spx_TraerDatoSerieChipIdCU_OT")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarSerieSaldo(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("serie") String serie,
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam("tipoMaterial") Integer tipoMaterial,
            @RequestParam("idRuta") Integer idRuta) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.validarSerieSaldo(serie, idProducto, tipoMaterial, idRuta, idSucursal),
                "Validacion de serie contra saldo."));
    }

    @GetMapping("/spx_TraerDatoSerieChipIdCU")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> traerDatoSerieChipIdCU(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("serie") String serie) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.traerDatoSerieChipIdCU(serie, idSucursal),
                "Validacion de serie contra saldo."));
    }

    @GetMapping("/spx_TraerDatoSerieChipIdCU_CUNR2")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> traerDatoSerieChipIdCUCUNR2(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("serie") String serie,
            @RequestParam("chipId") String chipId) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.traerDatoSerieChipIdCUCUNR2(serie, chipId, idSucursal),
                "Validacion de serie y chipId contra saldo."));
    }

    @GetMapping("/productos/mascara")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarProductosMascara(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarProductosMascara(idSucursal),
                "Listado de mascaras de productos."));
    }

    @GetMapping("/kits-decodificadores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarKitsDecodificadores(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        Integer idSucursal = resolveOptionalIdSucursal(token);
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarKitsDecodificadores(idSucursal),
                "Listado de kits decodificadores."));
    }

    @GetMapping("/sucursales")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarSucursales() {
        return ResponseEntity.ok(ApiResponse.of(
                catalogoService.listarSucursales(),
                "Listado de sucursales."));
    }

    private Integer resolveOptionalIdSucursal(String token) {
        AuthMeResponse me = resolveSession(token);
        return extractIdSucursal(me);
    }

    private Integer extractIdSucursal(AuthMeResponse me) {
        if (me == null || me.getUsuario() == null) {
            return null;
        }
        return me.getUsuario().getIdSucursal();
    }

    private AuthMeResponse resolveSession(String token) {
        if (isBlank(token)) {
            return null;
        }
        return authService.me(token);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
