package com.example.TigoStarSystem.tor.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.tor.dto.TorRegistroRequest;
import com.example.TigoStarSystem.tor.dto.TorRegistroResponse;
import com.example.TigoStarSystem.tor.repository.TorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TorService {
    private final TorRepository repository;
    private final AuthService authService;

    public TorService(TorRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public TorRegistroResponse registrar(TorRegistroRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        String usuarioRegistra = resolverUsuarioRegistra(usuario);
        if (usuarioRegistra == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo identificar el usuario que registra TOR."
            );
        }

        String detalle = requireUpper(request.getDetalle(), "Detalle es requerido.");
        String tor = requireUpper(request.getTor(), "TOR es requerido.");
        String tipoServicio = requireUpper(request.getTipoServicio(), "Tipo de servicio es requerido.");
        int existentes = repository.contarDetalleExistente(detalle);
        if (existentes > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TOR_DETALLE_DUPLICADO",
                    "No se puede registrar TOR porque el detalle ya existe."
            );
        }

        Integer id = repository.insertar(
                detalle,
                tor,
                tipoServicio,
                usuarioRegistra
        );
        return new TorRegistroResponse(id, usuarioRegistra);
    }

    public List<Map<String, Object>> listarRegistrados(String token) {
        authService.me(token);
        return repository.listarRegistrados();
    }

    private String resolverUsuarioRegistra(AuthLoginResponse usuario) {
        if (usuario == null) {
            return null;
        }
        String loggin = trimToNull(usuario.getLoggin());
        if (loggin != null) {
            return loggin;
        }
        String nombre = trimToNull(usuario.getNombre());
        if (nombre != null) {
            return nombre;
        }
        return usuario.getIdUsuario() == null ? null : String.valueOf(usuario.getIdUsuario());
    }

    private String requireUpper(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    message
            );
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
