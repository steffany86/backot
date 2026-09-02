package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.repository.EstadoDiaBoCitaRepository;
import com.example.TigoStarSystem.supervisor.dto.CruceAgendaMakiroVerificaBackRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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

    public int marcarVerificaBack(Integer idHistorial, CruceAgendaMakiroVerificaBackRequest request, String token) {
        AuthMeResponse me = authService.me(requireToken(token));
        String rol = me.getUsuario() == null || me.getUsuario().getRol() == null
                ? ""
                : me.getUsuario().getRol().trim().toLowerCase().replace(" ", "").replace("_", "");
        if (!"backoffice".equals(rol) && !"backofficev".equals(rol) && !"sistemas".equals(rol)
                && !"admin".equals(rol) && !"administrador".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Solo Back Office puede verificar este registro.");
        }
        if (idHistorial == null || idHistorial <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id de historial es requerido.");
        }
        String actualizado = request == null || request.getActualizado() == null
                ? "SI"
                : request.getActualizado().trim().toUpperCase();
        if (!"SI".equals(actualizado) && !"NO".equals(actualizado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Actualizado_VERIFICABACK debe ser SI o NO.");
        }
        String usuario = me.getUsuario() == null ? "" : me.getUsuario().getLoggin();
        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = me.getUsuario() == null ? "" : me.getUsuario().getNombre();
        }
        int actualizadoDb = 1;
        return repository.marcarVerificaBack(idHistorial, actualizadoDb, usuario, request == null ? "" : request.getObservacion());
    }

    private String requireToken(String token) {
        if (trimToNull(token) == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Debes enviar un X-Session-Token valido.");
        }
        return token;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
