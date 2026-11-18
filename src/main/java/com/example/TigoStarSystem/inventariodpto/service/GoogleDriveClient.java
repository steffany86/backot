package com.example.TigoStarSystem.inventariodpto.service;

import com.example.TigoStarSystem.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente minimo de Google Drive (API REST v3) para las fotos del inventario.
 * Usa un refresh token OAuth del propietario del Drive y el alcance "drive.file":
 * solo ve/administra los archivos y la carpeta que crea esta aplicacion.
 * Si no hay credenciales configuradas, {@link #estaConfigurado()} es false y las fotos se guardan en la base.
 */
@Component
public class GoogleDriveClient {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id";
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    @Value("${app.drive.client-id:}")
    private String clientId;
    @Value("${app.drive.client-secret:}")
    private String clientSecret;
    @Value("${app.drive.refresh-token:}")
    private String refreshToken;
    @Value("${app.drive.folder-name:InventarioDpto91}")
    private String folderName;

    private final RestTemplate http = new RestTemplate();
    private String accessToken;
    private long accessTokenExpira;
    private String carpetaId;

    public boolean estaConfigurado() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(refreshToken);
    }

    public String subir(byte[] imagen, String nombre) {
        try {
            String metadata = "{\"name\":\"" + nombre + "\",\"parents\":[\"" + carpeta() + "\"]}";
            String boundary = "inv91_" + System.nanoTime();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" + metadata
                    + "\r\n--" + boundary + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(imagen);
            body.write(("\r\n--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = auth();
            headers.setContentType(MediaType.parseMediaType("multipart/related; boundary=" + boundary));
            ResponseEntity<Map> resp = http.exchange(URI.create(UPLOAD_URL), HttpMethod.POST,
                    new HttpEntity<byte[]>(body.toByteArray(), headers), Map.class);
            Object id = resp.getBody() == null ? null : resp.getBody().get("id");
            if (id == null) {
                throw error("Drive no devolvio el id del archivo.", null);
            }
            return String.valueOf(id);
        } catch (IOException | RestClientException ex) {
            throw error("No se pudo subir la foto a Google Drive.", ex);
        }
    }

    public byte[] descargar(String id) {
        try {
            ResponseEntity<byte[]> resp = http.exchange(URI.create(FILES_URL + "/" + id + "?alt=media"), HttpMethod.GET,
                    new HttpEntity<Void>(auth()), byte[].class);
            return resp.getBody();
        } catch (RestClientException ex) {
            throw error("No se pudo leer la foto desde Google Drive.", ex);
        }
    }

    /** Borra el archivo; si ya no existe en Drive no es un error. */
    public void eliminar(String id) {
        try {
            http.exchange(URI.create(FILES_URL + "/" + id), HttpMethod.DELETE, new HttpEntity<Void>(auth()), Void.class);
        } catch (HttpClientErrorException.NotFound ex) {
            // ya borrado
        } catch (RestClientException ex) {
            throw error("No se pudo borrar la foto de Google Drive.", ex);
        }
    }

    /** Carpeta de las fotos: la busca por nombre (solo ve las creadas por esta app) y la crea si no existe. */
    @SuppressWarnings("unchecked")
    private synchronized String carpeta() {
        if (carpetaId != null) {
            return carpetaId;
        }
        try {
            String q = "name='" + folderName.replace("'", "\\'") + "' and mimeType='" + FOLDER_MIME + "' and trashed=false";
            URI uri = URI.create(FILES_URL + "?fields=files(id)&q=" + URLEncoder.encode(q, "UTF-8"));
            ResponseEntity<Map> resp = http.exchange(uri, HttpMethod.GET, new HttpEntity<Void>(auth()), Map.class);
            Object files = resp.getBody() == null ? null : resp.getBody().get("files");
            if (files instanceof List && !((List<?>) files).isEmpty()) {
                carpetaId = String.valueOf(((Map<String, Object>) ((List<?>) files).get(0)).get("id"));
                return carpetaId;
            }
            Map<String, Object> meta = new LinkedHashMap<String, Object>();
            meta.put("name", folderName);
            meta.put("mimeType", FOLDER_MIME);
            HttpHeaders headers = auth();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> creada = http.exchange(URI.create(FILES_URL + "?fields=id"), HttpMethod.POST,
                    new HttpEntity<Map<String, Object>>(meta, headers), Map.class);
            carpetaId = String.valueOf(creada.getBody().get("id"));
            return carpetaId;
        } catch (IOException | RestClientException ex) {
            throw error("No se pudo preparar la carpeta en Google Drive.", ex);
        }
    }

    private HttpHeaders auth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token());
        return headers;
    }

    @SuppressWarnings("unchecked")
    private synchronized String token() {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpira - 60000L) {
            return accessToken;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("refresh_token", refreshToken);
            form.add("grant_type", "refresh_token");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            Map<String, Object> resp = http.postForObject(TOKEN_URL, new HttpEntity<MultiValueMap<String, String>>(form, headers), Map.class);
            if (resp == null || resp.get("access_token") == null) {
                throw error("Google no entrego el token de acceso.", null);
            }
            accessToken = String.valueOf(resp.get("access_token"));
            long segundos = resp.get("expires_in") instanceof Number ? ((Number) resp.get("expires_in")).longValue() : 3000L;
            accessTokenExpira = System.currentTimeMillis() + segundos * 1000L;
            return accessToken;
        } catch (RestClientException ex) {
            throw error("No se pudo autenticar con Google Drive (revisa client id, secret y refresh token).", ex);
        }
    }

    private ApiException error(String mensaje, Exception causa) {
        ApiException ex = new ApiException(HttpStatus.BAD_GATEWAY, "DRIVE_ERROR", mensaje);
        if (causa != null) {
            ex.initCause(causa);
        }
        return ex;
    }

    private boolean notBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }
}
