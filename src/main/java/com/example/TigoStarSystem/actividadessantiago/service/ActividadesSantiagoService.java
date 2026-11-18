package com.example.TigoStarSystem.actividadessantiago.service;

import com.example.TigoStarSystem.actividadessantiago.dto.ActividadSantiagoDiaRequest;
import com.example.TigoStarSystem.actividadessantiago.dto.ActividadSantiagoRegistroRequest;
import com.example.TigoStarSystem.actividadessantiago.repository.ActividadesSantiagoRepository;
import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ActividadesSantiagoService {
    /** Unicos usuarios (loggin) que pueden entrar a esta seccion, sin importar su rol. */
    private static final List<String> USUARIOS_PERMITIDOS = Arrays.asList("stefany", "sistemask");

    private final ActividadesSantiagoRepository repository;
    private final AuthService authService;

    public ActividadesSantiagoService(ActividadesSantiagoRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public Map<String, Object> datos(String token) {
        requireSistemas(token);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("registros", repository.listarRegistros());
        out.put("dias", repository.listarDias());
        return out;
    }

    public void guardarRegistro(ActividadSantiagoRegistroRequest r, String token) {
        AuthLoginResponse usuario = requireSistemas(token);
        repository.guardarRegistro(r.getFecha(), r.getId(), r.getNivel(), r.getMinutos(), limpiar(r.getObservacion()), usuario.getIdUsuario());
    }

    public void guardarDia(ActividadSantiagoDiaRequest r, String token) {
        AuthLoginResponse usuario = requireSistemas(token);
        repository.guardarDia(r.getFecha(), limpiar(r.getAnimo()), limpiar(r.getObservacion()), usuario.getIdUsuario());
    }

    /** Sube de una vez los registros que estaban guardados solo en el navegador. */
    public int importar(List<ActividadSantiagoRegistroRequest> registros, String token) {
        AuthLoginResponse usuario = requireSistemas(token);
        int total = 0;
        for (ActividadSantiagoRegistroRequest r : registros) {
            if (r.getNivel() == null) {
                continue;
            }
            repository.guardarRegistro(r.getFecha(), r.getId(), r.getNivel(), r.getMinutos(), limpiar(r.getObservacion()), usuario.getIdUsuario());
            total++;
        }
        return total;
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private AuthLoginResponse requireSistemas(String token) {
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
