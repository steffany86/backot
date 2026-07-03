package com.example.TigoStarSystem.backoffice.nombresnps.controller;

import com.example.TigoStarSystem.backoffice.nombresnps.dto.ActualizarNombreNpsRequest;
import com.example.TigoStarSystem.backoffice.nombresnps.service.BackofficeNombresNpsService;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backoffice/nombres-nps")
public class BackofficeNombresNpsController {
    private final BackofficeNombresNpsService service;

    public BackofficeNombresNpsController(BackofficeNombresNpsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token),
                "Listado de nombres NPS por sucursal."
        ));
    }

    @PostMapping("/{sucursal}/obtener")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerNombresNps(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("sucursal") String sucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.obtenerYGuardar(token, sucursal),
                "Proceso de nombres NPS ejecutado."
        ));
    }

    @PatchMapping("/{sucursal}/vendedores/{idVendedor}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> actualizarManual(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("sucursal") String sucursal,
            @PathVariable("idVendedor") Integer idVendedor,
            @RequestBody ActualizarNombreNpsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.actualizarManual(token, sucursal, idVendedor, request == null ? null : request.getNombreNps()),
                "Nombre NPS actualizado."
        ));
    }
}
