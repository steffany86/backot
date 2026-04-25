package com.example.TigoStarSystem.llamadaatencion.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.llamadaatencion.dto.LlamadaAtencionCrearRequest;
import com.example.TigoStarSystem.llamadaatencion.repository.LlamadaAtencionRepository;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LlamadaAtencionService {
    private static final String SP_TECNICOS = "EXEC dbo.spx_ObtenerTecnicosLlamadaAtencion ?";
    private static final String SP_TECNICOS_SIN_FILTRO = "EXEC dbo.spx_ObtenerTecnicosLlamadaAtencion";
    private final LlamadaAtencionRepository repository;
    private final LlamadaAtencionFirmaStorageService firmaStorageService;
    private final DbConnectionManager dbConnectionManager;
    private final AuthService authService;

    public LlamadaAtencionService(
            LlamadaAtencionRepository repository,
            LlamadaAtencionFirmaStorageService firmaStorageService,
            DbConnectionManager dbConnectionManager,
            AuthService authService) {
        this.repository = repository;
        this.firmaStorageService = firmaStorageService;
        this.dbConnectionManager = dbConnectionManager;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(
            String idTecnico,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        authService.me(token);
        validarRangoFechas(fechaDesde, fechaHasta);
        return repository.listarLlamadasAtencion(idTecnico, fechaDesde, fechaHasta, limite);
    }

    public Map<String, Object> registrar(LlamadaAtencionCrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de llamada de atencion es requerido."
            );
        }

        String firmaTecnico = firmaStorageService.guardarFirmaTecnico(request.getFirmaTecnico());
        String firmaTestigo = firmaStorageService.guardarFirmaTestigo(request.getFirmaTestigo());

        String idGenerado = repository.insertarLlamadaAtencion(
                request.getIdTecnico(),
                request.getIdTipoComunicacion(),
                request.getMotivo(),
                request.getDescripcion(),
                request.getComentarioColaborador(),
                request.getAcuerdos(),
                request.getFechaSeguimiento(),
                firmaTecnico,
                firmaTestigo
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idLlamadaAtencion", idGenerado);
        out.put("idUsuarioSesion", me.getUsuario() == null ? null : me.getUsuario().getIdUsuario());
        return out;
    }

    public List<Map<String, Object>> listarTiposComunicacion(String token) {
        authService.me(token);
        return repository.listarTiposComunicacion();
    }

    public List<Map<String, Object>> listarTecnicos(
            String q,
            Integer limit,
            String sucursal,
            String token) {
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        JdbcTemplate template = dbConnectionManager.connDb(resolveTecnicosDb(sucursalResuelta));
        String filtro = trimToNull(q);

        List<Map<String, Object>> rows;
        if (filtro == null) {
            rows = template.queryForList(SP_TECNICOS_SIN_FILTRO);
        } else {
            rows = template.queryForList(SP_TECNICOS, filtro);
        }

        List<Map<String, Object>> normalizadas = normalizarTecnicos(rows);
        int max = resolveLimit(limit);
        if (normalizadas.size() <= max) {
            return normalizadas;
        }
        return new ArrayList<>(normalizadas.subList(0, max));
    }

    private String resolveTecnicosDb(String sucursal) {
        String normalized = normalizeText(sucursal);
        if (normalized.contains("sucre")) {
            return "sucre";
        }
        return "operativa";
    }

    private String resolveSucursalNombre(String sucursal, String token) {
        if (!isBlank(sucursal)) {
            return SucursalCanonicalizer.canonicalize(sucursal);
        }

        AuthMeResponse me = authService.me(token);
        Integer idSucursal = me.getUsuario() == null ? null : me.getUsuario().getIdSucursal();
        if (idSucursal == null) {
            return null;
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    private List<Map<String, Object>> normalizarTecnicos(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalizada = new LinkedHashMap<>();
            if (row != null) {
                normalizada.putAll(row);

                Object idTecnico = findValue(row, "id_tecnico", "idtecnico", "id_vendedor", "idvendedor");
                Object tecnico = findValue(row, "tecnico", "nombre", "vendedor", "nombrevendedor");
                Object cuentaSf = findValue(row, "cuenta_sf", "cuentasf", "cuentaSf");
                Object salesforce = findValue(row, "salesforce");
                Object habilidad = findValue(row, "habilidad");
                Object vehiculo = findValue(row, "vehiculo");

                if (idTecnico != null) {
                    normalizada.put("idTecnico", idTecnico);
                    normalizada.put("id_tecnico", idTecnico);
                }
                if (tecnico != null) {
                    normalizada.put("tecnico", tecnico);
                }
                if (cuentaSf != null) {
                    normalizada.put("cuentaSf", cuentaSf);
                    normalizada.put("cuenta_sf", cuentaSf);
                }
                if (salesforce != null) {
                    normalizada.put("salesforce", salesforce);
                }
                if (habilidad != null) {
                    normalizada.put("habilidad", habilidad);
                }
                if (vehiculo != null) {
                    normalizada.put("vehiculo", vehiculo);
                }
            }
            out.add(normalizada);
        }
        return out;
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "fechaDesde no puede ser mayor a fechaHasta."
            );
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 200;
        }
        return Math.min(limit, 1000);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (current.equals(normalizeKey(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
