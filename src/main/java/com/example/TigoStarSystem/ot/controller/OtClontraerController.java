package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.ot.service.OtClontraerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/ot-clon")
public class OtClontraerController {
    private final OtClontraerService otClontraerService;

    public OtClontraerController(OtClontraerService otClontraerService) {
        this.otClontraerService = otClontraerService;
    }

    @GetMapping("/por-tecnico")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listarPorTecnico(
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("tecnico") String tecnico) {
        Map<String, Object> payload = otClontraerService.listarPorTecnico(fecha, tecnico);
        return ResponseEntity.ok(ApiResponse.of(payload, "Listado OT con trazabilidad de SP."));
    }
}
