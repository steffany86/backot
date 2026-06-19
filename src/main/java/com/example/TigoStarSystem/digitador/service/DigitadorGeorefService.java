package com.example.TigoStarSystem.digitador.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
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

    public int confirmar(String token, Long id) {
        requireDigitador(token);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "id es requerido.");
        }
        int updated = repository.confirmarAnalisisDistancia(id);
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro el registro para confirmar.");
        }
        return updated;
    }

    private void requireDigitador(String token) {
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
}
