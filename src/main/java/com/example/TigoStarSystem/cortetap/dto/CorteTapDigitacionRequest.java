package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;

public class CorteTapDigitacionRequest {
    @NotBlank(message = "zonaHfc es requerida")
    private String zonaHfc;

    public String getZonaHfc() {
        return zonaHfc;
    }

    public void setZonaHfc(String zonaHfc) {
        this.zonaHfc = zonaHfc;
    }

}
