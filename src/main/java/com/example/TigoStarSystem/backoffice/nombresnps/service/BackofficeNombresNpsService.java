package com.example.TigoStarSystem.backoffice.nombresnps.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.backoffice.nombresnps.repository.BackofficeNombresNpsRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BackofficeNombresNpsService {
    private static final double MATCH_THRESHOLD = 0.78D;
    private static final double SUGGESTION_THRESHOLD = 0.55D;
    private static final List<Integer> IDS_SUCURSALES = Arrays.asList(9, 7, 19);

    private final BackofficeNombresNpsRepository repository;
    private final DbConnectionManager dbConnectionManager;
    private final JdbcTemplate centralTemplate;
    private final AuthService authService;
    private final String dbUsername;
    private final String dbPassword;

    public BackofficeNombresNpsService(
            BackofficeNombresNpsRepository repository,
            DbConnectionManager dbConnectionManager,
            @Qualifier("centralJdbcTemplate") JdbcTemplate centralTemplate,
            AuthService authService,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword) {
        this.repository = repository;
        this.dbConnectionManager = dbConnectionManager;
        this.centralTemplate = centralTemplate;
        this.authService = authService;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public List<Map<String, Object>> listar(String token) {
        requireBackofficeV(token);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (SucursalResponse sucursal : sucursalesObjetivo()) {
            out.add(buildSucursalResult(sucursal, false));
        }
        return out;
    }

    public Map<String, Object> obtenerYGuardar(String token, String sucursalParam) {
        requireBackofficeV(token);
        SucursalResponse sucursal = resolverSucursalObjetivo(sucursalParam);
        Map<String, Object> result = buildSucursalResult(sucursal, true);
        result.put("actualizadoEnDb", true);
        return result;
    }

    public Map<String, Object> actualizarManual(String token, String sucursalParam, Integer idVendedor, String nombreNps) {
        requireBackofficeV(token);
        if (idVendedor == null || idVendedor <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id vendedor requerido.");
        }
        String normalizedNombre = trimToNull(nombreNps);
        if (normalizedNombre == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Nombre NPS requerido.");
        }
        SucursalResponse sucursal = resolverSucursalObjetivo(sucursalParam);
        JdbcTemplate sucursalTemplate = templateSucursal(sucursal);
        int updated = repository.actualizarNombreNps(sucursalTemplate, idVendedor, normalizedNombre);
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VENDEDOR_NOT_FOUND", "No se encontro vendedor activo para actualizar.");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("sucursal", sucursal.getSucursal());
        out.put("idSucursal", sucursal.getIdSucursal());
        out.put("idVendedor", idVendedor);
        out.put("nombreNps", normalizedNombre);
        out.put("actualizados", updated);
        return out;
    }

    private Map<String, Object> buildSucursalResult(SucursalResponse sucursal, boolean actualizar) {
        JdbcTemplate sucursalTemplate = templateSucursal(sucursal);
        List<Map<String, Object>> vendedores = repository.listarVendedores(sucursalTemplate);
        List<String> nombresNps = actualizar ? listarNombresNps(sucursal.getIdSucursal()) : new ArrayList<String>();

        int conMatch = 0;
        int sinMatch = 0;
        int actualizados = 0;
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> vendedor : vendedores) {
            Integer idVendedor = toInteger(vendedor.get("idVendedor"));
            String nombre = trimToNull(toText(vendedor.get("nombre")));
            String nombreActual = trimToNull(toText(vendedor.get("nombreNps")));
            MatchCandidate match = actualizar ? findBestMatch(nombre, nombresNps) : null;
            boolean matched = match != null && match.score >= MATCH_THRESHOLD;

            String nombreNps = nombreActual;
            String estado = nombreActual == null ? "sin_match" : "match";
            if (matched) {
                estado = "match";
                nombreNps = match.nombre;
                conMatch++;
                if (actualizar && idVendedor != null && !equalsIgnoreCaseTrim(nombreActual, match.nombre)) {
                    actualizados += repository.actualizarNombreNps(sucursalTemplate, idVendedor, match.nombre);
                }
            } else {
                if (nombreActual == null) {
                    sinMatch++;
                } else {
                    conMatch++;
                }
            }

            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("sucursal", sucursal.getSucursal());
            row.put("idSucursal", sucursal.getIdSucursal());
            row.put("idVendedor", idVendedor);
            row.put("nombre", nombre);
            row.put("nombreNps", nombreNps);
            row.put("nombreNpsActual", nombreActual);
            row.put("sugeridoNombreNps", isMeaningfulSuggestion(match) ? match.nombre : null);
            row.put("score", isMeaningfulSuggestion(match) ? roundScore(match.score) : null);
            row.put("estado", estado);
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sucursal", sucursal.getSucursal());
        result.put("idSucursal", sucursal.getIdSucursal());
        result.put("total", rows.size());
        result.put("conMatch", conMatch);
        result.put("sinMatch", sinMatch);
        result.put("actualizados", actualizados);
        result.put("rows", rows);
        return result;
    }

    private List<String> listarNombresNps(Integer idSucursal) {
        List<Map<String, Object>> rows = repository.listarNombresNpsCentral(centralTemplate, idSucursal);
        List<String> out = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (Map<String, Object> row : rows) {
            String nombre = trimToNull(toText(row.get("nombre")));
            if (nombre == null) continue;
            String key = normalizeForMatch(nombre);
            if (seen.add(key)) {
                out.add(nombre);
            }
        }
        return out;
    }

    private MatchCandidate findBestMatch(String nombre, List<String> candidatos) {
        if (nombre == null || candidatos == null || candidatos.isEmpty()) return null;
        MatchCandidate best = null;
        for (String candidato : candidatos) {
            double score = score(nombre, candidato);
            if (best == null || score > best.score) {
                best = new MatchCandidate(candidato, score);
            }
        }
        return best;
    }

    private double score(String left, String right) {
        String a = normalizeForMatch(left);
        String b = normalizeForMatch(right);
        if (a.isEmpty() || b.isEmpty()) return 0D;
        if (a.equals(b)) return 1D;

        NameParts leftParts = splitName(a);
        NameParts rightParts = splitName(b);
        double surnameScore = weightedTokenCoverage(leftParts.surnames, rightParts.surnames, true);
        if (surnameScore < 0.45D) {
            return Math.min(0.45D, surnameScore);
        }

        double givenScore = weightedTokenCoverage(leftParts.givenNames, rightParts.givenNames, false);

        int maxLen = Math.max(a.length(), b.length());
        double editScore = maxLen == 0 ? 0D : 1D - ((double) levenshtein(a, b) / (double) maxLen);
        double total = (surnameScore * 0.70D) + (givenScore * 0.25D) + (Math.max(0D, editScore) * 0.05D);
        if (surnameScore < 0.70D && givenScore < 0.50D) {
            return Math.min(total, 0.69D);
        }
        return total;
    }

    private NameParts splitName(String value) {
        List<String> parts = new ArrayList<String>();
        if (value == null) {
            return new NameParts(new ArrayList<String>(), new ArrayList<String>());
        }
        for (String part : value.split("\\s+")) {
            if (part.length() >= 2) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return new NameParts(new ArrayList<String>(), new ArrayList<String>());
        }
        int surnameCount = parts.size() >= 3 ? 2 : 1;
        int surnameStart = Math.max(0, parts.size() - surnameCount);
        return new NameParts(
                new ArrayList<String>(parts.subList(0, surnameStart)),
                new ArrayList<String>(parts.subList(surnameStart, parts.size()))
        );
    }

    private double weightedTokenCoverage(List<String> leftTokens, List<String> rightTokens, boolean surnameMode) {
        if (leftTokens == null || leftTokens.isEmpty() || rightTokens == null || rightTokens.isEmpty()) return 0D;
        double total = 0D;
        for (String leftToken : leftTokens) {
            double best = 0D;
            for (String rightToken : rightTokens) {
                best = Math.max(best, tokenSimilarity(leftToken, rightToken, surnameMode));
            }
            total += best;
        }
        return total / (double) leftTokens.size();
    }

    private double tokenSimilarity(String left, String right, boolean surnameMode) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return 0D;
        if (left.equals(right)) return 1D;
        int maxLen = Math.max(left.length(), right.length());
        int minLen = Math.min(left.length(), right.length());
        if (maxLen < 4) return 0D;
        double similarity = 1D - ((double) levenshtein(left, right) / (double) maxLen);
        double threshold = surnameMode && minLen >= 6 ? 0.72D : 0.82D;
        return similarity >= threshold ? similarity : 0D;
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    private String normalizeForMatch(String value) {
        String text = trimToNull(value);
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('Ñ', 'N')
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private boolean isMeaningfulSuggestion(MatchCandidate match) {
        return match != null && match.score >= SUGGESTION_THRESHOLD;
    }

    private void requireBackofficeV(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse user = me == null ? null : me.getUsuario();
        String role = user == null ? null : user.getRol();
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (!("backofficev".equals(normalized)
                || "backoffice".equals(normalized)
                || "sistemas".equals(normalized)
                || "admin".equals(normalized)
                || "administrador".equals(normalized))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Rol sin acceso a nombres NPS.");
        }
    }

    private List<SucursalResponse> sucursalesObjetivo() {
        List<SucursalResponse> out = new ArrayList<SucursalResponse>();
        for (SucursalResponse sucursal : authService.listarSucursales()) {
            if (sucursal != null && IDS_SUCURSALES.contains(sucursal.getIdSucursal())) {
                out.add(sucursal);
            }
        }
        return out;
    }

    private SucursalResponse resolverSucursalObjetivo(String sucursalParam) {
        String normalizedParam = normalizeSucursalParam(sucursalParam);
        for (SucursalResponse sucursal : sucursalesObjetivo()) {
            if (String.valueOf(sucursal.getIdSucursal()).equals(normalizedParam)
                    || normalizeSucursalParam(sucursal.getSucursal()).equals(normalizedParam)) {
                return sucursal;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "SUCURSAL_NOT_FOUND", "Sucursal no soportada para nombres NPS.");
    }

    private JdbcTemplate templateSucursal(SucursalResponse sucursal) {
        return dbConnectionManager.connDb(
                "nombres-nps-" + sucursal.getIdSucursal(),
                sucursal.getIp(),
                sucursal.getBaseDeDatos(),
                dbUsername,
                dbPassword
        );
    }

    private String normalizeSucursalParam(String value) {
        String text = normalizeForMatch(value);
        return text.replace(" ", "").replace("_", "");
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean equalsIgnoreCaseTrim(String left, String right) {
        String a = trimToNull(left);
        String b = trimToNull(right);
        if (a == null) return b == null;
        return b != null && a.equalsIgnoreCase(b);
    }

    private double roundScore(double score) {
        return Math.round(score * 1000D) / 1000D;
    }

    private static class NameParts {
        private final List<String> givenNames;
        private final List<String> surnames;

        private NameParts(List<String> givenNames, List<String> surnames) {
            this.givenNames = givenNames;
            this.surnames = surnames;
        }
    }

    private static class MatchCandidate {
        private final String nombre;
        private final double score;

        private MatchCandidate(String nombre, double score) {
            this.nombre = nombre;
            this.score = score;
        }
    }
}
