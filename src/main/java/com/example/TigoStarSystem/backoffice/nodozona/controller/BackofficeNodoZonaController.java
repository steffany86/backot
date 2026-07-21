package com.example.TigoStarSystem.backoffice.nodozona.controller;

import com.example.TigoStarSystem.backoffice.nodozona.dto.NodoZonaCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.dto.NodoDistritoCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.dto.EstadoCorteTapCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.service.BackofficeNodoZonaService;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backoffice/nodo-zona")
public class BackofficeNodoZonaController {
    private final BackofficeNodoZonaService service;

    public BackofficeNodoZonaController(BackofficeNodoZonaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token),
                "Listado de nodo zona."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody NodoZonaCrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crear(token, request),
                "Nodo zona creado."
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eliminar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(
                service.eliminar(token, id),
                "Nodo zona eliminado."
        ));
    }

    @GetMapping("/distrito")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarDistrito(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarDistrito(token),
                "Listado de nodo distrito."
        ));
    }

    @PostMapping("/distrito")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearDistrito(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody NodoDistritoCrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crearDistrito(token, request),
                "Nodo distrito creado."
        ));
    }

    @DeleteMapping("/distrito/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eliminarDistrito(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(
                service.eliminarDistrito(token, id),
                "Nodo distrito eliminado."
        ));
    }

    @GetMapping("/estado-corte-tap")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarEstadoCorteTap(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarEstadoCorteTap(token),
                "Listado de estado corte TAP."
        ));
    }

    @PostMapping("/estado-corte-tap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearEstadoCorteTap(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestBody EstadoCorteTapCrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crearEstadoCorteTap(token, request),
                "Estado corte TAP creado."
        ));
    }

    @DeleteMapping("/estado-corte-tap/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eliminarEstadoCorteTap(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(
                service.eliminarEstadoCorteTap(token, id),
                "Estado corte TAP eliminado."
        ));
    }
}
