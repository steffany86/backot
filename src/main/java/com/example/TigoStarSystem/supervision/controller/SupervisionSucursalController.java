package com.example.TigoStarSystem.supervision.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.supervision.dto.SupervisionSucursalRequest;
import com.example.TigoStarSystem.supervision.service.SupervisionService;
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

@Validated
@RestController
@RequestMapping("/sistemas/supervision-sucursales")
public class SupervisionSucursalController {
    private final SupervisionService service;

    public SupervisionSucursalController(SupervisionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarSupervisionSucursales(token),
                "Listado de sucursales asignadas a usuarios."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody SupervisionSucursalRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crearSupervisionSucursal(request, token),
                "Sucursal asignada correctamente."
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("id") Integer id) {
        service.eliminarSupervisionSucursal(id, token);
        return ResponseEntity.ok(ApiResponse.of(null, "Asignacion eliminada."));
    }
}
