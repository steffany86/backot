package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.common.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/cuadre")
public class CuadreController {
    private final CuadreService cuadreService;

    public CuadreController(CuadreService cuadreService) {
        this.cuadreService = cuadreService;
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
            @RequestParam("cantidadOt") Integer cantidadOt,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        LocalDate fechaFinal = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.registrarCuadreTecnicoActual(token, fechaFinal, idRuta, observacion, cantidadOt, idSucursal),
                "Cuadre del tecnico registrado correctamente."
        ));
    }

    @GetMapping({"/spx_ValidarCuadreRuta", "/validar-hoy"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarCuadreHoy(
            @RequestParam(value = "ruta", required = false) Integer ruta,
            @RequestParam(value = "idRuta", required = false) Integer idRuta,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
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
                cuadreService.validarCuadreRuta(rutaFinal, fechaFinal, idSucursal),
                "Validacion de cuadre ejecutada correctamente."
        ));
    }

    @PostMapping("/validar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarCuadre(
            @RequestParam("ruta") Integer idRuta,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        if (idRuta == null || fecha == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "ruta y fecha son requeridos."
            );
        }
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.validarCuadreRuta(idRuta, fecha, idSucursal),
                "Validación de cuadre."
        ));
    }
}
