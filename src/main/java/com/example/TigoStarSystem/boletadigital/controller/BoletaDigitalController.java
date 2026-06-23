package com.example.TigoStarSystem.boletadigital.controller;

import com.example.TigoStarSystem.boletadigital.service.BoletaDigitalService;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping({"/boleta-digital", "/VerificacionBoletaDigital"})
public class BoletaDigitalController {
    private final BoletaDigitalService service;

    public BoletaDigitalController(BoletaDigitalService service) {
        this.service = service;
    }

    @GetMapping({"/ots", "/listado"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token),
                "Listado de OT con archivo digital."
        ));
    }

    @GetMapping("/archivo")
    public ResponseEntity<Resource> archivo(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("ruta") String ruta,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean download) {
        BoletaDigitalService.ArchivoPdf file = service.cargarArchivo(token, ruta);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.getFileName())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.getResource());
    }
}
