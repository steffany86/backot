package com.example.TigoStarSystem.privilegios.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioMenuResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioMenuPaginasResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioMenuSidebarNombreResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioRolDetalleResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioRolResponse;
import com.example.TigoStarSystem.privilegios.dto.PrivilegioUsuarioResponse;
import com.example.TigoStarSystem.privilegios.repository.PrivilegioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PrivilegioService {
    private static final List<Integer> MENU_IDS_PRESET_SUPERVISOR_CUADRILLAS =
            java.util.Arrays.asList(7, 8, 9, 10, 60, 62);
    private final PrivilegioRepository repository;

    /**
     * Inicializa el servicio de privilegios con su repositorio de acceso a datos.
     */
    public PrivilegioService(PrivilegioRepository repository) {
        this.repository = repository;
    }



    /**
     * Obtiene el catalogo de roles disponibles para administrar privilegios.
     */
    public List<PrivilegioRolResponse> listarRoles() {
        List<Map<String, Object>> rows = repository.listarRoles();
        List<PrivilegioRolResponse> response = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer idRol = toInteger(findValue(row, "id_rol", "idrol"));
            String nombre = toString(findValue(row, "rol", "nombre"));
            if (idRol == null) {
                continue;
            }
            response.add(new PrivilegioRolResponse(idRol, nombre));
        }
        return response;
    }





    /**
     * Retorna el detalle de menus asignados/no asignados para un rol dado.
     */
    public PrivilegioRolDetalleResponse obtenerPrivilegiosPorRol(Integer idRol) {
        String rol = resolverNombreRol(idRol);
        List<PrivilegioMenuResponse> menus = construirMenus(repository.obtenerPrivilegiosRolDetalle(idRol));
        return new PrivilegioRolDetalleResponse(idRol, rol, menus);
    }




    /**
     * Reemplaza los privilegios de un rol con el conjunto de menus recibido.
     */
    public PrivilegioRolDetalleResponse actualizarPrivilegiosRol(Integer idRol, List<Integer> menuIds) {
        String rol = resolverNombreRol(idRol);
        List<Integer> seleccion = sanitizarMenuIds(menuIds);
        String menuIdsCsv = joinCsv(seleccion);

        List<Map<String, Object>> rows = repository.guardarPrivilegiosRol(idRol, menuIdsCsv);
        List<PrivilegioMenuResponse> menus = construirMenus(rows);
        return new PrivilegioRolDetalleResponse(idRol, rol, menus);
    }



    /**
     * Aplica un set predefinido de menus para el perfil Supervisor Cuadrillas.
     */
    public PrivilegioRolDetalleResponse aplicarPresetSupervisorCuadrillas(Integer idRol) {
        return actualizarPrivilegiosRol(idRol, MENU_IDS_PRESET_SUPERVISOR_CUADRILLAS);
    }

    /**
     * Reemplaza la relacion de paginas asociadas para un menu.
     */
    public PrivilegioMenuPaginasResponse actualizarPaginasPorMenu(Integer idMenu, List<String> paginasAsociadas) {
        if (idMenu == null || idMenu <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idMenu es requerido.");
        }
        List<String> paginasSanitizadas = sanitizarPaginasAsociadas(paginasAsociadas);
        String paginasCsv = joinCsvStrings(paginasSanitizadas);
        List<Map<String, Object>> rows = repository.guardarPaginasPorMenu(idMenu, paginasCsv);

        String nombreMenu = null;
        List<String> paginas = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Integer idMenuRow = toInteger(findValue(row, "id_menu", "idmenu"));
            if (idMenuRow != null && !idMenu.equals(idMenuRow)) {
                continue;
            }
            if (nombreMenu == null) {
                nombreMenu = toString(findValue(row, "nombre", "menu"));
            }
            String pagina = toString(findValue(row, "pagina_asociada", "paginaasociada"));
            if (pagina == null) {
                continue;
            }
            String normalizada = pagina.trim();
            if (normalizada.isEmpty()) {
                continue;
            }
            String key = normalizada.toLowerCase(Locale.ROOT);
            if (unique.add(key)) {
                paginas.add(normalizada);
            }
        }

        if (nombreMenu == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "Menu no encontrado o inactivo.");
        }

        return new PrivilegioMenuPaginasResponse(idMenu, nombreMenu, paginas);
    }

    /**
     * Actualiza el nombre mostrado en sidebar para un menu.
     */
    public PrivilegioMenuSidebarNombreResponse actualizarNombreSidebarPorMenu(Integer idMenu, String nombreSidebarRaw) {
        if (idMenu == null || idMenu <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idMenu es requerido.");
        }
        String nombreSidebar = sanitizeNombreSidebar(nombreSidebarRaw);
        List<Map<String, Object>> rows = repository.guardarNombreSidebarPorMenu(idMenu, nombreSidebar);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "Menu no encontrado o inactivo.");
        }
        Map<String, Object> row = rows.get(0);
        Integer idMenuResp = toInteger(findValue(row, "id_menu", "idmenu"));
        String nombre = toString(findValue(row, "nombre", "menu"));
        String nombreSidebarResp = toString(findValue(row, "nombre_sidebar", "nombresidebar"));
        return new PrivilegioMenuSidebarNombreResponse(
                idMenuResp == null ? idMenu : idMenuResp,
                nombre,
                nombreSidebarResp
        );
    }




    /**
     * Arma los permisos efectivos del usuario autenticado a partir de su rol.
     */
    public PrivilegioUsuarioResponse obtenerPermisosUsuario(AuthLoginResponse usuario, boolean administrador) {
        if (usuario == null || usuario.getIdRol() == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo resolver el rol del usuario autenticado."
            );
        }
        PrivilegioRolDetalleResponse detalle = obtenerPrivilegiosPorRol(usuario.getIdRol());
        List<PrivilegioMenuResponse> menus = detalle.getMenus();
        List<Integer> menuIds = new ArrayList<>();
        for (PrivilegioMenuResponse menu : menus) {
            if (menu.isAsignado()) {
                menuIds.add(menu.getIdMenu());
            }
        }
        return new PrivilegioUsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getIdRol(),
                usuario.getRol(),
                administrador,
                menuIds,
                menus
        );
    }




    /**
     * Mapea filas de BD a DTO de menu, normalizando nombres y banderas de asignacion.
     */
    private List<PrivilegioMenuResponse> construirMenus(List<Map<String, Object>> rows) {
        List<PrivilegioMenuResponse> response = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer idMenu = toInteger(findValue(row, "id_menu", "idmenu"));
            if (idMenu == null) {
                continue;
            }
            String nombre = toString(findValue(row, "nombre", "menu"));
            String nombreMostrar = limpiarNombreMenu(nombre);
            String nombreSidebar = toString(findValue(row, "nombre_sidebar", "nombresidebar"));
            String paginaAsociadaLegacy = toString(findValue(row, "pagina_asociada", "paginaasociada"));
            String paginasAsociadasCsv = toString(findValue(row, "paginas_asociadas_csv", "paginasasociadascsv"));
            List<String> paginasAsociadas = parsePaginasAsociadas(paginasAsociadasCsv, paginaAsociadaLegacy);
            String paginaAsociada = paginasAsociadas.isEmpty() ? null : paginasAsociadas.get(0);
            Integer nivel = toInteger(findValue(row, "nivel"));
            Integer padre = toInteger(findValue(row, "padre", "id_padre"));
            boolean asignado = toBoolean(findValue(row, "asignado")) == Boolean.TRUE;
            response.add(new PrivilegioMenuResponse(
                    idMenu,
                    nombre,
                    nombreMostrar,
                    nombreSidebar,
                    paginaAsociada,
                    paginasAsociadas,
                    nivel,
                    padre,
                    asignado
            ));
        }
        return response;
    }

    /**
     * Convierte CSV de paginas asociadas en lista deduplicada y ordenada por llegada.
     * Mantiene compatibilidad con el campo legacy de pagina unica.
     */
    private List<String> parsePaginasAsociadas(String paginasCsv, String paginaLegacy) {
        Set<String> uniqueNormalized = new HashSet<>();
        List<String> pages = new ArrayList<>();

        if (paginasCsv != null) {
            String[] rawPages = paginasCsv.split(",");
            for (String raw : rawPages) {
                String value = raw == null ? null : raw.trim();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                String key = value.toLowerCase(Locale.ROOT);
                if (uniqueNormalized.add(key)) {
                    pages.add(value);
                }
            }
        }

        if (paginaLegacy != null) {
            String value = paginaLegacy.trim();
            if (!value.isEmpty()) {
                String key = value.toLowerCase(Locale.ROOT);
                if (uniqueNormalized.add(key)) {
                    pages.add(value);
                }
            }
        }

        return pages;
    }





    /**
     * Busca el nombre de rol por id y valida que exista.
     */
    private String resolverNombreRol(Integer idRol) {
        if (idRol == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idRol es requerido.");
        }
        List<PrivilegioRolResponse> roles = listarRoles();
        for (PrivilegioRolResponse rol : roles) {
            if (idRol.equals(rol.getIdRol())) {
                return rol.getNombre();
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "ROL_NOT_FOUND", "Rol no encontrado.");
    }




    /**
     * Convierte una lista de ids en formato CSV para enviarlo al SP.
     */
    private String joinCsv(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer value : values) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    /**
     * Convierte una lista de textos en CSV para persistencia en SP.
     */
    private String joinCsvStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(trimmed);
        }
        return sb.toString();
    }





    /**
     * Limpia ids de menu: elimina nulos, no positivos y duplicados.
     */
    private List<Integer> sanitizarMenuIds(List<Integer> menuIds) {
        if (menuIds == null) {
            return Collections.emptyList();
        }
        Set<Integer> unique = new HashSet<>();
        List<Integer> result = new ArrayList<>();
        for (Integer id : menuIds) {
            if (id == null || id <= 0) {
                continue;
            }
            if (unique.add(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Limpia paginas asociadas: elimina vacios, duplicados y valida formato de persistencia.
     */
    private List<String> sanitizarPaginasAsociadas(List<String> paginasAsociadas) {
        if (paginasAsociadas == null) {
            return Collections.emptyList();
        }
        Set<String> unique = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String pagina : paginasAsociadas) {
            if (pagina == null) {
                continue;
            }
            String value = pagina.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.length() > 150) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "El nombre de pagina no puede exceder 150 caracteres."
                );
            }
            if (value.contains(",")) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "El nombre de pagina no puede contener comas."
                );
            }
            String key = value.toLowerCase(Locale.ROOT);
            if (unique.add(key)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Limpia nombre para sidebar; vacio equivale a NULL.
     */
    private String sanitizeNombreSidebar(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > 150) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "nombreSidebar no puede exceder 150 caracteres."
            );
        }
        return value;
    }





    /**
     * Obtiene un valor del map usando aliases de columna equivalentes.
     */
    private Object findValue(Map<String, Object> row, String... candidates) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(normalize(entry.getKey()), entry.getValue());
        }
        for (String candidate : candidates) {
            String key = normalize(candidate);
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
        }
        return null;
    }






    /**
     * Normaliza claves removiendo "_" y forzando minusculas.
     */
    private String normalize(String value) {
        return value == null ? "" : value.replace("_", "").toLowerCase(Locale.ROOT);
    }



    

    /**
     * Convierte un valor dinamico a Integer de forma segura.
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Convierte un valor dinamico a Boolean aceptando formatos numericos y texto.
     */
    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if (text.equals("true") || text.equals("1") || text.equals("si") || text.equals("s")) {
            return true;
        }
        if (text.equals("false") || text.equals("0") || text.equals("no") || text.equals("n")) {
            return false;
        }
        return null;
    }

    /**
     * Normaliza el nombre tecnico de menu para mostrarlo en UI.
     */
    private String limpiarNombreMenu(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return value;
        }
        value = value.replaceFirst("^(?i)(ms_|tsm_)", "");
        value = value.replace('_', ' ');
        value = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        value = value.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        value = value.replaceAll("\\s+", " ").trim();

        String[] parts = value.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(formatToken(part));
        }
        return sb.toString();
    }

    /**
     * Da formato legible a cada token del nombre de menu respetando siglas.
     */
    private String formatToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        String upper = token.toUpperCase(Locale.ROOT);
        if (token.matches(".*\\d.*")) {
            return upper;
        }
        if (upper.length() <= 4 && token.equals(upper)) {
            return upper;
        }
        if (upper.equals("OT") || upper.equals("CU") || upper.equals("MET") || upper.equals("CUNR")
                || upper.equals("BO") || upper.equals("DTH") || upper.equals("PDA")) {
            return upper;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Convierte un valor dinamico a String.
     */
    private String toString(Object value) {
        return value == null ? null : value.toString();
    }
}
