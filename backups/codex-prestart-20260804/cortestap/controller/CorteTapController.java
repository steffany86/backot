package com.example.TigoStarSystem.cortestap.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.cortestap.dto.CorteTapCrearRequest;
import com.example.TigoStarSystem.cortestap.dto.CorteTapDigitacionRequest;
import com.example.TigoStarSystem.cortestap.dto.CorteTapEjecucionRequest;
import com.example.TigoStarSystem.cortestap.service.CorteTapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cortes-tap")
public class CorteTapController {
    private final CorteTapService service;

    public CorteTapController(CorteTapService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.listar(token), "Listado de cortes TAP."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtener(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(service.obtenerPorId(token, id), "Detalle de Corte TAP."));
    }

    @GetMapping("/catalogos/digitacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalogosDigitacion(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.catalogosDigitacion(token), "Catalogos de Corte TAP."));
    }

    @GetMapping("/resolver-zona-hfc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolverZonaHfc(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("zonaHfc") String zonaHfc) {
        return ResponseEntity.ok(ApiResponse.of(service.resolverZonaHfc(token, zonaHfc), "Zona HFC resuelta."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody CorteTapCrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.crear(token, request), "Corte TAP creado."));
    }

    @PutMapping("/{id}/digitacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guardarDigitacion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id,
            @RequestBody CorteTapDigitacionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.guardarDigitacion(token, id, request), "Digitacion de Corte TAP guardada."));
    }

    @PutMapping("/{id}/ejecucion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guardarEjecucion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id,
            @RequestBody CorteTapEjecucionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.guardarEjecucion(token, id, request), "Ejecucion de Corte TAP guardada."));
    }

    @PutMapping("/{id}/finalizacion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(service.finalizar(token, id), "Corte TAP finalizado."));
    }
}
