package com.example.TigoStarSystem.digitador.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.digitador.dto.DigitadorCruceNoFinalizadoRequest;
import com.example.TigoStarSystem.digitador.repository.DigitadorCruceNoFinalizadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

@Service
public class DigitadorCruceNoFinalizadoService {
    private final DigitadorCruceNoFinalizadoRepository repository;
    private final AuthService authService;

    public DigitadorCruceNoFinalizadoService(
            DigitadorCruceNoFinalizadoRepository repository,
            AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public Map<String, Object> listar(String token, LocalDate fechaDesde, LocalDate fechaHasta, Boolean marcados, Integer page, Integer pageSize) {
        requireDigitador(token);
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        LocalDate desde = fechaDesde == null ? LocalDate.now() : fechaDesde;
        LocalDate hasta = fechaHasta == null ? desde : fechaHasta;
        if (hasta.isBefore(desde)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Fecha hasta no puede ser anterior a fecha desde.");
        }
        if (hasta.isAfter(desde.plusDays(30))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El rango maximo permitido es de 31 dias.");
        }
        Map<String, Map<String, Object>> uniqueRows = new LinkedHashMap<>();
        for (LocalDate current = desde; !current.isAfter(hasta); current = current.plusDays(1)) {
            for (Map<String, Object> row : repository.listar(current)) {
                if (isMarcado(row) != Boolean.TRUE.equals(marcados)) {
                    continue;
                }
                String key = String.valueOf(row.getOrDefault("Id_BO_CITA_MAKIRO_Historial", ""))
                        + "|" + String.valueOf(row.getOrDefault("OT_int", ""))
                        + "|" + String.valueOf(row.getOrDefault("cliente_nro", ""));
                uniqueRows.putIfAbsent(key, row);
            }
        }
        List<Map<String, Object>> all = new ArrayList<>(uniqueRows.values());
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int from = Math.min((currentPage - 1) * size, total);
        int to = Math.min(from + size, total);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", all.subList(from, to));
        response.put("page", currentPage);
        response.put("pageSize", size);
        response.put("total", total);
        response.put("totalPages", totalPages);
        return response;
    }

    private boolean isMarcado(Map<String, Object> row) {
        Object value = row.get("Actualizado_DIGITACION_SI_NO");
        if (value == null) {
            value = row.get("Actualizado_DIGITACION");
        }
        String normalized = value == null ? "" : String.valueOf(value).trim().toLowerCase();
        return "si".equals(normalized) || "1".equals(normalized) || "true".equals(normalized);
    }

    public List<Map<String, Object>> listarEstados(String token) {
        requireDigitador(token);
        return repository.listarEstados();
    }

    public int actualizar(String token, Integer idHistorial, DigitadorCruceNoFinalizadoRequest request) {
        AuthLoginResponse usuario = requireDigitador(token);
        if (idHistorial == null || idHistorial <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id de historial es requerido.");
        }
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Los datos de digitacion son requeridos.");
        }
        LocalDate fechaEjecucion = parseDate(request.getFechaEjecuacionDigitacion());
        String estado = required(request.getEstadoDigitacion(), "Estado de digitacion");
        String observacion = required(request.getObservacionDigitacion(), "Observacion de digitacion");
        String usuarioModifica = firstNonBlank(usuario.getLoggin(), usuario.getNombre(), "SISTEMA");
        int updated = repository.actualizar(idHistorial, fechaEjecucion, estado, observacion, usuarioModifica);
        if (updated <= 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DIGITACION_NO_ACTUALIZABLE",
                    "El registro ya fue finalizado, no existe o no pertenece al cruce seleccionado."
            );
        }
        return updated;
    }

    private AuthLoginResponse requireDigitador(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        String rol = usuario == null || usuario.getRol() == null ? "" : usuario.getRol().trim().toLowerCase();
        if (!"digitador".equals(rol) && !"sistemas".equals(rol) && !"admin".equals(rol) && !"central".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_DIGITADOR_ONLY", "Esta funcionalidad es solo para rol Digitador.");
        }
        return usuario;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(required(value, "Fecha de ejecucion"));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La fecha de ejecucion no es valida.");
        }
    }

    private String required(String value, String label) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", label + " es obligatorio.");
        }
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "SISTEMA";
    }
}
