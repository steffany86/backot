package com.example.TigoStarSystem.digitador.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.digitador.dto.DigitadorCruceNoFinalizadoRequest;
import com.example.TigoStarSystem.digitador.service.DigitadorCruceNoFinalizadoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/digitador/cruce-no-finalizado")
public class DigitadorCruceNoFinalizadoController {
    private final DigitadorCruceNoFinalizadoService service;

    public DigitadorCruceNoFinalizadoController(DigitadorCruceNoFinalizadoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "marcados", required = false, defaultValue = "false") Boolean marcados,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        LocalDate desde = fechaDesde == null ? fecha : fechaDesde;
        return ResponseEntity.ok(ApiResponse.of(service.listar(token, desde, fechaHasta, marcados, page, pageSize), "Cruce de ordenes no finalizadas obtenido correctamente."));
    }

    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> estados(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.listarEstados(token), "Estados de digitacion obtenidos correctamente."));
    }

    @PostMapping("/{idHistorial}/guardar")
    public ResponseEntity<ApiResponse<Integer>> actualizar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable Integer idHistorial,
            @RequestBody DigitadorCruceNoFinalizadoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.actualizar(token, idHistorial, request),
                "Digitacion guardada correctamente."
        ));
    }
}
