package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;

public class CorteTapEjecucionRequest {
    @NotBlank(message = "ordenTrabajo es requerida")
    private String ordenTrabajo;

    @NotBlank(message = "observacion es requerida")
    private String observacion;

    private String foto1;
    private String foto2;

    @NotBlank(message = "fechaEjecucion es requerida")
    private String fechaEjecucion;

    public String getOrdenTrabajo() {
        return ordenTrabajo;
    }

    public void setOrdenTrabajo(String ordenTrabajo) {
        this.ordenTrabajo = ordenTrabajo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getFoto1() {
        return foto1;
    }

    public void setFoto1(String foto1) {
        this.foto1 = foto1;
    }

    public String getFoto2() {
        return foto2;
    }

    public void setFoto2(String foto2) {
        this.foto2 = foto2;
    }

    public String getFechaEjecucion() {
        return fechaEjecucion;
    }

    public void setFechaEjecucion(String fechaEjecucion) {
        this.fechaEjecucion = fechaEjecucion;
    }
}

