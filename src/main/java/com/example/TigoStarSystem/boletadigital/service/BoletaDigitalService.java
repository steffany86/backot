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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BoletaDigitalService {
    private final BoletaDigitalRepository repository;
    private final AuthService authService;
    private final DbConnectionManager dbConnectionManager;
    private final String dbUsername;
    private final String dbPassword;

    public BoletaDigitalService(
            BoletaDigitalRepository repository,
            AuthService authService,
            DbConnectionManager dbConnectionManager,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword) {
        this.repository = repository;
        this.authService = authService;
        this.dbConnectionManager = dbConnectionManager;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public List<Map<String, Object>> listar(String token) {
        AuthMeResponse me = authService.me(token);
        JdbcTemplate template = resolveSucursalTemplate(me);
        return repository.listarOtArchivo(template);
    }

    public ArchivoPdf cargarArchivo(String token, String rutaRaw) {
        authService.me(token);
        String ruta = trimToNull(rutaRaw);
        if (ruta == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "RutaPDF es requerida.");
        }

        Path path = Paths.get(ruta).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PDF_NOT_FOUND", "Archivo PDF no encontrado.");
        }
        String fileName = path.getFileName() == null ? "boleta.pdf" : path.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La ruta no corresponde a un archivo PDF.");
        }

        try {
            byte[] content = Files.readAllBytes(path);
            return new ArchivoPdf(new ByteArrayResource(content), fileName, "application/pdf");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_READ_ERROR", "No se pudo leer el archivo PDF.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
