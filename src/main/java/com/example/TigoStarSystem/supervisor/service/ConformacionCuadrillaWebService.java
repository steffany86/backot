package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaWebRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaWebResponse;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaRepository;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaWebRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
public class ConformacionCuadrillaWebService {
    private final ConformacionCuadrillaWebRepository repository;
    private final ConformacionCuadrillaRepository backofficeRepository;
    private final ConformacionCuadrillaMailService mailService;
    private final ConformacionCuadrillaRequestValidator validator;
    private final AuthService authService;

    /**
     * Inicializa el servicio web de conformacion de cuadrilla.
     */
    public ConformacionCuadrillaWebService(
            ConformacionCuadrillaWebRepository repository,
            ConformacionCuadrillaRepository backofficeRepository,
            ConformacionCuadrillaMailService mailService,
            AuthService authService) {
        this.repository = repository;
        this.backofficeRepository = backofficeRepository;
        this.mailService = mailService;
        this.authService = authService;
        this.validator = new ConformacionCuadrillaRequestValidator();
    }

    /**
     * Lista conformaciones web filtrando por fecha/sucursal y limite.
     */
    public List<ConformacionCuadrillaWebResponse> listar(
            LocalDate fecha,
            String sucursal,
            Integer limite,
            String token) {
        if (limite != null && limite < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "limite no puede ser negativo."
            );
        }
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        List<Map<String, Object>> rows = backofficeRepository.listarGruposFiltroEdicion(sucursalResuelta);
        List<ConformacionCuadrillaWebResponse> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }

        LocalDate fechaSalida = fecha == null ? LocalDate.now() : fecha;
        for (Map<String, Object> row : rows) {
            out.add(mapRutaRowToWebResponse(row, sucursalResuelta, fechaSalida));
        }
        if (limite == null || limite <= 0 || out.size() <= limite) {
            return out;
        }
        return new ArrayList<>(out.subList(0, limite));
    }

    /**
     * Obtiene el detalle de una conformacion web por id.
     */
    public ConformacionCuadrillaWebResponse obtenerPorId(Long id, String sucursal, String token) {
        validarId(id);
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        ConformacionCuadrillaWebResponse row = repository.obtenerPorId(id, sucursalResuelta);
        if (row == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Registro de conformacion cuadrilla web no encontrado."
            );
        }
        return row;
    }

    /**
     * Crea una conformacion web, valida datos y envia correo de seguimiento.
     */
    public ConformacionCuadrillaWebResponse crear(ConformacionCuadrillaWebRequest request, String token) {
        completarContextoSesion(request, token);
        validator.validarWeb(request);
        Long id = repository.crear(request);
        if (id == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INSERT_FAILED",
                    "No se pudo registrar la conformacion cuadrilla web."
            );
        }

        enviarCorreo(request);
        ConformacionCuadrillaWebResponse persisted = repository.obtenerPorId(id, request.getSucursal());
        return persisted == null ? repository.obtenerPorIdPersistido(id) : persisted;
    }

    /**
     * Actualiza una conformacion; si no existe intenta fallback y luego upsert.
     */
    public ConformacionCuadrillaWebResponse actualizar(Long id, ConformacionCuadrillaWebRequest request, String token) {
        validarId(id);
        completarContextoSesion(request, token);
        validator.validarWeb(request);
        int affected = repository.actualizar(id, request);
        if (affected == 0) {
            int affectedBackoffice = backofficeRepository.actualizarFila(id, mapToBackOfficeRow(request));
            if (affectedBackoffice > 0) {
                enviarCorreo(request);
                ConformacionCuadrillaWebResponse byRoute = repository.obtenerPorId(id, request.getSucursal());
                if (byRoute != null) {
                    return byRoute;
                }
                return construirRespuestaDesdeRequest(id, request);
            }

            Long nuevoId = repository.crear(request);
            if (nuevoId == null) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "UPSERT_FAILED",
                        "No se pudo actualizar ni registrar la conformacion cuadrilla web."
                );
            }
            enviarCorreo(request);
            ConformacionCuadrillaWebResponse byRoute = repository.obtenerPorId(id, request.getSucursal());
            if (byRoute != null) {
                return byRoute;
            }
            ConformacionCuadrillaWebResponse bySavedId = repository.obtenerPorIdPersistido(nuevoId);
            if (bySavedId != null) {
                return bySavedId;
            }
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UPSERT_READ_FAILED",
                    "Conformacion cuadrilla web guardada, pero no se pudo recuperar el registro."
            );
        }
        enviarCorreo(request);
        ConformacionCuadrillaWebResponse persisted = repository.obtenerPorId(id, request.getSucursal());
        return persisted == null ? repository.obtenerPorIdPersistido(id) : persisted;
    }

    /**
     * Construye respuesta minima con datos del request cuando no se pudo releer de BD.
     */
    private ConformacionCuadrillaWebResponse construirRespuestaDesdeRequest(Long id, ConformacionCuadrillaWebRequest request) {
        ConformacionCuadrillaWebResponse out = new ConformacionCuadrillaWebResponse();
        out.setId(id);
        out.setFecha(request.getFecha());
        out.setEstado(request.getEstado());
        out.setActividad(request.getActividad());
        out.setIdTecnico(request.getIdTecnico());
        out.setCuentaSf(request.getCuentaSf());
        out.setSalesforce(request.getSalesforce());
        out.setHabilidad(request.getHabilidad());
        out.setVehiculo(request.getVehiculo());
        out.setGrupo(request.getGrupo());
        out.setAlmacen(request.getAlmacen());
        out.setGrupoDigitacion(request.getGrupoDigitacion());
        out.setIdUsuarioDigitador(request.getIdUsuarioDigitador());
        out.setDigitador(request.getDigitador());
        out.setTecnico(request.getTecnico());
        out.setIdTecnicoAuxiliar(request.getIdTecnicoAuxiliar());
        out.setAuxiliar(request.getAuxiliar());
        out.setIdUsuarioSupervisor(request.getIdUsuarioSupervisor());
        out.setSupervisorACargo(request.getSupervisorACargo());
        out.setSucursal(request.getSucursal());
        out.setObservacion(request.getObservacion());
        out.setIdUsuarioRegistra(request.getIdUsuarioRegistra());
        out.setEEliminado(false);
        return out;
    }

    /**
     * Elimina (logico/fisico segun SP) una conformacion web por id.
     */
    public int eliminar(Long id) {
        validarId(id);
        int affected = repository.eliminar(id);
        if (affected == 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Registro de conformacion cuadrilla web no encontrado."
            );
        }
        return affected;
    }

    /**
     * Lista tecnicos para el formulario web aplicando busqueda y limite.
     */
    public List<Map<String, Object>> listarTecnicos(String q, Integer limit, String sucursal, String token) {
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        return TecnicoSearchUtil.filterAndLimit(repository.listarTecnicos(sucursalResuelta), q, limit);
    }

    /**
     * Obtiene detalle de tecnico para autocompletado/formulario web.
     */
    public List<Map<String, Object>> obtenerTecnicoDetalle(Integer idTecnico, String sucursal, String token) {
        if (idTecnico == null || idTecnico <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idTecnico es requerido."
            );
        }
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        return repository.obtenerTecnicoDetalle(idTecnico, sucursalResuelta);
    }

    /**
     * Lista auxiliares disponibles en la sucursal resuelta.
     */
    public List<Map<String, Object>> listarAuxiliares(String sucursal, String token) {
        return repository.listarAuxiliares(resolveSucursalNombre(sucursal, token));
    }

    /**
     * Lista digitadores disponibles en la sucursal resuelta.
     */
    public List<Map<String, Object>> listarDigitadores(String sucursal, String token) {
        return repository.listarDigitadores(resolveSucursalNombre(sucursal, token));
    }

    /**
     * Lista supervisores disponibles en la sucursal resuelta.
     */
    public List<Map<String, Object>> listarSupervisores(String sucursal, String token) {
        return repository.listarSupervisores(resolveSucursalNombre(sucursal, token));
    }

    /**
     * Lista actividades disponibles en la sucursal resuelta.
     */
    public List<Map<String, Object>> listarActividades(String sucursal, String token) {
        return repository.listarActividades(resolveSucursalNombre(sucursal, token));
    }

    /**
     * Lista vehiculos por filtro textual en la sucursal resuelta.
     */
    public List<Map<String, Object>> listarVehiculos(String filtro, String sucursal, String token) {
        return repository.listarVehiculos(resolveSucursalNombre(sucursal, token), filtro);
    }

    /**
     * Lista sucursales visibles para el flujo web.
     */
    public List<Map<String, Object>> listarSucursales(String sucursal, String token) {
        return repository.listarSucursales(resolveSucursalNombre(sucursal, token));
    }

    /**
     * Valida que un id sea positivo y no nulo.
     */
    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "id es requerido."
            );
        }
    }

    /**
     * Envia correo con cuadrillas no confirmadas usando contexto del request.
     */
    private void enviarCorreo(ConformacionCuadrillaWebRequest request) {
        List<ConformacionCuadrillaRowRequest> confirmadas = new ArrayList<>();
        confirmadas.add(mapToBackOfficeRow(request));
        mailService.enviarDetalleCuadrillasNoConfirmadas(
                backofficeRepository.listarGruposFiltroEdicion(request.getSucursal()),
                confirmadas
        );
    }

    /**
     * Convierte request web al modelo row usado por backoffice.
     */
    private ConformacionCuadrillaRowRequest mapToBackOfficeRow(ConformacionCuadrillaWebRequest in) {
        ConformacionCuadrillaRowRequest out = new ConformacionCuadrillaRowRequest();
        out.setFecha(in.getFecha());
        out.setEstado(in.getEstado());
        out.setActividad(in.getActividad());
        out.setIdTecnico(in.getIdTecnico());
        out.setCuentaSf(in.getCuentaSf());
        out.setSalesforce(in.getSalesforce());
        out.setHabilidad(in.getHabilidad());
        out.setVehiculo(in.getVehiculo());
        out.setGrupo(in.getGrupo());
        out.setAlmacen(in.getAlmacen());
        out.setGrupoDigitacion(in.getGrupoDigitacion());
        out.setIdUsuarioDigitador(in.getIdUsuarioDigitador());
        out.setDigitador(in.getDigitador());
        out.setTecnico(in.getTecnico());
        out.setIdTecnicoAuxiliar(in.getIdTecnicoAuxiliar());
        out.setAuxiliar(in.getAuxiliar());
        out.setIdUsuarioSupervisor(in.getIdUsuarioSupervisor());
        out.setSupervisorACargo(in.getSupervisorACargo());
        out.setSucursal(SucursalCanonicalizer.canonicalize(in.getSucursal()));
        out.setObservacion(in.getObservacion());
        out.setIdUsuarioRegistra(in.getIdUsuarioRegistra());
        return out;
    }

    /**
     * Completa sucursal desde sesion cuando no llega en el request.
     */
    private void completarContextoSesion(ConformacionCuadrillaWebRequest request, String token) {
        if (request == null) {
            return;
        }
        if (isBlank(request.getSucursal())) {
            request.setSucursal(resolveSucursalNombre(null, token));
        }
    }

    /**
     * Mapea una fila del SP de rutas al DTO web usado por el front.
     */
    private ConformacionCuadrillaWebResponse mapRutaRowToWebResponse(
            Map<String, Object> row,
            String sucursal,
            LocalDate fecha) {
        ConformacionCuadrillaWebResponse out = new ConformacionCuadrillaWebResponse();
        out.setId(toLong(readValue(row, "id", "id_ruta", "idruta", "Id_Ruta")));
        out.setFecha(fecha);
        out.setActividad(resolverActividadDesdeRuta(row));
        out.setIdTecnico(toInteger(readValue(row, "id_tecnico", "idtecnico", "id_vendedor", "Id_Vendedor")));
        out.setTecnico(toString(readValue(row, "tecnico", "nombrevendedor", "vendedor", "nombre")));
        out.setGrupo(toString(readValue(row, "grupo", "cuadrilla", "ruta", "nombre", "Nombre")));
        out.setVehiculo(toString(readValue(row, "vehiculo", "Vehiculo", "placa", "placaVehiculo")));
        out.setAlmacen(toString(readValue(row, "almacen", "almacen_tigo", "almacenTigo", "BodegaTigo")));
        out.setGrupoDigitacion(toString(readValue(row, "grupoDigitacion", "grupodigitacion")));
        out.setSucursal(SucursalCanonicalizer.canonicalize(
                isBlank(sucursal) ? toString(readValue(row, "sucursal", "Sucursal")) : sucursal
        ));
        out.setEEliminado(toBoolean(readValue(row, "e_eliminado", "eeliminado", "eliminado", "E_Eliminado")));
        return out;
    }

    private Object readValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (current.equals(normalizeKey(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String resolverActividadDesdeRuta(Map<String, Object> row) {
        String actividad = toUpperTrim(readValue(row, "actividad", "tipoactividad", "tipo"));
        if ("BACKUP".equals(actividad)) {
            return "BACKUP";
        }
        if ("TITULAR".equals(actividad)) {
            return "TITULAR";
        }
        return "TITULAR";
    }

    private String toUpperTrim(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if ("1".equals(text) || "true".equals(text) || "si".equals(text) || "s".equals(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equals(text) || "no".equals(text) || "n".equals(text)) {
            return false;
        }
        return null;
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Resuelve nombre de sucursal con prioridad: parametro > token > id como texto.
     */
    private String resolveSucursalNombre(String sucursal, String token) {
        if (!isBlank(sucursal)) {
            return SucursalCanonicalizer.canonicalize(sucursal);
        }
        if (isBlank(token)) {
            return null;
        }

        AuthMeResponse me = authService.me(token);
        Integer idSucursal = me.getUsuario() == null ? null : me.getUsuario().getIdSucursal();
        if (idSucursal == null) {
            return null;
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return SucursalCanonicalizer.canonicalize(String.valueOf(idSucursal));
    }

    /**
     * Verifica si un texto es nulo o vacio.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
