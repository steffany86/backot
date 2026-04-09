package com.example.TigoStarSystem.privilegios.controller;

import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioActualizarRequest;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioRolDetalleResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioRolResponse;
import com.example.TigoStarSystem.privilegios.service.PrivilegioService;
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

@Validated
@RestController
@RequestMapping("/admin/privilegios")
public class PrivilegioAdminController {
    private final AuthService authService;
    private final PrivilegioService privilegioService;

    public PrivilegioAdminController(AuthService authService, PrivilegioService privilegioService) {
        this.authService = authService;
        this.privilegioService = privilegioService;
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<PrivilegioRolResponse>>> listarRoles(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.of(
                privilegioService.listarRoles(),
                "Listado de roles."
        ));
    }

    @GetMapping("/roles/{idRol}/menu")
    public ResponseEntity<ApiResponse<PrivilegioRolDetalleResponse>> obtenerPrivilegiosPorRol(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idRol") Integer idRol) {
        authService.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.of(
                privilegioService.obtenerPrivilegiosPorRol(idRol),
                "Listado de privilegios por rol."
        ));
    }

    @PutMapping("/roles/{idRol}/menu")
    public ResponseEntity<ApiResponse<PrivilegioRolDetalleResponse>> actualizarPrivilegiosRol(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idRol") Integer idRol,
            @Valid @RequestBody PrivilegioActualizarRequest request) {
        authService.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.of(
                privilegioService.actualizarPrivilegiosRol(idRol, request.getMenuIds()),
                "Privilegios actualizados."
        ));
    }

    @PutMapping("/roles/{idRol}/preset/supervisor-cuadrillas")
    public ResponseEntity<ApiResponse<PrivilegioRolDetalleResponse>> aplicarPresetSupervisorCuadrillas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idRol") Integer idRol) {
        authService.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.of(
                privilegioService.aplicarPresetSupervisorCuadrillas(idRol),
                "Preset de Supervisor Cuadrillas aplicado."
        ));
    }
}
