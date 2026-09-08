package com.example.TigoStarSystem.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Permite verificar desde el navegador (sin login) que fecha/hora de build
 * esta corriendo el backend desplegado, para confirmar que un push a Railway
 * ya se aplico.
 */
@RestController
public class VersionController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONA_BOLIVIA = ZoneId.of("America/La_Paz");

    @Autowired(required = false)
    @Nullable
    private BuildProperties buildProperties;

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> version() {
        Map<String, Object> out = new LinkedHashMap<>();
        if (buildProperties != null) {
            out.put("version", buildProperties.getVersion());
            out.put("fechaCompilacion", FORMATTER.format(buildProperties.getTime().atZone(ZONA_BOLIVIA)));
        } else {
            out.put("version", null);
            out.put("fechaCompilacion", null);
        }
        out.put("fechaServidorAhora", FORMATTER.format(java.time.ZonedDateTime.now(ZONA_BOLIVIA)));
        return ResponseEntity.ok(ApiResponse.of(out, "Version del backend en ejecucion."));
    }
}
