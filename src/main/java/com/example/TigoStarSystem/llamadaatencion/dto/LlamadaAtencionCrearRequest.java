package com.example.TigoStarSystem.llamadaatencion.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class LlamadaAtencionCrearRequest {
    @NotBlank(message = "idTecnico es requerido")
    private String idTecnico;

    @NotBlank(message = "idTipoComunicacion es requerido")
    private String idTipoComunicacion;

    @NotBlank(message = "motivo es requerido")
    @Size(max = 500, message = "motivo no puede exceder 500 caracteres")
    private String motivo;

    @Size(max = 500, message = "descripcion no puede exceder 500 caracteres")
    private String descripcion;

    @Size(max = 500, message = "comentarioColaborador no puede exceder 500 caracteres")
    private String comentarioColaborador;

    @Size(max = 500, message = "acuerdos no puede exceder 500 caracteres")
    private String acuerdos;

    private LocalDateTime fechaSeguimiento;

    private String firmaTecnico;

    private String firmaTestigo;

    public String getIdTecnico() {
        return idTecnico;
    }

    public void setIdTecnico(String idTecnico) {
        this.idTecnico = idTecnico;
    }

    public String getIdTipoComunicacion() {
        return idTipoComunicacion;
    }

    public void setIdTipoComunicacion(String idTipoComunicacion) {
        this.idTipoComunicacion = idTipoComunicacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getComentarioColaborador() {
        return comentarioColaborador;
    }

    public void setComentarioColaborador(String comentarioColaborador) {
        this.comentarioColaborador = comentarioColaborador;
    }

    public String getAcuerdos() {
        return acuerdos;
    }

    public void setAcuerdos(String acuerdos) {
        this.acuerdos = acuerdos;
    }

    public LocalDateTime getFechaSeguimiento() {
        return fechaSeguimiento;
    }

    public void setFechaSeguimiento(LocalDateTime fechaSeguimiento) {
        this.fechaSeguimiento = fechaSeguimiento;
    }

    public String getFirmaTecnico() {
        return firmaTecnico;
    }

    public void setFirmaTecnico(String firmaTecnico) {
        this.firmaTecnico = firmaTecnico;
    }

    public String getFirmaTestigo() {
        return firmaTestigo;
    }

    public void setFirmaTestigo(String firmaTestigo) {
        this.firmaTestigo = firmaTestigo;
    }
}
