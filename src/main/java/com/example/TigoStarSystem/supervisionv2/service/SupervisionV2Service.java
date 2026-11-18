package com.example.TigoStarSystem.supervisionv2.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2CrearRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2LlamadaAtencionRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2LlamadaFirmaRequest;
import com.example.TigoStarSystem.llamadaatencion.dto.LlamadaAtencionCrearRequest;
import com.example.TigoStarSystem.llamadaatencion.service.LlamadaAtencionService;
import com.example.TigoStarSystem.supervisionv2.dto.ChecklistRespuestaRequest;
import com.example.TigoStarSystem.supervisionv2.dto.MaterialCruceRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2CrearPendienteRequest;
import com.example.TigoStarSystem.supervision.dto.SupervisionSucursalRequest;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import com.example.TigoStarSystem.supervisionv2.repository.SupervisionV2Repository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class SupervisionV2Service {
    private final SupervisionV2Repository repository;
    private final AuthService authService;
    private final SupervisionV2FotoStorageService fotoStorage;

    /** Nombres de las columnas de foto en tbl_Supervision (en el mismo orden que leerFotos/escribirFotos). */
    private static final String[] FOTO_COLUMNAS = {
            "FotoBoletaSupervision", "FotoCanalesPilotos", "FotoNivelesDocsis", "FotoMedicionRuido",
            "FotoBarridoCanales", "FotoObservacion1", "FotoObservacion2", "FotoObservacion3", "FotoObservacion4"
    };

    public static class JornadaImagen {
        private final byte[] bytes;
        private final String contentType;

        public JornadaImagen(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getContentType() {
            return contentType;
        }
    }

    private final LlamadaAtencionService llamadaAtencionService;

    public SupervisionV2Service(
            SupervisionV2Repository repository,
            AuthService authService,
            SupervisionV2FotoStorageService fotoStorage,
            LlamadaAtencionService llamadaAtencionService) {
        this.repository = repository;
        this.authService = authService;
        this.fotoStorage = fotoStorage;
        this.llamadaAtencionService = llamadaAtencionService;
    }

    // ---------- Llamada de atencion al tecnico principal ----------

    /** Id de la llamada ya enlazada a la supervision, o null. */
    private Integer idLlamadaDe(Map<String, Object> datos) {
        return datos == null ? null : toInteger(datos.get("idLlamada"));
    }

    /**
     * Genera la llamada de atencion (estado Pendiente, sin firmas) al tecnico principal de una supervision COMPLETADA.
     * Solo el supervisor, y solo una llamada por supervision.
     */
    public Map<String, Object> crearLlamadaAtencion(String idSupervision, SupervisionV2LlamadaAtencionRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        requireSupervisor(me);
        if (request == null || isBlank(request.getIdTipoComunicacion()) || isBlank(request.getMotivo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El tipo de comunicacion y el motivo son obligatorios.");
        }
        Map<String, Object> datos = repository.obtenerDatosParaLlamada(idSupervision);
        if (datos == null || Boolean.TRUE.equals(datos.get("eliminado")) || "1".equals(String.valueOf(datos.get("eliminado")))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro la supervision.");
        }
        String estado = asText(datos.get("estadoSup")) == null ? "" : asText(datos.get("estadoSup")).trim().toLowerCase();
        if (!estado.startsWith("completad")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SUPERVISION_NO_COMPLETADA",
                    "Solo se puede generar la llamada de atencion de una supervision completada.");
        }
        if (idLlamadaDe(datos) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "LLAMADA_YA_EXISTE", "Esta supervision ya tiene una llamada de atencion.");
        }
        String idTecnico = asText(datos.get("idTecnico"));
        if (isBlank(idTecnico)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SIN_TECNICO", "La supervision no tiene tecnico principal.");
        }
        LlamadaAtencionCrearRequest llamada = new LlamadaAtencionCrearRequest();
        llamada.setIdTecnico(idTecnico);
        llamada.setCodEmpleado(idTecnico);
        llamada.setTecnico(asText(datos.get("tecnico")));
        llamada.setTabla("tbl_tecnico");
        llamada.setIdTipoComunicacion(request.getIdTipoComunicacion());
        llamada.setMotivo(request.getMotivo());
        llamada.setDescripcion(request.getDescripcion());
        llamada.setComentarioColaborador(request.getComentarioColaborador());
        llamada.setAcuerdos(request.getAcuerdos());
        llamada.setFechaSeguimiento(request.getFechaSeguimiento());

        Map<String, Object> creada = llamadaAtencionService.registrarPendiente(llamada, token);
        Integer idLlamada = toInteger(creada.get("idLlamadaAtencion"));
        if (idLlamada == null || repository.vincularLlamada(idSupervision, idLlamada) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "LLAMADA_YA_EXISTE",
                    "No se pudo enlazar la llamada de atencion con la supervision (ya tiene una).");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idLlamadaAtencion", idLlamada);
        out.put("estado", "Pendiente");
        return out;
    }

    /** Estado de la llamada de atencion de una supervision: tiene=false si aun no se genero. */
    public Map<String, Object> resumenLlamadaAtencion(String idSupervision, String token) {
        authService.me(token);
        Map<String, Object> out = new LinkedHashMap<>();
        Integer idLlamada = idLlamadaDe(repository.obtenerDatosParaLlamada(idSupervision));
        out.put("tiene", idLlamada != null);
        if (idLlamada != null) {
            Map<String, Object> resumen = llamadaAtencionService.obtenerResumen(idLlamada, token);
            out.put("idLlamadaAtencion", idLlamada);
            if (resumen != null) {
                out.put("estado", resumen.get("estado"));
                out.put("motivo", resumen.get("motivo"));
                out.put("fechaRegistro", resumen.get("fechaRegistro"));
                out.put("tecnico", resumen.get("tecnico"));
                out.put("descripcion", resumen.get("descripcion"));
                out.put("comentarioColaborador", resumen.get("comentarioColaborador"));
                out.put("acuerdos", resumen.get("acuerdos"));
                out.put("fechaSeguimiento", resumen.get("fechaSeguimiento"));
                out.put("testigo", resumen.get("testigo"));
                Object firma = resumen.get("firmaTecnico");
                out.put("firmado", firma != null && !String.valueOf(firma).trim().isEmpty());
            }
        }
        return out;
    }

    /** El supervisor hace firmar al tecnico: se guardan las firmas y la llamada pasa a COMPLETO. */
    public Map<String, Object> firmarLlamadaAtencion(String idSupervision, SupervisionV2LlamadaFirmaRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        requireSupervisor(me);
        Integer idLlamada = idLlamadaDe(repository.obtenerDatosParaLlamada(idSupervision));
        if (idLlamada == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Esta supervision no tiene llamada de atencion.");
        }
        return llamadaAtencionService.firmarPendiente(
                idLlamada,
                request == null ? null : request.getFirmaTecnico(),
                request == null ? null : request.getFirmaTestigo(),
                request == null ? null : request.getTestigo(),
                token);
    }

    private String[] leerFotos(SupervisionV2CrearRequest r) {
        return new String[] {
                r.getFotoBoletaSupervision(), r.getFotoCanalesPilos(), r.getFotoNivelesDocsis(),
                r.getFotoMedicionRuido(), r.getFotoBarridoCanales(), r.getFotoObservacion1(),
                r.getFotoObservacion2(), r.getFotoObservacion3(), r.getFotoObservacion4()
        };
    }

    private void escribirFotos(SupervisionV2CrearRequest r, String[] v) {
        r.setFotoBoletaSupervision(v[0]);
        r.setFotoCanalesPilos(v[1]);
        r.setFotoNivelesDocsis(v[2]);
        r.setFotoMedicionRuido(v[3]);
        r.setFotoBarridoCanales(v[4]);
        r.setFotoObservacion1(v[5]);
        r.setFotoObservacion2(v[6]);
        r.setFotoObservacion3(v[7]);
        r.setFotoObservacion4(v[8]);
    }

    /** Columnas de firma en tbl_Supervision (misma logica que las fotos: archivo + ruta en la columna). */
    private static final String[] FIRMA_COLUMNAS = {"FirmaTecnico", "FirmaAuxiliar", "FirmaCliente"};

    private String[] leerFirmas(SupervisionV2CrearRequest r) {
        return new String[] {r.getFirmaTecnico(), r.getFirmaAuxiliar(), r.getFirmaCliente()};
    }

    private void validarFirmas(String[] firmas) {
        for (int i = 0; i < firmas.length; i++) {
            fotoStorage.validar(firmas[i], FIRMA_COLUMNAS[i]);
        }
    }

    private void guardarFirmas(String[] firmas, String idSupervision) {
        String[] rutas = new String[firmas.length];
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < firmas.length; i++) {
            rutas[i] = fotoStorage.guardar(firmas[i], FIRMA_COLUMNAS[i], idSupervision, hoy);
        }
        repository.actualizarFotos(idSupervision, FIRMA_COLUMNAS, rutas);
    }

    // ---------- Checklist ----------

    public List<Map<String, Object>> listarChecklistItems(String token) {
        authService.me(token);
        try {
            return repository.listarChecklistItems();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarChecklistRespuestas(String idSupervision, String token) {
        authService.me(token);
        try {
            return repository.listarChecklistRespuestas(Integer.parseInt(idSupervision.trim()));
        } catch (NumberFormatException | DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    // ---------- Cruce de materiales ----------

    public List<Map<String, Object>> listarMaterialesCruce(String token) {
        authService.me(token);
        try {
            return repository.listarMaterialesCruce();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    /** Cantidades que el sistema tiene registradas en la OT (cruce de materiales, columna Cant/Sistema). */
    public List<Map<String, Object>> listarMaterialesSistemaOt(String ordenTrabajo, String codigoCliente, String tecnico, String token) {
        AuthMeResponse me = authService.me(token);
        if (isBlank(ordenTrabajo) || isBlank(codigoCliente)) {
            return java.util.Collections.emptyList();
        }
        try {
            return repository.obtenerMaterialesOtSistema(resolveSucursalNombre(me), ordenTrabajo.trim(), codigoCliente.trim(),
                    isBlank(tecnico) ? null : tecnico.trim());
        } catch (DataAccessException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CRUCE_ERROR",
                    "No se pudieron obtener los materiales de la OT (spx_ObtenerMaterialesOT_PSupervision).");
        }
    }

    public List<Map<String, Object>> listarCruceMateriales(String idSupervision, String token) {
        authService.me(token);
        try {
            return repository.listarCruceMateriales(Integer.parseInt(idSupervision.trim()));
        } catch (NumberFormatException | DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    /** Cada material enviado debe existir, llevar cantidad fisica y, si difiere del sistema, una observacion. */
    private void validarCruceMateriales(List<MaterialCruceRequest> filas) {
        if (filas == null || filas.isEmpty()) {
            return;
        }
        Map<Integer, String> catalogo = new java.util.HashMap<>();
        try {
            for (Map<String, Object> m : repository.listarMaterialesCruce()) {
                catalogo.put(((Number) m.get("id")).intValue(), String.valueOf(m.get("material")));
            }
        } catch (DataAccessException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CRUCE_ERROR",
                    "No se pudo leer el catalogo de materiales. Ejecutar SQL_V2_TABLAS_CHECKLIST_FIRMAS.sql.");
        }
        for (MaterialCruceRequest f : filas) {
            String nombre = f == null || f.getIdMaterial() == null ? null : catalogo.get(f.getIdMaterial());
            if (nombre == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Material del cruce no valido.");
            }
            if (f.getCantFisico() == null || f.getCantFisico() < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Falta la cantidad fisica de: " + nombre);
            }
            // Material que el sistema no registra en la OT cuenta como 0.
            int sistema = f.getCantSistema() == null ? 0 : f.getCantSistema();
            if (f.getCantFisico() != sistema && isBlank(f.getObservacion())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "La observacion es obligatoria cuando hay diferencia en: " + nombre);
            }
        }
    }

    private void guardarCruceMateriales(String idSupervision, List<MaterialCruceRequest> filas) {
        if (filas == null || filas.isEmpty()) {
            return;
        }
        List<Object[]> datos = new ArrayList<>();
        for (MaterialCruceRequest f : filas) {
            datos.add(new Object[] {f.getIdMaterial(), f.getCantSistema(), f.getCantFisico(), isBlank(f.getObservacion()) ? null : f.getObservacion().trim()});
        }
        repository.guardarCruceMateriales(Integer.parseInt(idSupervision.trim()), datos);
    }

    private static String sinTildes(String valor) {
        return java.text.Normalizer.normalize(valor == null ? "" : valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Valida las respuestas contra el catalogo: el item existe, la respuesta es una opcion valida y,
     * cuando la respuesta es la negativa del item (No / Incorrecto / ...), la observacion es obligatoria.
     */
    private void validarChecklist(List<ChecklistRespuestaRequest> respuestas) {
        if (respuestas == null || respuestas.isEmpty()) {
            return;
        }
        Map<Integer, Map<String, Object>> items = new java.util.HashMap<>();
        try {
            for (Map<String, Object> item : repository.listarChecklistItems()) {
                items.put(((Number) item.get("id")).intValue(), item);
            }
        } catch (DataAccessException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CHECKLIST_ERROR",
                    "No se pudo leer el catalogo del checklist. Ejecutar SQL_V2_TABLAS_CHECKLIST_FIRMAS.sql.");
        }
        for (ChecklistRespuestaRequest r : respuestas) {
            Map<String, Object> item = r == null || r.getIdItem() == null ? null : items.get(r.getIdItem());
            if (item == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Item de checklist no valido.");
            }
            String nombre = String.valueOf(item.get("item"));
            String respuesta = r.getRespuesta() == null ? "" : r.getRespuesta().trim();
            if (respuesta.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Falta la respuesta de: " + nombre);
            }
            String tipo = String.valueOf(item.get("tipoRespuesta"));
            List<String> validas = new ArrayList<>();
            if ("SI_NO".equals(tipo)) {
                validas.add("si");
                validas.add("no");
            } else if ("CORRECTO_INCORRECTO".equals(tipo)) {
                validas.add("correcto");
                validas.add("incorrecto");
            } else if (item.get("opciones") != null) {
                for (String opcion : String.valueOf(item.get("opciones")).split("\\|")) {
                    validas.add(sinTildes(opcion));
                }
            }
            if (!validas.isEmpty() && !validas.contains(sinTildes(respuesta))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Respuesta no valida en: " + nombre);
            }
            Object obligan = item.get("respuestasObliganObs");
            if (obligan != null && isBlank(r.getObservacion())) {
                for (String negativa : String.valueOf(obligan).split("\\|")) {
                    if (!negativa.trim().isEmpty() && sinTildes(negativa).equals(sinTildes(respuesta))) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                                "La observacion es obligatoria cuando la respuesta es \"" + respuesta + "\" en: " + nombre);
                    }
                }
            }
        }
    }

    private void guardarChecklist(String idSupervision, List<ChecklistRespuestaRequest> respuestas) {
        if (respuestas == null || respuestas.isEmpty()) {
            return;
        }
        List<Object[]> filas = new ArrayList<>();
        for (ChecklistRespuestaRequest r : respuestas) {
            filas.add(new Object[] {r.getIdItem(), r.getRespuesta().trim(), isBlank(r.getObservacion()) ? null : r.getObservacion().trim()});
        }
        repository.guardarChecklist(Integer.parseInt(idSupervision.trim()), filas);
    }

    private void validarFotos(String[] fotos) {
        for (int i = 0; i < fotos.length; i++) {
            fotoStorage.validar(fotos[i], FOTO_COLUMNAS[i]);
        }
    }

    /** Guarda las imagenes nuevas como archivo y devuelve las rutas (las que ya son ruta se mantienen). */
    private String[] guardarFotos(String[] fotos, String idSupervision) {
        String[] rutas = new String[fotos.length];
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < fotos.length; i++) {
            rutas[i] = fotoStorage.guardar(fotos[i], FOTO_COLUMNAS[i], idSupervision, hoy);
        }
        return rutas;
    }

    public SupervisionV2FotoStorageService.FotoArchivo obtenerFoto(String ruta, String token) {
        authService.me(token);
        return fotoStorage.cargar(ruta);
    }

    public List<Map<String, Object>> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String sucursalFiltro,
            String token) {
        return listar(fechaDesde, fechaHasta, idSupervisorFiltro, limite, sucursalFiltro, null, token);
    }

    public List<Map<String, Object>> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String sucursalFiltro,
            String tipoFecha,
            String token) {
        validarRangoFechas(fechaDesde, fechaHasta);
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalPermitida(me, sucursalFiltro);
        Integer idSupervisor = resolveSupervisorConsulta(me, sucursal, idSupervisorFiltro);
        String idSupervisorTexto = idSupervisor == null ? null : String.valueOf(idSupervisor);
        // Buscar por fecha de supervision: spx_ListarSupervisionManual solo filtra por fecha de registro.
        List<Map<String, Object>> filas = "supervision".equalsIgnoreCase(tipoFecha == null ? "" : tipoFecha.trim())
                ? repository.listarPorEstado("completado", idSupervisorTexto, fechaDesde, fechaHasta, limite, null, null, "supervision")
                : repository.listar(idSupervisorTexto, fechaDesde, fechaHasta, limite);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : filas) {
            out.add(repository.enriquecerDetalleConNombres(new LinkedHashMap<>(row), sucursal));
        }
        return out;
    }

    public List<Map<String, Object>> listarPendientes(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String sucursalFiltro,
            String token) {
        validarRangoFechas(fechaDesde, fechaHasta);
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalPermitida(me, sucursalFiltro);
        Integer idSupervisor = resolveSupervisorConsulta(me, sucursal, idSupervisorFiltro);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> pendiente : repository.listarPendientesTodos(fechaDesde, fechaHasta, limite)) {
            if (idSupervisor != null) {
                Integer idSupervisorFila = toInteger(findValue(
                        pendiente,
                        "idSupervisor",
                        "id_supervisor",
                        "idUsuarioSupervisor",
                        "id_usuariosupervisor",
                        "supervisor"
                ));
                if (idSupervisorFila == null || !idSupervisorFila.equals(idSupervisor)) {
                    continue;
                }
            }
            out.add(repository.enriquecerDetalleConNombres(new LinkedHashMap<>(pendiente), sucursal));
        }
        out.addAll(enriquecerRevisionesPenalizadas(
                repository.listarRevisionesPenalizadasSupervisor(idSupervisor),
                sucursal
        ));
        return out;
    }

    public List<String> listarMisSucursalesPermitidas(String token) {
        AuthMeResponse me = authService.me(token);
        Integer idUsuarioSesion = resolveIdUsuario(me);
        LinkedHashSet<String> nombres = new LinkedHashSet<>();
        String propia = resolveSucursalNombre(me);
        if (!isBlank(propia)) {
            nombres.add(propia);
        }
        for (Map<String, Object> permiso : repository.listarSucursalesPermitidas(idUsuarioSesion)) {
            String nombre = asText(findValue(permiso, "nombreSucursalPermiso", "nombre_SucursalPermiso"));
            if (!isBlank(nombre)) {
                nombres.add(nombre);
            }
        }
        return new ArrayList<>(nombres);
    }

    private static final String SUPERVISOR_FILTRO_TODOS = "TODOS";

    private Integer resolveSupervisorConsulta(AuthMeResponse me, String sucursal, String idSupervisorFiltro) {
        Integer idUsuarioSesion = resolveIdUsuario(me);
        if (isBlank(idSupervisorFiltro)) {
            return idUsuarioSesion;
        }
        if (SUPERVISOR_FILTRO_TODOS.equalsIgnoreCase(idSupervisorFiltro.trim())) {
            return null;
        }

        Integer idSupervisor = toInteger(idSupervisorFiltro);
        if (idSupervisor == null || idSupervisor <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSupervisor no es valido.");
        }
        if (idSupervisor.equals(idUsuarioSesion)) {
            return idSupervisor;
        }

        for (Map<String, Object> supervisor : repository.listarSupervisores(sucursal)) {
            Integer idPermitido = toInteger(findValue(
                    supervisor,
                    "idSupervisor",
                    "id_supervisor",
                    "idUsuarioSupervisor",
                    "id_usuario_supervisor",
                    "idUsuario",
                    "id_usuario",
                    "Id_Usuario"
            ));
            if (idSupervisor.equals(idPermitido)) {
                return idSupervisor;
            }
        }

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "SUPERVISOR_NOT_ALLOWED",
                "El supervisor seleccionado no pertenece a la sucursal de la sesion."
        );
    }

    public List<Map<String, Object>> listarBackofficePorEstado(
            String estadoSup,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String token) {
        return listarBackofficePorEstado(estadoSup, fechaDesde, fechaHasta, idSupervisorFiltro, limite, null, null, token);
    }

    public List<Map<String, Object>> listarBackofficePorEstado(
            String estadoSup,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String sucursalFiltro,
            String supervisorNombreFiltro,
            String token) {
        return listarBackofficePorEstado(estadoSup, fechaDesde, fechaHasta, idSupervisorFiltro, limite, sucursalFiltro, supervisorNombreFiltro, null, token);
    }

    public List<Map<String, Object>> listarBackofficePorEstado(
            String estadoSup,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String idSupervisorFiltro,
            Integer limite,
            String sucursalFiltro,
            String supervisorNombreFiltro,
            String tipoFecha,
            String token) {
        AuthMeResponse me = authService.me(token);
        validarRangoFechas(fechaDesde, fechaHasta);
        String estado = normalizarEstadoSup(estadoSup);
        String sucursalTabla = isBlank(sucursalFiltro) || SUCURSAL_FILTRO_TODOS.equalsIgnoreCase(sucursalFiltro.trim())
                ? null
                : sucursalFiltro.trim();
        String supervisorNombreTabla = isBlank(supervisorNombreFiltro) || SUCURSAL_FILTRO_TODOS.equalsIgnoreCase(supervisorNombreFiltro.trim())
                ? null
                : supervisorNombreFiltro.trim();
        if ("pendiente".equals(estado)) {
            String sucursal = resolveSucursalNombre(me);
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> pendiente : repository.listarPendientesTodos(fechaDesde, fechaHasta, limite, sucursalTabla, supervisorNombreTabla)) {
                out.add(repository.enriquecerDetalleConNombres(new LinkedHashMap<>(pendiente), sucursal));
            }
            return out;
        }
        String idSupervisor = isBlank(idSupervisorFiltro) || SUPERVISOR_FILTRO_TODOS.equalsIgnoreCase(idSupervisorFiltro.trim())
                ? null
                : idSupervisorFiltro.trim();
        String sucursal = resolveSucursalNombre(me);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> registro : repository.listarPorEstado(estado, idSupervisor, fechaDesde, fechaHasta, limite, sucursalTabla, supervisorNombreTabla, tipoFecha)) {
            out.add(repository.enriquecerDetalleConNombres(new LinkedHashMap<>(registro), sucursal));
        }
        return out;
    }

    public Map<String, Object> obtenerDetalle(String idSupervision, String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        Map<String, Object> detalle = repository.obtenerDetalle(idSupervision, null);
        if (detalle == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "No se encontro la nota de supervision indicada."
            );
        }
        // Llamada de atencion enlazada (si ya se genero): la pantalla decide entre generar, firmar o solo mostrar el estado.
        try {
            Integer idLlamada = idLlamadaDe(repository.obtenerDatosParaLlamada(idSupervision));
            if (idLlamada != null) {
                detalle.put("idLlamadaAtencion", idLlamada);
            }
        } catch (RuntimeException ex) {
            // Sin esa columna o sin acceso, el detalle sigue sin el dato de la llamada.
        }
        // Fotos y firmas se completan desde la tabla: el detalle no depende de la version del procedimiento.
        try {
            for (Map.Entry<String, Object> medio : repository.leerMediosDeSupervision(idSupervision).entrySet()) {
                Object actual = detalle.get(medio.getKey());
                if ((actual == null || String.valueOf(actual).trim().isEmpty()) && medio.getValue() != null) {
                    detalle.put(medio.getKey(), medio.getValue());
                }
            }
        } catch (RuntimeException ex) {
            // Si no se pueden leer, el detalle se devuelve igual, pero se indica el motivo para poder diagnosticarlo.
            Throwable causa = ex instanceof DataAccessException ? ((DataAccessException) ex).getMostSpecificCause() : ex;
            detalle.put("mediosError", causa == null ? ex.toString() : causa.getMessage());
        }
        return repository.enriquecerDetalleConNombres(detalle, sucursal);
    }

    public Map<String, Object> registrar(SupervisionV2CrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        requireSupervisor(me);
        Integer idSupervisor = resolveIdUsuario(me);

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de supervision es requerido."
            );
        }
        validarOrdenYCliente(request.getIdTipoSupervision(), request.getIdTipoTrabajo(), request.getOrdenTrabajo(), request.getCodigo());

        String sucursal = resolveSucursalNombre(me);
        CamposDenormalizados campos = resolverCamposDenormalizados(
                sucursal,
                "MANUAL",
                idSupervisor,
                resolveNombreUsuario(me),
                request.getIdTecnicoPrincipal(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion()
        );

        // Las fotos ya no viajan en base64 a la BD: se insertan sin foto y luego se guardan como archivo
        // (el nombre lleva el id generado) y se actualiza cada columna con su ruta.
        String[] fotosOriginales = leerFotos(request);
        validarFotos(fotosOriginales);
        String[] firmasOriginales = leerFirmas(request);
        validarFirmas(firmasOriginales);
        validarChecklist(request.getChecklist());
        validarCruceMateriales(request.getMaterialesCruce());
        escribirFotos(request, new String[fotosOriginales.length]);

        String idGenerado = repository.registrar(
                idSupervisor,
                request.getIdTecnicoPrincipal(),
                request.getIdTecnicoAuxiliar(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion(),
                request.getSupervisionPor(),
                request.getTecnologia(),
                request.getCodigo(),
                request.getOrdenTrabajo(),
                request.getTipoRevision(),
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion(),
                campos.nombreSupervisor,
                campos.sucursal,
                campos.origen,
                campos.nombreTecnicoPrincipal,
                campos.nombreTipoSupervision,
                campos.nombreTipoTrabajo,
                campos.nombreTipoPenalizacion,
                resolveNombreUsuario(me),
                resolveCargoUsuario(me)
        );

        String[] rutas = guardarFotos(fotosOriginales, idGenerado);
        repository.actualizarFotos(idGenerado, FOTO_COLUMNAS, rutas);
        guardarFirmas(firmasOriginales, idGenerado);
        guardarChecklist(idGenerado, request.getChecklist());
        guardarCruceMateriales(idGenerado, request.getMaterialesCruce());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idGenerado);
        out.put("idUsuarioSesion", idSupervisor);
        return out;
    }

    public Map<String, Object> realizarPendiente(String idSupervision, SupervisionV2CrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        requireSupervisor(me);
        Integer idSupervisor = resolveIdUsuario(me);

        if (isBlank(idSupervision)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "idSupervision es requerido.");
        }
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request de supervision es requerido.");
        }
        validarOrdenYCliente(request.getIdTipoSupervision(), request.getIdTipoTrabajo(), request.getOrdenTrabajo(), request.getCodigo());
        if (isBlank(request.getUbicacion())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ubicacion es requerido.");
        }

        // Mensajes claros antes de tocar nada: la supervision debe existir, seguir pendiente y estar asignada a este supervisor.
        Map<String, Object> estadoActual = repository.obtenerEstadoSupervision(idSupervision);
        if (estadoActual != null) {
            String estado = asText(estadoActual.get("estadoSup")) == null ? "" : asText(estadoActual.get("estadoSup")).trim().toLowerCase();
            Object eliminado = estadoActual.get("eliminado");
            if (Boolean.TRUE.equals(eliminado) || "1".equals(String.valueOf(eliminado))) {
                throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "La supervision fue eliminada.");
            }
            if (!estado.startsWith("pendiente")) {
                throw new ApiException(HttpStatus.CONFLICT, "YA_FINALIZADA", "La supervision ya fue finalizada (estado: " + estado + ").");
            }
            // Cobertura: si esta asignada a otro supervisor, puede finalizarla cualquier supervisor de la MISMA sucursal.
            String asignado = asText(estadoActual.get("idSupervisor")) == null ? "" : asText(estadoActual.get("idSupervisor")).trim();
            if (idSupervisor != null && !asignado.equals(String.valueOf(idSupervisor)) && !supervisorEsDeMiSucursal(asignado, me)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "OTRA_SUCURSAL",
                        "Esta supervision esta asignada a un supervisor de otra sucursal; no puedes finalizarla.");
            }
        }

        // El id ya existe: se guardan las fotos como archivo y la BD recibe solo las rutas.
        String[] fotosOriginales = leerFotos(request);
        validarFotos(fotosOriginales);
        escribirFotos(request, guardarFotos(fotosOriginales, idSupervision));
        String[] firmasOriginales = leerFirmas(request);
        validarFirmas(firmasOriginales);
        validarChecklist(request.getChecklist());
        validarCruceMateriales(request.getMaterialesCruce());
        // Firmas y checklist se guardan ANTES de marcar la supervision como completada: si algo falla aqui,
        // la supervision sigue pendiente y se puede reintentar (los pasos se pueden repetir sin duplicar).
        if (estadoActual != null) {
            guardarFirmas(firmasOriginales, idSupervision);
            guardarChecklist(idSupervision, request.getChecklist());
            guardarCruceMateriales(idSupervision, request.getMaterialesCruce());
        }

        int updated = repository.realizarPendiente(
                idSupervision,
                idSupervisor,
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion(),
                resolveNombreUsuario(me)
        );
        if (updated <= 0 && esRevisionPenalizada(idSupervision)) {
            String sucursalPenalizada = resolveSucursalNombre(me);
            CamposDenormalizados camposPenalizada = resolverCamposDenormalizados(
                    sucursalPenalizada,
                    "REV_PENALIZADA",
                    idSupervisor,
                    resolveNombreUsuario(me),
                    request.getIdTecnicoPrincipal(),
                    request.getIdTipoSupervision(),
                    request.getIdTipoTrabajo(),
                    request.getIdTipoPenalizacion()
            );
            String idGenerado = repository.registrar(
                    idSupervisor,
                    request.getIdTecnicoPrincipal(),
                    request.getIdTecnicoAuxiliar(),
                    request.getIdTipoSupervision(),
                    request.getIdTipoTrabajo(),
                    request.getIdTipoPenalizacion(),
                    request.getSupervisionPor(),
                    request.getTecnologia(),
                    request.getCodigo(),
                    request.getOrdenTrabajo(),
                    request.getTipoRevision(),
                    request.getFotoBoletaSupervision(),
                    request.getFotoCanalesPilos(),
                    request.getFotoNivelesDocsis(),
                    request.getFotoMedicionRuido(),
                    request.getFotoBarridoCanales(),
                    request.getFotoObservacion1(),
                    request.getFotoObservacion2(),
                    request.getFotoObservacion3(),
                    request.getFotoObservacion4(),
                    request.getObservacion(),
                    request.getDescripcionAdicionalObservacion(),
                    request.getUbicacion(),
                    camposPenalizada.nombreSupervisor,
                    camposPenalizada.sucursal,
                    camposPenalizada.origen,
                    camposPenalizada.nombreTecnicoPrincipal,
                    camposPenalizada.nombreTipoSupervision,
                    camposPenalizada.nombreTipoTrabajo,
                    camposPenalizada.nombreTipoPenalizacion,
                    resolveNombreUsuario(me),
                resolveCargoUsuario(me)
            );
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idSupervision", idGenerado);
            out.put("idUsuarioSesion", idSupervisor);
            out.put("estadoSup", "completado");
            out.put("origen", "REV_PENALIZADA");
            return out;
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro supervision pendiente para realizar.");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idSupervision);
        out.put("idUsuarioSesion", idSupervisor);
        out.put("estadoSup", "completado");
        return out;
    }

    public List<Map<String, Object>> listarTiposSupervision(String token) {
        authService.me(token);
        try {
            return repository.listarTiposSupervision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposTrabajo(String token) {
        authService.me(token);
        try {
            return repository.listarTiposTrabajo();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposPenalizacion(String token) {
        authService.me(token);
        try {
            return repository.listarTiposPenalizacion();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposSupervisionPor(String token) {
        authService.me(token);
        try {
            return repository.listarTiposSupervisionPor();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposTecnologia(String token) {
        authService.me(token);
        try {
            return repository.listarTiposTecnologia();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposRevision(String token) {
        authService.me(token);
        try {
            return repository.listarTiposRevision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTecnicosSupervisor(String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        try {
            return repository.listarTecnicosDeGrupos(sucursal);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarIniciosPendientes(String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        Integer idSucursal = resolveIdSucursal(me);
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> row : repository.listarIniciosJornadaPendientesSucursal(idSucursal, sucursal)) {
                if (perteneceSucursalEstricta(row, sucursal, idSucursal)) {
                    out.add(row);
                }
            }
            return out;
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarIniciosConfirmadosHoy(String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        Integer idSucursal = resolveIdSucursal(me);
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> row : repository.listarIniciosJornadaConfirmadosHoyTodos(sucursal)) {
                if (perteneceSucursalEstricta(row, sucursal, idSucursal)) {
                    out.add(row);
                }
            }
            return out;
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarHistoricoJornadasSupervisor(LocalDate fecha, Integer idTecnico, String token) {
        return listarHistoricoJornadasSupervisor(fecha, null, null, idTecnico, token);
    }

    public List<Map<String, Object>> listarHistoricoJornadasSupervisor(LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, Integer idTecnico, String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        LocalDate desde = fechaDesde == null ? (fecha == null ? LocalDate.now() : fecha) : fechaDesde;
        LocalDate hasta = fechaHasta == null ? desde : fechaHasta;
        if (hasta.isBefore(desde)) {
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }
        try {
            return listarHistoricoJornadasRango(desde, hasta, sucursal, null, idTecnico, false);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> listarHistoricoJornadasBackoffice(LocalDate fecha, String sucursal, Integer idTecnico, String token) {
        return listarHistoricoJornadasBackoffice(fecha, null, null, sucursal, idTecnico, token);
    }

    public List<Map<String, Object>> listarHistoricoJornadasBackoffice(LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, String sucursal, Integer idTecnico, String token) {
        AuthMeResponse me = authService.me(token);
        LocalDate desde = fechaDesde == null ? (fecha == null ? LocalDate.now() : fecha) : fechaDesde;
        LocalDate hasta = fechaHasta == null ? desde : fechaHasta;
        if (hasta.isBefore(desde)) {
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }
        String sucursalResuelta = SucursalCanonicalizer.canonicalize(
                isBlank(sucursal) ? null : sucursal
        );
        try {
            return listarHistoricoJornadasRango(desde, hasta, sucursalResuelta, null, idTecnico, false);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> listarHistoricoJornadasRango(
            LocalDate desde,
            LocalDate hasta,
            String sucursal,
            Integer idSupervisor,
            Integer idTecnico,
            boolean limitarSupervisor) {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate cursor = desde;
        while (!cursor.isAfter(hasta)) {
            out.addAll(repository.listarHistoricoJornadas(cursor, sucursal, idSupervisor, idTecnico, limitarSupervisor));
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    public Map<String, Object> obtenerDetalleInicioJornada(Integer idInicio, String token) {
        AuthMeResponse me = authService.me(token);
        String sucursal = resolveSucursalNombre(me);
        Integer idSucursal = resolveIdSucursal(me);
        try {
            Map<String, Object> detalle = repository.obtenerDetalleInicioJornada(idInicio, sucursal);
            if (!perteneceSucursalEstricta(detalle, sucursal, idSucursal)) {
                return new LinkedHashMap<>();
            }
            return detalle;
        } catch (DataAccessException ex) {
            return new LinkedHashMap<>();
        }
    }

    public JornadaImagen obtenerImagenInicioJornada(Integer idInicio, boolean miniatura, String token) {
        authService.me(token);
        Object raw = repository.obtenerImagenInicioJornada(idInicio);
        JornadaImagen imagen = decodeImagen(raw);
        if (imagen == null || imagen.getBytes().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro imagen para el inicio de jornada.");
        }
        if (!miniatura) {
            return imagen;
        }
        JornadaImagen thumb = crearMiniatura(imagen, 96, 96);
        return thumb == null ? imagen : thumb;
    }

    public JornadaImagen obtenerImagenAuxiliarInicioJornada(Integer idInicio, boolean miniatura, String token) {
        authService.me(token);
        Object raw = repository.obtenerImagenAuxiliarInicioJornada(idInicio);
        JornadaImagen imagen = decodeImagen(raw);
        if (imagen == null || imagen.getBytes().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro imagen de auxiliar para el inicio de jornada.");
        }
        if (!miniatura) {
            return imagen;
        }
        JornadaImagen thumb = crearMiniatura(imagen, 96, 96);
        return thumb == null ? imagen : thumb;
    }

    public JornadaImagen obtenerFirmaInicioJornada(Integer idInicio, String token) {
        authService.me(token);
        Object raw = repository.obtenerFirmaInicioJornada(idInicio);
        JornadaImagen imagen = decodeImagen(raw);
        if (imagen == null || imagen.getBytes().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro firma de inicio para la jornada.");
        }
        return imagen;
    }

    public JornadaImagen obtenerFirmaCierreJornada(Integer idInicio, String token) {
        authService.me(token);
        Object raw = repository.obtenerFirmaCierreJornada(idInicio);
        JornadaImagen imagen = decodeImagen(raw);
        if (imagen == null || imagen.getBytes().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro firma de cierre para la jornada.");
        }
        return imagen;
    }

    public Map<String, Object> aprobarInicioPendiente(Integer idInicio, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String nombreSupervisor = me.getUsuario().getNombre();
        int updated = repository.aprobarInicioJornada(idSupervisor, nombreSupervisor, idInicio);
        if (updated <= 0) {
            updated = repository.aprobarInicioJornadaPorId(idInicio, idSupervisor, nombreSupervisor);
        }
        if (updated <= 0) {
            updated = repository.aprobarInicioJornadaDirecto(idInicio, idSupervisor, nombreSupervisor);
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro inicio pendiente para aprobar.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idInicio", idInicio);
        out.put("aprobado", true);
        return out;
    }

    public Map<String, Object> rechazarInicioPendiente(Integer idInicio, String token, String observacionRechazado) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        int updated = repository.rechazarInicioJornada(idSupervisor, idInicio);
        if (updated <= 0) {
            updated = repository.rechazarInicioJornadaPorId(idInicio);
        }
        if (updated <= 0) {
            updated = repository.rechazarInicioJornadaDirecto(idInicio);
        }
        if (updated <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontro inicio pendiente para rechazar.");
        }
        if (observacionRechazado != null && !observacionRechazado.trim().isEmpty()) {
            repository.actualizarObservacionRechazado(idInicio, observacionRechazado.trim());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idInicio", idInicio);
        out.put("rechazado", true);
        return out;
    }

    private Integer resolveIdUsuario(AuthMeResponse me) {
        Integer idUsuario = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;
        if (idUsuario == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo identificar el usuario de la sesion."
            );
        }
        return idUsuario;
    }

    private Integer resolveIdSucursal(AuthMeResponse me) {
        Integer idSucursal = me != null && me.getUsuario() != null ? me.getUsuario().getIdSucursal() : null;
        if (idSucursal == null || idSucursal <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SUCURSAL_REQUIRED",
                    "No se pudo identificar la sucursal de la sesion."
            );
        }
        return idSucursal;
    }

    private JornadaImagen decodeImagen(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof byte[]) {
            byte[] bytes = (byte[]) raw;
            return new JornadaImagen(bytes, detectarContentType(bytes, "image/jpeg"));
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        String contentType = "image/jpeg";
        int comma = text.indexOf(',');
        if (text.startsWith("data:image") && comma > 0) {
            String header = text.substring(0, comma);
            int semicolon = header.indexOf(';');
            if (semicolon > 5) {
                contentType = header.substring(5, semicolon);
            }
            text = text.substring(comma + 1);
        }
        text = text.replaceAll("\\s+", "");
        try {
            byte[] bytes = Base64.getDecoder().decode(text);
            return new JornadaImagen(bytes, detectarContentType(bytes, contentType));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private JornadaImagen crearMiniatura(JornadaImagen imagen, int maxWidth, int maxHeight) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imagen.getBytes()));
            if (source == null) {
                return null;
            }
            int width = source.getWidth();
            int height = source.getHeight();
            if (width <= 0 || height <= 0) {
                return null;
            }
            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            if (scale > 1.0d) {
                scale = 1.0d;
            }
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));
            BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            return new JornadaImagen(out.toByteArray(), "image/jpeg");
        } catch (Exception ex) {
            return null;
        }
    }

    private String detectarContentType(byte[] bytes, String fallback) {
        if (bytes == null || bytes.length < 12) {
            return fallback;
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) {
            return "image/gif";
        }
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return "image/webp";
        }
        return fallback;
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

    private String normalizarEstadoSup(String estadoSup) {
        String estado = estadoSup == null ? "pendiente" : estadoSup.trim().toLowerCase();
        if ("pendientes".equals(estado)) {
            estado = "pendiente";
        } else if ("completados".equals(estado) || "completada".equals(estado) || "completadas".equals(estado)) {
            estado = "completado";
        }
        if (!"pendiente".equals(estado) && !"completado".equals(estado)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "estado debe ser pendiente o completado."
            );
        }
        return estado;
    }

    private String resolveSucursalNombre(AuthMeResponse me) {
        Integer idSucursal = me != null && me.getUsuario() != null ? me.getUsuario().getIdSucursal() : null;
        if (idSucursal == null) return null;
        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    /**
     * Resuelve la sucursal a usar para una consulta: si no se pide una sucursal
     * distinta a la propia, se usa la de la sesion (comportamiento historico).
     * Si se pide una sucursal distinta, solo se permite cuando el usuario tiene
     * un permiso vigente sobre ella en tbl_SupervisionSucursal (ver
     * SupervisionV2Repository.listarSucursalesPermitidas / spx_ListarSupervisionSucursal);
     * de lo contrario se rechaza con 403 en vez de dejar pasar cualquier sucursal.
     */
    private String resolveSucursalPermitida(AuthMeResponse me, String sucursalFiltro) {
        String sucursalPropia = resolveSucursalNombre(me);
        // "Todos" en el filtro equivale a no filtrar por sucursal.
        if (isBlank(sucursalFiltro) || SUCURSAL_FILTRO_TODOS.equalsIgnoreCase(sucursalFiltro.trim())) {
            return sucursalPropia;
        }
        String filtroCanon = SucursalCanonicalizer.canonicalize(sucursalFiltro);
        if (filtroCanon != null && filtroCanon.equalsIgnoreCase(SucursalCanonicalizer.canonicalize(sucursalPropia))) {
            return sucursalPropia;
        }
        Integer idUsuarioSesion = resolveIdUsuario(me);
        for (Map<String, Object> permiso : repository.listarSucursalesPermitidas(idUsuarioSesion)) {
            String nombrePermiso = asText(findValue(permiso, "nombreSucursalPermiso", "nombre_SucursalPermiso"));
            if (!isBlank(nombrePermiso) && SucursalCanonicalizer.canonicalize(nombrePermiso).equalsIgnoreCase(filtroCanon)) {
                return nombrePermiso;
            }
        }
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "SUCURSAL_NOT_ALLOWED",
                "No tienes permiso para consultar la sucursal seleccionada."
        );
    }

    private boolean perteneceSucursal(Map<String, Object> row, String sucursalSesion) {
        if (isBlank(sucursalSesion)) {
            return true;
        }
        String sucursalRow = asText(findValue(row, "sucursal", "Sucursal"));
        if (isBlank(sucursalRow)) {
            return true;
        }
        return normalizeName(sucursalSesion).equals(normalizeName(SucursalCanonicalizer.canonicalize(sucursalRow)));
    }

    private boolean perteneceSucursalEstricta(Map<String, Object> row, String sucursalSesion, Integer idSucursalSesion) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        Integer idSucursalRow = toInteger(findValue(
                row,
                "idSucursal",
                "id_sucursal",
                "Id_Sucursal",
                "IDSucursal",
                "idSucursalInicio",
                "id_sucursal_inicio",
                "idSucursalJornada",
                "id_sucursal_jornada",
                "idSucursalTecnico",
                "id_sucursal_tecnico"
        ));
        if (idSucursalRow != null && idSucursalSesion != null) {
            return idSucursalSesion.equals(idSucursalRow);
        }
        String sucursalRow = asText(findValue(
                row,
                "sucursal",
                "Sucursal",
                "nombreSucursal",
                "NombreSucursal",
                "nombre_sucursal",
                "Nombre_Sucursal",
                "sucursalNombre",
                "SucursalNombre"
        ));
        if (!isBlank(sucursalRow) && !isBlank(sucursalSesion)) {
            return normalizeName(sucursalSesion).equals(normalizeName(SucursalCanonicalizer.canonicalize(sucursalRow)));
        }
        return false;
    }

    private List<Map<String, Object>> listarRevisionesPenalizadasSucursal(String sucursal) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<String, Object>> supervisores = repository.listarSupervisores(sucursal);
        for (Map<String, Object> supervisor : supervisores) {
            Integer idSupervisor = toInteger(findValue(
                    supervisor,
                    "idSupervisor",
                    "id_supervisor",
                    "idUsuarioSupervisor",
                    "id_usuario_supervisor",
                    "idUsuario",
                    "id_usuario",
                    "Id_Usuario"
            ));
            if (idSupervisor == null || idSupervisor <= 0) {
                continue;
            }
            out.addAll(enriquecerRevisionesPenalizadas(
                    repository.listarRevisionesPenalizadasSupervisor(idSupervisor),
                    sucursal
            ));
        }
        return out;
    }

    private List<Map<String, Object>> enriquecerRevisionesPenalizadas(List<Map<String, Object>> revisiones, String sucursal) {
        if (revisiones == null || revisiones.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> tecnicos = repository.listarTecnicosDeGrupos(sucursal);
        Map<String, Map<String, Object>> tecnicoPorNombre = new LinkedHashMap<>();
        for (Map<String, Object> tecnico : tecnicos) {
            String nombre = asText(findValue(tecnico, "tecnico", "nombre", "tecnicoNombre"));
            String key = normalizeName(nombre);
            if (key != null && !tecnicoPorNombre.containsKey(key)) {
                tecnicoPorNombre.put(key, tecnico);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> revision : revisiones) {
            Map<String, Object> item = new LinkedHashMap<>(revision);
            String tecnicoNombre = asText(findValue(item, "tecnicoPrincipal", "tecnicoPrincipalNombre", "tecnico_nombre"));
            Map<String, Object> match = tecnicoPorNombre.get(normalizeName(tecnicoNombre));
            if (match != null) {
                Object idTecnico = findValue(match, "idTecnico", "id_tecnico", "idUsuarioTecnico", "id_usuario_tecnico");
                if (idTecnico != null) {
                    item.put("idTecnicoPrincipal", String.valueOf(idTecnico));
                    item.put("id_tecnico_principal", String.valueOf(idTecnico));
                }
                String nombreMatch = asText(findValue(match, "tecnico", "nombre", "tecnicoNombre"));
                if (!isBlank(nombreMatch)) {
                    item.put("tecnicoPrincipal", nombreMatch);
                    item.put("tecnicoPrincipalNombre", nombreMatch);
                }
            }
            out.add(item);
        }
        return out;
    }

    private boolean esRevisionPenalizada(String idSupervision) {
        return idSupervision != null && idSupervision.trim().startsWith("REV_PENALIZADA:");
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) return null;
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim().replaceAll("[^0-9-]", ""));
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeName(String value) {
        if (isBlank(value)) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    public Map<String, Object> registrarPendiente(SupervisionV2CrearPendienteRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        String creadoPor = resolveNombreUsuario(me);

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de supervision es requerido."
            );
        }
        validarOrdenYCliente(request.getIdTipoSupervision(), request.getIdTipoTrabajo(), request.getOrdenTrabajo(), request.getCodigo());

        String sucursal = resolveSucursalNombre(me);
        CamposDenormalizados campos = resolverCamposDenormalizados(
                sucursal,
                "PENDIENTE",
                request.getIdSupervisorAsignado(),
                null,
                request.getIdTecnicoPrincipal(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion()
        );

        // Fotos como archivo: se inserta sin foto y luego se guardan con el id generado.
        String[] fotosOriginales = new String[] {
                request.getFotoBoletaSupervision(), request.getFotoCanalesPilos(), request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(), request.getFotoBarridoCanales(), request.getFotoObservacion1(),
                request.getFotoObservacion2(), request.getFotoObservacion3(), request.getFotoObservacion4()
        };
        validarFotos(fotosOriginales);
        request.setFotoBoletaSupervision(null);
        request.setFotoCanalesPilos(null);
        request.setFotoNivelesDocsis(null);
        request.setFotoMedicionRuido(null);
        request.setFotoBarridoCanales(null);
        request.setFotoObservacion1(null);
        request.setFotoObservacion2(null);
        request.setFotoObservacion3(null);
        request.setFotoObservacion4(null);

        String idGenerado = repository.registrarPendiente(
                request.getIdSupervisorAsignado(),
                request.getIdTecnicoPrincipal(),
                request.getIdTecnicoAuxiliar(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion(),
                request.getSupervisionPor(),
                request.getTecnologia(),
                request.getCodigo(),
                request.getOrdenTrabajo(),
                request.getTipoRevision(),
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion(),
                creadoPor,
                campos.nombreSupervisor,
                campos.sucursal,
                campos.origen,
                campos.nombreTecnicoPrincipal,
                campos.nombreTipoSupervision,
                campos.nombreTipoTrabajo,
                campos.nombreTipoPenalizacion,
                resolveCargoUsuario(me)
        );

        repository.actualizarFotos(idGenerado, FOTO_COLUMNAS, guardarFotos(fotosOriginales, idGenerado));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idGenerado);
        out.put("idSupervisorAsignado", request.getIdSupervisorAsignado());
        out.put("creadoPor", creadoPor);
        return out;
    }

    private static final String SUCURSAL_FILTRO_TODOS = "TODOS";

    public List<Map<String, Object>> listarSucursalesSupervisionBackoffice(String token) {
        authService.me(token);
        try {
            return repository.listarSucursalesSupervision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarNombresSupervisorSupervisionBackoffice(String sucursalFiltro, String token) {
        return listarNombresSupervisorSupervisionBackoffice(sucursalFiltro, "pendiente", token);
    }

    public List<Map<String, Object>> listarNombresSupervisorSupervisionBackoffice(String sucursalFiltro, String estadoSup, String token) {
        authService.me(token);
        String sucursalTabla = isBlank(sucursalFiltro) || SUCURSAL_FILTRO_TODOS.equalsIgnoreCase(sucursalFiltro.trim())
                ? null
                : sucursalFiltro.trim();
        String estado = normalizarEstadoSup(estadoSup);
        try {
            return repository.listarNombresSupervisorSupervision(sucursalTabla, estado);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarSupervisores(String sucursal, String token) {
        AuthMeResponse me = authService.me(token);
        if (!isBlank(sucursal) && SUCURSAL_FILTRO_TODOS.equalsIgnoreCase(sucursal.trim())) {
            return listarSupervisoresTodasSucursales();
        }
        String sucursalResuelta = SucursalCanonicalizer.canonicalize(
                isBlank(sucursal) ? resolveSucursalNombre(me) : sucursal
        );
        try {
            return repository.listarSupervisores(sucursalResuelta);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private List<Map<String, Object>> listarSupervisoresTodasSucursales() {
        Map<String, Map<String, Object>> porSupervisor = new LinkedHashMap<>();
        for (SucursalResponse item : authService.listarSucursales()) {
            if (item == null || isBlank(item.getSucursal())) {
                continue;
            }
            String canon = SucursalCanonicalizer.canonicalize(item.getSucursal());
            try {
                for (Map<String, Object> supervisor : repository.listarSupervisores(canon)) {
                    Integer idSupervisor = toInteger(findValue(
                            supervisor,
                            "idSupervisor",
                            "id_supervisor",
                            "idUsuarioSupervisor",
                            "id_usuario_supervisor"
                    ));
                    String key = idSupervisor != null ? String.valueOf(idSupervisor) : String.valueOf(supervisor);
                    porSupervisor.putIfAbsent(key, supervisor);
                }
            } catch (DataAccessException ignored) {
                // Sucursal sin conexion o sin datos; se continua con las demas.
            }
        }
        return new ArrayList<>(porSupervisor.values());
    }

    private String resolverNombreSupervisorPorId(Object idSupervisor) {
        if (idSupervisor == null) return null;
        String idNormalizado = String.valueOf(idSupervisor).trim();
        if (idNormalizado.isEmpty()) return null;
        try {
            for (Map<String, Object> item : listarSupervisoresTodasSucursales()) {
                Object idValor = findValue(item, "idSupervisor", "idUsuarioSupervisor", "id_supervisor", "id_usuario_supervisor");
                if (idValor != null && idNormalizado.equalsIgnoreCase(String.valueOf(idValor).trim())) {
                    Object nombre = findValue(item, "nombre", "supervisor", "Nombre", "Supervisor");
                    return nombre == null ? null : asText(String.valueOf(nombre));
                }
            }
        } catch (Exception ignored) {
            // Si falla la resolucion no bloqueamos el registro de la supervision.
        }
        return null;
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal) {
        return listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursal, null);
    }

    public List<Map<String, Object>> listarTecnicosPorSupervisorBackoffice(Integer idSupervisor, String sucursal, String supervisor) {
        try {
            String sucursalResuelta = SucursalCanonicalizer.canonicalize(sucursal);
            return repository.listarTecnicosPorSupervisorBackoffice(idSupervisor, sucursalResuelta, supervisor);
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    /** Tipos de supervision donde la OT y el nro de cliente son opcionales: 5 = ACCIDENTE / AUXILIO A PERSONAL TECNICO. */
    private static final java.util.Set<String> TIPOS_OT_Y_CLIENTE_OPCIONALES = new java.util.HashSet<>(java.util.Arrays.asList("5"));

    private void validarOrdenYCliente(String idTipoSupervision, String idTipoTrabajo, String ordenTrabajo, String codigo) {
        String tipo = idTipoSupervision == null ? "" : idTipoSupervision.trim();
        if (TIPOS_OT_Y_CLIENTE_OPCIONALES.contains(tipo)) {
            return;
        }
        // Tipo de trabajo "NINGUNA": no hay OT ni cliente asociados (tambien en INTERCEPCION DE RUTA (2)).
        if (!isBlank(idTipoTrabajo)) {
            String nombreTrabajo = resolverNombreCatalogo(
                    listarTiposTrabajoSeguro(),
                    idTipoTrabajo,
                    new String[]{"idTipoTrabajo", "Id_TipoTrabajo", "id"},
                    new String[]{"tipoTrabajo", "TipoTrabajo", "Nombre", "nombre"});
            if (nombreTrabajo != null && "NINGUNA".equalsIgnoreCase(nombreTrabajo.trim())) {
                return;
            }
        }
        if (isBlank(ordenTrabajo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ordenTrabajo es requerido para este tipo de supervision.");
        }
        if (isBlank(codigo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "codigo es requerido para este tipo de supervision.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** True si el supervisor asignado pertenece a la sucursal de la sesion (lista de supervisores de esa sucursal). */
    private boolean supervisorEsDeMiSucursal(String idSupervisorAsignado, AuthMeResponse me) {
        Integer idBuscado = toInteger(idSupervisorAsignado);
        if (idBuscado == null) {
            return false;
        }
        try {
            for (Map<String, Object> supervisor : repository.listarSupervisores(resolveSucursalNombre(me))) {
                Integer id = toInteger(findValue(
                        supervisor,
                        "idSupervisor", "id_supervisor", "idUsuarioSupervisor", "id_usuario_supervisor",
                        "idUsuario", "id_usuario", "Id_Usuario"));
                if (idBuscado.equals(id)) {
                    return true;
                }
            }
        } catch (DataAccessException ex) {
            return false;
        }
        return false;
    }

    /** Solo el supervisor finaliza supervisiones (registro directo o realizar una pendiente). */
    private void requireSupervisor(AuthMeResponse me) {
        String rol = me != null && me.getUsuario() != null ? me.getUsuario().getRol() : null;
        String normalizado = rol == null ? "" : java.text.Normalizer.normalize(rol, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (!"supervisor".equals(normalizado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_SUPERVISOR_ONLY",
                    "Solo el supervisor puede finalizar una supervision.");
        }
    }

    /** Cargo de quien registra: el rol de la sesion (BackOffice, Digitador, Supervisor...). */
    private String resolveCargoUsuario(AuthMeResponse me) {
        String rol = me != null && me.getUsuario() != null ? me.getUsuario().getRol() : null;
        return isBlank(rol) ? null : rol.trim();
    }

    private String resolveNombreUsuario(AuthMeResponse me) {
        String nombre = me != null && me.getUsuario() != null ? me.getUsuario().getNombre() : null;
        if (!isBlank(nombre)) return nombre.trim();
        String login = me != null && me.getUsuario() != null ? me.getUsuario().getLoggin() : null;
        if (!isBlank(login)) return login.trim();
        Integer idUsuario = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;
        return idUsuario == null ? null : String.valueOf(idUsuario);
    }

    private static final class CamposDenormalizados {
        final String nombreSupervisor;
        final String sucursal;
        final String origen;
        final String nombreTecnicoPrincipal;
        final String nombreTipoSupervision;
        final String nombreTipoTrabajo;
        final String nombreTipoPenalizacion;

        CamposDenormalizados(
                String nombreSupervisor,
                String sucursal,
                String origen,
                String nombreTecnicoPrincipal,
                String nombreTipoSupervision,
                String nombreTipoTrabajo,
                String nombreTipoPenalizacion) {
            this.nombreSupervisor = nombreSupervisor;
            this.sucursal = sucursal;
            this.origen = origen;
            this.nombreTecnicoPrincipal = nombreTecnicoPrincipal;
            this.nombreTipoSupervision = nombreTipoSupervision;
            this.nombreTipoTrabajo = nombreTipoTrabajo;
            this.nombreTipoPenalizacion = nombreTipoPenalizacion;
        }
    }

    private CamposDenormalizados resolverCamposDenormalizados(
            String sucursal,
            String origen,
            Object idSupervisor,
            String nombreSupervisorConocido,
            String idTecnicoPrincipal,
            String idTipoSupervision,
            String idTipoTrabajo,
            String idTipoPenalizacion) {
        Map<String, Object> temp = new LinkedHashMap<>();
        if (idSupervisor != null) temp.put("idSupervisor", idSupervisor);
        if (!isBlank(idTecnicoPrincipal)) temp.put("idTecnicoPrincipal", idTecnicoPrincipal);
        Map<String, Object> enriquecido = temp.isEmpty() ? temp : repository.enriquecerDetalleConNombres(temp, sucursal);

        String nombreSupervisor = nombreSupervisorConocido;
        if (isBlank(nombreSupervisor)) {
            nombreSupervisor = resolverNombreSupervisorPorId(idSupervisor);
        }
        if (isBlank(nombreSupervisor)) {
            nombreSupervisor = asText(findValue(enriquecido, "supervisorNombre", "supervisor"));
        }
        String nombreTecnicoPrincipal = asText(findValue(enriquecido, "tecnicoPrincipalNombre", "tecnicoNombre"));

        String nombreTipoSupervision = resolverNombreCatalogo(
                listarTiposSupervisionSeguro(),
                idTipoSupervision,
                new String[]{"idTipoSupervision", "Id_TipoSupervision", "id"},
                new String[]{"tipoSupervision", "TipoSupervision", "Nombre", "nombre"});
        String nombreTipoTrabajo = resolverNombreCatalogo(
                listarTiposTrabajoSeguro(),
                idTipoTrabajo,
                new String[]{"idTipoTrabajo", "Id_TipoTrabajo", "id"},
                new String[]{"tipoTrabajo", "TipoTrabajo", "Nombre", "nombre"});
        String nombreTipoPenalizacion = resolverNombreCatalogo(
                listarTiposPenalizacionSeguro(),
                idTipoPenalizacion,
                new String[]{"idTipoPenalizacion", "Id_TipoPenalizacion", "id"},
                new String[]{"tipoPenalizacion", "Nombre", "nombre"});

        return new CamposDenormalizados(
                nombreSupervisor, sucursal, origen, nombreTecnicoPrincipal,
                nombreTipoSupervision, nombreTipoTrabajo, nombreTipoPenalizacion);
    }

    private String resolverNombreCatalogo(
            List<Map<String, Object>> catalogo,
            String idBuscado,
            String[] idKeys,
            String[] nombreKeys) {
        if (isBlank(idBuscado) || catalogo == null) return null;
        String idNormalizado = idBuscado.trim();
        for (Map<String, Object> item : catalogo) {
            Object idValor = findValue(item, idKeys);
            if (idValor != null && idNormalizado.equalsIgnoreCase(String.valueOf(idValor).trim())) {
                Object nombre = findValue(item, nombreKeys);
                return nombre == null ? null : asText(String.valueOf(nombre));
            }
        }
        return null;
    }

    private List<Map<String, Object>> listarTiposSupervisionSeguro() {
        try {
            return repository.listarTiposSupervision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private List<Map<String, Object>> listarTiposTrabajoSeguro() {
        try {
            return repository.listarTiposTrabajo();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private List<Map<String, Object>> listarTiposPenalizacionSeguro() {
        try {
            return repository.listarTiposPenalizacion();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarSupervisionSucursales(String token) {
        requireSistemas(token);
        return repository.listarSupervisionSucursales();
    }

    public Map<String, Object> crearSupervisionSucursal(SupervisionSucursalRequest request, String token) {
        AuthLoginResponse actor = requireSistemas(token);
        if (request == null || request.getIdUsuario() == null || request.getIdSucursalUsuario() == null
                || request.getIdSucursalPermiso() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idUsuario, idSucursalUsuario e idSucursalPermiso son requeridos."
            );
        }
        String nombreUsuario = resolverNombreSupervisorPorId(request.getIdUsuario());
        String nombreSucursalUsuario = resolverNombreSucursalPorId(request.getIdSucursalUsuario());
        String nombreSucursalPermiso = resolverNombreSucursalPorId(request.getIdSucursalPermiso());
        try {
            return repository.crearSupervisionSucursal(
                    request.getIdUsuario(),
                    nombreUsuario,
                    request.getIdSucursalUsuario(),
                    nombreSucursalUsuario,
                    request.getIdSucursalPermiso(),
                    nombreSucursalPermiso,
                    actor == null ? null : actor.getIdUsuario()
            );
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "SUPERVISION_SUCURSAL_DUPLICADA", ex.getMessage());
        }
    }

    public void eliminarSupervisionSucursal(Integer id, String token) {
        requireSistemas(token);
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "id es requerido.");
        }
        try {
            repository.eliminarSupervisionSucursal(id);
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SUPERVISION_SUCURSAL_NOT_FOUND", ex.getMessage());
        }
    }

    private String resolverNombreSucursalPorId(Integer idSucursal) {
        if (idSucursal == null) return null;
        for (SucursalResponse item : authService.listarSucursales()) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return item.getSucursal();
            }
        }
        return null;
    }

    private AuthLoginResponse requireSistemas(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me == null ? null : me.getUsuario();
        if (!authService.esAdministrador(usuario)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_SISTEMAS_ONLY",
                    "Solo el rol sistemas puede administrar sucursales de supervision."
            );
        }
        return usuario;
    }

}
