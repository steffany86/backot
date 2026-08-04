package com.example.TigoStarSystem.ot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CuadreAutomaticoMailService {
    private static final Logger logger = LoggerFactory.getLogger(CuadreAutomaticoMailService.class);
    private static final DateTimeFormatter SUBJECT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String destinatario;
    private final String remitente;

    public CuadreAutomaticoMailService(
            JavaMailSender mailSender,
            @Value("${app.cuadre-automatico.mail.enabled:true}") boolean enabled,
            @Value("${app.cuadre-automatico.mail.to:erojas@makiro.com.bo}") String destinatario,
            @Value("${app.cuadre-automatico.mail.from:${spring.mail.username:}}") String remitente
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.destinatario = destinatario;
        this.remitente = remitente;
    }

    public void enviarResultado(Map<String, Object> resultado) {
        if (!enabled) {
            return;
        }
        if (isBlank(destinatario)) {
            logger.warn("No se envio correo de cuadre automatico porque no hay destinatario configurado.");
            return;
        }
        if (isBlank(remitente)) {
            logger.warn("No se envio correo de cuadre automatico porque no hay remitente configurado.");
            return;
        }
        try {
            LocalDate fecha = parseFecha(resultado == null ? null : resultado.get("fecha"));
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente, "Cierre y cuadre automatico");
            helper.setTo(destinatario);
            helper.setSubject("Resultado Cuadre Automatico " + fecha.format(SUBJECT_DATE));
            helper.setText(crearCuerpo(resultado, fecha), true);
            mailSender.send(message);
            logger.info("Correo de resultado de cuadre automatico enviado a: {}", destinatario);
        } catch (Exception ex) {
            logger.error("No se pudo enviar correo de resultado de cuadre automatico.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String crearCuerpo(Map<String, Object> resultado, LocalDate fecha) {
        List<Map<String, Object>> rutas = resultado == null
                ? Collections.emptyList()
                : (List<Map<String, Object>>) resultado.getOrDefault("rutas", Collections.emptyList());
        List<Map<String, Object>> cierres = resultado == null
                ? Collections.emptyList()
                : (List<Map<String, Object>>) resultado.getOrDefault("cierres", Collections.emptyList());

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("<!DOCTYPE html><html><head><style>");
        cuerpo.append("table{font-family:arial,sans-serif;border-collapse:collapse;width:100%;}");
        cuerpo.append("td,th{border:1px solid #dddddd;text-align:left;padding:8px;}");
        cuerpo.append("tr:nth-child(even){background-color:#FDECC9;}");
        cuerpo.append("h2,p{font-family:arial,sans-serif;}");
        cuerpo.append("</style></head><body>");
        cuerpo.append("<h2>Resultado Cuadre Automatico</h2>");
        cuerpo.append("<p>Fecha de ejecucion: ").append(escape(fecha.format(SUBJECT_DATE))).append("</p>");
        cuerpo.append("<table>");
        cuerpo.append("<tr><th>Ruta</th><th>Grupo</th><th>Tecnico</th><th>Estado</th><th>Id Cuadre</th><th>Observacion</th></tr>");
        for (Map<String, Object> ruta : rutas) {
            cuerpo.append("<tr>");
            td(cuerpo, ruta.get("idRuta"));
            td(cuerpo, ruta.get("ruta"));
            td(cuerpo, ruta.get("vendedor"));
            td(cuerpo, ruta.get("estado"));
            td(cuerpo, ruta.get("idCuadre"));
            td(cuerpo, ruta.get("mensaje"));
            cuerpo.append("</tr>");
        }
        cuerpo.append("</table>");
        if (!cierres.isEmpty()) {
            cuerpo.append("<h2>Resultado Cierre Automatico</h2>");
            cuerpo.append("<table>");
            cuerpo.append("<tr><th>Tipo</th><th>Fecha</th><th>Estado</th><th>Id Registro</th><th>Observacion</th></tr>");
            for (Map<String, Object> cierre : cierres) {
                cuerpo.append("<tr>");
                td(cuerpo, cierre.get("tipo"));
                td(cuerpo, cierre.get("fecha"));
                td(cuerpo, cierre.get("estado"));
                td(cuerpo, cierre.get("idRegistro"));
                td(cuerpo, cierre.get("mensaje"));
                cuerpo.append("</tr>");
            }
            cuerpo.append("</table>");
        }
        cuerpo.append("<br />Saludos.<br />Tigo Hogar");
        cuerpo.append("</body></html>");
        return cuerpo.toString();
    }

    private LocalDate parseFecha(Object value) {
        if (value == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private void td(StringBuilder out, Object value) {
        out.append("<td>").append(escape(value)).append("</td>");
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
