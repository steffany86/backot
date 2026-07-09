package com.example.TigoStarSystem.supervision.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.supervision.dto.SupervisionCrearPendienteRequest;
import com.example.TigoStarSystem.supervision.service.SupervisionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/backoffice/supervision")
public class BackOfficeSupervisionController {
    private final SupervisionService service;

    public BackOfficeSupervisionController(SupervisionService service) {
        this.service = service;
    }

    @PostMapping("/pendiente")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearPendiente(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody SupervisionCrearPendienteRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.registrarPendiente(request, token),
                "Supervision pendiente creada exitosamente."
        ));
    }

    @GetMapping("/listado")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarPorEstado(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "estado", required = false, defaultValue = "pendiente") String estado,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "limite", required = false) Integer limite) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarBackofficePorEstado(estado, fechaDesde, fechaHasta, limite, token),
                "Listado de supervisiones por estado."
        ));
    }

    @GetMapping("/supervisores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarSupervisores(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "sucursal", required = false) String sucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarSupervisores(sucursal, token),
                "Listado de supervisores."
        ));
    }

    @GetMapping("/filtros/supervisores")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarSupervisoresFiltro(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "sucursal", required = false) String sucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarSupervisores(sucursal, token),
                "Listado de supervisores."
        ));
    }

    @GetMapping("/tecnicos-por-supervisor")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTecnicosPorSupervisor(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "idSupervisor", required = true) Integer idSupervisor,
            @RequestParam(value = "sucursal", required = false) String sucursal,
            @RequestParam(value = "supervisor", required = false) String supervisor) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursal, supervisor),
                "Listado de tecnicos del supervisor."
        ));
    }

    @GetMapping("/jornadas/historico")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarHistoricoJornadas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "sucursal", required = false) String sucursal,
            @RequestParam(value = "idTecnico", required = false) Integer idTecnico) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarHistoricoJornadasBackoffice(fecha, fechaDesde, fechaHasta, sucursal, idTecnico, token),
                "Historico de inicios y cierres de jornada."
        ));
    }

    @GetMapping("/jornadas/{idInicio}/detalle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerDetalleJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio) {
        return ResponseEntity.ok(ApiResponse.of(
                service.obtenerDetalleInicioJornada(idInicio, token),
                "Detalle de inicio de jornada."
        ));
    }

    @GetMapping("/jornadas/{idInicio}/imagen-auxiliar")
    public ResponseEntity<byte[]> obtenerImagenAuxiliarJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio,
            @RequestParam(value = "miniatura", required = false, defaultValue = "true") boolean miniatura) {
        SupervisionService.JornadaImagen imagen = service.obtenerImagenAuxiliarInicioJornada(idInicio, miniatura, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(imagen.getBytes());
    }
}
