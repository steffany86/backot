package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;

public class CorteTapEstadoRequest {
    @NotBlank(message = "estado es requerido")
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
