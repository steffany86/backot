package com.example.TigoStarSystem.cortetap.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.cortetap.dto.CorteTapCrearRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapDigitacionRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapEjecucionRequest;
import com.example.TigoStarSystem.cortetap.service.CorteTapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cortes-tap")
public class CorteTapController {
    private final CorteTapService service;

    public CorteTapController(CorteTapService service) {
        this.service = service;
    }

    @GetMapping("/catalogos/digitacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalogosDigitacion(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.catalogosDigitacion(token),
                "Catalogos de digitacion de Corte TAP."
        ));
    }

    @GetMapping("/resolver-zona-hfc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolverZonaHfc(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam String zonaHfc) {
        return ResponseEntity.ok(ApiResponse.of(
                service.resolverZonaHfc(token, zonaHfc),
                "Zona HFC resuelta."
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detalle(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.of(service.detalle(token, id), "Detalle de Corte TAP."));
    }

    @PutMapping("/{id}/digitacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guardarDigitacion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable Integer id,
            @Valid @RequestBody CorteTapDigitacionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.guardarDigitacion(token, id, request),
                "Digitacion de Corte TAP guardada."
        ));
    }

    @PutMapping("/{id}/ejecucion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guardarEjecucion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable Integer id,
            @Valid @RequestBody CorteTapEjecucionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.guardarEjecucion(token, id, request),
                "Ejecucion de Corte TAP guardada."
        ));
    }

    @PutMapping("/{id}/finalizacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.of(
                service.finalizar(token, id),
                "Corte TAP finalizado."
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token),
                "Listado de cortes TAP."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody CorteTapCrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crear(token, request),
                "Corte TAP creado."
        ));
    }
}
