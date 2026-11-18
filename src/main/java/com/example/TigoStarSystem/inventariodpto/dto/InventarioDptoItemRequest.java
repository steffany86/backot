package com.example.TigoStarSystem.inventariodpto.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class InventarioDptoItemRequest {
    @NotNull
    private Integer item;

    @NotBlank
    @Size(max = 30)
    private String estado;

    @Size(max = 1000)
    private String observacion;

    /** yyyy-MM-dd. Solo se usa cuando el estado es "A cambiar". */
    @Pattern(regexp = "^(\\d{4}-\\d{2}-\\d{2})?$", message = "fechaCambio debe ser yyyy-MM-dd")
    private String fechaCambio;

    @Size(max = 500)
    private String motivoCambio;

    public Integer getItem() { return item; }
    public void setItem(Integer item) { this.item = item; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(String fechaCambio) { this.fechaCambio = fechaCambio; }
    public String getMotivoCambio() { return motivoCambio; }
    public void setMotivoCambio(String motivoCambio) { this.motivoCambio = motivoCambio; }
}
