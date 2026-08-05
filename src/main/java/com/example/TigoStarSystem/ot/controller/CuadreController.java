package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.ot.service.CuadreAutomaticoJobService;
import com.example.TigoStarSystem.ot.service.CuadreService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/cuadre")
public class CuadreController {
    private final CuadreService cuadreService;
    private final CuadreAutomaticoJobService cuadreAutomaticoJobService;

    public CuadreController(CuadreService cuadreService, CuadreAutomaticoJobService cuadreAutomaticoJobService) {
        this.cuadreService = cuadreService;
        this.cuadreAutomaticoJobService = cuadreAutomaticoJobService;
    }

    @GetMapping({"/tecnico/actual", "/tecnico/mi-cuadre"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerCuadreTecnicoActual(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.obtenerCuadreTecnicoActual(token, fechaFinal, idSucursal),
                "Cuadre del tecnico obtenido correctamente."
        ));
    }

    @PostMapping({"/tecnico/registrar", "/tecnico/actual"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarCuadreTecnicoActual(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("idRuta") Integer idRuta,
            @RequestParam(value = "observacion", required = false) String observacion,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.registrarCuadreTecnicoActual(token, fechaFinal, idRuta, observacion, idSucursal),
                "Cuadre del tecnico registrado correctamente."
        ));
    }

    @GetMapping("/sistemas/automatico/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewCuadreAutomaticoSistemas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.previewCuadreAutomaticoSistemas(token, fechaFinal, idSucursal),
                "Preview de cuadre automatico."
        ));
    }

    @PostMapping("/sistemas/automatico/ejecutar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ejecutarCuadreAutomaticoSistemas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.ejecutarCuadreAutomaticoSistemas(token, fechaFinal, idSucursal),
                "Cuadre automatico ejecutado."
        ));
    }

    @PostMapping("/sistemas/automatico/iniciar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> iniciarCuadreAutomaticoSistemas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreAutomaticoJobService.iniciar(token, fechaFinal, idSucursal),
                "Proceso de cuadre automatico iniciado."
        ));
    }

    @PostMapping("/sistemas/cierre/ejecutar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ejecutarCierreAutomaticoSistemas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.ejecutarCierreAutomaticoSistemas(token, fechaFinal, idSucursal),
                "Cierre automatico ejecutado."
        ));
    }

    @GetMapping("/sistemas/cierres/notificaciones")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> notificacionesCierreAutomaticoSistemas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.obtenerNotificacionesCierreSistemas(token, idSucursal),
                "Notificaciones de cierre automatico."
        ));
    }

    @GetMapping("/sistemas/automatico/progreso/{jobId}")
    public SseEmitter progresoCuadreAutomaticoSistemas(@PathVariable("jobId") String jobId) {
        return cuadreAutomaticoJobService.stream(jobId);
    }

    @GetMapping({"/spx_ValidarCuadreRuta", "/validar-hoy"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarCuadreHoy(
            @RequestParam(value = "ruta", required = false) Integer ruta,
            @RequestParam(value = "idRuta", required = false) Integer idRuta,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Integer rutaFinal = idRuta != null ? idRuta : ruta;
        if (rutaFinal == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "ruta o idRuta es requerido."
            );
        }
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.validarCuadreRuta(rutaFinal, fechaFinal),
                "Validacion de cuadre ejecutada correctamente."
        ));
    }

    @PostMapping("/validar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarCuadre(
            @RequestParam("ruta") Integer idRuta,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (idRuta == null || fecha == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "ruta y fecha son requeridos."
            );
        }
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.validarCuadreRuta(idRuta, fecha),
                "Validación de cuadre."
        ));
    }
}
