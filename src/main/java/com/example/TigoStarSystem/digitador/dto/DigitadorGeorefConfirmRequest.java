package com.example.TigoStarSystem.digitador.dto;

public class DigitadorGeorefConfirmRequest {
    private Boolean confirmarUbicacion;
    private Boolean confirmarNodo;

    public Boolean getConfirmarUbicacion() {
        return confirmarUbicacion;
    }

    public void setConfirmarUbicacion(Boolean confirmarUbicacion) {
        this.confirmarUbicacion = confirmarUbicacion;
    }

    public Boolean getConfirmarNodo() {
        return confirmarNodo;
    }

    public void setConfirmarNodo(Boolean confirmarNodo) {
        this.confirmarNodo = confirmarNodo;
    }
}
