package com.example.TigoStarSystem.cortetap.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.cortetap.dto.CorteTapCrearRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapDigitacionRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapEjecucionRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapEstadoRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapObservacionRequest;
import com.example.TigoStarSystem.cortetap.dto.CorteTapDigitadorRequest;
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
    private static final Pattern ZONA_HFC_PATTERN = Pattern.compile("(?i)^NODO\\s+([A-Z]{3}\\d{3,4})$");
    private static final Pattern NODO_TAP_BOCA_PATTERN = Pattern.compile("(?i)^NODO\\s+[A-Z]{3}\\d{3,4}\\s+RAMAL\\s+[A-Z]\\s+\\d{3}\\s+BOCA\\s+\\d+$");
    private static final int MAX_PHOTO_LENGTH = 8_000_000;
    private final CorteTapRepository repository;
    private final AuthService authService;
    private final OtRepository otRepository;
    private final SucursalRepository sucursalRepository;

    public CorteTapService(CorteTapRepository repository, AuthService authService, OtRepository otRepository, SucursalRepository sucursalRepository) {
        this.repository = repository;
        this.authService = authService;
        this.otRepository = otRepository;
        this.sucursalRepository = sucursalRepository;
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
        result.put("estados", repository.listarEstadosCorteTap());
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
        requireDigitadorEditable(corte);
        String estadoActual = normalize(findValue(corte, "Estado"));
        if ("ejecutada".equals(estadoActual) || "finalizado".equals(estadoActual)) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_CERRADO", "El Corte TAP ya no admite cambios de digitacion.");
        }

        String nodoTapBocaAntiguo = toUpper(trimToNull(request == null ? null : request.getNodoTapBocaAntiguo()));
        if (nodoTapBocaAntiguo == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Nodo/TAP/Boca antiguo es requerido."
            );
        }
        if (nodoTapBocaAntiguo.length() > 50) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Nodo/TAP/Boca antiguo no puede superar 50 caracteres."
            );
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
                nodoTapBocaAntiguo,
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
        requirePasoEditable(corte);
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

    public Map<String, Object> guardarEstado(String token, Integer id, CorteTapEstadoRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        requireDigitadorEditable(corte);
        String estado = request == null ? null : trimToNull(request.getEstado());
        estado = estado == null ? null : estado.toUpperCase(Locale.ROOT);
        if ("EJECUTADA".equals(estado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CORTE_TAP_ESTADO_TECNICO",
                    "El estado EJECUTADA solo puede ser asignado por el tecnico.");
        }
        if (!estadoPermitido(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CORTE_TAP_ESTADO_INVALIDO",
                    "Estado invalido.");
        }
        int updated = repository.actualizarEstado(id, estado);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "El estado no pudo actualizarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> guardarObservacion(String token, Integer id, CorteTapObservacionRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        requireDigitadorEditable(corte);
        String observacion = request == null ? null : request.getObservacion();
        observacion = observacion == null ? "" : observacion.trim();
        if (observacion.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSERVACION_MUY_LARGA",
                    "La observacion no puede superar 1000 caracteres.");
        }
        int updated = repository.actualizarObservacion(id, observacion);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "La observacion no pudo actualizarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> guardarDatosDigitador(String token, Integer id, CorteTapDigitadorRequest request) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        requireDigitadorEditable(corte);
        String estado = request == null ? null : trimToNull(request.getEstado());
        estado = estado == null ? null : estado.toUpperCase(Locale.ROOT);
        if ("EJECUTADA".equals(estado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CORTE_TAP_ESTADO_TECNICO",
                    "El estado EJECUTADA solo puede ser asignado por el tecnico.");
        }
        if (!estadoPermitido(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CORTE_TAP_ESTADO_INVALIDO", "Estado invalido.");
        }
        String nodoTapBocaAntiguo = toUpper(trimToNull(request == null ? null : request.getNodoTapBocaAntiguo()));
        if (nodoTapBocaAntiguo == null || nodoTapBocaAntiguo.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nodo/TAP/Boca antiguo es requerido y no puede superar 50 caracteres.");
        }
        String zonaHfc = toUpper(trimToNull(request == null ? null : request.getZonaHfc()));
        Map<String, Object> resolucion = resolveZonaHfc(zonaHfc);
        String observacion = request == null || request.getObservacion() == null ? "" : request.getObservacion().trim();
        if (observacion.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSERVACION_MUY_LARGA", "La observacion no puede superar 1000 caracteres.");
        }
        String paso = esEstadoFinal(estado) ? "PF" : "P2";
        int updated = repository.actualizarDatosDigitador(id, nodoTapBocaAntiguo, zonaHfc,
                String.valueOf(resolucion.get("zona")), String.valueOf(resolucion.get("distrito")), estado, paso, observacion,
                resolveUsuario(user));
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_NO_ACTUALIZADO", "Los datos no pudieron actualizarse.");
        }
        return requireCorte(id);
    }

    public Map<String, Object> finalizar(String token, Integer id) {
        AuthLoginResponse user = requireUser(token);
        requireDigitador(user);
        Map<String, Object> corte = requireCorte(id);
        requirePasoEditable(corte);
        String estado = normalize(findValue(corte, "Estado"));
        if ("finalizado".equals(estado)) {
            return corte;
        }
        if (!"ejecutada".equals(estado)) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_ESTADO_NO_FINALIZABLE",
                    "El Corte TAP debe estar en estado EJECUTADA para finalizarse.");
        }
        if (findValue(corte, "FechaRegDig_D2") == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CORTE_TAP_SIN_DIGITACION",
                    "El digitador debe completar el Corte TAP antes de finalizarlo."
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
        if (request.isCreadoDesdeOt()) {
            requireCorteTapCreator(user, request.getIdTecnico());
        } else {
            requireDigitador(user);
        }

        String tor = trimToNull(request.getTor());
        tor = tor == null ? "SO1" : tor.toUpperCase(Locale.ROOT);
        String codigoCliente = trimToNull(request.getCodigoCliente());
        String tecnico = trimToNull(request.getTecnico());
        String sucursal = resolveSucursalNombre(user.getIdSucursal());
        String nodoTapBocaAntiguo = toUpper(trimToNull(request.getNodoTapBocaAntiguo()));
        String nodoTapBoca = toUpper(trimToNull(request.getNodoTapBoca()));
        String zonaHfc = toUpper(trimToNull(request.getZonaHfc()));
        boolean creadoDesdeOt = request.isCreadoDesdeOt();
        String estado = trimToNull(request.getEstado());
        estado = estado == null ? null : estado.toUpperCase(Locale.ROOT);
        String observacion = request.getObservacion() == null ? "" : request.getObservacion().trim();
        Integer idTecnico = request.getIdTecnico();
        if (codigoCliente == null || tecnico == null || sucursal == null
                || (!creadoDesdeOt && (nodoTapBoca == null || nodoTapBocaAntiguo == null || zonaHfc == null)) || estado == null
                || idTecnico == null || idTecnico <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Cliente, tecnico, nodos, zona HFC y estado son requeridos."
            );
        }
        if (!codigoCliente.matches("\\d+")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cliente solo puede contener digitos.");
        }
        if (!creadoDesdeOt && (nodoTapBocaAntiguo.length() > 50 || !NODO_TAP_BOCA_PATTERN.matcher(nodoTapBocaAntiguo).matches())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nodo/TAP/Boca supera los 50 caracteres.");
        }
        if (!estadoPermitido(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CORTE_TAP_ESTADO_INVALIDO", "Estado invalido.");
        }
        String zona = null;
        String distrito = null;
        if (!creadoDesdeOt) {
            Map<String, Object> resolucion = resolveZonaHfc(zonaHfc);
            zona = trimToNull(String.valueOf(resolucion.getOrDefault("zona", "")));
            distrito = trimToNull(String.valueOf(resolucion.getOrDefault("distrito", "")));
            if (zona == null || distrito == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ZONA_HFC_NO_ENCONTRADA", "No se encontro Zona y Distrito para la Zona HFC indicada.");
            }
        }
        String paso = creadoDesdeOt ? "P1" : (esEstadoFinal(estado) ? "PF" : "P2");

        repository.insertar(
                codigoCliente,
                tor,
                idTecnico,
                tecnico,
                sucursal,
                resolveUsuario(user),
                nodoTapBocaAntiguo,
                nodoTapBoca,
                zonaHfc,
                zona,
                distrito,
                estado,
                paso,
                creadoDesdeOt ? null : observacion,
                creadoDesdeOt ? null : resolveUsuario(user),
                creadoDesdeOt
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

    private String resolveSucursalNombre(Integer idSucursal) {
        if (idSucursal == null) return null;
        for (Map<String, Object> row : sucursalRepository.obtenerSucursales()) {
            Integer id = toInteger(findValue(row, "idSucursal", "IdSucursal", "id_sucursal", "Id_Sucursal"));
            if (idSucursal.equals(id)) {
                return trimToNull(String.valueOf(findValue(row, "sucursal", "Sucursal", "nombre", "Nombre")));
            }
        }
        return null;
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

    private void requireCorteTapCreator(AuthLoginResponse user, Integer idTecnico) {
        String role = normalizeRole(user);
        if ("digitador".equals(role) || "sistemas".equals(role) || "admin".equals(role)
                || "administrador".equals(role)) {
            return;
        }
        if (idTecnico != null && resolveIdsTecnico(user).contains(idTecnico)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Solo el tecnico asignado o el digitador puede crear el Corte TAP.");
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

    private boolean estadoPermitido(String estado) {
        if (estado == null || estado.trim().isEmpty()) return false;
        List<Map<String, Object>> estados = repository.listarEstadosCorteTap();
        if (estados == null || estados.isEmpty()) {
            return "PENDIENTE".equals(estado) || "EJECUTADA".equals(estado) || "FINALIZADO".equals(estado);
        }
        for (Map<String, Object> row : estados) {
            Object value = findValue(row, "estado", "Estado", "EstadoCorteTap", "Nombre", "Descripcion");
            if (value != null && estado.equalsIgnoreCase(String.valueOf(value).trim())) return true;
        }
        return false;
    }

    private boolean esEstadoFinal(String estado) {
        return "FINALIZADA".equalsIgnoreCase(estado)
                || "FINALIZADO".equalsIgnoreCase(estado)
                || "CANCELADA".equalsIgnoreCase(estado);
    }

    private void requirePasoEditable(Map<String, Object> corte) {
        Object paso = findValue(corte, "Paso", "paso");
        if (paso != null && "PF".equalsIgnoreCase(String.valueOf(paso).trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_CERRADO", "El Corte TAP tiene Paso PF y no admite modificaciones.");
        }
    }

    private void requireDigitadorEditable(Map<String, Object> corte) {
        Object paso = findValue(corte, "Paso", "paso");
        if (paso != null && ("P2".equalsIgnoreCase(String.valueOf(paso).trim())
                || "P3".equalsIgnoreCase(String.valueOf(paso).trim())
                || "PF".equalsIgnoreCase(String.valueOf(paso).trim()))) {
            throw new ApiException(HttpStatus.CONFLICT, "CORTE_TAP_DIGITACION_CERRADA",
                    "La digitacion ya fue registrada y solo puede visualizarse.");
        }
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
