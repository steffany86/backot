package com.example.TigoStarSystem.supervisor.dto;

public class ConformacionCuadrillaEstadoRequest {
    private String estado;
    private Boolean eliminado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getEliminado() {
        return eliminado;
    }

    public void setEliminado(Boolean eliminado) {
        this.eliminado = eliminado;
    }
}
