package com.example.TigoStarSystem.ot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CuadreAutomaticoJobService {
    private final CuadreService cuadreService;
    private final CuadreAutomaticoMailService mailService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public CuadreAutomaticoJobService(CuadreService cuadreService, CuadreAutomaticoMailService mailService) {
        this.cuadreService = cuadreService;
        this.mailService = mailService;
    }

    public Map<String, Object> iniciar(String token, LocalDate fecha, Integer idSucursal) {
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId);
        jobs.put(jobId, state);

        publish(state, event("progress", "pending", "Inicio", "Proceso en cola para ejecucion.", null));
        executor.submit(() -> ejecutarJob(state, token, fecha, idSucursal));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jobId", jobId);
        out.put("estado", "INICIADO");
        out.put("fecha", fecha == null ? LocalDate.now().toString() : fecha.toString());
        out.put("idSucursal", idSucursal);
        return out;
    }

    public SseEmitter stream(String jobId) {
        JobState state = jobs.get(jobId);
        SseEmitter emitter = new SseEmitter(0L);
        if (state == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data(event(
                        "error",
                        "error",
                        "Job no encontrado",
                        "No se encontro el proceso de cuadre automatico solicitado.",
                        null
                )));
            } catch (IOException ignored) {
                // No hay cliente al cual notificar.
            }
            emitter.complete();
            return emitter;
        }

        synchronized (state) {
            state.emitters.add(emitter);
            for (Map<String, Object> event : state.events) {
                send(emitter, event);
            }
            if (state.done) {
                emitter.complete();
                state.emitters.remove(emitter);
            }
        }

        emitter.onCompletion(() -> removeEmitter(state, emitter));
        emitter.onTimeout(() -> removeEmitter(state, emitter));
        emitter.onError(error -> removeEmitter(state, emitter));
        return emitter;
    }

    private void ejecutarJob(JobState state, String token, LocalDate fecha, Integer idSucursal) {
        try {
            Map<String, Object> result = cuadreService.ejecutarCuadreAutomaticoSistemas(
                    token,
                    fecha,
                    idSucursal,
                    event -> publish(state, event)
            );
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("resultado", result);
            publish(state, event("complete", "success", "Proceso finalizado", "Cuadre automatico finalizado.", extra));
            mailService.enviarResultado(result);
            complete(state);
        } catch (Exception ex) {
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("error", ex.getMessage());
            publish(state, event("error", "error", "Proceso detenido", ex.getMessage(), extra));
            complete(state);
        }
    }

    private void publish(JobState state, Map<String, Object> event) {
        synchronized (state) {
            state.events.add(event);
            List<SseEmitter> failed = new ArrayList<>();
            for (SseEmitter emitter : state.emitters) {
                if (!send(emitter, event)) {
                    failed.add(emitter);
                }
            }
            state.emitters.removeAll(failed);
        }
    }

    private boolean send(SseEmitter emitter, Map<String, Object> event) {
        try {
            String type = String.valueOf(event.getOrDefault("type", "progress"));
            emitter.send(SseEmitter.event().name(type).data(event));
            return true;
        } catch (IOException | IllegalStateException ex) {
            return false;
        }
    }

    private void complete(JobState state) {
        synchronized (state) {
            state.done = true;
            for (SseEmitter emitter : new ArrayList<>(state.emitters)) {
                emitter.complete();
            }
            state.emitters.clear();
        }
    }

    private void removeEmitter(JobState state, SseEmitter emitter) {
        synchronized (state) {
            state.emitters.remove(emitter);
        }
    }

    public static Map<String, Object> event(
            String type,
            String status,
            String step,
            String message,
            Map<String, Object> extra
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("status", status);
        event.put("step", step);
        event.put("message", message == null ? "" : message);
        event.put("timestamp", OffsetDateTime.now().toString());
        if (extra != null && !extra.isEmpty()) {
            event.putAll(extra);
        }
        return event;
    }

    private static final class JobState {
        private final String jobId;
        private final List<Map<String, Object>> events = new ArrayList<>();
        private final List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());
        private boolean done;

        private JobState(String jobId) {
            this.jobId = jobId;
        }
    }
}
