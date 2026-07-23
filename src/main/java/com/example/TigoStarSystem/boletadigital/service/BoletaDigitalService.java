package com.example.TigoStarSystem.boletadigital.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.boletadigital.repository.BoletaDigitalRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BoletaDigitalService {
    private static final Logger logger = LoggerFactory.getLogger(BoletaDigitalService.class);

    private final BoletaDigitalRepository repository;
    private final AuthService authService;
    private final DbConnectionManager dbConnectionManager;
    private final String dbUsername;
    private final String dbPassword;
    private final String pdfUncShare;

    public BoletaDigitalService(
            BoletaDigitalRepository repository,
            AuthService authService,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${app.boleta-digital.pdf-unc-share:}") String pdfUncShare) {
        this.repository = repository;
        this.authService = authService;
        this.dbConnectionManager = dbConnectionManager;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.pdfUncShare = trimToNull(pdfUncShare);
    }

    public List<Map<String, Object>> listar(String token, LocalDate fechaInicio, LocalDate fechaFin) {
        AuthMeResponse me = authService.me(token);
        LocalDate fechaFinConsulta = fechaFin != null ? fechaFin : LocalDate.now(ZoneId.of("America/La_Paz"));
        LocalDate fechaInicioConsulta = fechaInicio != null ? fechaInicio : fechaFinConsulta.withDayOfMonth(1);
        if (fechaInicioConsulta.isAfter(fechaFinConsulta)) {
            fechaInicioConsulta = fechaFinConsulta.withDayOfMonth(1);
        }
        JdbcTemplate template = resolveSucursalTemplate(me);
        return repository.listarOtArchivo(template, fechaInicioConsulta, fechaFinConsulta);
    }

    public ArchivoPdf cargarArchivo(String token, String rutaRaw) {
        authService.me(token);
        String ruta = trimToNull(rutaRaw);
        if (ruta == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "RutaPDF es requerida.");
        }

        Path path = resolvePdfPath(ruta);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "Archivo no encontrado.");
        }
        String fileName = path.getFileName() == null ? "archivo" : path.getFileName().toString();
        String contentType = contentTypeArchivo(fileName);
        if (contentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La ruta no corresponde a un archivo permitido.");
        }

        try {
            byte[] content = Files.readAllBytes(path);
            return new ArchivoPdf(new ByteArrayResource(content), fileName, contentType);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_READ_ERROR", "No se pudo leer el archivo.");
        }
    }

    @Transactional
    public Map<String, Object> cambiarArchivoDigital(String token, Integer idVenta, MultipartFile archivo) {
        AuthMeResponse me = authService.me(token);
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idVenta es requerido.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Archivo es requerido.");
        }
        String originalName = archivo.getOriginalFilename();
        String originalContentType = contentTypeArchivo(originalName);
        if (originalName == null || originalContentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Solo se permite PDF o imagen JPG/PNG.");
        }

        JdbcTemplate template = resolveSucursalTemplate(me);
        Map<String, Object> venta = repository.obtenerVenta(template, idVenta);
        if (venta == null || venta.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VENTA_NOT_FOUND", "Venta no encontrada.");
        }
        Integer codigoCliente = toInteger(valueOf(venta, "codigoCliente"));
        Integer ordenTrabajo = toInteger(valueOf(venta, "ordenTrabajo"));
        if (codigoCliente == null || ordenTrabajo == null) {
            throw new ApiException(HttpStatus.CONFLICT, "VENTA_INVALIDA", "La venta no tiene cliente u orden valida.");
        }
        String rutaAnterior = trimToNull(valueOf(venta, "rutaPdf") == null ? null : String.valueOf(valueOf(venta, "rutaPdf")));
        boolean reemplazoImagen = esImagen(originalName);
        boolean rutaAnteriorImagen = esImagen(rutaAnterior);
        if (reemplazoImagen && !rutaAnteriorImagen) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Solo se permite reemplazar por imagen cuando el registro actual tiene imagen.");
        }
        Map<String, Object> cita = repository.obtenerCita(codigoCliente, ordenTrabajo);
        String otFisica = trimToNull(cita == null ? null : String.valueOf(valueOf(cita, "OT_FISICA")));
        String comparacion = reemplazoImagen ? "IMAGEN" : calcularComparacionArchivo(originalName, otFisica);
        if (!reemplazoImagen) {
            if (otFisica == null) {
                throw new ApiException(HttpStatus.CONFLICT, "OT_FISICA_NOT_FOUND", "No se encontro OT_FISICA para esta venta.");
            }
            if (!"IGUAL".equals(comparacion)) {
                Map<String, Object> details = new HashMap<>();
                details.put("archivo", originalName);
                details.put("otFisicaEsperada", otFisica);
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "PDF_NAME_MISMATCH",
                        "No coincide el nombre del PDF con la OT fisica esperada.",
                        details
                );
            }
        }
        String nuevaRuta = construirRutaDestino(venta, rutaAnterior, originalName, me);
        Path destino = resolvePdfPath(nuevaRuta);
        if (Files.exists(destino)) {
            Map<String, Object> details = new HashMap<>();
            details.put("rutaPdf", nuevaRuta);
            details.put("archivo", originalName);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PDF_ALREADY_EXISTS",
                    "Ya existe un PDF con ese nombre en la carpeta destino.",
                    details
            );
        }
        nuevaRuta = toRutaServidor(destino);

        boolean archivoCopiado = false;
        try {
            destino = guardarArchivo(destino, nuevaRuta, archivo);
            nuevaRuta = toRutaServidor(destino);
            archivoCopiado = true;

            String usuario = me != null && me.getUsuario() != null ? trimToNull(me.getUsuario().getLoggin()) : null;
            if (usuario == null && me != null && me.getUsuario() != null) {
                usuario = trimToNull(me.getUsuario().getNombre());
            }
            repository.registrarCambioArchivo(template, idVenta, fileNameOrPath(rutaAnterior), originalName, reemplazoImagen ? "IMAGEN" : comparacion, usuario);
            repository.actualizarRutaPdf(template, idVenta, nuevaRuta);
            // El cambio de archivo es el momento en que se confirma la boleta
            // en el historial, tanto para PDF como para imagen.
            marcarHistorialSinFallar(codigoCliente, ordenTrabajo, usuario);

            Map<String, Object> out = new HashMap<>();
            out.put("idVenta", idVenta);
            out.put("rutaPdfAnterior", rutaAnterior);
            out.put("rutaPdf", nuevaRuta);
            out.put("OT_FISICA", otFisica);
            out.put("Comparacion", reemplazoImagen ? "IMAGEN" : comparacion);
            out.put("usuario", usuario);
            return out;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_WRITE_ERROR", "No se pudo guardar el PDF.");
        } catch (RuntimeException ex) {
            if (archivoCopiado) {
                try {
                    Files.deleteIfExists(destino);
                } catch (IOException ignored) {
                    // Si falla el rollback del archivo, se conserva el error original de BD/aplicacion.
                }
            }
            throw ex;
        }
    }

    @Transactional
    public Map<String, Object> renombrarArchivoDigital(String token, Integer idVenta, String nombreArchivo) {
        AuthMeResponse me = authService.me(token);
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idVenta es requerido.");
        }
        String nombreNuevo = trimToNull(nombreArchivo);
        if (nombreNuevo == null || nombreNuevo.contains("\\") || nombreNuevo.contains("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El nombre de archivo no es valido.");
        }

        JdbcTemplate template = resolveSucursalTemplate(me);
        Map<String, Object> venta = repository.obtenerVenta(template, idVenta);
        if (venta == null || venta.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VENTA_NOT_FOUND", "Venta no encontrada.");
        }
        String rutaAnterior = trimToNull(valueOf(venta, "rutaPdf") == null ? null : String.valueOf(valueOf(venta, "rutaPdf")));
        if (rutaAnterior == null) {
            throw new ApiException(HttpStatus.CONFLICT, "FILE_NOT_FOUND", "La venta no tiene archivo para renombrar.");
        }

        String extensionActual = extensionArchivo(rutaAnterior);
        if (extensionActual == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo actual no tiene una extension permitida.");
        }
        if (!hasExtension(nombreNuevo, extensionActual)) {
            nombreNuevo = nombreNuevo + extensionActual;
        }

        Path origen = resolvePdfPath(rutaAnterior);
        if (!Files.exists(origen) || !Files.isRegularFile(origen)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "El archivo actual no existe en el recurso compartido.");
        }
        Path destino = origen.resolveSibling(sanitizeFileName(nombreNuevo));
        if (Files.exists(destino)) {
            throw new ApiException(HttpStatus.CONFLICT, "FILE_ALREADY_EXISTS", "Ya existe un archivo con ese nombre.");
        }

        try {
            Files.move(origen, destino);
            String nuevaRuta = toRutaServidor(destino);
            String usuario = me != null && me.getUsuario() != null ? trimToNull(me.getUsuario().getLoggin()) : null;
            if (usuario == null && me != null && me.getUsuario() != null) {
                usuario = trimToNull(me.getUsuario().getNombre());
            }
            repository.registrarCambioArchivo(template, idVenta, fileNameOrPath(rutaAnterior), nombreNuevo, "RENOMBRADO", usuario);
            repository.actualizarRutaPdf(template, idVenta, nuevaRuta);

            Integer codigoCliente = toInteger(valueOf(venta, "codigoCliente"));
            Integer ordenTrabajo = toInteger(valueOf(venta, "ordenTrabajo"));
            if (codigoCliente != null && ordenTrabajo != null) {
                marcarHistorialSinFallar(codigoCliente, ordenTrabajo, usuario);
            }

            Map<String, Object> out = new HashMap<>();
            out.put("idVenta", idVenta);
            out.put("rutaPdfAnterior", rutaAnterior);
            out.put("rutaPdf", nuevaRuta);
            out.put("nombreArchivoNuevo", nombreNuevo);
            out.put("comparacion", "RENOMBRADO");
            return out;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_RENAME_ERROR", "No se pudo renombrar el archivo.");
        }
    }

    @Transactional
    public Map<String, Object> marcarTodoOk(String token, Integer idVenta, boolean todoOk) {
        AuthMeResponse me = authService.me(token);
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idVenta es requerido.");
        }
        JdbcTemplate template = resolveSucursalTemplate(me);
        Map<String, Object> venta = repository.obtenerVenta(template, idVenta);
        if (venta == null || venta.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VENTA_NOT_FOUND", "Venta no encontrada.");
        }

        String usuario = me != null && me.getUsuario() != null ? trimToNull(me.getUsuario().getLoggin()) : null;
        if (usuario == null && me != null && me.getUsuario() != null) {
            usuario = trimToNull(me.getUsuario().getNombre());
        }
        if (usuario == null) {
            usuario = "SISTEMA";
        }

        repository.marcarTodoOk(template, idVenta, todoOk);
        if (todoOk) {
            Integer codigoCliente = toInteger(valueOf(venta, "codigoCliente"));
            Integer ordenTrabajo = toInteger(valueOf(venta, "ordenTrabajo"));
            if (codigoCliente == null || ordenTrabajo == null) {
                throw new ApiException(HttpStatus.CONFLICT, "VENTA_INVALIDA", "La venta no tiene cliente u orden valida para actualizar el historial.");
            }

            Map<String, Object> cita = repository.obtenerCita(codigoCliente, ordenTrabajo);
            Integer idBoCitaMakiroHistorial = cita == null ? null : toInteger(
                    valueOf(cita, "Id_BO_CITA_MAKIRO_Historial", "id_BO_CITA_MAKIRO_Historial", "idBoCitaMakiroHistorial")
            );
            int historialActualizado = idBoCitaMakiroHistorial != null
                    ? repository.marcarActualizacionBoletaHistorial(idBoCitaMakiroHistorial, usuario)
                    : repository.marcarActualizacionBoletaHistorial(codigoCliente, ordenTrabajo, usuario);
            if (historialActualizado <= 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "HISTORIAL_NOT_FOUND", "No se encontro el historial para marcar Todo OK.");
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("idVenta", idVenta);
        out.put("TodoOk", todoOk);
        out.put("Actualizado_BOLETA", todoOk ? 1 : 0);
        out.put("usuarioModifica_BOLETA", usuario);
        return out;
    }

    @Transactional
    public Map<String, Object> confirmarBoleta(String token, Integer idVenta) {
        AuthMeResponse me = authService.me(token);
        if (idVenta == null || idVenta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idVenta es requerido.");
        }

        JdbcTemplate template = resolveSucursalTemplate(me);
        Map<String, Object> venta = repository.obtenerVenta(template, idVenta);
        if (venta == null || venta.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VENTA_NOT_FOUND", "Venta no encontrada.");
        }

        Integer codigoCliente = toInteger(valueOf(venta, "codigoCliente"));
        Integer ordenTrabajo = toInteger(valueOf(venta, "ordenTrabajo"));
        if (codigoCliente == null || ordenTrabajo == null) {
            throw new ApiException(HttpStatus.CONFLICT, "VENTA_INVALIDA", "La venta no tiene cliente u orden valida.");
        }
        Map<String, Object> cita = repository.obtenerCita(codigoCliente, ordenTrabajo);
        Integer idBoCitaMakiroHistorial = cita == null ? null : toInteger(
                valueOf(cita, "Id_BO_CITA_MAKIRO_Historial", "id_BO_CITA_MAKIRO_Historial", "idBoCitaMakiroHistorial")
        );

        String usuario = me != null && me.getUsuario() != null ? trimToNull(me.getUsuario().getLoggin()) : null;
        if (usuario == null && me != null && me.getUsuario() != null) {
            usuario = trimToNull(me.getUsuario().getNombre());
        }
        if (usuario == null) {
            usuario = "SISTEMA";
        }

        int updated = idBoCitaMakiroHistorial != null
                ? repository.marcarActualizacionBoletaHistorial(idBoCitaMakiroHistorial, usuario)
                : repository.marcarActualizacionBoletaHistorial(codigoCliente, ordenTrabajo, usuario);
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "HISTORIAL_NOT_FOUND", "No se encontro el historial para confirmar.");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("idVenta", idVenta);
        if (idBoCitaMakiroHistorial != null) {
            out.put("Id_BO_CITA_MAKIRO_Historial", idBoCitaMakiroHistorial);
        }
        out.put("Actualizado_BOLETA", 1);
        out.put("usuarioModifica_BOLETA", usuario);
        return out;
    }

    private Path resolvePdfPath(String ruta) {
        String normalized = ruta.replace('/', '\\');
        String prefix = "C:\\archivos_ot_pdf\\";
        Path localPath = Paths.get(ruta).toAbsolutePath().normalize();

        // Las rutas guardadas en BD apuntan al disco C: del servidor de archivos.
        // Cuando el back corre en otra computadora, C: es el disco local del back;
        // por eso se debe intentar primero el recurso UNC remoto.
        if (pdfUncShare != null && normalized.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            String relative = normalized.substring(prefix.length());
            String uncBase = pdfUncShare.endsWith("\\")
                    ? pdfUncShare.substring(0, pdfUncShare.length() - 1)
                    : pdfUncShare;
            Path uncPath = Paths.get(uncBase + "\\" + relative).normalize();
            if (Files.exists(uncPath) && Files.isRegularFile(uncPath)) {
                return uncPath;
            }
            if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
                return localPath;
            }
            logger.warn("No se encontro el archivo de boleta. UNC={} LOCAL={}", uncPath, localPath);
            return uncPath;
        }

        return localPath;
    }

    private Path guardarArchivo(Path destino, String rutaServidor, MultipartFile archivo) throws IOException {
        try {
            copiarArchivo(destino, archivo);
            return destino;
        } catch (IOException ex) {
            Path destinoLocal = Paths.get(rutaServidor).toAbsolutePath().normalize();
            if (destinoLocal.equals(destino)) {
                throw ex;
            }
            logger.warn(
                    "No se pudo guardar PDF en ruta primaria {}; se intentara ruta local {}",
                    destino,
                    destinoLocal,
                    ex
            );
            copiarArchivo(destinoLocal, archivo);
            return destinoLocal;
        }
    }

    private void copiarArchivo(Path destino, MultipartFile archivo) throws IOException {
        Files.createDirectories(destino.getParent());
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, destino);
        }
    }

    private void marcarHistorialSinFallar(Integer codigoCliente, Integer ordenTrabajo, String usuario) {
        try {
            Map<String, Object> cita = repository.obtenerCita(codigoCliente, ordenTrabajo);
            Integer idBoCitaMakiroHistorial = cita == null ? null : toInteger(
                    valueOf(cita, "Id_BO_CITA_MAKIRO_Historial", "id_BO_CITA_MAKIRO_Historial", "idBoCitaMakiroHistorial")
            );
            if (idBoCitaMakiroHistorial != null) {
                repository.marcarActualizacionBoletaHistorial(idBoCitaMakiroHistorial, usuario);
                return;
            }
            repository.marcarActualizacionBoletaHistorial(codigoCliente, ordenTrabajo, usuario);
        } catch (DataAccessException ex) {
            logger.warn(
                    "No se pudo marcar actualizacion BOLETA en historial central. cliente={}, ot={}, usuario={}",
                    codigoCliente,
                    ordenTrabajo,
                    usuario,
                    ex
            );
        }
    }

    private String construirRutaDestino(
            Map<String, Object> venta,
            String rutaAnterior,
            String nombreArchivoOriginal,
            AuthMeResponse me) {
        String fileName = sanitizeFileName(nombreArchivoOriginal);
        String baseRuta = rutaAnterior;
        if (baseRuta != null) {
            String normalized = baseRuta.replace('/', '\\');
            int lastSlash = normalized.lastIndexOf('\\');
            if (lastSlash >= 0) {
                return normalized.substring(0, lastSlash + 1) + fileName;
            }
        }
        LocalDate fecha = toLocalDate(valueOf(venta, "fechaEjecucion"));
        if (fecha == null) {
            fecha = LocalDate.now(ZoneId.of("America/La_Paz"));
        }
        String sucursal = nombreSucursal(me);
        return "C:\\archivos_ot_pdf\\venta\\" +
                sanitizeFolder(sucursal) + "\\" +
                fecha.format(DateTimeFormatter.ofPattern("yyyy\\\\MM\\\\dd")) + "\\" +
                fileName;
    }

    private Path siguienteRutaDisponible(Path base) {
        if (!Files.exists(base)) {
            return base;
        }
        String fileName = base.getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);
        String stem = lower.endsWith(".pdf") ? fileName.substring(0, fileName.length() - 4) : fileName;
        Path parent = base.getParent();
        for (int version = 2; version <= 99; version++) {
            String candidate = stem.replaceFirst("_V\\d+_COD_", "_V" + version + "_COD_") + ".pdf";
            Path path = parent.resolve(candidate);
            if (!Files.exists(path)) {
                return path;
            }
        }
        return parent.resolve(stem + "_" + System.currentTimeMillis() + ".pdf");
    }

    private String toRutaServidor(Path destino) {
        String path = destino.toString();
        if (pdfUncShare != null) {
            String uncBase = pdfUncShare.endsWith("\\") ? pdfUncShare.substring(0, pdfUncShare.length() - 1) : pdfUncShare;
            if (path.toLowerCase(Locale.ROOT).startsWith(uncBase.toLowerCase(Locale.ROOT))) {
                String relative = path.substring(uncBase.length());
                if (relative.startsWith("\\") || relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return "C:\\archivos_ot_pdf\\" + relative.replace('/', '\\');
            }
        }
        return path;
    }

    private String fileNameOrPath(String ruta) {
        if (ruta == null) {
            return null;
        }
        String normalized = ruta.replace('/', '\\');
        int lastSlash = normalized.lastIndexOf('\\');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String sanitizeFileName(String value) {
        String text = value == null ? "documento" : value.trim();
        return text.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sanitizeFolder(String value) {
        String text = value == null || value.trim().isEmpty() ? "Sucursal" : value.trim();
        return text.replace(' ', '_').replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String calcularComparacionArchivo(String nombreArchivoOriginal, String otFisica) {
        String nombre = trimToNull(nombreArchivoOriginal);
        String ot = trimToNull(otFisica);
        if (nombre == null || ot == null) {
            return "DIFERENTE";
        }
        return normalizarComparacion(nombre).contains(normalizarComparacion(ot)) ? "IGUAL" : "DIFERENTE";
    }

    private String contentTypeArchivo(String nombreArchivo) {
        String name = trimToNull(nombreArchivo);
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        int queryIndex = lower.indexOf('?');
        if (queryIndex >= 0) {
            lower = lower.substring(0, queryIndex);
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return null;
    }

    private String extensionArchivo(String nombreArchivo) {
        String name = trimToNull(nombreArchivo);
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        int queryIndex = lower.indexOf('?');
        if (queryIndex >= 0) {
            lower = lower.substring(0, queryIndex);
        }
        String[] extensions = {".pdf", ".png", ".jpg", ".jpeg", ".webp"};
        for (String extension : extensions) {
            if (lower.endsWith(extension)) {
                return extension;
            }
        }
        return null;
    }

    private boolean hasExtension(String nombreArchivo, String extension) {
        return nombreArchivo != null
                && extension != null
                && nombreArchivo.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT));
    }

    private boolean esImagen(String nombreArchivo) {
        String contentType = contentTypeArchivo(nombreArchivo);
        return contentType != null && contentType.startsWith("image/");
    }

    private String normalizarComparacion(String value) {
        return value == null
                ? ""
                : value.trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("\\.PDF$", "")
                        .replaceAll("[^A-Z0-9]", "");
    }

    private String nombreSucursal(AuthMeResponse me) {
        Integer idSucursal = me != null && me.getUsuario() != null ? me.getUsuario().getIdSucursal() : null;
        if (idSucursal == null) {
            return "Sucursal";
        }
        for (SucursalResponse item : authService.listarSucursales()) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                String sucursal = trimToNull(item.getSucursal());
                if (sucursal != null) {
                    return sucursal;
                }
            }
        }
        return "Sucursal_" + idSucursal;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Object valueOf(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Object valueOf(Map<String, Object> row, String key, String... fallbackKeys) {
        Object value = valueOf(row, key);
        if (value != null || fallbackKeys == null) {
            return value;
        }
        for (String fallbackKey : fallbackKeys) {
            value = valueOf(row, fallbackKey);
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(ZoneId.of("America/La_Paz")).toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(value).substring(0, 10));
        } catch (Exception ex) {
            return null;
        }
    }

    private JdbcTemplate resolveSucursalTemplate(AuthMeResponse me) {
        Integer idSucursal = me != null && me.getUsuario() != null ? me.getUsuario().getIdSucursal() : null;
        if (idSucursal == null || idSucursal <= 0) {
            return dbConnectionManager.connDb("operativa");
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item == null || !idSucursal.equals(item.getIdSucursal())) {
                continue;
            }
            String host = trimToNull(item.getIp());
            String base = trimToNull(item.getBaseDeDatos());
            if (host != null && base != null) {
                return dbConnectionManager.connDb(
                        "boleta-digital-sucursal-" + idSucursal,
                        host,
                        base,
                        dbUsername,
                        dbPassword
                );
            }
        }
        return dbConnectionManager.connDb("operativa");
    }

    public static final class ArchivoPdf {
        private final Resource resource;
        private final String fileName;
        private final String contentType;

        public ArchivoPdf(Resource resource, String fileName, String contentType) {
            this.resource = resource;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
