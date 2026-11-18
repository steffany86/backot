package com.example.TigoStarSystem.supervisionv2.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisionv2.repository.SupervisionV2CatalogoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SupervisionV2CatalogoService {
    private static final List<String> ROLES_PERMITIDOS = Arrays.asList("backoffice", "backofficev", "sistemas");
    private static final int NOMBRE_MAX = 100;

    private final SupervisionV2CatalogoRepository repository;
    private final AuthService authService;

    public SupervisionV2CatalogoService(SupervisionV2CatalogoRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String catalogo, String token) {
        validar(catalogo, token);
        try {
            return repository.listar(catalogo);
        } catch (DataAccessException ex) {
            throw error("No se pudo listar el catalogo.", ex);
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CATALOGO_ERROR", ex.getMessage());
        }
    }

    public void crear(String catalogo, String nombreRaw, String token) {
        validar(catalogo, token);
        String nombre = nombreRaw == null ? "" : nombreRaw.trim();
        if (nombre.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El nombre es obligatorio.");
        }
        if (nombre.length() > NOMBRE_MAX) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El nombre no puede superar " + NOMBRE_MAX + " caracteres.");
        }
        try {
            if (repository.existeNombreActivo(catalogo, nombre)) {
                throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE", "Ya existe un registro activo con ese nombre.");
            }
            repository.crear(catalogo, nombre);
        } catch (DataAccessException ex) {
            throw error("No se pudo registrar.", ex);
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CATALOGO_ERROR", ex.getMessage());
        }
    }

    public void baja(String catalogo, String idRaw, String token) {
        validar(catalogo, token);
        long id;
        try {
            id = Long.parseLong(idRaw == null ? "" : idRaw.trim());
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id invalido.");
        }
        try {
            int filas = repository.baja(catalogo, id);
            if (filas < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CATALOGO_SIN_BAJA",
                        "La tabla no tiene columna e_eliminado para dar de baja.");
            }
            if (filas == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Registro no encontrado o ya dado de baja.");
            }
        } catch (DataAccessException ex) {
            throw error("No se pudo dar de baja.", ex);
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CATALOGO_ERROR", ex.getMessage());
        }
    }

    private void validar(String catalogo, String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        if (usuario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesion requerida.");
        }
        if (!ROLES_PERMITIDOS.contains(normalizarRol(usuario.getRol()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Solo BackOffice y Sistemas pueden administrar estos catalogos.");
        }
        if (!repository.existeCatalogo(catalogo)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Catalogo no encontrado: " + catalogo);
        }
    }

    private ApiException error(String mensaje, DataAccessException ex) {
        Throwable causa = ex.getMostSpecificCause();
        String detalle = causa == null ? ex.getMessage() : causa.getMessage();
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CATALOGO_ERROR", mensaje + " " + (detalle == null ? "" : detalle));
    }

    private String normalizarRol(String rol) {
        if (rol == null) {
            return "";
        }
        return Normalizer.normalize(rol, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
