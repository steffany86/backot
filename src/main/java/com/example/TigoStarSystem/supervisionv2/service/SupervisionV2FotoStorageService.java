package com.example.TigoStarSystem.supervisionv2.service;

import com.example.TigoStarSystem.common.ApiException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Guarda las fotos de Supervision V2 como archivos y deja en la columna solo la ruta relativa:
 * {@code yyyy/yyyyMM/yyyyMMdd_NombreColumna_Id.jpg}. Las fotos anteriores (yyyy/archivo.jpg) siguen cargando
 * porque en la columna queda siempre la ruta relativa completa.
 */
@Service
public class SupervisionV2FotoStorageService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ANIO_MES = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path baseDir;

    public SupervisionV2FotoStorageService(
            @Value("${app.supervisionv2.fotos.dir:D:/BDControlOrdenesImagenes/Supervision}") String baseDirRaw) {
        String resolved = baseDirRaw == null || baseDirRaw.trim().isEmpty()
                ? "D:/BDControlOrdenesImagenes/Supervision"
                : baseDirRaw.trim();
        this.baseDir = Paths.get(resolved).toAbsolutePath().normalize();
    }

    /** True si el valor es una imagen nueva (data URL en base64) que hay que guardar como archivo. */
    public boolean esImagenNueva(String valor) {
        if (valor == null) {
            return false;
        }
        String lower = valor.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("data:image/") && lower.contains(";base64,");
    }

    /** Falla antes de insertar nada si alguna imagen no es base64 valido. */
    public void validar(String valor, String columna) {
        if (esImagenNueva(valor)) {
            decodificar(valor, columna);
        }
    }

    /**
     * Guarda la foto si es una imagen nueva; si ya es una ruta (o esta vacia) la devuelve tal cual.
     * @return ruta relativa guardada en la columna.
     */
    public String guardar(String valor, String columna, String idSupervision, LocalDate fecha) {
        String limpio = valor == null ? null : valor.trim();
        if (limpio == null || limpio.isEmpty()) {
            return null;
        }
        if (!esImagenNueva(limpio)) {
            return limpio;
        }
        Imagen imagen = decodificar(limpio, columna);
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        String anio = String.valueOf(dia.getYear());
        String nombre = FECHA.format(dia) + "_" + limpiar(columna) + "_" + limpiar(idSupervision) + "." + imagen.extension;
        String mes = ANIO_MES.format(dia);
        Path carpeta = baseDir.resolve(anio).resolve(mes);
        Path archivo = carpeta.resolve(nombre);
        try {
            Files.createDirectories(carpeta);
            Files.write(archivo, imagen.bytes);
        } catch (IOException ex) {
            LoggerFactory.getLogger(SupervisionV2FotoStorageService.class)
                    .error("Error guardando foto {} en {}: {}", columna, archivo, ex.toString(), ex);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FOTO_STORAGE_ERROR",
                    "No se pudo guardar la foto " + columna + " en disco (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")."
            );
        }
        return anio + "/" + mes + "/" + nombre;
    }

    public FotoArchivo cargar(String rutaRaw) {
        String ruta = rutaRaw == null ? "" : rutaRaw.trim().replace("\\", "/");
        while (ruta.startsWith("/")) {
            ruta = ruta.substring(1);
        }
        if (ruta.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Ruta de foto requerida.");
        }
        Path archivo = baseDir.resolve(ruta).normalize();
        if (!archivo.startsWith(baseDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Ruta de foto invalida.");
        }
        if (!Files.isRegularFile(archivo)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Foto no encontrada.");
        }
        try {
            return new FotoArchivo(Files.readAllBytes(archivo), contentType(archivo));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FOTO_STORAGE_ERROR", "No se pudo leer la foto.");
        }
    }

    private Imagen decodificar(String valor, String columna) {
        int idx = valor.toLowerCase(Locale.ROOT).indexOf(";base64,");
        String mime = valor.substring(5, idx).trim().toLowerCase(Locale.ROOT);
        String base64 = valor.substring(idx + 8).replaceAll("\\s+", "");
        if (base64.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La foto " + columna + " no tiene contenido.");
        }
        try {
            return new Imagen(Base64.getDecoder().decode(base64), extension(mime));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La foto " + columna + " no es base64 valido.");
        }
    }

    private String extension(String mime) {
        switch (mime) {
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }

    private String contentType(Path archivo) {
        String nombre = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".png")) {
            return "image/png";
        }
        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String limpiar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : (texto == null ? "" : texto.trim()).toCharArray()) {
            sb.append(Character.isLetterOrDigit(c) || c == '-' ? c : '_');
        }
        return sb.toString();
    }

    private static final class Imagen {
        private final byte[] bytes;
        private final String extension;

        private Imagen(byte[] bytes, String extension) {
            this.bytes = bytes;
            this.extension = extension;
        }
    }

    public static final class FotoArchivo {
        private final byte[] bytes;
        private final String contentType;

        public FotoArchivo(byte[] bytes, String contentType) {
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
}
