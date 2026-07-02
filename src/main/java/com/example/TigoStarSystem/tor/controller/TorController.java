package com.example.TigoStarSystem.tor.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.tor.dto.TorRegistroRequest;
import com.example.TigoStarSystem.tor.dto.TorRegistroResponse;
import com.example.TigoStarSystem.tor.service.TorService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/tor")
public class TorController {
    private final TorService service;

    public TorController(TorService service) {
        this.service = service;
    }

    @PostMapping({"/registro", "/Registro_TOR"})
    public ResponseEntity<ApiResponse<TorRegistroResponse>> registrar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody TorRegistroRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.registrar(request, token),
                "TOR registrado correctamente."
        ));
    }

    @GetMapping({"/registrados", "/Registro_TOR/registrados"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarRegistrados(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarRegistrados(token),
                "Listado de TOR registrados."
        ));
    }
}
