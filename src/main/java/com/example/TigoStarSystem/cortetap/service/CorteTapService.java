package com.example.TigoStarSystem.cortetap.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.cortetap.dto.CorteTapCrearRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapDigitacionRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapEjecucionRequest;
import com.example.TigoStarSystem.cortetap.repository.CorteTapRepository;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CorteTapService {
    private static final Pattern ZONA_HFC_PATTERN = Pattern.compile("(?i)\\b(?:NODO\\s+)?([A-Z]{3}\\d{3,4})\\b");
    private static final int MAX_PHOTO_LENGTH = 8_000_000;
    private final CorteTapRepository repository;
    private final AuthService authService;
    private final OtRepository otRepository;

    public CorteTapService(CorteTapRepository repository, AuthService authService, OtRepository otRepository) {
        this.repository = repository;
        this.authService = authService;
        this.otRepository = otRepository;
    }

    public List<Map<String, Object>> listar(String token) {
        AuthLoginResponse user = requireUser(token);
        if (canViewAll(user)) {
            return repository.listar();
        }
        Set<Integer> idsTecnico = resolveIdsTecnico(user);
        String nombreTecnico = normalize(user.getNombre());
        List<Map<String, Object>> filtradas = new ArrayList<>();
        for (Map<String, Object> row : repository.listar()) {
            Integer idTecnico = toInteger(findValue(row, "Id_Tecnico_OT1", "id_tecnico_ot1", "IdTecnico"));
            String tecnico = normalize(findValue(row, "Tecnico1_OT1", "tecnico1_ot1", "Tecnico"));
            if ((idTecnico != null && idsTecnico.contains(idTecnico))
                    || (!nombreTecnico.isEmpty() && nombreTecnico.equals(tecnico))) {
                filtradas.add(row);
            }
        }
        return filtradas;
    }

    public Map<String, Object> detalle(String token, Integer id) {
        AuthLoginResponse user = requireUser(token);
        Map<String, Object> row = requireCorte(id);
        requireRowAccess(user, row);
        return row;
    }

    public Map<String, Object> catalogosDigitacion(String token) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> result = new HashMap<>();
        result.put("zonas", repository.listarZonasDigitacion());
        result.put("distritos", repository.listarDistritosDigitacion());
        return result;
    }

    public Map<String, Object> resolverZonaHfc(String token, String zonaHfc) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        return resolveZonaHfc(zonaHfc);
    }

    public Map<String, Object> guardarDigitacion(String token, Integer id, CorteTapDigitacionRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        String estadoActual = normalize(findValue(corte, "Estado"));
        if ("ejecutada".equals(estadoActual) || "finalizado".equals(estadoActual)) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_CERRADO", "El Corte TAP ya no admite cambios de digitacion.");
        }

        String zonaHfc = toUpper(trimToNull(request == null ? null : request.getZonaHfc()));
        if (zonaHfc == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Zona HFC es requerida."
            );
        }

        Map<String, Object> resolucion = resolveZonaHfc(zonaHfc);
        String zona = String.valueOf(resolucion.get("zona"));
        String distrito = String.valueOf(resolucion.get("distrito"));

        int updated = repository.actualizarDigitacion(
                id,
                zonaHfc,
                zona,
                distrito,
                resolveUsuario(user)
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "El Corte TAP no pudo actualizarse.");
        }
        return requireCorte(id);
    }

    private Map<String, Object> resolveZonaHfc(String zonaHfc) {
        String normalized = toUpper(trimToNull(zonaHfc));
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ZONA_HFC_INVALIDA", "Zona HFC es requerida.");
        }
        String nodoZonaHfc = normalizeZonaHfc(normalized);
        Map<String, Object> nodoZona = repository.buscarNodoZona(nodoZonaHfc);
        if (nodoZona == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ZONA_HFC_NO_ENCONTRADA",
                    "La Zona HFC " + nodoZonaHfc + " no existe en tbl_Nodo_Zona."
            );
        }
        String zona = trimToNull(String.valueOf(findValue(nodoZona, "Zona")));
        Map<String, Object> nodoDistrito = repository.buscarNodoDistrito(nodoZonaHfc, zona);
        String distrito = nodoDistrito == null ? null : trimToNull(String.valueOf(findValue(nodoDistrito, "DistritoNuevo")));
        if (distrito == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DISTRITO_NO_ENCONTRADO",
                    "No existe distrito para la Zona HFC " + nodoZonaHfc + " y la zona " + zona + "."
            );
        }
        Map<String, Object> result = new HashMap<>();
        result.put("nodo", nodoZonaHfc);
        result.put("zona", zona);
        result.put("distrito", distrito);
        return result;
    }

    private String normalizeZonaHfc(String zonaHfc) {
        Matcher matcher = ZONA_HFC_PATTERN.matcher(zonaHfc);
        if (!matcher.find()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ZONA_HFC_INVALIDA",
                    "Zona HFC debe contener un nodo valido, por ejemplo NODO COT001."
            );
        }
        return "NODO " + matcher.group(1).toUpperCase(Locale.ROOT);
    }

    public Map<String, Object> guardarEjecucion(String token, Integer id, CorteTapEjecucionRequest request) {
        AuthLoginResponse user = requireUser(token);
        Map<String, Object> corte = requireCorte(id);
        requireTecnicoOwner(user, corte);
        if (findValue(corte, "FechaRegDig_D2") == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CORTE_TAP_SIN_DIGITACION",
                    "El digitador debe completar el Corte TAP antes de la ejecucion tecnica."
            );
        }
        String estadoActual = normalize(findValue(corte, "Estado"));
        if ("ejecutada".equals(estadoActual) || "finalizado".equals(estadoActual)) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_CERRADO", "El Corte TAP ya fue ejecutado.");
        }

        String ordenTrabajo = trimToNull(request == null ? null : request.getOrdenTrabajo());
        String observacion = trimToNull(request == null ? null : request.getObservacion());
        String foto1 = trimToNull(request == null ? null : request.getFoto1());
        String foto2 = trimToNull(request == null ? null : request.getFoto2());
        if (ordenTrabajo == null || observacion == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Orden de trabajo y observacion son requeridas.");
        }
        if (foto1 == null && foto2 == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe adjuntar al menos una foto.");
        }
        validatePhoto(foto1);
        validatePhoto(foto2);

        Timestamp fechaEjecucion = parseFechaEjecucion(request == null ? null : request.getFechaEjecucion());
        int updated = repository.actualizarEjecucion(
                id,
                ordenTrabajo,
                observacion,
                foto1,
                foto2,
                fechaEjecucion,
                resolveUsuario(user)
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "El Corte TAP no pudo ejecutarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> finalizar(String token, Integer id) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        String estado = normalize(findValue(corte, "Estado"));
        if ("finalizado".equals(estado)) {
            return corte;
        }
        if (findValue(corte, "FechaRegTec_T3") == null || !"ejecutada".equals(estado)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CORTE_TAP_SIN_EJECUCION",
                    "El tecnico debe completar la ejecucion antes de finalizar el Corte TAP."
            );
        }
        int updated = repository.finalizar(id);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_FINALIZADO", "El Corte TAP no pudo finalizarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> crear(String token, CorteTapCrearRequest request) {
        AuthLoginResponse user = requireUser(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos de Corte TAP requeridos.");
        }

        String tor = trimToNull(request.getTor());
        tor = tor == null ? null : tor.toUpperCase(Locale.ROOT);
        if (!("TE".equals(tor) || "SE".equals(tor))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CORTE_TAP_TOR_INVALIDO", "Solo se puede crear Corte TAP para TOR TE o SE.");
        }

        String codigoCliente = trimToNull(request.getCodigoCliente());
        String tecnico = trimToNull(request.getTecnico());
        String sucursal = trimToNull(request.getSucursal());
        String nodoTapBoca = toUpper(trimToNull(request.getNodoTapBoca()));
        Integer idTecnico = request.getIdTecnico();
        if (codigoCliente == null || tecnico == null || sucursal == null || nodoTapBoca == null
                || idTecnico == null || idTecnico <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Cliente, tecnico, sucursal y Nodo/TAP/Boca son requeridos."
            );
        }
        if (nodoTapBoca.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nodo/TAP/Boca supera los 50 caracteres.");
        }

        Set<Integer> idsTecnicoSesion = resolveIdsTecnico(user);
        if (!idsTecnicoSesion.contains(idTecnico)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TECNICO_INVALIDO", "El tecnico del Corte TAP no corresponde a la sesion.");
        }

        repository.insertar(
                codigoCliente,
                tor,
                idTecnico,
                tecnico,
                sucursal,
                resolveUsuario(user),
                nodoTapBoca
        );

        Map<String, Object> result = new HashMap<>();
        result.put("creado", true);
        result.put("codigoCliente", codigoCliente);
        result.put("tor", tor);
        result.put("idTecnico", idTecnico);
        return result;
    }

    private AuthLoginResponse requireUser(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse user = me == null ? null : me.getUsuario();
        if (user == null || user.getIdUsuario() == null || user.getIdSucursal() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Sesion invalida.");
        }
        return user;
    }

    private Map<String, Object> requireCorte(Integer id) {
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id de Corte TAP requerido.");
        }
        Map<String, Object> row = repository.buscarPorId(id);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CORTE_TAP_NO_ENCONTRADO", "Corte TAP no encontrado.");
        }
        return row;
    }

    private void requireRowAccess(AuthLoginResponse user, Map<String, Object> row) {
        if (canViewAll(user)) return;
        requireTecnicoOwner(user, row);
    }

    private void requireTecnicoOwner(AuthLoginResponse user, Map<String, Object> row) {
        Set<Integer> idsTecnico = resolveIdsTecnico(user);
        Integer idTecnico = toInteger(findValue(row, "Id_Tecnico_OT1", "id_tecnico_ot1", "IdTecnico"));
        String tecnico = normalize(findValue(row, "Tecnico1_OT1", "tecnico1_ot1", "Tecnico"));
        if ((idTecnico == null || !idsTecnico.contains(idTecnico))
                && (normalize(user.getNombre()).isEmpty() || !normalize(user.getNombre()).equals(tecnico))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "El Corte TAP no corresponde al tecnico de la sesion.");
        }
    }

    private void requireDigitador(AuthLoginResponse user) {
        String role = normalizeRole(user);
        if (!("digitador".equals(role) || "sistemas".equals(role) || "admin".equals(role)
                || "administrador".equals(role))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Solo el digitador puede completar esta etapa.");
        }
    }

    private boolean canViewAll(AuthLoginResponse user) {
        String role = normalizeRole(user);
        return "digitador".equals(role) || "sistemas".equals(role) || "admin".equals(role)
                || "administrador".equals(role);
    }

    private String normalizeRole(AuthLoginResponse user) {
        String role = user == null ? null : user.getRol();
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    private Timestamp parseFechaEjecucion(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Fecha de ejecucion requerida.");
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(normalized));
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", "Fecha de ejecucion invalida.");
        }
    }

    private void validatePhoto(String value) {
        if (value == null) return;
        if (!value.startsWith("data:image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOTO_INVALIDA", "Las fotos deben ser archivos de imagen.");
        }
        if (value.length() > MAX_PHOTO_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOTO_MUY_GRANDE", "Cada foto debe pesar menos de 6 MB.");
        }
    }

    private Set<Integer> resolveIdsTecnico(AuthLoginResponse user) {
        Set<Integer> ids = new HashSet<>(otRepository.obtenerIdsVendedorPorIdUsuario(user.getIdUsuario(), user.getIdSucursal()));
        ids.add(user.getIdUsuario());
        return ids;
    }

    private String resolveUsuario(AuthLoginResponse user) {
        String login = trimToNull(user.getLoggin());
        if (login != null) return login;
        String nombre = trimToNull(user.getNombre());
        return nombre == null ? String.valueOf(user.getIdUsuario()) : nombre;
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
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
