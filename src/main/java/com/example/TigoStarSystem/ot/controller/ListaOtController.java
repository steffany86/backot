package com.example.TigoStarSystem.ot.controller;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.ot.service.ListaOtService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/ListaOt")
public class ListaOtController {
    private final ListaOtService listaOtService;
    private final AuthService authService;

    public ListaOtController(ListaOtService listaOtService, AuthService authService) {
        this.listaOtService = listaOtService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "rol", required = false) String rol,
            @RequestParam(value = "tecnico", required = false) String tecnico,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "estados", required = false) List<String> estados) {
        AuthMeResponse me = resolveSession(token);
        String rolResuelto = resolveRol(me, rol);
        boolean administrador = isAdministrador(me, rolResuelto);
        boolean tecnicoRol = isTecnico(rolResuelto);

        String tecnicoFiltro = tecnico;
        boolean tecnicoExacto = false;

        if (tecnicoRol && !administrador) {
            tecnicoFiltro = resolveTecnicoPropio(me);
            tecnicoExacto = true;
        }

        List<String> estadosFiltro = resolveEstados(estado, estados);
        List<Map<String, Object>> data = listaOtService.listar(
                fecha,
                tecnicoFiltro,
                tecnicoExacto,
                estadosFiltro
        );
        return ResponseEntity.ok(ApiResponse.of(data, "Listado de OT (SP BO CITA MAKIRO)."));
    }

    private AuthMeResponse resolveSession(String token) {
        if (isBlank(token)) {
            return null;
        }
        return authService.me(token);
    }

    private String resolveRol(AuthMeResponse me, String rolParam) {
        if (me != null && me.getUsuario() != null && !isBlank(me.getUsuario().getRol())) {
            return me.getUsuario().getRol();
        }
        if (!isBlank(rolParam)) {
            return rolParam;
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "rol es requerido cuando no se envia sesion."
        );
    }

    private boolean isAdministrador(AuthMeResponse me, String rolResuelto) {
        if (me != null && me.getUsuario() != null && authService.esAdministrador(me.getUsuario())) {
            return true;
        }
        String normalized = normalizeText(rolResuelto);
        return "sistemas".equals(normalized)
                || "admin".equals(normalized)
                || "administrador".equals(normalized);
    }

    private boolean isTecnico(String rol) {
        String normalized = normalizeText(rol);
        return normalized != null
                && (normalized.contains("tecnico") || "tec".equals(normalized) || normalized.contains("tech"));
    }

    private String resolveTecnicoPropio(AuthMeResponse me) {
        if (me != null && me.getUsuario() != null && !isBlank(me.getUsuario().getNombre())) {
            return me.getUsuario().getNombre();
        }
        throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "SESSION_REQUIRED",
                "Para rol tecnico se requiere token de sesion valido."
        );
    }

    private List<String> resolveEstados(String estado, List<String> estados) {
        List<String> out = new ArrayList<>();
        if (!isBlank(estado)) {
            String[] parts = estado.split(",");
            for (String item : parts) {
                if (!isBlank(item)) {
                    out.add(item.trim());
                }
            }
        }
        if (estados != null && !estados.isEmpty()) {
            for (String item : estados) {
                if (!isBlank(item)) {
                    out.add(item.trim());
                }
            }
        }
        return out;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
