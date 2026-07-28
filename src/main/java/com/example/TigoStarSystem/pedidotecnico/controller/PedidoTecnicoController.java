package com.example.TigoStarSystem.pedidotecnico.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.pedidotecnico.dto.PedidoTecnicoCrearRequest;
import com.example.TigoStarSystem.pedidotecnico.service.PedidoTecnicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/pedidos-tecnico")
public class PedidoTecnicoController {
    private final PedidoTecnicoService service;

    public PedidoTecnicoController(PedidoTecnicoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token, idSucursal),
                "Listado de pedidos de tecnico."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> crear(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody PedidoTecnicoCrearRequest request,
            @RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crear(token, request, idSucursal),
                "Pedido de material registrado."
        ));
    }
}
