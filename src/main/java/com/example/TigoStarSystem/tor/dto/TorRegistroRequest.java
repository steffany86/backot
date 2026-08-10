package com.example.TigoStarSystem.tor.dto;

import javax.validation.constraints.NotBlank;

public class TorRegistroRequest {
    @NotBlank(message = "Detalle es requerido.")
    private String detalle;

    @NotBlank(message = "TOR es requerido.")
    private String tor;

    @NotBlank(message = "Tipo de servicio es requerido.")
    private String tipoServicio;

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getTor() {
        return tor;
    }

    public void setTor(String tor) {
        this.tor = tor;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }
}
