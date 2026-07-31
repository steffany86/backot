package com.example.TigoStarSystem.supervisor.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.supervisor.dto.CruceVerificaBackRequest;
import com.example.TigoStarSystem.supervisor.service.EstadoDiaBoCitaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/supervisor")
public class EstadoDiaBoCitaController {
    private final EstadoDiaBoCitaService service;

    public EstadoDiaBoCitaController(EstadoDiaBoCitaService service) {
        this.service = service;
    }

    @GetMapping("/spy_Ultimo_Estado_Dia_BO_CITA_MAKIRO")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerUltimoEstadoDia(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "tecnico", required = false) String tecnico) {
        return ResponseEntity.ok(ApiResponse.of(
                service.consultarUltimoEstadoDia(fecha, tecnico, token),
                "Consulta ejecutada en BDControlOrdenes."
        ));
    }

    @GetMapping("/cruce-ordenes-agenda-makiro")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerCruceOrdenesAgendaMakiro(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.of(
                service.consultarCruceOrdenesAgendaMakiro(fecha, token),
                "Cruce Agenda vs Makiro ejecutado en BDControlOrdenes."
        ));
    }

    @PostMapping("/cruce-ordenes-agenda-makiro/{idHistorial}/verifica-back")
    public ResponseEntity<ApiResponse<Map<String, Object>>> marcarVerificaBack(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idHistorial") Integer idHistorial,
            @RequestBody(required = false) CruceVerificaBackRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.marcarVerificaBack(idHistorial, request, token),
                "Cruce marcado como revisado por BackOffice."
        ));
    }
}
