package com.example.TigoStarSystem.auth.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginRequest;
import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.repository.AuthRepository;
import com.example.TigoStarSystem.auth.repository.SucursalRepository;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final int ROL_ID_SISTEMAS = 4;
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final AuthRepository authRepository;
    private final SucursalRepository sucursalRepository;
    private final DbConnectionManager dbConnectionManager;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();
    private final String dbUsername;
    private final String dbPassword;
    private final boolean validarSucursal;

    /**
     * Inicializa el servicio de autenticacion y resuelve configuracion de conexiones.
     */
    public AuthService(
            AuthRepository authRepository,
            SucursalRepository sucursalRepository,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${auth.login.validar-sucursal:true}") boolean validarSucursal) {
        this.authRepository = authRepository;
        this.sucursalRepository = sucursalRepository;
        this.dbConnectionManager = dbConnectionManager;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.validarSucursal = validarSucursal;
    }

    /**
     * Ejecuta login: valida credenciales por SP, crea token y registra sesion en memoria.
     */
    public AuthSession login(AuthLoginRequest request) {
        logger.info(
                "Login attempt usuario={}, idSucursal={}, validarSucursal={}",
                safe(request == null ? null : request.getUsuario()),
                request == null ? null : request.getIdSucursal(),
                validarSucursal
        );
        SucursalInfo sucursal = obtenerSucursalPorId(request.getIdSucursal());
        logger.debug(
                "Sucursal resolved idSucursal={}, host={}, baseDeDatos={}",
                sucursal.idSucursal,
                sucursal.host,
                sucursal.baseDeDatos
        );
        List<SucursalInfo> destinosLogin = resolverDestinosLogin(sucursal);
        String passwordHash = hashMd5Base64(request.getPassword());
        List<Map<String, Object>> rows = null;
        Exception lastError = null;
        for (int i = 0; i < destinosLogin.size(); i++) {
            SucursalInfo sucursalLogin = destinosLogin.get(i);
            logger.debug(
                    "Login target attempt={} idSucursal={}, sucursal={}, host={}, baseDeDatos={}",
                    i + 1,
                    sucursalLogin.idSucursal,
                    sucursalLogin.sucursal,
                    sucursalLogin.host,
                    sucursalLogin.baseDeDatos
            );
            JdbcTemplate jdbcTemplate = crearJdbcTemplateSucursal(sucursalLogin, dbUsername, dbPassword);
            try {
                rows = ejecutarValidacionConSpAlternativo(jdbcTemplate, request, passwordHash, validarSucursal);
                if (rows != null && !rows.isEmpty()) {
                    break;
                }
            } catch (DataAccessException ex) {
                lastError = ex;
                logger.warn(
                        "Login SP failed on attempt {} usuario={}, idSucursal={}, host={}, baseDeDatos={}",
                        i + 1,
                        safe(request.getUsuario()),
                        request.getIdSucursal(),
                        sucursalLogin.host,
                        sucursalLogin.baseDeDatos,
                        ex
                );
            } catch (Exception ex) {
                lastError = ex;
                logger.warn(
                        "Login failed on attempt {} usuario={}, idSucursal={}, host={}, baseDeDatos={}",
                        i + 1,
                        safe(request.getUsuario()),
                        request.getIdSucursal(),
                        sucursalLogin.host,
                        sucursalLogin.baseDeDatos,
                        ex
                );
            }
        }
        if ((rows == null || rows.isEmpty()) && lastError instanceof RuntimeException) {
            throw (RuntimeException) lastError;
        }
        logger.debug("Login SP returned {} rows", rows == null ? 0 : rows.size());
        if (rows == null || rows.isEmpty()) {
            logger.warn(
                    "Login invalid credentials usuario={}, idSucursal={}, validarSucursal={}",
                    safe(request.getUsuario()),
                    request.getIdSucursal(),
                    validarSucursal
            );
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Usuario o password invÃ¡lidos.");
        }
        AuthLoginResponse userFromDb = mapToResponse(rows.get(0), sucursal.idSucursal);
        Integer idSucursalSeleccionada = request.getIdSucursal();
        if (userFromDb.getIdSucursal() != null
                && idSucursalSeleccionada != null
                && !idSucursalSeleccionada.equals(userFromDb.getIdSucursal())) {
            logger.warn(
                    "Login sucursal mismatch usuario={}, idSucursalSeleccionada={}, idSucursalDevueltaSP={}. Se prioriza la sucursal seleccionada.",
                    safe(request.getUsuario()),
                    idSucursalSeleccionada,
                    userFromDb.getIdSucursal()
            );
        }
        AuthLoginResponse user = new AuthLoginResponse(
                userFromDb.getIdUsuario(),
                userFromDb.getNombre(),
                userFromDb.getRol(),
                userFromDb.getIdRol(),
                idSucursalSeleccionada
        );
        String token = UUID.randomUUID().toString();
        OffsetDateTime expira = OffsetDateTime.now().plus(SESSION_TTL);
        AuthSession session = new AuthSession(token, user, expira);
        sessions.put(token, session);
        return session;
    }

    /**
     * Devuelve el catalogo de sucursales disponibles para autenticacion.
     */
    public List<SucursalResponse> listarSucursales() {
        List<Map<String, Object>> rows = sucursalRepository.obtenerSucursales();
        List<SucursalResponse> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SucursalInfo info = mapToSucursal(row);
            result.add(new SucursalResponse(info.idSucursal, info.sucursal, info.host, info.baseDeDatos));
        }
        return result;
    }

    /**
     * Valida token de sesion y retorna datos de usuario autenticado.
     */
    public AuthMeResponse me(String token) {
        if (token == null || isBlank(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "SesiÃ³n requerida.");
        }
        AuthSession session = sessions.get(token);
        if (session == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "SesiÃ³n invÃ¡lida.");
        }
        if (session.getExpira().isBefore(OffsetDateTime.now())) {
            sessions.remove(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "SesiÃ³n expirada.");
        }
        return new AuthMeResponse(session.getUsuario(), session.getExpira(), resolveHostName());
    }

    private String resolveHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName == null ? null : hostName.trim();
        } catch (UnknownHostException ex) {
            logger.warn("No se pudo resolver el nombre del host para la sesion.", ex);
            return null;
        }
    }

    /**
     * Exige sesion valida y que el usuario tenga rol administrador.
     */
    public AuthMeResponse requireAdmin(String token) {
        AuthMeResponse me = me(token);
        if (!esAdministrador(me.getUsuario())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_ADMIN_ONLY",
                    "Solo administradores pueden acceder a este recurso."
            );
        }
        return me;
    }

    /**
     * Determina si un usuario pertenece al rol de Sistemas (administrador).
     */
    public boolean esAdministrador(AuthLoginResponse usuario) {
        if (usuario == null) {
            return false;
        }
        Integer idRol = usuario.getIdRol();
        return idRol != null && idRol == ROL_ID_SISTEMAS;
    }

    /**
     * Mapea una fila devuelta por el SP de login a un DTO de respuesta.
     */
    private AuthLoginResponse mapToResponse(Map<String, Object> row, Integer idSucursalFallback) {
        Integer idUsuario = toInteger(findValue(row, "idusuario", "id_usuario", "iduser", "usuarioid"));
        Integer idRol = toInteger(findValue(row, "idrol", "id_rol", "rolid"));
        Integer idSucursal = toInteger(findValue(row, "idsucursal", "id_sucursal", "sucursalid"));
        if (idSucursal == null) {
            idSucursal = idSucursalFallback;
        }
        String nombre = toString(findValue(row, "nombre", "nombres", "nombreusuario", "usuario"));
        String rol = toString(findValue(row, "rol", "nombrerol", "descripcionrol"));

        if (idUsuario == null || nombre == null) {
            Map<String, Object> details = new HashMap<>();
            details.put("rowKeys", row.keySet());
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_SP_RESULT",
                    "El SP spx_ValidarUsuario no devuelve las columnas esperadas.",
                    details
            );
        }
        return new AuthLoginResponse(idUsuario, nombre, rol, idRol, idSucursal);
    }

    /**
     * Busca y valida la sucursal seleccionada por id.
     */
    private SucursalInfo obtenerSucursalPorId(Integer idSucursal) {
        if (idSucursal == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSucursal es requerido.");
        }
        List<Map<String, Object>> rows = sucursalRepository.obtenerSucursales();
        for (Map<String, Object> row : rows) {
            SucursalInfo info = mapToSucursal(row);
            if (idSucursal.equals(info.idSucursal)) {
                return info;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "SUCURSAL_NOT_FOUND", "Sucursal no encontrada.");
    }

    /**
     * Convierte una fila de sucursal en una estructura interna normalizada.
     */
    private SucursalInfo mapToSucursal(Map<String, Object> row) {
        Integer idSucursal = toInteger(findValue(row, "idsucursal", "id_sucursal"));
        String sucursal = canonicalizarSucursal(toString(findValue(row, "sucursal")));
        String ip = trimToNull(toString(findValue(row, "ip")));
        String ip2 = trimToNull(toString(findValue(row, "ip2")));
        String baseDeDatos = toString(findValue(row, "basededatos", "base_de_datos"));
        String host = firstNonBlank(ip, ip2);
        String hostAlterno = null;
        if (!isBlank(ip) && !isBlank(ip2) && !ip.equalsIgnoreCase(ip2)) {
            hostAlterno = ip2;
        }
        if (idSucursal == null) {
            Map<String, Object> details = new HashMap<>();
            details.put("rowKeys", row.keySet());
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_SUCURSAL_ROW",
                    "tbl_sucursal no contiene el campo requerido Id_Sucursal.",
                    details
            );
        }
        return new SucursalInfo(idSucursal, sucursal, trimToNull(host), trimToNull(baseDeDatos), trimToNull(hostAlterno));
    }

    /**
     * Unifica variantes comunes de sucursal para evitar inconsistencias en el front.
     */
    private String canonicalizarSucursal(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = normalizeText(trimmed).replaceAll("[\\s_\\-]+", "");
        if ("santacruz".equals(normalized)) {
            return "SantaCruz";
        }
        if ("sucre".equals(normalized)) {
            return "Sucre";
        }
        return trimmed;
    }

    /**
     * Resuelve el/los destinos de login para la sucursal seleccionada.
     */
    private List<SucursalInfo> resolverDestinosLogin(SucursalInfo sucursalOriginal) {
        if (sucursalOriginal == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SUCURSAL_DB_NOT_RESOLVED",
                    "No se pudo resolver host/base de datos para la sucursal seleccionada."
            );
        }
        String host = trimToNull(sucursalOriginal.host);
        String database = trimToNull(sucursalOriginal.baseDeDatos);

        if (isBlank(host) || isBlank(database)) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SUCURSAL_DB_NOT_RESOLVED",
                    "No se pudo resolver host/base de datos para la sucursal seleccionada."
            );
        }
        List<SucursalInfo> destinos = new ArrayList<>();
        destinos.add(new SucursalInfo(
                sucursalOriginal.idSucursal,
                sucursalOriginal.sucursal,
                host.trim(),
                database.trim(),
                null
        ));
        if (!isBlank(sucursalOriginal.hostAlterno) && !host.equalsIgnoreCase(sucursalOriginal.hostAlterno)) {
            destinos.add(new SucursalInfo(
                    sucursalOriginal.idSucursal,
                    sucursalOriginal.sucursal,
                    sucursalOriginal.hostAlterno.trim(),
                    database.trim(),
                    null
            ));
        }
        return destinos;
    }

    /**
     * Crea un JdbcTemplate apuntando a la sucursal de login.
     */
    private JdbcTemplate crearJdbcTemplateSucursal(SucursalInfo sucursal, String username, String password) {
        logger.debug(
                "Creating JDBC connection for sucursal host={} baseDeDatos={}",
                sucursal == null ? null : sucursal.host,
                sucursal == null ? null : sucursal.baseDeDatos
        );
        return dbConnectionManager.connDb(
                sucursal == null ? "sucursal" : firstNonBlank(sucursal.sucursal, "sucursal"),
                sucursal == null ? null : sucursal.host,
                sucursal == null ? null : sucursal.baseDeDatos,
                username,
                password
        );
    }

    /**
     * Ejecuta el SP de validacion segun la bandera validarSucursal.
     */
    private List<Map<String, Object>> ejecutarValidacion(
            JdbcTemplate template,
            AuthLoginRequest request,
            String passwordHash,
            boolean validarSucursal
    ) {
        if (validarSucursal) {
            logger.debug("Calling SP dbo.spx_ValidarUsuarioSucursal");
            return authRepository.validarUsuarioSucursal(
                    template,
                    request.getUsuario(),
                    passwordHash,
                    request.getIdSucursal()
            );
        }
        logger.debug("Calling SP dbo.spx_ValidarUsuario");
        return authRepository.validarUsuario(template, request.getUsuario(), passwordHash);
    }

    /**
     * Reintenta la validacion con el SP alternativo si el primero no existe.
     */
    private List<Map<String, Object>> ejecutarValidacionConSpAlternativo(
            JdbcTemplate template,
            AuthLoginRequest request,
            String passwordHash,
            boolean validarSucursal
    ) {
        try {
            return ejecutarValidacion(template, request, passwordHash, validarSucursal);
        } catch (DataAccessException ex) {
            if (!isMissingStoredProcedure(ex)) {
                throw ex;
            }
            boolean validarSucursalAlternativo = !validarSucursal;
            logger.warn(
                    "SP de login no encontrado (validarSucursal={}). Reintentando con validarSucursal={} en la misma BD.",
                    validarSucursal,
                    validarSucursalAlternativo
            );
            return ejecutarValidacion(template, request, passwordHash, validarSucursalAlternativo);
        }
    }

    /**
     * Genera hash MD5 en Base64 para comparar password con SP legacy.
     */
    private String hashMd5Base64(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "HASH_ERROR",
                    "No se pudo generar el hash MD5."
            );
        }
    }

    /**
     * Busca valor en un map por multiples nombres de columna equivalentes.
     */
    private Object findValue(Map<String, Object> row, String... candidates) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = normalize(entry.getKey());
            normalized.put(key, entry.getValue());
        }
        for (String candidate : candidates) {
            String key = normalize(candidate);
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
        }
        return null;
    }

    /**
     * Normaliza una clave de columna removiendo guiones bajos y usando minusculas.
     */
    private String normalize(String key) {
        return key == null ? "" : key.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza texto removiendo tildes y pasando a minusculas.
     */
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Convierte un valor generico a Integer de forma segura.
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Convierte un valor generico a String.
     */
    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Verifica si un texto es nulo o vacio tras trim.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Devuelve el primer texto no vacio, priorizando preferred.
     */
    private String firstNonBlank(String preferred, String fallback) {
        if (!isBlank(preferred)) {
            return preferred.trim();
        }
        return fallback;
    }

    /**
     * Retorna null cuando el texto llega vacio; en otro caso retorna trim.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Detecta si la excepcion representa "stored procedure no encontrado".
     */
    private boolean isMissingStoredProcedure(DataAccessException ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) root;
            if (sqlEx.getErrorCode() == 2812) {
                return true;
            }
            String msg = sqlEx.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                return lower.contains("procedimiento almacenado") && lower.contains("no se encontr");
            }
        }
        return false;
    }

    /**
     * Limpia texto para uso en logs.
     */
    private String safe(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private static final class SucursalInfo {
        private final Integer idSucursal;
        private final String sucursal;
        private final String host;
        private final String baseDeDatos;
        private final String hostAlterno;

        /**
         * Estructura interna para transportar datos de sucursal ya resueltos.
         */
        private SucursalInfo(Integer idSucursal, String sucursal, String host, String baseDeDatos, String hostAlterno) {
            this.idSucursal = idSucursal;
            this.sucursal = sucursal;
            this.host = host;
            this.baseDeDatos = baseDeDatos;
            this.hostAlterno = hostAlterno;
        }
    }
}
