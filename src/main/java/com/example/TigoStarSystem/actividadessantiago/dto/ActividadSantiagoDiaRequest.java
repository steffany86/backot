package com.example.TigoStarSystem.actividadessantiago.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ActividadSantiagoDiaRequest {
    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "fecha debe ser yyyy-MM-dd")
    private String fecha;

    @Size(max = 50)
    private String animo;

    @Size(max = 4000)
    private String observacion;

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getAnimo() { return animo; }
    public void setAnimo(String animo) { this.animo = animo; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
