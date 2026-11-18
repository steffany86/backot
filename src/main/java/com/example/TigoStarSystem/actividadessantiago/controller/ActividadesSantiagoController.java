package com.example.TigoStarSystem.actividadessantiago.controller;

import com.example.TigoStarSystem.actividadessantiago.dto.ActividadSantiagoDiaRequest;
import com.example.TigoStarSystem.actividadessantiago.dto.ActividadSantiagoImportarRequest;
import com.example.TigoStarSystem.actividadessantiago.dto.ActividadSantiagoRegistroRequest;
import com.example.TigoStarSystem.actividadessantiago.service.ActividadesSantiagoService;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/** Control de actividades de Santiago (solo sistemas). */
@Validated
@RestController
@RequestMapping("/actividades-santiago")
public class ActividadesSantiagoController {
    private final ActividadesSantiagoService service;

    public ActividadesSantiagoController(ActividadesSantiagoService service) {
        this.service = service;
    }

    @GetMapping("/datos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> datos(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.datos(token), "Registros de actividades."));
    }

    @PutMapping("/registro")
    public ResponseEntity<ApiResponse<Boolean>> guardarRegistro(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody ActividadSantiagoRegistroRequest request) {
        service.guardarRegistro(request, token);
        return ResponseEntity.ok(ApiResponse.of(Boolean.TRUE, "Registro guardado."));
    }

    @PutMapping("/dia")
    public ResponseEntity<ApiResponse<Boolean>> guardarDia(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody ActividadSantiagoDiaRequest request) {
        service.guardarDia(request, token);
        return ResponseEntity.ok(ApiResponse.of(Boolean.TRUE, "Notas del dia guardadas."));
    }

    @PostMapping("/importar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody ActividadSantiagoImportarRequest request) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("importados", service.importar(request.getRegistros(), token));
        return ResponseEntity.ok(ApiResponse.of(out, "Registros importados."));
    }
}
