package com.example.TigoStarSystem.configuraciones.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ConfiguracionUpdateRequest {
    @NotNull
    @Size(max = 500)
    private String valor;

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
