package com.example.TigoStarSystem.inventariodpto.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.inventariodpto.dto.InventarioDptoFotoRequest;
import com.example.TigoStarSystem.inventariodpto.dto.InventarioDptoItemRequest;
import com.example.TigoStarSystem.inventariodpto.repository.InventarioDptoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InventarioDptoService {
    /** Unico usuario (loggin) que puede entrar a esta seccion, sin importar su rol. */
    private static final List<String> USUARIOS_PERMITIDOS = Arrays.asList("stefany");
    private static final List<String> ESTADOS = Arrays.asList("Bueno", "Regular", "Dañado", "Faltante", "A cambiar");
    private static final String ESTADO_A_CAMBIAR = "A cambiar";

    private final InventarioDptoRepository repository;
    private final AuthService authService;
    private final GoogleDriveClient drive;

    public InventarioDptoService(InventarioDptoRepository repository, AuthService authService, GoogleDriveClient drive) {
        this.repository = repository;
        this.authService = authService;
        this.drive = drive;
    }

    public Map<String, Object> datos(String token) {
        requireUsuario(token);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", repository.listarItems());
        out.put("fotos", repository.listarFotos());
        return out;
    }

    public void guardarItem(InventarioDptoItemRequest r, String token) {
        AuthLoginResponse usuario = requireUsuario(token);
        if (!ESTADOS.contains(r.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO", "Estado no valido.");
        }
        String fecha = null;
        String motivo = null;
        if (ESTADO_A_CAMBIAR.equals(r.getEstado())) {
            fecha = limpiar(r.getFechaCambio());
            motivo = limpiar(r.getMotivoCambio());
            if (fecha == null || fecha.isEmpty() || motivo == null || motivo.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CAMBIO_INCOMPLETO", "Indica la fecha y el motivo del cambio.");
            }
        }
        repository.guardarItem(r.getItem(), r.getEstado(), limpiar(r.getObservacion()), fecha, motivo, usuario.getIdUsuario());
    }

    public Map<String, Object> agregarFoto(InventarioDptoFotoRequest r, String token) {
        AuthLoginResponse usuario = requireUsuario(token);
        String data = r.getImagen();
        int coma = data.indexOf(',');
        if (!data.startsWith("data:image/") || coma < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", "Imagen no valida.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data.substring(coma + 1));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", "Imagen no valida.");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (drive.estaConfigurado()) {
            // Con Drive configurado la imagen vive en Drive; en la base solo queda su id.
            String driveId = drive.subir(bytes, "inv91_item" + r.getItem() + "_" + System.currentTimeMillis() + ".jpg");
            out.put("id", repository.insertarFoto(r.getItem(), null, driveId, usuario.getIdUsuario()));
        } else {
            out.put("id", repository.insertarFoto(r.getItem(), bytes, null, usuario.getIdUsuario()));
        }
        return out;
    }

    public Map<String, Object> obtenerFoto(Integer id, String token) {
        requireUsuario(token);
        Map<String, Object> foto = repository.obtenerFoto(id);
        if (foto == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FOTO_NO_ENCONTRADA", "Foto no encontrada.");
        }
        byte[] bytes = (byte[]) foto.get("Imagen");
        Object driveId = foto.get("DriveId");
        if (bytes == null && driveId != null) {
            bytes = drive.descargar(String.valueOf(driveId));
        }
        if (bytes == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FOTO_NO_ENCONTRADA", "Foto no encontrada.");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("imagen", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes));
        return out;
    }

    public void eliminarFoto(Integer id, String token) {
        requireUsuario(token);
        String driveId = repository.eliminarFoto(id);
        if (driveId != null && drive.estaConfigurado()) {
            try {
                drive.eliminar(driveId);
            } catch (ApiException ex) {
                // El registro ya se borro; si Drive falla solo queda un archivo huerfano en la carpeta.
            }
        }
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private AuthLoginResponse requireUsuario(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        if (usuario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesion requerida.");
        }
        if (!USUARIOS_PERMITIDOS.contains(normalize(usuario.getLoggin()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_USER", "No tienes acceso a esta seccion.");
        }
        return usuario;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
