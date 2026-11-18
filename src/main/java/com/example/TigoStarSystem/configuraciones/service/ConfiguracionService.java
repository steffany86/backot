package com.example.TigoStarSystem.configuraciones.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.configuraciones.repository.ConfiguracionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ConfiguracionService {
    /** Para agregar una opcion nueva: sumar una linea aqui y usar la clave desde el front. */
    private static final List<Definicion> DEFINICIONES = new ArrayList<Definicion>();
    static {
        DEFINICIONES.add(new Definicion(
                "supervision_v2.tuerca_habilitada",
                "Tuerca de configuracion en Supervision V2",
                "Habilita el boton de la tuerca en la pagina Supervision V2 (solo BackOffice y Sistemas pueden usarlo).",
                "bool",
                "0"));
    }

    private final ConfiguracionRepository repository;
    private final AuthService authService;

    public ConfiguracionService(ConfiguracionRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(String token) {
        requireSistemas(token);
        Map<String, String> valores = repository.listarValores();
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Definicion d : DEFINICIONES) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("clave", d.clave);
            row.put("titulo", d.titulo);
            row.put("descripcion", d.descripcion);
            row.put("tipo", d.tipo);
            row.put("valor", valorDe(valores, d));
            out.add(row);
        }
        return out;
    }

    /** Lectura de una configuracion para cualquier usuario con sesion (el front la usa para habilitar/ocultar opciones). */
    public Map<String, Object> obtener(String clave, String token) {
        requireSesion(token);
        Definicion d = definicion(clave);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("clave", d.clave);
        row.put("valor", valorDe(repository.listarValores(), d));
        return row;
    }

    public Map<String, Object> actualizar(String clave, String valor, String token) {
        AuthLoginResponse usuario = requireSistemas(token);
        Definicion d = definicion(clave);
        String limpio = valor == null ? "" : valor.trim();
        if ("bool".equals(d.tipo) && !"1".equals(limpio) && !"0".equals(limpio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El valor debe ser 1 o 0.");
        }
        repository.guardar(d.clave, limpio, usuario.getLoggin());
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("clave", d.clave);
        row.put("valor", limpio);
        return row;
    }

    private Definicion definicion(String clave) {
        for (Definicion d : DEFINICIONES) {
            if (d.clave.equals(clave)) {
                return d;
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Configuracion no encontrada: " + clave);
    }

    private String valorDe(Map<String, String> valores, Definicion d) {
        String valor = valores.get(d.clave);
        return valor == null ? d.valorPorDefecto : valor;
    }

    private AuthLoginResponse requireSesion(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        if (usuario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesion requerida.");
        }
        return usuario;
    }

    private AuthLoginResponse requireSistemas(String token) {
        AuthLoginResponse usuario = requireSesion(token);
        if (!"sistemas".equals(normalize(usuario.getRol())) && !"sistemas".equals(normalize(usuario.getLoggin()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_SISTEMAS_ONLY", "Solo sistemas puede administrar configuraciones.");
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

    private static final class Definicion {
        private final String clave;
        private final String titulo;
        private final String descripcion;
        private final String tipo;
        private final String valorPorDefecto;

        private Definicion(String clave, String titulo, String descripcion, String tipo, String valorPorDefecto) {
            this.clave = clave;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.tipo = tipo;
            this.valorPorDefecto = valorPorDefecto;
        }
    }
}
