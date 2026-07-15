package com.example.TigoStarSystem.ot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class OtRealizadaRequest {
    @NotBlank(message = "observacion es requerida")
    private String observacion;

    @NotNull(message = "idEstado es requerido")
    private Integer idEstado;

    @NotBlank(message = "numeroOrden es requerido")
    private String numeroOrden;

    @JsonAlias({"inicioAgendado", "inicio_agendado", "Fecha_Agenda", "fecha_agenda"})
    private String fechaAgenda;

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Integer getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Integer idEstado) {
        this.idEstado = idEstado;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getFechaAgenda() {
        return fechaAgenda;
    }

    public void setFechaAgenda(String fechaAgenda) {
        this.fechaAgenda = fechaAgenda;
    }
}

