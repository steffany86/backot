package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;

public class CorteTapDigitacionRequest {
    @NotBlank(message = "nodoTapBocaAntiguo es requerido")
    private String nodoTapBocaAntiguo;

    @NotBlank(message = "zonaHfc es requerida")
    private String zonaHfc;

    public String getNodoTapBocaAntiguo() {
        return nodoTapBocaAntiguo;
    }

    public void setNodoTapBocaAntiguo(String nodoTapBocaAntiguo) {
        this.nodoTapBocaAntiguo = nodoTapBocaAntiguo;
    }

    public String getZonaHfc() {
        return zonaHfc;
    }

    public void setZonaHfc(String zonaHfc) {
        this.zonaHfc = zonaHfc;
    }

}

