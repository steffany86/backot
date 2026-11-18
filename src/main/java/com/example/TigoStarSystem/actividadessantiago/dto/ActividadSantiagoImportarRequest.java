package com.example.TigoStarSystem.actividadessantiago.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class ActividadSantiagoImportarRequest {
    @NotNull
    @Size(max = 2000)
    @Valid
    private List<ActividadSantiagoRegistroRequest> registros;

    public List<ActividadSantiagoRegistroRequest> getRegistros() { return registros; }
    public void setRegistros(List<ActividadSantiagoRegistroRequest> registros) { this.registros = registros; }
}
