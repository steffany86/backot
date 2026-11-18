package com.example.TigoStarSystem.inventariodpto.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.inventariodpto.dto.InventarioDptoFotoRequest;
import com.example.TigoStarSystem.inventariodpto.dto.InventarioDptoItemRequest;
import com.example.TigoStarSystem.inventariodpto.service.InventarioDptoService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/** Inventario del Dpto 91 (solo usuario stefany). */
@Validated
@RestController
@RequestMapping("/inventario-dpto-91")
public class InventarioDptoController {
    private final InventarioDptoService service;

    public InventarioDptoController(InventarioDptoService service) {
        this.service = service;
    }

    @GetMapping("/datos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> datos(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.datos(token), "Inventario."));
    }

    @PutMapping("/item")
    public ResponseEntity<ApiResponse<Boolean>> guardarItem(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody InventarioDptoItemRequest request) {
        service.guardarItem(request, token);
        return ResponseEntity.ok(ApiResponse.of(Boolean.TRUE, "Articulo guardado."));
    }

    @PostMapping("/foto")
    public ResponseEntity<ApiResponse<Map<String, Object>>> agregarFoto(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody InventarioDptoFotoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.agregarFoto(request, token), "Foto guardada."));
    }

    @GetMapping("/foto/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerFoto(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.of(service.obtenerFoto(id, token), "Foto."));
    }

    @DeleteMapping("/foto/{id}")
    public ResponseEntity<ApiResponse<Boolean>> eliminarFoto(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        service.eliminarFoto(id, token);
        return ResponseEntity.ok(ApiResponse.of(Boolean.TRUE, "Foto eliminada."));
    }
}
