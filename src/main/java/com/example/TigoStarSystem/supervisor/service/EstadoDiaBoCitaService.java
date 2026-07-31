package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.dto.CruceVerificaBackRequest;
import com.example.TigoStarSystem.supervisor.repository.EstadoDiaBoCitaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EstadoDiaBoCitaService {
    private final EstadoDiaBoCitaRepository repository;
    private final AuthService authService;

    public EstadoDiaBoCitaService(EstadoDiaBoCitaRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> consultarUltimoEstadoDia(LocalDate fecha, String tecnico, String token) {
        String tecnicoResuelto = resolveTecnico(tecnico, token);
        LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
        return repository.obtenerUltimoEstadoDia(fechaConsulta, tecnicoResuelto);
    }

    public List<Map<String, Object>> consultarCruceOrdenesAgendaMakiro(LocalDate fecha, String token) {
        if (trimToNull(token) == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_REQUIRED",
                    "Debes enviar un X-Session-Token valido."
            );
        }
        authService.me(token);
        LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
        return repository.obtenerCruceOrdenesAgendaMakiro(fechaConsulta);
    }

    @Transactional
    public Map<String, Object> marcarVerificaBack(Integer idHistorial, CruceVerificaBackRequest request, String token) {
        AuthMeResponse me = validarSesion(token);
        AuthLoginResponse usuarioSesion = me == null ? null : me.getUsuario();
        String rol = usuarioSesion == null ? null : usuarioSesion.getRol();
        if (!esBackoffice(rol)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Solo BackOffice puede marcar la revision del cruce."
            );
        }
        if (idHistorial == null || idHistorial <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id de historial requerido.");
        }
        String observacion = request == null ? null : trimToNull(request.getObservacion());
        if (observacion == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Observacion requerida.");
        }
        String login = usuarioSesion == null ? null : trimToNull(usuarioSesion.getLoggin());
        if (login == null && usuarioSesion != null) {
            login = trimToNull(usuarioSesion.getNombre());
        }
        if (login == null) {
            login = "SISTEMA";
        }

        int updated = repository.marcarVerificaBack(idHistorial, login, observacion);
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "HISTORIAL_NOT_FOUND", "No se encontro el cruce para marcar.");
        }
        return repository.obtenerVerificaBack(idHistorial);
    }

    private String resolveTecnico(String tecnico, String token) {
        String tecnicoParam = trimToNull(tecnico);
        if (tecnicoParam != null) {
            return tecnicoParam;
        }
        if (trimToNull(token) == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Debes enviar el parametro tecnico o un X-Session-Token valido."
            );
        }
        AuthMeResponse me = authService.me(token);
        String tecnicoSesion = me == null || me.getUsuario() == null
                ? null
                : trimToNull(me.getUsuario().getNombre());
        if (tecnicoSesion == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "No se pudo resolver el tecnico desde la sesion."
            );
        }
        return tecnicoSesion;
    }

    private AuthMeResponse validarSesion(String token) {
        if (trimToNull(token) == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_REQUIRED",
                    "Debes enviar un X-Session-Token valido."
            );
        }
        return authService.me(token);
    }

    private boolean esBackoffice(String rol) {
        String normalized = trimToNull(rol);
        if (normalized == null) {
            return false;
        }
        normalized = normalized.toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "");
        return "backoffice".equals(normalized) || "backofficev".equals(normalized);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
