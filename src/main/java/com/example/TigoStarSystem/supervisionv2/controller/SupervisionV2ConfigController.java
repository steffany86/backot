package com.example.TigoStarSystem.supervisionv2.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.supervisionv2.service.SupervisionV2CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** Mantenimiento de catalogos de Supervision V2 (solo BackOffice y Sistemas). */
@Validated
@RestController
@RequestMapping("/supervisor/supervision_v2/config")
public class SupervisionV2ConfigController {
    private final SupervisionV2CatalogoService service;

    public SupervisionV2ConfigController(SupervisionV2CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("catalogo") String catalogo) {
        return ResponseEntity.ok(ApiResponse.of(service.listar(catalogo, token), "Listado del catalogo."));
    }

    @PostMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("catalogo") String catalogo,
            @Valid @RequestBody CatalogoRequest request) {
        service.crear(catalogo, request.getNombre(), token);
        return ResponseEntity.ok(ApiResponse.of(null, "Registro creado."));
    }

    @DeleteMapping("/{catalogo}/{id}")
    public ResponseEntity<ApiResponse<Void>> baja(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("catalogo") String catalogo,
            @PathVariable("id") String id) {
        service.baja(catalogo, id, token);
        return ResponseEntity.ok(ApiResponse.of(null, "Registro dado de baja."));
    }

    public static class CatalogoRequest {
        private String nombre;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }
}
