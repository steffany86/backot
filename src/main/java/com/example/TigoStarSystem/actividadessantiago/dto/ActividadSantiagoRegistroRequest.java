package com.example.TigoStarSystem.actividadessantiago.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ActividadSantiagoRegistroRequest {
    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "fecha debe ser yyyy-MM-dd")
    private String fecha;

    @NotBlank
    @Pattern(regexp = "^(w\\d{1,3}-)?\\d-\\d{1,2}$", message = "id de actividad no valido")
    private String id;

    @Min(0)
    @Max(3)
    private Integer nivel;

    @Min(0)
    @Max(1440)
    private Integer minutos;

    @Size(max = 4000)
    private String observacion;

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }
    public Integer getMinutos() { return minutos; }
    public void setMinutos(Integer minutos) { this.minutos = minutos; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
