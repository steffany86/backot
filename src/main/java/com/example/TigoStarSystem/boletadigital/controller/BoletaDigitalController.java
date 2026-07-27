package com.example.TigoStarSystem.boletadigital.controller;

import com.example.TigoStarSystem.boletadigital.service.BoletaDigitalService;
import com.example.TigoStarSystem.boletadigital.dto.BoletaDigitalConfirmBoletaRequest;
import com.example.TigoStarSystem.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(token, fechaInicio, fechaFin),
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

    @PostMapping(value = "/archivo-digital", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> cambiarArchivo(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idVenta", required = false) Integer idVenta,
            @RequestParam(value = "id_venta", required = false) Integer idVentaSnake,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.of(
                service.cambiarArchivoDigital(
                        token,
                        idVenta != null ? idVenta : idVentaSnake,
                        archivo != null ? archivo : file
                ),
                "Archivo digital actualizado."
        ));
    }

    @PostMapping("/renombrar-archivo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> renombrarArchivo(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idVenta", required = false) Integer idVenta,
            @RequestParam(value = "id_venta", required = false) Integer idVentaSnake,
            @RequestParam("nombreArchivo") String nombreArchivo) {
        return ResponseEntity.ok(ApiResponse.of(
                service.renombrarArchivoDigital(
                        token,
                        idVenta != null ? idVenta : idVentaSnake,
                        nombreArchivo
                ),
                "Archivo digital renombrado."
        ));
    }

    @PostMapping("/todo-ok")
    public ResponseEntity<ApiResponse<Map<String, Object>>> marcarTodoOk(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idVenta", required = false) Integer idVenta,
            @RequestParam(value = "id_venta", required = false) Integer idVentaSnake,
            @RequestParam(value = "todoOk", required = false, defaultValue = "true") boolean todoOk) {
        return ResponseEntity.ok(ApiResponse.of(
                service.marcarTodoOk(
                        token,
                        idVenta != null ? idVenta : idVentaSnake,
                        todoOk
                ),
                "Boleta marcada como Todo OK."
        ));
    }

    @PostMapping("/confirmar-boleta")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmarBoleta(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idVenta", required = false) Integer idVenta,
            @RequestParam(value = "id_venta", required = false) Integer idVentaSnake,
            @RequestBody(required = false) BoletaDigitalConfirmBoletaRequest request) {
        Integer requestId = request != null && request.getIdVenta() != null ? request.getIdVenta() : request != null ? request.getId_venta() : null;
        return ResponseEntity.ok(ApiResponse.of(
                service.confirmarBoleta(
                        token,
                        idVenta != null ? idVenta : idVentaSnake != null ? idVentaSnake : requestId
                ),
                "Boleta confirmada correctamente."
        ));
    }
}
