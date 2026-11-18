package com.example.TigoStarSystem.supervisionv2.controller;

import com.example.TigoStarSystem.common.ApiResponse;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2CrearRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2LlamadaAtencionRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2LlamadaFirmaRequest;
import com.example.TigoStarSystem.supervisionv2.dto.SupervisionV2CrearPendienteRequest;
import com.example.TigoStarSystem.supervisionv2.service.SupervisionV2Service;
import com.example.TigoStarSystem.supervisionv2.service.SupervisionV2FotoStorageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/supervisor/supervision_v2")
public class SupervisionV2Controller {
    private final SupervisionV2Service service;

    public SupervisionV2Controller(SupervisionV2Service service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "idSupervisor", required = false) String idSupervisor,
            @RequestParam(value = "limite", required = false) Integer limite,
            @RequestParam(value = "sucursal", required = false) String sucursal,
            @RequestParam(value = "tipoFecha", required = false) String tipoFecha) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listar(fechaDesde, fechaHasta, idSupervisor, limite, sucursal, tipoFecha, token),
                "Listado de notas de supervision."
        ));
    }

    @GetMapping("/mis-sucursales")
    public ResponseEntity<ApiResponse<List<String>>> listarMisSucursales(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarMisSucursalesPermitidas(token),
                "Sucursales que el usuario puede consultar."
        ));
    }

    @GetMapping("/agenda")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarAgenda(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "idSupervisor", required = false) String idSupervisor,
            @RequestParam(value = "limite", required = false) Integer limite,
            @RequestParam(value = "sucursal", required = false) String sucursal) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarPendientes(fechaDesde, fechaHasta, idSupervisor, limite, sucursal, token),
                "Listado de supervisiones pendientes (agenda)."
        ));
    }

    @GetMapping("/{idSupervision}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerDetalle(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision) {
        return ResponseEntity.ok(ApiResponse.of(
                service.obtenerDetalle(idSupervision, token),
                "Detalle de supervision."
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrar(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody SupervisionV2CrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.registrar(request, token),
                "Nota de supervision registrada."
        ));
    }

    @PostMapping("/{idSupervision}/realizar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> realizarPendiente(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision,
            @Valid @RequestBody SupervisionV2CrearRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.realizarPendiente(idSupervision, request, token),
                "Supervision pendiente realizada."
        ));
    }

    @GetMapping("/catalogos/tipos-supervision")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposSupervision(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposSupervision(token),
                "Listado de tipos de supervision."
        ));
    }

    @GetMapping("/catalogos/tipos-trabajo")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposTrabajo(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposTrabajo(token),
                "Listado de tipos de trabajo."
        ));
    }

    @GetMapping("/catalogos/tipos-penalizacion")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposPenalizacion(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposPenalizacion(token),
                "Listado de tipos de penalizacion."
        ));
    }

    /** BackOffice / Digitador: crea una supervision PENDIENTE asignada a un supervisor (queda con su nombre en creado_por). */
    @PostMapping("/pendiente")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarPendiente(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody SupervisionV2CrearPendienteRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.registrarPendiente(request, token),
                "Supervision pendiente registrada."
        ));
    }

    /**
     * BackOffice / Digitador / Sistemas: lista las supervisiones de TODOS los supervisores por estado
     * (pendiente o completado), con filtros opcionales de sucursal y supervisor.
     */
    @GetMapping("/backoffice/listado")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarBackofficePorEstado(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "estado", required = false, defaultValue = "pendiente") String estado,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "idSupervisor", required = false) String idSupervisor,
            @RequestParam(value = "limite", required = false) Integer limite,
            @RequestParam(value = "sucursal", required = false) String sucursal,
            @RequestParam(value = "supervisorNombre", required = false) String supervisorNombre,
            @RequestParam(value = "tipoFecha", required = false) String tipoFecha) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarBackofficePorEstado(estado, fechaDesde, fechaHasta, idSupervisor, limite, sucursal, supervisorNombre, tipoFecha, token),
                "Listado de supervisiones por estado."
        ));
    }

    /** Genera la llamada de atencion (estado Pendiente) al tecnico principal de una supervision completada. Solo el supervisor. */
    @PostMapping("/{idSupervision}/llamada-atencion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crearLlamadaAtencion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision,
            @RequestBody SupervisionV2LlamadaAtencionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.crearLlamadaAtencion(idSupervision, request, token),
                "Llamada de atencion generada (pendiente de firma)."
        ));
    }

    @GetMapping("/{idSupervision}/llamada-atencion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resumenLlamadaAtencion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision) {
        return ResponseEntity.ok(ApiResponse.of(
                service.resumenLlamadaAtencion(idSupervision, token),
                "Llamada de atencion de la supervision."
        ));
    }

    /** El supervisor hace firmar al tecnico: guarda las firmas y completa la llamada. */
    @PostMapping("/{idSupervision}/llamada-atencion/firmar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> firmarLlamadaAtencion(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision,
            @RequestBody SupervisionV2LlamadaFirmaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                service.firmarLlamadaAtencion(idSupervision, request, token),
                "Llamada de atencion firmada."
        ));
    }

    /** Catalogo de items del checklist (EPP, vehiculo, herramientas, revision). */
    @GetMapping("/checklist/items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarChecklistItems(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.listarChecklistItems(token), "Items del checklist."));
    }

    /** Respuestas del checklist de una supervision. */
    @GetMapping("/{idSupervision}/checklist")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarChecklistRespuestas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision) {
        return ResponseEntity.ok(ApiResponse.of(service.listarChecklistRespuestas(idSupervision, token), "Respuestas del checklist."));
    }

    /** Catalogo de materiales del cruce (supervision de campo, revision Interna / Interna-Externa). */
    @GetMapping("/materiales-cruce")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarMaterialesCruce(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(service.listarMaterialesCruce(token), "Materiales del cruce."));
    }

    /** Materiales de la OT segun el sistema (Cant/Sistema del cruce). */
    @GetMapping("/materiales-cruce/sistema")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarMaterialesSistemaOt(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "ordenTrabajo", required = false) String ordenTrabajo,
            @RequestParam(value = "codigoCliente", required = false) String codigoCliente,
            @RequestParam(value = "tecnico", required = false) String tecnico) {
        return ResponseEntity.ok(ApiResponse.of(service.listarMaterialesSistemaOt(ordenTrabajo, codigoCliente, tecnico, token), "Materiales de la OT."));
    }

    /** Cruce de materiales guardado de una supervision. */
    @GetMapping("/{idSupervision}/materiales-cruce")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarCruceMateriales(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idSupervision") String idSupervision) {
        return ResponseEntity.ok(ApiResponse.of(service.listarCruceMateriales(idSupervision, token), "Cruce de materiales."));
    }

    @GetMapping("/fotos")
    public ResponseEntity<byte[]> obtenerFoto(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam("ruta") String ruta) {
        SupervisionV2FotoStorageService.FotoArchivo foto = service.obtenerFoto(ruta, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(foto.getBytes());
    }

    @GetMapping("/catalogos/supervision-por")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposSupervisionPor(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposSupervisionPor(token),
                "Listado de opciones de supervision por."
        ));
    }

    @GetMapping("/catalogos/tecnologias")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposTecnologia(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposTecnologia(token),
                "Listado de tecnologias."
        ));
    }

    @GetMapping("/catalogos/tipos-revision")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTiposRevision(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTiposRevision(token),
                "Listado de tipos de revision."
        ));
    }

    @GetMapping("/catalogos/tecnicos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarTecnicosSupervisor(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarTecnicosSupervisor(token),
                "Listado de tecnicos asociados al supervisor."
        ));
    }

    @GetMapping("/jornadas/pendientes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarJornadasPendientes(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarIniciosPendientes(token),
                "Listado de inicios de jornada pendientes de aprobacion."
        ));
    }

    @GetMapping("/jornadas/confirmadas-hoy")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarJornadasConfirmadasHoy(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarIniciosConfirmadosHoy(token),
                "Listado de inicios de jornada confirmados hoy."
        ));
    }

    @GetMapping("/jornadas/historico")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarHistoricoJornadas(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(value = "idTecnico", required = false) Integer idTecnico) {
        return ResponseEntity.ok(ApiResponse.of(
                service.listarHistoricoJornadasSupervisor(fecha, fechaDesde, fechaHasta, idTecnico, token),
                "Historico de inicios y cierres de jornada."
        ));
    }

    @GetMapping("/jornadas/{idInicio}/detalle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerDetalleJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio) {
        return ResponseEntity.ok(ApiResponse.of(
                service.obtenerDetalleInicioJornada(idInicio, token),
                "Detalle de inicio de jornada."
        ));
    }

    @GetMapping("/jornadas/{idInicio}/imagen")
    public ResponseEntity<byte[]> obtenerImagenJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio,
            @RequestParam(value = "miniatura", required = false, defaultValue = "true") boolean miniatura) {
        SupervisionV2Service.JornadaImagen imagen = service.obtenerImagenInicioJornada(idInicio, miniatura, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(imagen.getBytes());
    }

    @GetMapping("/jornadas/{idInicio}/imagen-auxiliar")
    public ResponseEntity<byte[]> obtenerImagenAuxiliarJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio,
            @RequestParam(value = "miniatura", required = false, defaultValue = "true") boolean miniatura) {
        SupervisionV2Service.JornadaImagen imagen = service.obtenerImagenAuxiliarInicioJornada(idInicio, miniatura, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(imagen.getBytes());
    }

    @GetMapping("/jornadas/{idInicio}/firma-inicio")
    public ResponseEntity<byte[]> obtenerFirmaInicioJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio) {
        SupervisionV2Service.JornadaImagen imagen = service.obtenerFirmaInicioJornada(idInicio, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(imagen.getBytes());
    }

    @GetMapping("/jornadas/{idInicio}/firma-cierre")
    public ResponseEntity<byte[]> obtenerFirmaCierreJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio) {
        SupervisionV2Service.JornadaImagen imagen = service.obtenerFirmaCierreJornada(idInicio, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(imagen.getBytes());
    }

    @PostMapping("/jornadas/{idInicio}/aprobar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aprobarJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio) {
        return ResponseEntity.ok(ApiResponse.of(
                service.aprobarInicioPendiente(idInicio, token),
                "Inicio de jornada aprobado."
        ));
    }

    @PostMapping("/jornadas/{idInicio}/rechazar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rechazarJornada(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @PathVariable("idInicio") Integer idInicio,
            @RequestBody(required = false) Map<String, Object> body) {
        Object observacionRechazado = body == null ? null : body.get("observacionRechazado");
        return ResponseEntity.ok(ApiResponse.of(
                service.rechazarInicioPendiente(idInicio, token, observacionRechazado == null ? null : String.valueOf(observacionRechazado)),
                "Inicio de jornada rechazado."
        ));
    }
}
