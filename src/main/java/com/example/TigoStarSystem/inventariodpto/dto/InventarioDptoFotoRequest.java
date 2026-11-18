package com.example.TigoStarSystem.inventariodpto.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class InventarioDptoFotoRequest {
    @NotNull
    private Integer item;

    /** Imagen en data URL (data:image/jpeg;base64,...). */
    @NotBlank
    @Size(max = 8000000)
    private String imagen;

    public Integer getItem() { return item; }
    public void setItem(Integer item) { this.item = item; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
