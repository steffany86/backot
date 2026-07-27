package com.example.TigoStarSystem.digitador.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class DigitadorCruceNoFinalizadoRequest {
    @JsonAlias({"fechaEjecuacionDigitacion", "fecha_Ejecuacion_DIGITACION", "fechaEjecucionDigitacion"})
    private String fechaEjecuacionDigitacion;

    @JsonAlias({"estadoDigitacion", "Estado_DIGITACION"})
    private String estadoDigitacion;

    @JsonAlias({"observacionDigitacion", "Observacion_DIGITACION"})
    private String observacionDigitacion;

    public String getFechaEjecuacionDigitacion() {
        return fechaEjecuacionDigitacion;
    }

    public void setFechaEjecuacionDigitacion(String fechaEjecuacionDigitacion) {
        this.fechaEjecuacionDigitacion = fechaEjecuacionDigitacion;
    }

    public String getEstadoDigitacion() {
        return estadoDigitacion;
    }

    public void setEstadoDigitacion(String estadoDigitacion) {
        this.estadoDigitacion = estadoDigitacion;
    }

    public String getObservacionDigitacion() {
        return observacionDigitacion;
    }

    public void setObservacionDigitacion(String observacionDigitacion) {
        this.observacionDigitacion = observacionDigitacion;
    }
}
