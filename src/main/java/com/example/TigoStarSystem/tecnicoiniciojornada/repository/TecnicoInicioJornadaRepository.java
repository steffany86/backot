package com.example.TigoStarSystem.tecnicoiniciojornada.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
public class TecnicoInicioJornadaRepository {

    public boolean existeRegistroHoy(JdbcTemplate template, Integer idTecnico) {
        Integer existe = template.queryForObject(
                "EXEC dbo.SP_InicioJornada_ExisteRegistroHoy ?",
                Integer.class,
                idTecnico
        );
        return existe != null && existe > 0;
    }

    public int marcarNoCierreAtrasado(JdbcTemplate template, Integer idTecnico) {
        Integer updated = template.queryForObject(
                "EXEC dbo.SP_InicioJornada_MarcarNoCierreAtrasado ?",
                Integer.class,
                idTecnico
        );
        return updated == null ? 0 : updated;
    }

    public Map<String, Object> estadoCierreHoy(JdbcTemplate template, Integer idTecnico) {
        List<Map<String, Object>> rows = template.queryForList(
                "EXEC dbo.SP_InicioJornada_EstadoCierreHoy ?",
                idTecnico
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> buscarCierrePendienteAyer(JdbcTemplate template, Integer idTecnico) {
        if (template == null || idTecnico == null || idTecnico <= 0) {
            return null;
        }
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT TOP 1 ultimo.id_inicio, ultimo.id_tecnico, ultimo.id_encargado, ultimo.fecha_registro, " +
                        "ultimo.fecha_cierre, ultimo.pendiente, ultimo.no_marco_cierre " +
                        "FROM ( " +
                        "  SELECT TOP 1 id_inicio, id_tecnico, id_encargado, fecha_registro, fecha_cierre, pendiente, no_marco_cierre, e_eliminado " +
                        "  FROM dbo.tbl_InicioJornadaAlturas " +
                        "  WHERE id_tecnico = ? " +
                        "    AND CAST(fecha_registro AS DATE) < CAST(GETDATE() AS DATE) " +
                        "  ORDER BY fecha_registro DESC, id_inicio DESC " +
                        ") ultimo " +
                        "WHERE fecha_cierre IS NULL " +
                        "  AND ISNULL(e_eliminado, 0) = 0 " +
                        "  AND ISNULL(no_marco_cierre, 0) = 1",
                idTecnico
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int countNoMarco(JdbcTemplate template, Integer idTecnico) {
        Integer total = template.queryForObject(
                "EXEC dbo.SP_InicioJornada_CountNoMarco ?",
                Integer.class,
                idTecnico
        );
        return total == null ? 0 : total;
    }

    public List<Map<String, Object>> listarEncargados(JdbcTemplate tecnicosTemplate) {
        List<Map<String, Object>> rows = queryForListConSpAlternativos(
                tecnicosTemplate,
                "EXEC dbo.spx_ObtenerSupervisoresConformacionCuadrillaWeb",
                "EXEC spx_ObtenerSupervisoresConformacionCuadrillaWeb",
                "EXEC dbo.spx_ObtenerSupervisores",
                "EXEC spx_ObtenerSupervisores"
        );
        return normalizarEncargados(rows);
    }

    public Map<String, Object> buscarEncargadoActualPorTecnico(
            JdbcTemplate centralTemplate,
            JdbcTemplate template,
            String sucursal,
            Integer idTecnico,
            String nombreTecnico
    ) {
        if (template == null || centralTemplate == null) {
            return null;
        }

        List<Map<String, Object>> rows = queryConformacionHoyPorTecnicoCentral(centralTemplate, sucursal, idTecnico, nombreTecnico);
        if (rows == null || rows.isEmpty()) {
            rows = queryConformacionPorTecnicoCentral(centralTemplate, sucursal, idTecnico, nombreTecnico);
        }
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        for (Map<String, Object> row : rows) {
            Object id = findValue(row,
                    "idEncargado", "id_encargado", "id_usuario_supervisor", "idusuariosupervisor",
                    "id_supervisor", "idsupervisor", "id_usuario", "idusuario");
            Object sucursalRow = findValue(row, "sucursal", "Sucursal");
            Integer idEncargado = toInteger(id);
            String nombreEncargado = obtenerNombreUsuarioPorId(template, idEncargado);
            if (nombreEncargado == null || nombreEncargado.trim().isEmpty()) {
                Object nombreFromRow = findValue(row, "supervisorACargo", "supervisor_a_cargo", "encargado", "supervisor");
                if (nombreFromRow != null) {
                    String text = String.valueOf(nombreFromRow).trim();
                    if (!text.isEmpty()) {
                        nombreEncargado = text;
                    }
                }
            }
            if (idEncargado == null || idEncargado <= 0) {
                // Fallback: en algunos registros historicos viene id=0 pero si nombre valido.
                idEncargado = obtenerIdUsuarioPorNombre(template, nombreEncargado);
            }
            if (idEncargado == null || idEncargado <= 0) {
                continue;
            }
            Integer idAuxiliar = toInteger(findValue(row,
                    "idTecnicoAuxiliar",
                    "id_tecnico_auxiliar",
                    "id_tecnicoAuxiliar",
                    "idAuxiliar",
                    "id_auxiliar"));
            String nombreAuxiliar = toText(findValue(row,
                    "auxiliar",
                    "tecnicoAuxiliar",
                    "tecnico_auxiliar",
                    "nombreAuxiliar",
                    "auxiliarNombre"));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idEncargado", String.valueOf(idEncargado));
            if (nombreEncargado != null && !nombreEncargado.trim().isEmpty()) {
                out.put("encargado", nombreEncargado.trim());
            }
            if (idAuxiliar != null && idAuxiliar > 0) {
                out.put("idAuxiliar", idAuxiliar);
            }
            if (nombreAuxiliar != null) {
                out.put("auxiliar", nombreAuxiliar);
            }
            if (sucursalRow != null) {
                out.put("sucursal", String.valueOf(sucursalRow).trim());
            }
            return out;
        }
        return null;
    }

    private List<Map<String, Object>> queryConformacionHoyPorTecnicoCentral(
            JdbcTemplate centralTemplate,
            String sucursal,
            Integer idTecnico,
            String nombreTecnico
    ) {
        String nombre = nombreTecnico == null ? "" : nombreTecnico.trim().toLowerCase(Locale.ROOT);
        int id = idTecnico == null ? -1 : idTecnico;
        String sucursalNorm = toText(sucursal);
        try {
            return centralTemplate.queryForList(
                    "SELECT TOP 1 " +
                            "CAST(c.idUsuarioSupervisor AS INT) AS idEncargado, " +
                            "CAST(c.supervisorACargo AS NVARCHAR(200)) AS encargado, " +
                            "CAST(c.id AS INT) AS idConformacion, " +
                            "CAST(c.id_tecnicoAuxiliar AS INT) AS idAuxiliar, " +
                            "CAST(c.auxiliar AS NVARCHAR(250)) AS auxiliar, " +
                            "c.sucursal, c.fecha, c.fechaRegistro " +
                            "FROM dbo.tbl_ConformacionCuadrillaDiario c " +
                            "WHERE ISNULL(c.e_eliminado, 0) = 0 " +
                            "  AND CONVERT(date, c.fecha) = CONVERT(date, GETDATE()) " +
                            "  AND (? IS NULL OR LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(ISNULL(c.sucursal, ''))), '_', ''), '-', ''), ' ', '')) = " +
                            "                  LOWER(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(?)), '_', ''), '-', ''), ' ', ''))) " +
                            "  AND ( " +
                            "       (? > 0 AND (? = c.id_tecnico OR ? = c.id_tecnicoAuxiliar)) " +
                            "       OR (? <> '' AND ( " +
                            "           LOWER(LTRIM(RTRIM(ISNULL(c.tecnico, '')))) = ? " +
                            "           OR LOWER(LTRIM(RTRIM(ISNULL(c.auxiliar, '')))) = ? " +
                            "       )) " +
                            "  ) " +
                            "ORDER BY ISNULL(c.fechaRegistro, '19000101') DESC, c.id DESC",
                    sucursalNorm,
                    sucursalNorm,
                    id,
                    id,
                    id,
                    nombre,
                    nombre,
                    nombre
            );
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private Integer obtenerIdUsuarioPorNombre(JdbcTemplate template, String nombreUsuario) {
        String nombre = toText(nombreUsuario);
        if (template == null || nombre == null) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = template.queryForList(
                    "SELECT TOP 1 Id_Usuario FROM dbo.tbl_Usuario " +
                            "WHERE ISNULL(E_Eliminado,0)=0 AND UPPER(LTRIM(RTRIM(Nombre))) = UPPER(LTRIM(RTRIM(?)))",
                    nombre
            );
            if (rows != null && !rows.isEmpty()) {
                Integer id = toInteger(findValue(rows.get(0), "Id_Usuario", "id_usuario", "idUsuario"));
                if (id != null && id > 0) {
                    return id;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            List<Map<String, Object>> rows = template.queryForList("EXEC dbo.SP_Usuario_ListarActivosBasico");
            for (Map<String, Object> row : rows) {
                String nombreRow = toText(findValue(row, "nombre", "Nombre"));
                if (nombreRow == null || !nombreRow.equalsIgnoreCase(nombre)) {
                    continue;
                }
                Integer id = toInteger(findValue(row, "idUsuario", "Id_Usuario", "id_usuario"));
                if (id != null && id > 0) {
                    return id;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<Map<String, Object>> queryConformacionPorTecnicoCentral(
            JdbcTemplate centralTemplate,
            String sucursal,
            Integer idTecnico,
            String nombreTecnico
    ) {
        try {
            return centralTemplate.queryForList(
                    "EXEC dbo.spx_Central_BuscarSupervisorPorTecnicoConformacion ?, ?, ?",
                    sucursal,
                    idTecnico,
                    nombreTecnico
            );
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> queryConformacionPorTecnico(
            JdbcTemplate template,
            Integer idTecnico,
            String nombreTecnico
    ) {
        String nombre = nombreTecnico == null ? "" : nombreTecnico.trim().toLowerCase(Locale.ROOT);
        int id = idTecnico == null ? -1 : idTecnico;
        return queryConSqlAlternativos(
                template,
                new Object[]{id, id, nombre, nombre},
                "SELECT TOP 10 * " +
                        "FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "  AND (" +
                        "       id_tecnico = ? " +
                        "    OR id_tecnicoAuxiliar = ? " +
                        "    OR LOWER(LTRIM(RTRIM(ISNULL(CAST(tecnico AS NVARCHAR(250)), '')))) = ? " +
                        "    OR LOWER(LTRIM(RTRIM(ISNULL(CAST(auxiliar AS NVARCHAR(250)), '')))) = ? " +
                        "  ) " +
                        "ORDER BY CASE WHEN CAST(fecha AS DATE) = CAST(GETDATE() AS DATE) THEN 0 ELSE 1 END, " +
                        "         ISNULL(fecha, '19000101') DESC, " +
                        "         ISNULL(fechaRegistro, '19000101') DESC, " +
                        "         id DESC",
                "SELECT TOP 10 * " +
                        "FROM dbo.tbl_ConformacionCuadrillaDiario " +
                        "WHERE ISNULL(e_eliminado, 0) = 0 " +
                        "  AND (" +
                        "       id_tecnico = ? " +
                        "    OR id_auxiliar = ? " +
                        "    OR LOWER(LTRIM(RTRIM(ISNULL(CAST(tecnico AS NVARCHAR(250)), '')))) = ? " +
                        "    OR LOWER(LTRIM(RTRIM(ISNULL(CAST(auxiliar AS NVARCHAR(250)), '')))) = ? " +
                        "  ) " +
                        "ORDER BY CASE WHEN CAST(fecha AS DATE) = CAST(GETDATE() AS DATE) THEN 0 ELSE 1 END, " +
                        "         ISNULL(fecha, '19000101') DESC, " +
                        "         ISNULL(fechaRegistro, '19000101') DESC, " +
                        "         id DESC"
        );
    }

    private List<Map<String, Object>> queryConSqlAlternativos(
            JdbcTemplate template,
            Object[] args,
            String... sqlAlternativos
    ) {
        if (template == null || sqlAlternativos == null || sqlAlternativos.length == 0) {
            return new ArrayList<>();
        }
        for (String sql : sqlAlternativos) {
            try {
                List<Map<String, Object>> rows = template.queryForList(sql, args);
                if (rows != null && !rows.isEmpty()) {
                    return rows;
                }
            } catch (DataAccessException ex) {
                // intenta siguiente variante de schema
            }
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> queryForListConSpAlternativos(
            JdbcTemplate template,
            String... sqlAlternativos
    ) {
        if (template == null || sqlAlternativos == null || sqlAlternativos.length == 0) {
            return new ArrayList<>();
        }
        for (String sql : sqlAlternativos) {
            try {
                List<Map<String, Object>> rows = template.queryForList(sql);
                if (rows != null && !rows.isEmpty()) {
                    return rows;
                }
            } catch (DataAccessException ex) {
                // intenta siguiente SP
            }
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> normalizarEncargados(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Object id = findValue(row,
                    "idEncargado", "id_encargado", "id_usuario_supervisor", "idusuariosupervisor",
                    "id_supervisor", "idsupervisor", "id_usuario", "idusuario");
            Object nombre = findValue(row,
                    "encargado", "supervisor", "supervisor_a_cargo", "supervisorACargo", "nombre");
            if (id == null || nombre == null) {
                continue;
            }
            String idText = String.valueOf(id).trim();
            String nombreText = String.valueOf(nombre).trim();
            if (idText.isEmpty() || nombreText.isEmpty()) {
                continue;
            }
            Map<String, Object> normalizada = new LinkedHashMap<>();
            normalizada.put("idEncargado", idText);
            normalizada.put("encargado", nombreText);
            out.add(normalizada);
        }
        return out;
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

    public List<Map<String, Object>> registrar(
            JdbcTemplate template,
            Integer idTecnico,
            Integer idAuxiliar,
            Integer idEncargado,
            Integer idUsuarioSupervisorGrupo,
            Integer idSucursal,
            String sucursal,
            String nombreTecnico,
            String capacitado,
            String charla,
            String botiquin,
            String extintor,
            LocalDate fechaVencimiento,
            String equipoEpp,
            String estadoEpp,
            String apr,
            String escalera,
            String anclaje,
            String imagen,
            String aceptoInicioJornada,
            String aceptoCierreJornada
    ) {
        return template.queryForList(
                "EXEC dbo.SP_InicioJornada_Registrar ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?",
                idTecnico,
                idAuxiliar,
                idEncargado,
                idUsuarioSupervisorGrupo,
                idSucursal,
                sucursal,
                nombreTecnico,
                capacitado,
                charla,
                botiquin,
                extintor,
                fechaVencimiento == null ? null : Date.valueOf(fechaVencimiento),
                equipoEpp,
                estadoEpp,
                apr,
                escalera,
                anclaje,
                imagen,
                aceptoInicioJornada,
                aceptoCierreJornada
        );
    }

    public int actualizarUbicacionInicio(JdbcTemplate template, Integer idInicio, String ubicacionGeoRef) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        String ubicacion = toText(ubicacionGeoRef);
        if (ubicacion == null) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET ubicacion_georef = ? WHERE id_inicio = ?",
                    ubicacion,
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int actualizarImagenAuxiliarInicio(JdbcTemplate template, Integer idInicio, String imagenAuxiliar) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        String imagen = toText(imagenAuxiliar);
        if (imagen == null) {
            return 0;
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String columnaImagenAuxiliar = firstExistingColumn(columnas, "imagen_auxiliar", "imagenAuxiliar");
        if (columnaImagenAuxiliar == null) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET [" + columnaImagenAuxiliar + "] = ? WHERE id_inicio = ?",
                    imagen,
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int actualizarFirmaInicio(JdbcTemplate template, Integer idInicio, String firmaInicio) {
        return actualizarTextoInicioJornada(
                template,
                idInicio,
                firmaInicio,
                "firma_inicio",
                "firmaInicio",
                "FirmaInicio"
        );
    }

    public int actualizarFirmaCierre(JdbcTemplate template, Integer idInicio, String firmaCierre) {
        return actualizarTextoInicioJornada(
                template,
                idInicio,
                firmaCierre,
                "firma_cierre",
                "firmaCierre",
                "FirmaCierre"
        );
    }

    private int actualizarTextoInicioJornada(JdbcTemplate template, Integer idInicio, String value, String... columnCandidates) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        String text = toText(value);
        if (text == null) {
            return 0;
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String columna = firstExistingColumn(columnas, columnCandidates);
        if (columna == null) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET [" + columna + "] = ? WHERE id_inicio = ?",
                    text,
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int marcarNoMarcoCierreInicio(JdbcTemplate template, Integer idInicio) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET no_marco_cierre = 1 WHERE id_inicio = ?",
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int marcarCierreCompletado(JdbcTemplate template, Integer idInicio) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET pendiente = 0, no_marco_cierre = 0 WHERE id_inicio = ?",
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public boolean inicioPendienteAprobacion(JdbcTemplate template, Integer idInicio, Integer idTecnico) {
        if (template == null || idInicio == null || idInicio <= 0 || idTecnico == null || idTecnico <= 0) {
            return false;
        }
        try {
            Integer total = template.queryForObject(
                    "SELECT COUNT(1) FROM dbo.tbl_InicioJornadaAlturas " +
                            "WHERE id_inicio = ? " +
                            "  AND id_tecnico = ? " +
                            "  AND ISNULL(pendiente, 0) = 1 " +
                            "  AND ISNULL(e_eliminado, 0) = 0",
                    Integer.class,
                    idInicio,
                    idTecnico
            );
            return total != null && total > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public int marcarNoMarcoCierreCompletado(JdbcTemplate template, Integer idInicio) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET no_marco_cierre = 0 WHERE id_inicio = ?",
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int actualizarAceptoCierreJornada(JdbcTemplate template, Integer idInicio, String aceptoCierreJornada) {
        if (template == null || idInicio == null || idInicio <= 0) {
            return 0;
        }
        String acepto = toText(aceptoCierreJornada);
        if (acepto == null) {
            return 0;
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String columnaAcepto = firstExistingColumn(columnas, "AceptoCierreJornada", "acepto_cierre_jornada");
        if (columnaAcepto == null) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET [" + columnaAcepto + "] = ? WHERE id_inicio = ?",
                    acepto,
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public int actualizarEstoyTrabajandoSolo(JdbcTemplate template, Integer idInicio, Boolean estoyTrabajandoSolo) {
        if (template == null || idInicio == null || idInicio <= 0 || estoyTrabajandoSolo == null) {
            return 0;
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String columnaTrabajoSolo = firstExistingColumn(
                columnas,
                "EstoyTrabajandoSolo",
                "estoy_trabajando_solo",
                "trabajando_solo",
                "trabajandoSolo"
        );
        if (columnaTrabajoSolo == null) {
            return 0;
        }
        try {
            return template.update(
                    "UPDATE dbo.tbl_InicioJornadaAlturas SET [" + columnaTrabajoSolo + "] = ? WHERE id_inicio = ?",
                    estoyTrabajandoSolo,
                    idInicio
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    public List<Map<String, Object>> cerrarJornada(
            JdbcTemplate template,
            Integer idTecnico,
            String codigoCliente,
            Boolean danoMaterial,
            String observacionMaterial,
            Boolean danoPersona,
            String observacionPersona,
            Boolean novedadesTrabajo,
            String observacionNovedades,
            String ubicacionGeoRef
    ) {
        List<Map<String, Object>> rows = template.queryForList(
                "EXEC dbo.SP_InicioJornada_Cerrar ?, ?, ?, ?, ?, ?, ?, ?, ?",
                idTecnico,
                codigoCliente,
                danoMaterial,
                observacionMaterial,
                danoPersona,
                observacionPersona,
                novedadesTrabajo,
                observacionNovedades,
                ubicacionGeoRef
        );
        return rows == null ? Collections.emptyList() : rows;
    }

    public List<Map<String, Object>> cerrarJornadaPorId(
            JdbcTemplate template,
            Integer idInicio,
            Integer idTecnico,
            String codigoCliente,
            Boolean danoMaterial,
            String observacionMaterial,
            Boolean danoPersona,
            String observacionPersona,
            Boolean novedadesTrabajo,
            String observacionNovedades,
            String ubicacionGeoRef
    ) {
        if (template == null || idInicio == null || idInicio <= 0 || idTecnico == null || idTecnico <= 0) {
            return Collections.emptyList();
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String codigoColumn = firstExistingColumn(columnas, "codigo_cliente_cierre", "codigo_cliente");
        String ubicacionColumn = firstExistingColumn(columnas, "ubicacion_cierre_georef", "ubicacion_georef_cierre", "ubicacion_georef");
        if (codigoColumn == null || ubicacionColumn == null) {
            return Collections.emptyList();
        }

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        sets.add("fecha_cierre = GETDATE()");
        sets.add("no_marco_cierre = 0");
        sets.add(codigoColumn + " = ?");
        params.add(codigoCliente);
        addSetIfColumnExists(columnas, sets, params, "dano_material", danoMaterial);
        addSetIfColumnExists(columnas, sets, params, "observacion_material", observacionMaterial);
        addSetIfColumnExists(columnas, sets, params, "dano_persona", danoPersona);
        addSetIfColumnExists(columnas, sets, params, "observacion_persona", observacionPersona);
        addSetIfColumnExists(columnas, sets, params, "novedades_trabajo", novedadesTrabajo);
        addSetIfColumnExists(columnas, sets, params, "observacion_novedades", observacionNovedades);
        sets.add(ubicacionColumn + " = ?");
        params.add(ubicacionGeoRef);

        params.add(idInicio);
        params.add(idTecnico);
        int updated = template.update(
                "UPDATE dbo.tbl_InicioJornadaAlturas SET " + String.join(", ", sets) + " " +
                        "WHERE id_inicio = ? " +
                        "  AND id_tecnico = ? " +
                        "  AND fecha_cierre IS NULL " +
                        "  AND ISNULL(e_eliminado, 0) = 0 " +
                        "  AND ISNULL(no_marco_cierre, 0) = 1",
                params.toArray()
        );
        if (updated <= 0) {
            return Collections.emptyList();
        }
        return template.queryForList(
                "SELECT TOP 1 * FROM dbo.tbl_InicioJornadaAlturas WHERE id_inicio = ? AND id_tecnico = ?",
                idInicio,
                idTecnico
        );
    }

    public String obtenerNombreTecnicoPorId(JdbcTemplate template, Integer idTecnico) {
        if (template == null || idTecnico == null || idTecnico <= 0) {
            return null;
        }
        String nombre = queryNombre(
                template,
                "SELECT TOP 1 u.Nombre AS Nombre " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario " +
                        "WHERE ut.id_Usuario = ? AND ISNULL(ut.e_eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;

        nombre = queryNombre(
                template,
                "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ? AND ISNULL(E_Eliminado,0)=0",
                idTecnico
        );
        if (nombre != null) return nombre;

        return queryNombre(
                template,
                "SELECT TOP 1 v.Nombre AS Nombre " +
                        "FROM dbo.tbl_UsuarioTecnico ut " +
                        "LEFT JOIN dbo.tbl_Vendedor v ON v.Id_Vendedor = ut.id_Vendedor " +
                        "WHERE ut.id_Usuario = ? AND ISNULL(ut.e_eliminado,0)=0 AND ISNULL(v.E_Eliminado,0)=0",
                idTecnico
        );
    }

    public String obtenerNombreUsuarioPorId(JdbcTemplate template, Integer idUsuario) {
        if (template == null || idUsuario == null || idUsuario <= 0) {
            return null;
        }
        String nombre = queryNombre(
                template,
                "SELECT TOP 1 Nombre FROM dbo.tbl_Usuario WHERE Id_Usuario = ? AND ISNULL(E_Eliminado,0)=0",
                idUsuario
        );
        if (nombre != null) return nombre;
        try {
            List<Map<String, Object>> rows = template.queryForList("EXEC dbo.SP_Usuario_ListarActivosBasico");
            for (Map<String, Object> row : rows) {
                Integer id = toInteger(findValue(row, "idUsuario", "id_usuario", "Id_Usuario"));
                if (id == null || !id.equals(idUsuario)) continue;
                String n = toText(findValue(row, "nombre", "Nombre"));
                if (n != null) return n;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String queryNombre(JdbcTemplate template, String sql, Integer id) {
        try {
            List<Map<String, Object>> rows = template.queryForList(sql, id);
            if (rows == null || rows.isEmpty()) return null;
            Object value = findValue(rows.get(0), "Nombre", "nombre", "tecnico");
            if (value == null) return null;
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : text;
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    public Map<String, Object> buscarInicioRechazadoHoy(JdbcTemplate template, Integer idTecnico) {
        if (template == null || idTecnico == null || idTecnico <= 0) {
            return null;
        }
        Set<String> columnas = obtenerColumnasInicioJornada(template);
        String columnaObservacion = firstExistingColumn(columnas, "observacionrechazado", "observacion_rechazado", "ObservacionRechazado");
        String selectObservacion = columnaObservacion == null ? "NULL AS observacion_rechazado" : ("[" + columnaObservacion + "] AS observacion_rechazado");
        try {
            List<Map<String, Object>> rows = template.queryForList(
                    "SELECT TOP 1 id_inicio, fecha_registro, " + selectObservacion + " " +
                            "FROM dbo.tbl_InicioJornadaAlturas " +
                            "WHERE id_tecnico = ? " +
                            "  AND CAST(fecha_registro AS DATE) = CAST(GETDATE() AS DATE) " +
                            "  AND ISNULL(e_eliminado, 0) = 1 " +
                            "ORDER BY fecha_registro DESC, id_inicio DESC",
                    idTecnico
            );
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    private Set<String> obtenerColumnasInicioJornada(JdbcTemplate template) {
        Set<String> out = new HashSet<>();
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='tbl_InicioJornadaAlturas'"
        );
        for (Map<String, Object> row : rows) {
            Object c = row.get("COLUMN_NAME");
            if (c != null) {
                out.add(normalizeKey(String.valueOf(c)));
            }
        }
        return out;
    }

    private String firstExistingColumn(Set<String> columnas, String... nombres) {
        if (columnas == null || nombres == null) {
            return null;
        }
        for (String nombre : nombres) {
            if (columnas.contains(normalizeKey(nombre))) {
                return nombre;
            }
        }
        return null;
    }

    private void addSetIfColumnExists(Set<String> columnas, List<String> sets, List<Object> params, String columna, Object value) {
        if (columnas == null || !columnas.contains(normalizeKey(columna))) {
            return;
        }
        sets.add(columna + " = ?");
        params.add(value);
    }

    private String construirValuesClause(int totalColumns) {
        List<String> values = new ArrayList<>();
        int paramsAssigned = 0;
        for (int i = 0; i < totalColumns; i++) {
            if (i == 3) {
                values.add("GETDATE()");
            } else if (i == 4) {
                values.add("1");
            } else {
                values.add("?");
                paramsAssigned++;
            }
        }
        return String.join(", ", values);
    }

    public boolean existePendienteAprobacionHoy(JdbcTemplate template, Integer idTecnico) {
        Integer existe = template.queryForObject(
                "EXEC dbo.SP_InicioJornada_ExistePendienteHoy ?",
                Integer.class,
                idTecnico
        );
        return existe != null && existe > 0;
    }
}
