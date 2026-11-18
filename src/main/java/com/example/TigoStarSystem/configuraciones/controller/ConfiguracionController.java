package com.example.TigoStarSystem.configuraciones.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.configuraciones.dto.ConfiguracionUpdateRequest;
import com.example.TigoStarSystem.configuraciones.service.ConfiguracionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/configuraciones")
public class ConfiguracionController {
    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    /** Solo sistemas: lista todas las configuraciones con su valor actual. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.listar(token), "Listado de configuraciones."));
    }

    /** Cualquier usuario con sesion: lee una configuracion puntual. */
    @GetMapping("/{clave}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtener(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("clave") String clave) {
        return ResponseEntity.ok(ApiResponse.of(service.obtener(clave, token), "Configuracion."));
    }

    /** Solo sistemas: cambia el valor. */
    @PutMapping("/{clave}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> actualizar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("clave") String clave,
            @Valid @RequestBody ConfiguracionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.actualizar(clave, request.getValor(), token), "Configuracion actualizada."));
    }
}
