package com.example.TigoStarSystem.supervision.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervision.dto.SupervisionCrearRequest;
import com.example.TigoStarSystem.supervision.dto.SupervisionCrearPendienteRequest;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import com.example.TigoStarSystem.supervision.repository.SupervisionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupervisionService {
    private final SupervisionRepository repository;
    private final AuthService authService;

    public static class JornadaImagen {
        private final byte[] bytes;
        private final String contentType;

        public JornadaImagen(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public SupervisionService(
            SupervisionRepository repository,
            AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        validarRangoFechas(fechaDesde, fechaHasta);
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        return repository.listar(String.valueOf(idSupervisor), fechaDesde, fechaHasta, limite);
    }

    public List<Map<String, Object>> listarPendientes(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        validarRangoFechas(fechaDesde, fechaHasta);
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursal = resolveSucursalNombre(me);
        List<Map<String, Object>> out = new ArrayList<>();
        out.addAll(repository.listarPendientes(String.valueOf(idSupervisor), fechaDesde, fechaHasta, limite));
        out.addAll(enriquecerRevisionesPenalizadas(
                repository.listarRevisionesPenalizadasSupervisor(idSupervisor),
                sucursal
        ));
        return out;
    }

    public List<Map<String, Object>> listarBackofficePorEstado(
            String estadoSup,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        authService.me(token);
        validarRangoFechas(fechaDesde, fechaHasta);
        String estado = normalizarEstadoSup(estadoSup);
        return repository.listarPorEstado(estado, null, fechaDesde, fechaHasta, limite);
    }

    public Map<String, Object> obtenerDetalle(String idSupervision, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursal = resolveSucursalNombre(me);
        Map<String, Object> detalle = repository.obtenerDetalle(idSupervision, String.valueOf(idSupervisor));
        if (detalle == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "No se encontro la nota de supervision indicada."
            );
        }
        return repository.enriquecerDetalleConNombres(detalle, sucursal);
    }

    public Map<String, Object> registrar(SupervisionCrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de supervision es requerido."
            );
        }

        String idGenerado = repository.registrar(
                idSupervisor,
                request.getIdTecnicoPrincipal(),
                request.getIdTecnicoAuxiliar(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion(),
                request.getSupervisionPor(),
                request.getTecnologia(),
                request.getCodigo(),
                request.getOrdenTrabajo(),
                request.getTipoRevision(),
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion()
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idGenerado);
        out.put("idUsuarioSesion", idSupervisor);
        return out;
    }

    public Map<String, Object> realizarPendiente(String idSupervision, SupervisionCrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);

        if (isBlank(idSupervision)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSupervision es requerido.");
        }
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request de supervision es requerido.");
        }
        if (isBlank(request.getUbicacion())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ubicacion es requerido.");
        }

        int updated = repository.realizarPendiente(
                idSupervision,
                idSupervisor,
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion()
        );
        if (updated <= 0 && esRevisionPenalizada(idSupervision)) {
            String idGenerado = repository.registrar(
                    idSupervisor,
                    request.getIdTecnicoPrincipal(),
                    request.getIdTecnicoAuxiliar(),
                    request.getIdTipoSupervision(),
                    request.getIdTipoTrabajo(),
                    request.getIdTipoPenalizacion(),
                    request.getSupervisionPor(),
                    request.getTecnologia(),
                    request.getCodigo(),
                    request.getOrdenTrabajo(),
                    request.getTipoRevision(),
                    request.getFotoBoletaSupervision(),
                    request.getFotoCanalesPilos(),
                    request.getFotoNivelesDocsis(),
                    request.getFotoMedicionRuido(),
                    request.getFotoBarridoCanales(),
                    request.getFotoObservacion1(),
                    request.getFotoObservacion2(),
                    request.getFotoObservacion3(),
                    request.getFotoObservacion4(),
                    request.getObservacion(),
                    request.getDescripcionAdicionalObservacion(),
                    request.getUbicacion()
            );
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idSupervision", idGenerado);
            out.put("idUsuarioSesion", idSupervisor);
            out.put("estadoSup", "completado");
            out.put("origen", "REV_PENALIZADA");
            return out;
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro supervision pendiente para realizar.");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idSupervision);
        out.put("idUsuarioSesion", idSupervisor);
        out.put("estadoSup", "completado");
        return out;
    }

    public List<Map<String, Object>> listarTiposSupervision(String token) {
        authService.me(token);
        try {
            return repository.listarTiposSupervision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposTrabajo(String token) {
        authService.me(token);
        try {
            return repository.listarTiposTrabajo();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposPenalizacion(String token) {
        authService.me(token);
        try {
            return repository.listarTiposPenalizacion();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTecnicosSupervisor(String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        try {
            return repository.listarTecnicosDeGrupos(sucursal);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarIniciosPendientes(String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursal = resolveSucursalNombre(me);
        try {
            return repository.listarIniciosJornadaPendientesSupervisor(idSupervisor, sucursal);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarIniciosConfirmadosHoy(String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursal = resolveSucursalNombre(me);
        try {
            return repository.listarIniciosJornadaConfirmadosHoySupervisor(idSupervisor, sucursal);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarHistoricoJornadasSupervisor(LocalDate fecha, Integer idTecnico, String token) {
        return listarHistoricoJornadasSupervisor(fecha, null, null, idTecnico, token);
    }

    public List<Map<String, Object>> listarHistoricoJornadasSupervisor(LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, Integer idTecnico, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursal = resolveSucursalNombre(me);
        LocalDate desde = fechaDesde == null ? (fecha == null ? LocalDate.now() : fecha) : fechaDesde;
        LocalDate hasta = fechaHasta == null ? desde : fechaHasta;
        if (hasta.isBefore(desde)) {
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }
        try {
            return listarHistoricoJornadasRango(desde, hasta, sucursal, idSupervisor, idTecnico, true);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarHistoricoJornadasBackoffice(LocalDate fecha, String sucursal, Integer idTecnico, String token) {
        return listarHistoricoJornadasBackoffice(fecha, null, null, sucursal, idTecnico, token);
    }

    public List<Map<String, Object>> listarHistoricoJornadasBackoffice(LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, String sucursal, Integer idTecnico, String token) {
        AuthMeResponse me = authService.me(token);
        LocalDate desde = fechaDesde == null ? (fecha == null ? LocalDate.now() : fecha) : fechaDesde;
        LocalDate hasta = fechaHasta == null ? desde : fechaHasta;
        if (hasta.isBefore(desde)) {
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }
        String sucursalResuelta = SucursalCanonicalizer.canonicalize(
                isBlank(sucursal) ? null : sucursal
        );
        try {
            return listarHistoricoJornadasRango(desde, hasta, sucursalResuelta, null, idTecnico, false);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> listarHistoricoJornadasRango(
            LocalDate desde,
            LocalDate hasta,
            String sucursal,
            Integer idSupervisor,
            Integer idTecnico,
            boolean limitarSupervisor) {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate cursor = desde;
        while (!cursor.isAfter(hasta)) {
            out.addAll(repository.listarHistoricoJornadas(cursor, sucursal, idSupervisor, idTecnico, limitarSupervisor));
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    public Map<String, Object> obtenerDetalleInicioJornada(Integer idInicio, String token) {
        authService.me(token);
        try {
            return repository.obtenerDetalleInicioJornada(idInicio);
        } catch (DataAccessException ex) {
            return new LinkedHashMap<>();
        }
    }

    public JornadaImagen obtenerImagenInicioJornada(Integer idInicio, boolean miniatura, String token) {
        authService.me(token);
        Object raw = repository.obtenerImagenInicioJornada(idInicio);
        JornadaImagen imagen = decodeImagen(raw);
        if (imagen == null || imagen.getBytes().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro imagen para el inicio de jornada.");
        }
        if (!miniatura) {
            return imagen;
        }
        JornadaImagen thumb = crearMiniatura(imagen, 96, 96);
        return thumb == null ? imagen : thumb;
    }

    public Map<String, Object> aprobarInicioPendiente(Integer idInicio, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        int updated = repository.aprobarInicioJornada(idSupervisor, idInicio);
        if (updated <= 0) {
            updated = repository.aprobarInicioJornadaPorId(idInicio);
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro inicio pendiente para aprobar.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idInicio", idInicio);
        out.put("aprobado", true);
        return out;
    }

    public Map<String, Object> rechazarInicioPendiente(Integer idInicio, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        int updated = repository.rechazarInicioJornada(idSupervisor, idInicio);
        if (updated <= 0) {
            updated = repository.rechazarInicioJornadaPorId(idInicio);
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro inicio pendiente para rechazar.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idInicio", idInicio);
        out.put("rechazado", true);
        return out;
    }

    private Integer resolveIdUsuario(AuthMeResponse me) {
        Integer idUsuario = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;
        if (idUsuario == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo identificar el usuario de la sesion."
            );
        }
        return idUsuario;
    }

    private JornadaImagen decodeImagen(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof byte[]) {
            byte[] bytes = (byte[]) raw;
            return new JornadaImagen(bytes, detectarContentType(bytes, "image/jpeg"));
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        String contentType = "image/jpeg";
        int comma = text.indexOf(',');
        if (text.startsWith("data:image") && comma > 0) {
            String header = text.substring(0, comma);
            int semicolon = header.indexOf(';');
            if (semicolon > 5) {
                contentType = header.substring(5, semicolon);
            }
            text = text.substring(comma + 1);
        }
        text = text.replaceAll("\\s+", "");
        try {
            byte[] bytes = Base64.getDecoder().decode(text);
            return new JornadaImagen(bytes, detectarContentType(bytes, contentType));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private JornadaImagen crearMiniatura(JornadaImagen imagen, int maxWidth, int maxHeight) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imagen.getBytes()));
            if (source == null) {
                return null;
            }
            int width = source.getWidth();
            int height = source.getHeight();
            if (width <= 0 || height <= 0) {
                return null;
            }
            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            if (scale > 1.0d) {
                scale = 1.0d;
            }
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));
            BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            return new JornadaImagen(out.toByteArray(), "image/jpeg");
        } catch (Exception ex) {
            return null;
        }
    }

    private String detectarContentType(byte[] bytes, String fallback) {
        if (bytes == null || bytes.length < 12) {
            return fallback;
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) {
            return "image/gif";
        }
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return "image/webp";
        }
        return fallback;
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "fechaDesde no puede ser mayor a fechaHasta."
            );
        }
    }

    private String normalizarEstadoSup(String estadoSup) {
        String estado = estadoSup == null ? "pendiente" : estadoSup.trim().toLowerCase();
        if ("pendientes".equals(estado)) {
            estado = "pendiente";
        } else if ("completados".equals(estado) || "completada".equals(estado) || "completadas".equals(estado)) {
            estado = "completado";
        }
        if (!"pendiente".equals(estado) && !"completado".equals(estado)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "estado debe ser pendiente o completado."
            );
        }
        return estado;
    }

    private String resolveSucursalNombre(AuthMeResponse me) {
        Integer idSucursal = me != null && me.getUsuario() != null ? me.getUsuario().getIdSucursal() : null;
        if (idSucursal == null) return null;
        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    private List<Map<String, Object>> enriquecerRevisionesPenalizadas(List<Map<String, Object>> revisiones, String sucursal) {
        if (revisiones == null || revisiones.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> tecnicos = repository.listarTecnicosDeGrupos(sucursal);
        Map<String, Map<String, Object>> tecnicoPorNombre = new LinkedHashMap<>();
        for (Map<String, Object> tecnico : tecnicos) {
            String nombre = asText(findValue(tecnico, "tecnico", "nombre", "tecnicoNombre"));
            String key = normalizeName(nombre);
            if (key != null && !tecnicoPorNombre.containsKey(key)) {
                tecnicoPorNombre.put(key, tecnico);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> revision : revisiones) {
            Map<String, Object> item = new LinkedHashMap<>(revision);
            String tecnicoNombre = asText(findValue(item, "tecnicoPrincipal", "tecnicoPrincipalNombre", "tecnico_nombre"));
            Map<String, Object> match = tecnicoPorNombre.get(normalizeName(tecnicoNombre));
            if (match != null) {
                Object idTecnico = findValue(match, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico");
                if (idTecnico != null) {
                    item.put("idTecnicoPrincipal", String.valueOf(idTecnico));
                    item.put("id_tecnico_principal", String.valueOf(idTecnico));
                }
                String nombreMatch = asText(findValue(match, "tecnico", "nombre", "tecnicoNombre"));
                if (!isBlank(nombreMatch)) {
                    item.put("tecnicoPrincipal", nombreMatch);
                    item.put("tecnicoPrincipalNombre", nombreMatch);
                }
            }
            out.add(item);
        }
        return out;
    }

    private boolean esRevisionPenalizada(String idSupervision) {
        return idSupervision != null && idSupervision.trim().startsWith("REV_PENALIZADA:");
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) return null;
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeName(String value) {
        if (isBlank(value)) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    public Map<String, Object> registrarPendiente(SupervisionCrearPendienteRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        String creadoPor = resolveNombreUsuario(me);

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de supervision es requerido."
            );
        }

        String idGenerado = repository.registrarPendiente(
                request.getIdSupervisorAsignado(),
                request.getIdTecnicoPrincipal(),
                request.getIdTecnicoAuxiliar(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion(),
                request.getSupervisionPor(),
                request.getTecnologia(),
                request.getCodigo(),
                request.getOrdenTrabajo(),
                request.getTipoRevision(),
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion(),
                creadoPor
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idGenerado);
        out.put("idSupervisorAsignado", request.getIdSupervisorAsignado());
        out.put("creadoPor", creadoPor);
        return out;
    }

    public List<Map<String, Object>> listarSupervisores(String sucursal, String token) {
        AuthMeResponse me = authService.me(token);
        String sucursalResuelta = SucursalCanonicalizer.canonicalize(
                isBlank(sucursal) ? resolveSucursalNombre(me) : sucursal
        );
        try {
            return repository.listarSupervisores(sucursalResuelta);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal) {
        return listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursal, null);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal, String supervisor) {
        try {
            String sucursalResuelta = SucursalCanonicalizer.canonicalize(sucursal);
            return repository.listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursalResuelta, supervisor);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveNombreUsuario(AuthMeResponse me) {
        String nombre = me != null && me.getUsuario() != null ? me.getUsuario().getNombre() : null;
        if (!isBlank(nombre)) return nombre.trim();
        String login = me != null && me.getUsuario() != null ? me.getUsuario().getLoggin() : null;
        if (!isBlank(login)) return login.trim();
        Integer idUsuario = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;
        return idUsuario == null ? null : String.valueOf(idUsuario);
    }

}
