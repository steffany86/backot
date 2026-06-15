package com.example.TigoStarSystem.digitador.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.digitador.service.DigitadorGeorefService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/digitador/georef")
public class DigitadorGeorefController {
    private final DigitadorGeorefService service;

    public DigitadorGeorefController(DigitadorGeorefService service) {
        this.service = service;
    }

    @GetMapping("/distancias")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token, fecha),
                "Analisis de distancias obtenido correctamente."
        ));
    }

    @PostMapping("/distancias/{id}/confirmar")
    public ResponseEntity<ApiResponse<Integer>> confirmar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.of(
                service.confirmar(token, id),
                "Registro confirmado correctamente."
        ));
    }
}
