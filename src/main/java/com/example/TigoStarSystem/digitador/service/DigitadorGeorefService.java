package com.example.TigoStarSystem.digitador.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.digitador.dto.DigitadorGeorefConfirmRequest;
import com.example.TigoStarSystem.digitador.repository.DigitadorGeorefRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DigitadorGeorefService {
    private final DigitadorGeorefRepository repository;
    private final AuthService authService;

    public DigitadorGeorefService(DigitadorGeorefRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String token, LocalDate fecha) {
        requireDigitador(token);
        LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
        return repository.listarAnalisisDistancias(fechaConsulta);
    }

    public int confirmar(String token, Long id, DigitadorGeorefConfirmRequest request) {
        AuthLoginResponse usuario = requireDigitador(token);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "id es requerido.");
        }
        boolean confirmarUbicacion = request != null && Boolean.TRUE.equals(request.getConfirmarUbicacion());
        boolean confirmarNodo = request != null && Boolean.TRUE.equals(request.getConfirmarNodo());
        if (!confirmarUbicacion && !confirmarNodo) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Debe seleccionar confirmar ubicacion, confirmar NODO o ambos."
            );
        }
        String usuarioModifica = trimToNull(usuario == null ? null : usuario.getLoggin());
        if (usuarioModifica == null) {
            usuarioModifica = trimToNull(usuario == null ? null : usuario.getNombre());
        }
        if (usuarioModifica == null) {
            usuarioModifica = "SISTEMA";
        }
        int updated = repository.confirmarAnalisisDistancia(id, confirmarUbicacion, confirmarNodo, usuarioModifica);
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro el registro para confirmar.");
        }
        return updated;
    }

    private AuthLoginResponse requireDigitador(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me.getUsuario();
        String rol = normalize(usuario == null ? null : usuario.getRol());
        if (!"digitador".equals(rol)
                && !"sistemas".equals(rol)
                && !"admin".equals(rol)
                && !"central".equals(rol)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_DIGITADOR_ONLY",
                    "Esta funcionalidad es solo para rol Digitador."
            );
        }
        return usuario;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[\\s_]+", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
