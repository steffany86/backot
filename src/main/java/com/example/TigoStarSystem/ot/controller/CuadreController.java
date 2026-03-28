package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.ot.service.CuadreService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/cuadre")
public class CuadreController {
    private final CuadreService cuadreService;

    public CuadreController(CuadreService cuadreService) {
        this.cuadreService = cuadreService;
    }

    @PostMapping("/validar")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> validarCuadre(
            @RequestParam("ruta") Integer idRuta,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (idRuta == null || fecha == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "ruta y fecha son requeridos."
            );
        }
        return ResponseEntity.ok(ApiResponse.of(
                cuadreService.validarCuadreRuta(idRuta, fecha),
                "Validación de cuadre."
        ));
    }
}
