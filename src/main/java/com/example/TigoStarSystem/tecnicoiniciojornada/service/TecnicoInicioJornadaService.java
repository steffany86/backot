package com.example.TigoStarSystem.tecnicoiniciojornada.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import com.example.TigoStarSystem.tecnicoiniciojornada.dto.TecnicoInicioJornadaCreateRequest;
import com.example.TigoStarSystem.tecnicoiniciojornada.dto.TecnicoCierreJornadaRequest;
import com.example.TigoStarSystem.tecnicoiniciojornada.repository.TecnicoInicioJornadaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TecnicoInicioJornadaService {
    private final TecnicoInicioJornadaRepository repository;
    private final JdbcTemplate tigohogarJdbcTemplate;
    private final AuthService authService;
    private final DbConnectionManager dbConnectionManager;
    private final String defaultDbUsername;
    private final String defaultDbPassword;

    public TecnicoInicioJornadaService(
            TecnicoInicioJornadaRepository repository,
            @Qualifier("tigohogarJdbcTemplate") JdbcTemplate tigohogarJdbcTemplate,
            AuthService authService,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String defaultDbUsername,
            @Value("${spring.datasource.password}") String defaultDbPassword
    ) {
        this.repository = repository;
        this.tigohogarJdbcTemplate = tigohogarJdbcTemplate;
        this.authService = authService;
        this.dbConnectionManager = dbConnectionManager;
        this.defaultDbUsername = defaultDbUsername;
        this.defaultDbPassword = defaultDbPassword;
    }

    public Map<String, Object> estado(String token, String sucursal) {
        AuthLoginResponse tecnico = requireUsuarioInicioJornada(token);
        repository.marcarNoCierreAtrasado(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        Map<String, Object> cierrePendienteAyer = repository.buscarCierrePendienteAyer(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        boolean existe = repository.existeRegistroHoy(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        String sucursalResuelta = resolveSucursalNombre(sucursal, tecnico);
        JdbcTemplate tecnicosTemplate = resolveTecnicosTemplate(sucursalResuelta, tecnico);
        Map<String, Object> encargadoActual = repository.buscarEncargadoActualPorTecnico(
                dbConnectionManager.connDb("central"),
                tecnicosTemplate,
                sucursalResuelta,
                tecnico.getIdUsuario(),
                tecnico.getNombre()
        );
        Map<String, Object> out = new HashMap<>();
        out.put("idTecnico", tecnico.getIdUsuario());
        out.put("pendiente", !existe);
        out.put("fechaServidor", java.time.OffsetDateTime.now().toString());
        agregarCierrePendienteAyer(out, cierrePendienteAyer);
        if (!existe) {
            Map<String, Object> rechazadoHoy = repository.buscarInicioRechazadoHoy(tigohogarJdbcTemplate, tecnico.getIdUsuario());
            if (rechazadoHoy != null) {
                String observacion = valueAsString(rechazadoHoy.get("observacion_rechazado"));
                out.put("inicioRechazadoHoy", true);
                out.put("idInicioRechazado", rechazadoHoy.get("id_inicio"));
                out.put("fechaInicioRechazado", valueAsString(rechazadoHoy.get("fecha_registro")));
                if (!isBlank(observacion)) {
                    out.put("observacionRechazado", observacion);
                }
            }
        }
        if (encargadoActual != null) {
            String encargado = valueAsString(encargadoActual.get("encargado"));
            String idEncargado = valueAsString(encargadoActual.get("idEncargado"));
            if (!isBlank(encargado)) {
                out.put("encargado", encargado);
            }
            if (!isBlank(idEncargado)) {
                out.put("idEncargado", idEncargado);
            }
            agregarAuxiliarConformacion(out, encargadoActual);
        }
        return out;
    }

    public Map<String, Object> estadoCierre(String token) {
        AuthLoginResponse tecnico = requireUsuarioInicioJornada(token);
        repository.marcarNoCierreAtrasado(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        Map<String, Object> cierrePendienteAyer = repository.buscarCierrePendienteAyer(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        Map<String, Object> row = repository.estadoCierreHoy(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        int noMarcoCount = repository.countNoMarco(tigohogarJdbcTemplate, tecnico.getIdUsuario());

        Map<String, Object> out = new HashMap<>();
        out.put("idTecnico", tecnico.getIdUsuario());
        out.put("tieneInicioHoy", row != null);
        out.put("cerradoHoy", row != null && row.get("fecha_cierre") != null);
        out.put("requiereCierre", row != null && row.get("fecha_cierre") == null);
        out.put("noMarcoCount", noMarcoCount);
        agregarCierrePendienteAyer(out, cierrePendienteAyer);
        return out;
    }

    public List<Map<String, Object>> listarEncargados(String token, String sucursal) {
        AuthLoginResponse tecnico = requireUsuarioInicioJornada(token);
        String sucursalResuelta = resolveSucursalNombre(sucursal, tecnico);
        JdbcTemplate tecnicosTemplate = resolveTecnicosTemplate(sucursalResuelta, tecnico);
        List<Map<String, Object>> encargados = repository.listarEncargados(tecnicosTemplate);
        Map<String, Object> encargadoActual = repository.buscarEncargadoActualPorTecnico(
                dbConnectionManager.connDb("central"),
                tecnicosTemplate,
                sucursalResuelta,
                tecnico.getIdUsuario(),
                tecnico.getNombre()
        );
        return ensureEncargadoActualEnLista(encargados, encargadoActual);
    }

    public Map<String, Object> registrar(String token, TecnicoInicioJornadaCreateRequest request) {
        AuthLoginResponse tecnico = requireUsuarioInicioJornada(token);

        repository.marcarNoCierreAtrasado(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        Map<String, Object> cierrePendienteAyer = repository.buscarCierrePendienteAyer(tigohogarJdbcTemplate, tecnico.getIdUsuario());
        if (cierrePendienteAyer != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CIERRE_AYER_REQUERIDO",
                    "Tiene una jornada anterior sin cierre. Antes de iniciar la jornada de hoy debe registrar el cierre pendiente."
            );
        }
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Solicitud requerida.");
        }
        if (isBlank(request.getCapacitado()) || isBlank(request.getCharla()) || isBlank(request.getBotiquin())
                || isBlank(request.getExtintor()) || request.getFechaVencimiento() == null
                || isBlank(request.getEquipoEpp()) || isBlank(request.getEstadoEpp())
                || isBlank(request.getApr()) || isBlank(request.getEscalera())
                || isBlank(request.getAnclaje()) || isBlank(request.getImagen())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Todos los campos del checklist y la foto son obligatorios.");
        }
        if (isBlank(request.getFirmaInicio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La firma de inicio es obligatoria.");
        }
        if (!"SI".equals(normalizeSiNo(request.getAceptoInicioJornada()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe aceptar la declaracion jurada para registrar el inicio de jornada.");
        }
        if (repository.existeRegistroHoy(tigohogarJdbcTemplate, tecnico.getIdUsuario())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "Ya registraste inicio de jornada hoy.");
        }
        String sucursalResuelta = resolveSucursalNombre(request.getSucursal(), tecnico);
        JdbcTemplate tecnicosTemplate = resolveTecnicosTemplate(sucursalResuelta, tecnico);
        Map<String, Object> encargadoActual = repository.buscarEncargadoActualPorTecnico(
                dbConnectionManager.connDb("central"),
                tecnicosTemplate,
                sucursalResuelta,
                tecnico.getIdUsuario(),
                tecnico.getNombre()
        );
        if (encargadoActual == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ENCARGADO_NO_ENCONTRADO",
                    "No se encontro supervisor para este tecnico en conformacion diaria."
            );
        }
        Integer idEncargado = toPositiveInteger(encargadoActual.get("idEncargado"));
        if (idEncargado == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ENCARGADO_INVALIDO",
                    "No se pudo resolver id de supervisor para este tecnico."
            );
        }
        String sucursalConformacion = valueAsString(encargadoActual.get("sucursal"));
        String sucursalFinal = isBlank(sucursalConformacion) ? sucursalResuelta : SucursalCanonicalizer.canonicalize(sucursalConformacion);
        Integer idSucursal = resolveSucursalId(sucursalFinal);
        String nombreTecnicoSucursal = trimOrNull(tecnico.getNombre());
        if (isBlank(nombreTecnicoSucursal)) {
            nombreTecnicoSucursal = repository.obtenerNombreTecnicoPorId(tecnicosTemplate, tecnico.getIdUsuario());
        }
        Integer idAuxiliarConformacion = toPositiveInteger(encargadoActual.get("idAuxiliar"));
        Integer idAuxiliarRegistro = request.getIdAuxiliar() != null && request.getIdAuxiliar() > 0
                ? request.getIdAuxiliar()
                : idAuxiliarConformacion;
        if (idAuxiliarRegistro != null && isBlank(request.getImagenAuxiliar())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Debe cargar la foto del auxiliar asignado para registrar el inicio de jornada."
            );
        }

        List<Map<String, Object>> rows = repository.registrar(
                tigohogarJdbcTemplate,
                tecnico.getIdUsuario(),
                idAuxiliarRegistro,
                idEncargado,
                idEncargado,
                idSucursal,
                sucursalFinal,
                nombreTecnicoSucursal,
                normalizeSiNo(request.getCapacitado()),
                normalizeSiNo(request.getCharla()),
                normalizeSiNo(request.getBotiquin()),
                normalizeSiNo(request.getExtintor()),
                request.getFechaVencimiento(),
                normalizeSiNo(request.getEquipoEpp()),
                normalizeSiNo(request.getEstadoEpp()),
                normalizeSiNo(request.getApr()),
                normalizeSiNo(request.getEscalera()),
                normalizeSiNo(request.getAnclaje()),
                request.getImagen(),
                normalizeSiNo(request.getAceptoInicioJornada()),
                "NO"
        );
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo registrar inicio de jornada.");
        }
        Map<String, Object> result = rows.get(0);
        Integer idInicio = toPositiveInteger(
                result.get("idInicio") != null ? result.get("idInicio") : result.get("id_inicio")
        );
        if (idInicio != null) {
            repository.marcarNoMarcoCierreInicio(tigohogarJdbcTemplate, idInicio);
            result.put("no_marco_cierre", true);
            result.put("noMarcoCierre", true);
            if (request.getEstoyTrabajandoSolo() != null) {
                repository.actualizarEstoyTrabajandoSolo(tigohogarJdbcTemplate, idInicio, request.getEstoyTrabajandoSolo());
                result.put("estoy_trabajando_solo", request.getEstoyTrabajandoSolo());
                result.put("estoyTrabajandoSolo", request.getEstoyTrabajandoSolo());
            }
            if (!isBlank(request.getUbicacionGeoRef())) {
                repository.actualizarUbicacionInicio(tigohogarJdbcTemplate, idInicio, request.getUbicacionGeoRef().trim());
            }
            if (!isBlank(request.getFirmaInicio())) {
                repository.actualizarFirmaInicio(tigohogarJdbcTemplate, idInicio, request.getFirmaInicio().trim());
                result.put("firma_inicio", request.getFirmaInicio().trim());
                result.put("firmaInicio", request.getFirmaInicio().trim());
            }
            if (idAuxiliarRegistro != null && !isBlank(request.getImagenAuxiliar())) {
                repository.actualizarImagenAuxiliarInicio(tigohogarJdbcTemplate, idInicio, request.getImagenAuxiliar().trim());
                result.put("imagen_auxiliar", request.getImagenAuxiliar().trim());
                result.put("imagenAuxiliar", request.getImagenAuxiliar().trim());
            }
        }
        return result;
    }

    private void agregarAuxiliarConformacion(Map<String, Object> out, Map<String, Object> conformacion) {
        if (out == null || conformacion == null) {
            return;
        }
        Integer idAuxiliar = toPositiveInteger(conformacion.get("idAuxiliar"));
        String auxiliar = valueAsString(conformacion.get("auxiliar"));
        if (idAuxiliar != null) {
            out.put("idAuxiliar", idAuxiliar);
            out.put("id_auxiliar", idAuxiliar);
        }
        if (!isBlank(auxiliar)) {
            out.put("auxiliar", auxiliar);
            out.put("auxiliarNombre", auxiliar);
        }
        out.put("tieneAuxiliar", idAuxiliar != null || !isBlank(auxiliar));
    }

    public Map<String, Object> cerrarJornada(String token, TecnicoCierreJornadaRequest request) {
        AuthLoginResponse tecnico = requireUsuarioInicioJornada(token);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Solicitud requerida.");
        }
        if (isBlank(request.getCodigoCliente()) || isBlank(request.getDanoMaterial())
                || isBlank(request.getDanoPersona()) || isBlank(request.getNovedadesTrabajo())
                || isBlank(request.getUbicacionGeoRef())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Campos obligatorios de cierre incompletos.");
        }
        if (isBlank(request.getFirmaCierre())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La firma de cierre es obligatoria.");
        }
        Integer idInicio = request.getIdInicio();
        if ((idInicio == null || idInicio <= 0) && repository.existePendienteAprobacionHoy(tigohogarJdbcTemplate, tecnico.getIdUsuario())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "INICIO_JORNADA_PENDIENTE_APROBACION",
                    "No puedes cerrar jornada hasta que tu supervisor apruebe el inicio de jornada."
            );
        }

        String danoMaterial = normalizeSiNo(request.getDanoMaterial());
        String danoPersona = normalizeSiNo(request.getDanoPersona());
        String novedadesTrabajo = normalizeSiNo(request.getNovedadesTrabajo());
        boolean danoMaterialBit = "SI".equals(danoMaterial);
        boolean danoPersonaBit = "SI".equals(danoPersona);
        boolean novedadesTrabajoBit = "SI".equals(novedadesTrabajo);

        if ("SI".equals(danoMaterial) && isBlank(request.getObservacionMaterial())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Observacion material es requerida.");
        }
        if ("SI".equals(danoPersona) && isBlank(request.getObservacionPersona())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Observacion persona es requerida.");
        }
        if ("SI".equals(novedadesTrabajo) && isBlank(request.getObservacionNovedades())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Observacion novedades es requerida.");
        }
        if (!"SI".equals(normalizeSiNo(request.getAceptoCierreJornada()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Debe aceptar la declaracion jurada para registrar el cierre de jornada.");
        }

        List<Map<String, Object>> rows;
        boolean cierrePendienteAnterior = idInicio != null && idInicio > 0;
        if (cierrePendienteAnterior && repository.inicioPendienteAprobacion(tigohogarJdbcTemplate, idInicio, tecnico.getIdUsuario())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "CIERRE_PENDIENTE_APROBACION_SUPERVISOR",
                    "No se puede registrar el cierre hasta que el supervisor confirme el dia de ayer."
            );
        }
        if (idInicio != null && idInicio > 0) {
            rows = repository.cerrarJornadaPorId(
                    tigohogarJdbcTemplate,
                    idInicio,
                    tecnico.getIdUsuario(),
                    request.getCodigoCliente().trim(),
                    danoMaterialBit,
                    trimOrNull(request.getObservacionMaterial()),
                    danoPersonaBit,
                    trimOrNull(request.getObservacionPersona()),
                    novedadesTrabajoBit,
                    trimOrNull(request.getObservacionNovedades()),
                    request.getUbicacionGeoRef().trim()
            );
        } else {
            rows = repository.cerrarJornada(
                    tigohogarJdbcTemplate,
                    tecnico.getIdUsuario(),
                    request.getCodigoCliente().trim(),
                    danoMaterialBit,
                    trimOrNull(request.getObservacionMaterial()),
                    danoPersonaBit,
                    trimOrNull(request.getObservacionPersona()),
                    novedadesTrabajoBit,
                    trimOrNull(request.getObservacionNovedades()),
                    request.getUbicacionGeoRef().trim()
            );
        }

        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_OPEN_JORNADA", "No existe jornada abierta hoy para cerrar.");
        }
        Map<String, Object> result = rows.get(0);
        Integer idInicioCerrado = toPositiveInteger(
                result.get("idInicio") != null ? result.get("idInicio") : result.get("id_inicio")
        );
        if (idInicioCerrado != null) {
            if (cierrePendienteAnterior) {
                repository.marcarNoMarcoCierreCompletado(tigohogarJdbcTemplate, idInicioCerrado);
            } else {
                repository.marcarCierreCompletado(tigohogarJdbcTemplate, idInicioCerrado);
            }
            repository.actualizarAceptoCierreJornada(tigohogarJdbcTemplate, idInicioCerrado, "SI");
            repository.actualizarFirmaCierre(tigohogarJdbcTemplate, idInicioCerrado, request.getFirmaCierre().trim());
            result.put("firma_cierre", request.getFirmaCierre().trim());
            result.put("firmaCierre", request.getFirmaCierre().trim());
            if (!cierrePendienteAnterior) {
                result.put("pendiente", false);
            }
            result.put("no_marco_cierre", false);
            result.put("noMarcoCierre", false);
        }
        return result;
    }

    private void agregarCierrePendienteAyer(Map<String, Object> out, Map<String, Object> cierrePendienteAyer) {
        boolean requiereCierreAyer = cierrePendienteAyer != null && !cierrePendienteAyer.isEmpty();
        out.put("requiereCierreAyer", requiereCierreAyer);
        out.put("cierreAyerPendiente", requiereCierreAyer);
        out.put("requiereCierrePendiente", requiereCierreAyer);
        out.put("cierrePendiente", requiereCierreAyer);
        if (!requiereCierreAyer) {
            return;
        }
        Object idInicio = cierrePendienteAyer.get("id_inicio") != null
                ? cierrePendienteAyer.get("id_inicio")
                : cierrePendienteAyer.get("idInicio");
        Object fechaInicio = cierrePendienteAyer.get("fecha_registro") != null
                ? cierrePendienteAyer.get("fecha_registro")
                : cierrePendienteAyer.get("fechaRegistro");
        Object idSupervisor = cierrePendienteAyer.get("id_encargado") != null
                ? cierrePendienteAyer.get("id_encargado")
                : cierrePendienteAyer.get("idSupervisor");
        Object supervisorNombre = cierrePendienteAyer.get("supervisor_nombre") != null
                ? cierrePendienteAyer.get("supervisor_nombre")
                : cierrePendienteAyer.get("supervisorNombre");
        if (supervisorNombre == null) {
            supervisorNombre = resolverNombreSupervisor(idSupervisor);
        }
        out.put("idInicioPendienteCierre", idInicio);
        out.put("id_inicio_pendiente_cierre", idInicio);
        out.put("idUltimoInicioPendienteCierre", idInicio);
        out.put("fechaInicioPendienteCierre", fechaInicio);
        out.put("fecha_inicio_pendiente_cierre", fechaInicio);
        out.put("fechaUltimoInicioPendienteCierre", fechaInicio);
        out.put("idSupervisorPendienteCierre", idSupervisor);
        out.put("id_supervisor_pendiente_cierre", idSupervisor);
        out.put("supervisorPendienteCierre", supervisorNombre);
        out.put("supervisor_pendiente_cierre", supervisorNombre);
    }

    private String resolverNombreSupervisor(Object idSupervisorRaw) {
        Integer idSupervisor = toPositiveInteger(idSupervisorRaw);
        if (idSupervisor == null) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = dbConnectionManager.connDb("operativa").queryForList(
                    "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ?",
                    idSupervisor
            );
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return trimOrNull(valueAsString(rows.get(0).get("Nombre")));
        } catch (Exception ex) {
            return null;
        }
    }

    private AuthLoginResponse requireUsuarioInicioJornada(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me.getUsuario();
        String rol = normalize(usuario == null ? null : usuario.getRol());
        boolean permitido = "tecnico".equals(rol);
        if (!permitido) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_INICIO_JORNADA_ONLY",
                    "Esta funcionalidad es solo para rol Tecnico."
            );
        }
        return usuario;
    }

    private String normalizeSiNo(String value) {
        String normalized = normalize(value);
        if ("si".equals(normalized) || "sí".equals(normalized) || "true".equals(normalized) || "1".equals(normalized)) {
            return "SI";
        }
        return "NO";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private Integer toPositiveInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            int n = ((Number) value).intValue();
            return n > 0 ? n : null;
        }
        try {
            int n = Integer.parseInt(String.valueOf(value).trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JdbcTemplate resolveTecnicosTemplate(String sucursal, AuthLoginResponse usuarioSesion) {
        SucursalResponse sucursalInfo = resolveSucursalInfo(sucursal, usuarioSesion);
        if (sucursalInfo != null && !isBlank(sucursalInfo.getIp()) && !isBlank(sucursalInfo.getBaseDeDatos())) {
            return dbConnectionManager.connDb(
                    "inicio-jornada-" + sucursalInfo.getIdSucursal(),
                    sucursalInfo.getIp(),
                    sucursalInfo.getBaseDeDatos(),
                    defaultDbUsername,
                    defaultDbPassword
            );
        }
        return dbConnectionManager.connDb(resolveTecnicosDb(sucursal));
    }

    private String resolveTecnicosDb(String sucursal) {
        String normalized = normalize(sucursal);
        if (normalized.contains("sucre")) {
            return "sucre";
        }
        return "operativa";
    }

    private SucursalResponse resolveSucursalInfo(String sucursal, AuthLoginResponse usuarioSesion) {
        List<SucursalResponse> sucursales = authService.listarSucursales();
        Integer idSucursal = usuarioSesion == null ? null : usuarioSesion.getIdSucursal();
        if (idSucursal != null) {
            for (SucursalResponse item : sucursales) {
                if (item != null && idSucursal.equals(item.getIdSucursal())) {
                    return item;
                }
            }
        }
        if (!isBlank(sucursal)) {
            String canonTarget = SucursalCanonicalizer.canonicalize(sucursal);
            for (SucursalResponse item : sucursales) {
                if (item == null) continue;
                String canon = SucursalCanonicalizer.canonicalize(item.getSucursal());
                if (canonTarget.equalsIgnoreCase(canon)) {
                    return item;
                }
            }
        }
        return null;
    }

    private String resolveSucursalNombre(String sucursal, AuthLoginResponse usuarioSesion) {
        Integer idSucursal = usuarioSesion == null ? null : usuarioSesion.getIdSucursal();
        if (idSucursal != null) {
            List<SucursalResponse> sucursales = authService.listarSucursales();
            for (SucursalResponse item : sucursales) {
                if (item != null && idSucursal.equals(item.getIdSucursal())) {
                    return SucursalCanonicalizer.canonicalize(item.getSucursal());
                }
            }
        }
        if (!isBlank(sucursal)) {
            return SucursalCanonicalizer.canonicalize(sucursal);
        }
        return null;
    }

    private Integer resolveSucursalId(String sucursalCanonica) {
        if (isBlank(sucursalCanonica)) {
            return null;
        }
        try {
            List<SucursalResponse> sucursales = authService.listarSucursales();
            for (SucursalResponse item : sucursales) {
                if (item == null) continue;
                String canon = SucursalCanonicalizer.canonicalize(item.getSucursal());
                if (sucursalCanonica.equalsIgnoreCase(canon)) {
                    return item.getIdSucursal();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String valueAsString(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private List<Map<String, Object>> ensureEncargadoActualEnLista(
            List<Map<String, Object>> encargados,
            Map<String, Object> encargadoActual
    ) {
        if (encargados == null) {
            encargados = new java.util.ArrayList<>();
        }
        if (encargadoActual == null) {
            return encargados;
        }
        String idActual = valueAsString(encargadoActual.get("idEncargado"));
        String nombreActual = valueAsString(encargadoActual.get("encargado"));
        if (isBlank(idActual)) {
            return encargados;
        }
        boolean exists = false;
        for (Map<String, Object> row : encargados) {
            String idRow = valueAsString(row == null ? null : row.get("idEncargado"));
            if (idActual.equals(idRow)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idEncargado", idActual);
            item.put("encargado", isBlank(nombreActual) ? ("Supervisor " + idActual) : nombreActual);
            encargados.add(0, item);
        }
        return encargados;
    }
}
