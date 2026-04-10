package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.ot.dto.OtCrearRequest;
import com.example.TigoStarSystem.ot.dto.OtCrearResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarDetalleAgendaRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarDetalleAgendaResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarCargoUsuarioRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarDatosRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistroAgendaValidacionResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaRequest;
import com.example.TigoStarSystem.ot.dto.OtRegistrarVentaResponse;
import com.example.TigoStarSystem.ot.dto.OtRegistrarRealizadaConCargoRequest;
import com.example.TigoStarSystem.ot.dto.OtRealizadaRequest;
import com.example.TigoStarSystem.ot.dto.OtValidarVentaDetalleResponse;
import com.example.TigoStarSystem.ot.service.OtService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/ot")
public class OtController {
    private final OtService otService;
    private final AuthService authService;

    public OtController(OtService otService, AuthService authService) {
        this.otService = otService;
        this.authService = authService;
    }

    @PostMapping("/realizada")
    public ResponseEntity<ApiResponse<Integer>> registrarOtRealizada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody OtRealizadaRequest request) {
        int filas = otService.registrarOtRealizada(request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(filas, "OT actualizada como realizada."));
    }

    @PostMapping({"/spx_RegistrarVentaParaRegistroOTwb", "/venta/registro-otwb"})
    public ResponseEntity<ApiResponse<OtRegistrarVentaResponse>> registrarVentaParaRegistroOtWb(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody OtRegistrarVentaRequest request) {
        OtRegistrarVentaResponse response = otService.registrarVentaParaRegistroOtWb(request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(response, "Venta registrada correctamente."));
    }

    @PostMapping("/detalle-materiales")
    public ResponseEntity<ApiResponse<OtRegistrarDetalleAgendaResponse>> registrarDetalleMateriales(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody OtRegistrarDetalleAgendaRequest request) {
        OtRegistrarDetalleAgendaResponse response = otService.registrarDetalleAgenda(request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(response, "Detalle de OT registrado correctamente."));
    }

    @PostMapping("/cargo-usuario")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarCargoUsuario(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody OtRegistrarCargoUsuarioRequest request) {
        int filas = otService.registrarCargoUsuario(request, resolveIdSucursal(token));
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("guardados", filas);
        return ResponseEntity.ok(ApiResponse.of(
                response,
                "Cargo usuario registrado correctamente."
        ));
    }

    @PostMapping("/realizada-cargo-usuario")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarRealizadaConCargoUsuario(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody OtRegistrarRealizadaConCargoRequest request) {
        Map<String, Object> response = otService.registrarRealizadaConCargo(request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(
                response,
                "OT realizada y cargo usuario registrados correctamente."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OtCrearResponse>> crearOt(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody OtCrearRequest request) {
        AuthMeResponse me = resolveSession(token);
        Integer idSucursal = extractIdSucursal(me);

        if (me != null && me.getUsuario() != null && me.getUsuario().getRol() != null) {
            String rol = me.getUsuario().getRol().trim().toLowerCase();
            if (rol.equals("tecnico")) {
                request.setIdUsuario(me.getUsuario().getIdUsuario());
            }
        }

        OtCrearResponse response = otService.crearOt(request, idSucursal);
        return ResponseEntity.ok(ApiResponse.of(response, "OT registrada."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarOt(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(value = "usuario", required = false) Integer idUsuario,
            @RequestParam(value = "rol", required = false) String rol,
            @RequestParam(value = "pendiente", required = false) Boolean pendiente) {
        Integer idSucursal = resolveIdSucursal(token);

        if (fecha != null) {
            return ResponseEntity.ok(ApiResponse.of(
                    otService.filtrarListado(
                            otService.listarPorFecha(fecha, idSucursal),
                            idUsuario,
                            rol,
                            pendiente),
                    "Listado de OT por fecha."));
        }
        if (inicio != null && fin != null) {
            return ResponseEntity.ok(ApiResponse.of(
                    otService.filtrarListado(
                            otService.listarPorRango(inicio, fin, idSucursal),
                            idUsuario,
                            rol,
                            pendiente),
                    "Listado de OT por rango."));
        }
        if (inicio == null && fin == null) {
            LocalDate hoy = LocalDate.now();
            return ResponseEntity.ok(ApiResponse.of(
                    otService.filtrarListado(
                            otService.listarPorFecha(hoy, idSucursal),
                            idUsuario,
                            rol,
                            pendiente),
                    "Listado de OT del dia."));
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Debe enviar 'fecha' o ambos 'inicio' y 'fin'."
        );
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPorId(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerPorId(id, resolveIdSucursal(token)),
                "OT encontrada."
        ));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPorNumero(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("numero") @NotBlank String numero) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerPorNumero(numero, resolveIdSucursal(token)),
                "OT encontrada."
        ));
    }

    @GetMapping("/{id}/instalados")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerInstalados(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerDetalleInstalado(id, resolveIdSucursal(token)),
                "Detalle instalado."
        ));
    }

    @GetMapping("/{id}/retirados")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerRetirados(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerDetalleRetirado(id, resolveIdSucursal(token)),
                "Detalle retirado."
        ));
    }

    @GetMapping("/{id}/excedentes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerExcedentes(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerDetalleExcedente(id, resolveIdSucursal(token)),
                "Detalle excedente."
        ));
    }

    @GetMapping("/{id}/cargo-usuario")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerCargoUsuario(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerDetalleCargoUsuario(id, resolveIdSucursal(token)),
                "Detalle cargo usuario."
        ));
    }

    @GetMapping({"/spx_ObtenerSaldoRuta", "/saldo-ruta"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerSaldoRuta(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idRuta", required = false) Integer idRuta,
            @RequestParam(value = "ruta", required = false) Integer ruta,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        Integer idRutaFinal = idRuta != null ? idRuta : ruta;
        if (idRutaFinal == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idRuta o ruta es requerido."
            );
        }
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerSaldoRuta(idRutaFinal, fecha, resolveIdSucursal(token, idSucursal)),
                "Saldo de ruta obtenido correctamente."
        ));
    }

    @PutMapping("/{id}/datos")
    public ResponseEntity<ApiResponse<Integer>> modificarDatos(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id,
            @Valid @RequestBody OtModificarDatosRequest request) {
        int filas = otService.modificarDatosOt(id, request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(
                filas,
                "Datos de OT modificados."
        ));
    }

    @PutMapping("/{id}/fecha")
    public ResponseEntity<ApiResponse<OtModificarFechaResponse>> modificarFecha(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id,
            @Valid @RequestBody OtModificarFechaRequest request) {
        OtModificarFechaResponse response = otService.modificarFecha(id, request, resolveIdSucursal(token));
        return ResponseEntity.ok(ApiResponse.of(
                response,
                "Fecha de OT modificada."
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> anularOt(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id,
            @RequestParam("modo") String modo,
            @RequestParam(value = "usuario", required = false) Integer idUsuario) {
        Integer idSucursal = resolveIdSucursal(token);
        if ("solo_cu".equalsIgnoreCase(modo)) {
            int filas = otService.anularSoloCu(id, idUsuario, idSucursal);
            return ResponseEntity.ok(ApiResponse.of(
                    filas,
                    "Cargo usuario anulado."
            ));
        }
        if ("con_cu".equalsIgnoreCase(modo)) {
            otService.anularConCu(id, idUsuario);
            return ResponseEntity.ok(ApiResponse.of(
                    null,
                    "OT anulada con CU."
            ));
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "modo debe ser con_cu o solo_cu."
        );
    }

    @GetMapping({"/spx_ObtenerCaberaVentaParaRegistroOTwb", "/cabecera-venta/registro-otwb"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerCabeceraVentaParaRegistroOtWb(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("clienteNro") Integer clienteNro,
            @RequestParam("ot") Integer ot,
            @RequestParam("tor") @NotBlank String tor,
            @RequestParam("grupo") @NotBlank String grupo,
            @RequestParam("tecnicoNombre") @NotBlank String tecnicoNombre,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.obtenerCabeceraVentaParaRegistroOtWb(
                        clienteNro,
                        ot,
                        tor,
                        grupo,
                        tecnicoNombre,
                        resolveIdSucursal(token, idSucursal)
                ),
                "Cabecera de venta obtenida correctamente."
        ));
    }

    @GetMapping({"/spx_ValidarVentaYDetallewb", "/venta/validar-detalle"})
    public ResponseEntity<ApiResponse<OtValidarVentaDetalleResponse>> validarVentaYDetalleWb(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("fecha") String fecha,
            @RequestParam("nroOT") Integer nroOT,
            @RequestParam("numeroCliente") Integer numeroCliente,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.validarVentaYDetalleWb(
                        fecha,
                        nroOT,
                        numeroCliente,
                        resolveIdSucursal(token, idSucursal)
                ),
                "Validacion de venta y detalle ejecutada correctamente."
        ));
    }

    @GetMapping({"/spx_ExisteCierreAlmacen", "/validaciones/registro-agenda", "/spx_ValidarRegistroAgenda"})
    public ResponseEntity<ApiResponse<OtRegistroAgendaValidacionResponse>> validarRegistroAgenda(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.of(
                otService.validarRegistroAgenda(fecha, resolveIdSucursal(token)),
                "Validacion de registro de agenda ejecutada correctamente."
        ));
    }

    private Integer resolveIdSucursal(String token) {
        return extractIdSucursal(resolveSession(token));
    }

    private Integer resolveIdSucursal(String token, Integer idSucursalFallback) {
        if (idSucursalFallback != null && idSucursalFallback > 0) {
            try {
                Integer idSucursalSesion = resolveIdSucursal(token);
                return idSucursalSesion != null ? idSucursalSesion : idSucursalFallback;
            } catch (ApiException ex) {
                if (isSesionNoDisponible(ex)) {
                    return idSucursalFallback;
                }
                throw ex;
            }
        }
        Integer idSucursalSesion = resolveIdSucursal(token);
        if (idSucursalSesion != null) {
            return idSucursalSesion;
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Debes enviar X-Session-Token o idSucursal para resolver la base de datos de la sucursal."
        );
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

    private boolean isSesionNoDisponible(ApiException ex) {
        if (ex == null) {
            return false;
        }
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
            return true;
        }
        String code = ex.getCode();
        return code != null && code.startsWith("SESSION_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
