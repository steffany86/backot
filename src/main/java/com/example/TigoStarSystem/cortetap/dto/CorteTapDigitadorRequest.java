package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;

public class CorteTapDigitadorRequest {
    @NotBlank(message = "nodoTapBocaAntiguo es requerido")
    private String nodoTapBocaAntiguo;
    @NotBlank(message = "zonaHfc es requerida")
    private String zonaHfc;
    @NotBlank(message = "estado es requerido")
    private String estado;
    private String observacion;

    public String getNodoTapBocaAntiguo() { return nodoTapBocaAntiguo; }
    public void setNodoTapBocaAntiguo(String value) { this.nodoTapBocaAntiguo = value; }
    public String getZonaHfc() { return zonaHfc; }
    public void setZonaHfc(String value) { this.zonaHfc = value; }
    public String getEstado() { return estado; }
    public void setEstado(String value) { this.estado = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { this.observacion = value; }
}
