package com.example.TigoStarSystem.cortestap.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.cortestap.dto.CorteTapCrearRequest;
import com.example.TigoStarSystem.cortestap.dto.CorteTapDigitacionRequest;
import com.example.TigoStarSystem.cortestap.dto.CorteTapEjecucionRequest;
import com.example.TigoStarSystem.cortestap.repository.CorteTapRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CorteTapService {
    private final CorteTapRepository repository;
    private final AuthService authService;

    public CorteTapService(CorteTapRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String token) {
        requireUser(token);
        return repository.listar();
    }

    public Map<String, Object> obtenerPorId(String token, Integer id) {
        requireUser(token);
        return requireCorte(id);
    }

    public Map<String, Object> catalogosDigitacion(String token) {
        requireUser(token);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("zonas", repository.listarZonas());
        out.put("distritos", repository.listarDistritos());
        return out;
    }

    public Map<String, Object> resolverZonaHfc(String token, String zonaHfc) {
        requireUser(token);
        return resolverZonaHfcInterno(zonaHfc);
    }

    public Map<String, Object> crear(String token, CorteTapCrearRequest request) {
        AuthLoginResponse user = requireUser(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        String codigoCliente = trimToNull(request.getCodigoCliente());
        String tor = upper(trimToNull(request.getTor()));
        Integer idTecnico = request.getIdTecnico();
        String tecnico = trimToNull(request.getTecnico());
        String sucursal = trimToNull(request.getSucursal());
        String nodoTapBoca = upper(trimToNull(request.getNodoTapBoca()));
        if (codigoCliente == null || tor == null || idTecnico == null || idTecnico <= 0
                || tecnico == null || sucursal == null || nodoTapBoca == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cliente, TOR, tecnico, sucursal y Nodo/TAP/Boca son requeridos.");
        }
        if (!("TE".equals(tor) || "SE".equals(tor))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "TOR debe ser TE o SE.");
        }
        return repository.crear(
                codigoCliente,
                tor,
                idTecnico,
                tecnico,
                sucursal,
                nodoTapBoca,
                resolveUsuario(user),
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public Map<String, Object> guardarDigitacion(String token, Integer id, CorteTapDigitacionRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireCorte(id);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        Map<String, Object> resolucion = resolverZonaHfcInterno(request.getZonaHfc());
        String zonaHfc = upper(trimToNull(request.getZonaHfc()));
        int rows = repository.guardarDigitacion(
                id,
                zonaHfc,
                valueAsString(resolucion.get("zona")),
                valueAsString(resolucion.get("distrito")),
                resolveUsuario(user)
        );
        if (rows <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "No se pudo guardar la digitacion del Corte TAP.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> guardarEjecucion(String token, Integer id, CorteTapEjecucionRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireCorte(id);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos requeridos.");
        }
        String ordenTrabajo = trimToNull(request.getOrdenTrabajo());
        String observacion = trimToNull(request.getObservacion());
        Timestamp fechaEjecucion = parseFechaEjecucion(request.getFechaEjecucion());
        String foto1 = trimToNull(request.getFoto1());
        String foto2 = trimToNull(request.getFoto2());
        if (ordenTrabajo == null || observacion == null || fechaEjecucion == null || (foto1 == null && foto2 == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "OT, observacion, fecha de ejecucion y al menos una foto son requeridos.");
        }
        int rows = repository.guardarEjecucion(id, ordenTrabajo, observacion, foto1, foto2, fechaEjecucion, resolveUsuario(user));
        if (rows <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "El Corte TAP debe estar digitado y no finalizado para ejecutarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> finalizar(String token, Integer id) {
        requireUser(token);
        requireCorte(id);
        int rows = repository.finalizar(id, "sistema");
        if (rows <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_FINALIZADO", "Solo se puede finalizar un Corte TAP ejecutado.");
        }
        return requireCorte(id);
    }

    private Map<String, Object> resolverZonaHfcInterno(String zonaHfc) {
        String raw = upper(trimToNull(zonaHfc));
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Zona HFC requerida.");
        }
        String withoutNodo = raw.startsWith("NODO ") ? raw.substring(5).trim() : raw;
        String withNodo = raw.startsWith("NODO ") ? raw : "NODO " + raw;
        String compactRaw = raw.replace(" ", "");
        String compactWithNodo = withNodo.replace(" ", "");
        List<Map<String, Object>> rows = repository.resolverZonaHfc(raw, withNodo, compactRaw, compactWithNodo);
        if (rows == null || rows.isEmpty()) {
            rows = repository.resolverZonaHfc(withoutNodo, "NODO " + withoutNodo, withoutNodo.replace(" ", ""), ("NODO " + withoutNodo).replace(" ", ""));
        }
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ZONA_HFC_NO_ENCONTRADA", "No se encontro Zona y Distrito para " + raw + ".");
        }
        return rows.get(0);
    }

    private Map<String, Object> requireCorte(Integer id) {
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id requerido.");
        }
        List<Map<String, Object>> rows = repository.obtenerPorId(id);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CORTE_TAP_NO_ENCONTRADO", "No se encontro el Corte TAP.");
        }
        return rows.get(0);
    }

    private AuthLoginResponse requireUser(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse user = me == null ? null : me.getUsuario();
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesion requerida.");
        }
        return user;
    }

    private Timestamp parseFechaEjecucion(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.replace("T", " ");
        try {
            if (normalized.length() == 16) {
                return Timestamp.valueOf(LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            if (normalized.length() == 19) {
                return Timestamp.valueOf(LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            return Timestamp.valueOf(LocalDateTime.parse(trimmed));
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Fecha de ejecucion invalida.");
        }
    }

    private String resolveUsuario(AuthLoginResponse user) {
        String login = user == null ? null : trimToNull(user.getLoggin());
        if (login != null) return login;
        String nombre = user == null ? null : trimToNull(user.getNombre());
        if (nombre != null) return nombre;
        return user == null || user.getIdUsuario() == null ? "sistema" : String.valueOf(user.getIdUsuario());
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
