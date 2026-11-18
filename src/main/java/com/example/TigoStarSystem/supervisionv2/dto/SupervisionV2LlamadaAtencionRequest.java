package com.example.TigoStarSystem.supervisionv2.dto;

import java.time.LocalDateTime;

/** Datos para generar la llamada de atencion al tecnico principal de una supervision completada. */
public class SupervisionV2LlamadaAtencionRequest {
    private String idTipoComunicacion;
    private String motivo;
    private String descripcion;
    private String comentarioColaborador;
    private String acuerdos;
    private LocalDateTime fechaSeguimiento;

    public String getIdTipoComunicacion() { return idTipoComunicacion; }
    public void setIdTipoComunicacion(String idTipoComunicacion) { this.idTipoComunicacion = idTipoComunicacion; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getComentarioColaborador() { return comentarioColaborador; }
    public void setComentarioColaborador(String comentarioColaborador) { this.comentarioColaborador = comentarioColaborador; }
    public String getAcuerdos() { return acuerdos; }
    public void setAcuerdos(String acuerdos) { this.acuerdos = acuerdos; }
    public LocalDateTime getFechaSeguimiento() { return fechaSeguimiento; }
    public void setFechaSeguimiento(LocalDateTime fechaSeguimiento) { this.fechaSeguimiento = fechaSeguimiento; }
}
