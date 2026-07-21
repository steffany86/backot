package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class OtVentaPdfStorageService {
    private static final Logger logger = LoggerFactory.getLogger(OtVentaPdfStorageService.class);
    private static final long MAX_BYTES = 10L * 1024L * 1024L;
    private static final DateTimeFormatter DATE_PARTITION = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final Path baseDir;

    public OtVentaPdfStorageService(
            @Value("${app.ot.venta.pdf.dir:C:/archivos_ot_pdf}") String baseDirRaw) {
        String resolved = baseDirRaw == null || baseDirRaw.trim().isEmpty()
                ? "C:/archivos_ot_pdf"
                : baseDirRaw.trim();
        this.baseDir = Paths.get(resolved).toAbsolutePath().normalize();
        ensureDirectory(this.baseDir);
    }

    public String guardarPdfVenta(MultipartFile archivo, Integer ordenTrabajo, Integer codigoCliente, String sucursalNombre) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        validarArchivo(archivo);

        String sucursalFolder = sanitizeFolderName(sucursalNombre);
        String datePath = DATE_PARTITION.format(LocalDate.now());
        Path targetDir = baseDir.resolve("venta").resolve(sucursalFolder).resolve(datePath);
        ensureDirectory(targetDir);

        String fileName = buildFileName(archivo, ordenTrabajo, codigoCliente);
        Path targetPath = resolveAvailablePath(targetDir, fileName);
        try {
            Files.copy(archivo.getInputStream(), targetPath);
        } catch (IOException ex) {
            logger.error("Error guardando archivo OT en disco. targetPath={} motivo={}", targetPath, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF_STORAGE_ERROR",
                    "No se pudo guardar el archivo en disco."
            );
        }
        return targetPath.toAbsolutePath().normalize().toString();
    }

    public void eliminarPdfSilencioso(String rutaPdf) {
        if (rutaPdf == null || rutaPdf.trim().isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(rutaPdf).toAbsolutePath().normalize();
            if (!path.startsWith(baseDir)) {
                logger.warn("Se omitio eliminacion de PDF fuera de baseDir. path={} baseDir={}", path, baseDir);
                return;
            }
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            logger.warn("No se pudo eliminar PDF tras fallo transaccional. rutaPdf={} motivo={}", rutaPdf, ex.getMessage());
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        String originalName = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().trim().toLowerCase(Locale.ROOT);
        String extension = extensionOf(originalName);
        boolean isPdf = ".pdf".equals(extension);
        boolean isImage = ".jpg".equals(extension) || ".jpeg".equals(extension) || ".png".equals(extension);
        if (!isPdf && !isImage) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo adjunto debe ser PDF o imagen JPG/PNG.");
        }
        long size = archivo.getSize();
        if (size <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo adjunto esta vacio.");
        }
        if (size > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo supera el limite de 10MB.");
        }
        if (isPdf && !tieneFirmaPdf(archivo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo no corresponde a un PDF valido.");
        }
        if (isImage && !tieneFirmaImagen(archivo, extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El archivo no corresponde a una imagen valida.");
        }
    }

    private boolean tieneFirmaPdf(MultipartFile pdf) {
        byte[] signature = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
        byte[] buffer = new byte[signature.length];
        try (InputStream inputStream = pdf.getInputStream()) {
            int read = inputStream.read(buffer);
            if (read < signature.length) {
                return false;
            }
            for (int i = 0; i < signature.length; i++) {
                if (buffer[i] != signature[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "No se pudo validar el contenido del PDF."
            );
        }
    }

    private boolean tieneFirmaImagen(MultipartFile archivo, String extension) {
        byte[] buffer = new byte[8];
        try (InputStream inputStream = archivo.getInputStream()) {
            int read = inputStream.read(buffer);
            if (read < 4) {
                return false;
            }
            if (".png".equals(extension)) {
                return read >= 8
                        && (buffer[0] & 0xFF) == 0x89
                        && buffer[1] == 0x50
                        && buffer[2] == 0x4E
                        && buffer[3] == 0x47
                        && buffer[4] == 0x0D
                        && buffer[5] == 0x0A
                        && buffer[6] == 0x1A
                        && buffer[7] == 0x0A;
            }
            return (buffer[0] & 0xFF) == 0xFF && (buffer[1] & 0xFF) == 0xD8 && (buffer[2] & 0xFF) == 0xFF;
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "No se pudo validar el contenido de la imagen."
            );
        }
    }

    private String buildFileName(MultipartFile archivo, Integer ordenTrabajo, Integer codigoCliente) {
        String originalName = archivo.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            String ot = ordenTrabajo == null ? "0" : String.valueOf(Math.max(ordenTrabajo, 0));
            String cliente = codigoCliente == null ? "0" : String.valueOf(Math.max(codigoCliente, 0));
            return "OT_" + ot + "_COD_" + cliente + ".pdf";
        }

        String cleanName = Paths.get(originalName.trim()).getFileName().toString();
        cleanName = cleanName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleanName;
    }

    private String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private Path resolveAvailablePath(Path targetDir, String fileName) {
        Path candidate = targetDir.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String extension = extensionOf(fileName);
        String baseName = extension.isEmpty()
                ? fileName
                : fileName.substring(0, fileName.length() - extension.length());

        int i = 1;
        while (true) {
            Path withSuffix = targetDir.resolve(baseName + "_" + i + extension);
            if (!Files.exists(withSuffix)) {
                return withSuffix;
            }
            i++;
        }
    }

    private void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF_STORAGE_DIR_ERROR",
                    "No se pudo crear/validar carpeta de PDFs: " + dir
            );
        }
    }

    private String sanitizeFolderName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "SinSucursal";
        }
        String cleaned = value.trim().replaceAll("[\\\\/:*?\"<>|]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return "SinSucursal";
        }
        return cleaned;
    }
}
