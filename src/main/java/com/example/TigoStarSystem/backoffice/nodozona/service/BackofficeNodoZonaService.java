package com.example.TigoStarSystem.backoffice.nodozona.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.backoffice.nodozona.dto.EstadoCorteTapCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.dto.NodoDistritoCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.dto.NodoZonaCrearRequest;
import com.example.TigoStarSystem.backoffice.nodozona.repository.BackofficeNodoZonaRepository;
import com.example.TigoStarSystem.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BackofficeNodoZonaService {
    private final BackofficeNodoZonaRepository repository;
    private final AuthService authService;

    public BackofficeNodoZonaService(BackofficeNodoZonaRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String token) {
        requireBackoffice(token);
        return repository.listar();
    }

    public List<Map<String, Object>> listarDistrito(String token) {
        requireBackoffice(token);
        return repository.listarDistrito();
    }

    public List<Map<String, Object>> listarEstadoCorteTap(String token) {
        requireBackoffice(token);
        return repository.listarEstadoCorteTap();
    }

    public Map<String, Object> crear(String token, NodoZonaCrearRequest request) {
        AuthLoginResponse user = requireBackoffice(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        String nodosAsociados = toUpper(trimToNull(request.getNodosAsociados()));
        String distrito = trimToNull(request.getDistrito());
        String zona = toUpper(trimToNull(request.getZona()));
        if (nodosAsociados == null || distrito == null || zona == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nodo, distrito y zona son requeridos.");
        }
        return firstRow(repository.crear(nodosAsociados, distrito, zona, resolveUsuario(user)));
    }

    public Map<String, Object> crearDistrito(String token, NodoDistritoCrearRequest request) {
        AuthLoginResponse user = requireBackoffice(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        String nodosAsociados = toUpper(trimToNull(request.getNodosAsociados()));
        String distrito = trimToNull(request.getDistrito());
        String zona = toUpper(trimToNull(request.getZona()));
        String distritoNuevo = toUpper(trimToNull(request.getDistritoNuevo()));
        if (nodosAsociados == null || distrito == null || zona == null || distritoNuevo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nodo, distrito, zona y distrito nuevo son requeridos.");
        }
        return firstRow(repository.crearDistrito(nodosAsociados, distrito, zona, distritoNuevo, resolveUsuario(user)));
    }

    public Map<String, Object> crearEstadoCorteTap(String token, EstadoCorteTapCrearRequest request) {
        AuthLoginResponse user = requireBackoffice(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        String estado = toUpper(trimToNull(request.getEstado()));
        if (estado == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Estado requerido.");
        }
        return firstRow(repository.crearEstadoCorteTap(estado, resolveUsuario(user)));
    }

    public Map<String, Object> eliminar(String token, Integer id) {
        AuthLoginResponse user = requireBackoffice(token);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id requerido.");
        }
        return firstRow(repository.eliminar(id, resolveUsuario(user)));
    }

    public Map<String, Object> eliminarDistrito(String token, Integer id) {
        AuthLoginResponse user = requireBackoffice(token);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id requerido.");
        }
        return firstRow(repository.eliminarDistrito(id, resolveUsuario(user)));
    }

    public Map<String, Object> eliminarEstadoCorteTap(String token, Integer id) {
        AuthLoginResponse user = requireBackoffice(token);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id requerido.");
        }
        return firstRow(repository.eliminarEstadoCorteTap(id, resolveUsuario(user)));
    }

    private AuthLoginResponse requireBackoffice(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse user = me == null ? null : me.getUsuario();
        String role = user == null ? null : user.getRol();
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (!("backoffice".equals(normalized)
                || "backofficev".equals(normalized)
                || "sistemas".equals(normalized)
                || "admin".equals(normalized)
                || "administrador".equals(normalized))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Rol sin acceso a nodo zona.");
        }
        return user;
    }

    private Map<String, Object> firstRow(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DB_ERROR", "El proceso no devolvio resultado.");
        }
        return rows.get(0);
    }

    private String resolveUsuario(AuthLoginResponse user) {
        String login = user == null ? null : trimToNull(user.getLoggin());
        if (login != null) return login;
        String nombre = user == null ? null : trimToNull(user.getNombre());
        if (nombre != null) return nombre;
        return user == null || user.getIdUsuario() == null ? "sistema" : String.valueOf(user.getIdUsuario());
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toUpper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
