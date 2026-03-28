package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.ot.repository.ListaOtRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ListaOtService {
    private final ListaOtRepository repository;

    public ListaOtService(ListaOtRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listar(
            LocalDate fecha,
            String tecnico,
            boolean tecnicoExacto,
            List<String> estadosSeleccionados) {
        List<Map<String, Object>> rows = repository.listarPorFecha(fecha);
        if (rows == null || rows.isEmpty()) {
            return rows;
        }

        String tecnicoNorm = normalizeText(tecnico);
        Set<String> estadosNorm = normalizeStates(estadosSeleccionados);
        boolean filtrarTecnico = tecnicoNorm != null && !tecnicoNorm.isEmpty();
        boolean filtrarEstado = !estadosNorm.isEmpty();

        if (!filtrarTecnico && !filtrarEstado) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (filtrarTecnico && !matchTecnico(row, tecnicoNorm, tecnicoExacto)) {
                continue;
            }
            if (filtrarEstado && !matchEstado(row, estadosNorm)) {
                continue;
            }
            result.add(row);
        }
        return result;
    }

    private boolean matchTecnico(Map<String, Object> row, String tecnicoNorm, boolean exacto) {
        String tecnico = normalizeText(asString(findValue(row,
                "TECNICO", "tecnico",
                "tecnico_nombre", "tecniconombre",
                "nombrevendedor", "vendedor")));
        if (tecnico == null || tecnico.isEmpty()) {
            return false;
        }
        if (exacto) {
            return tecnico.equals(tecnicoNorm);
        }
        return tecnico.contains(tecnicoNorm);
    }

    private boolean matchEstado(Map<String, Object> row, Set<String> estadosNorm) {
        String cierre = normalizeText(asString(findValue(row, "CIERRE", "cierre")));
        String estado = normalizeText(asString(findValue(row, "ESTADO", "estado")));
        return (cierre != null && estadosNorm.contains(cierre))
                || (estado != null && estadosNorm.contains(estado));
    }

    private Set<String> normalizeStates(List<String> estados) {
        Set<String> out = new HashSet<>();
        if (estados == null || estados.isEmpty()) {
            return out;
        }
        for (String estado : estados) {
            String normalized = normalizeText(estado);
            if (normalized != null && !normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private Object findValue(Map<String, Object> row, String... candidates) {
        if (row == null || row.isEmpty() || candidates == null || candidates.length == 0) {
            return null;
        }
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(normalizeKey(entry.getKey()), entry.getValue());
        }
        for (String candidate : candidates) {
            String key = normalizeKey(candidate);
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}

