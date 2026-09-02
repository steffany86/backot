package com.example.TigoStarSystem.ot.dto;

import java.util.List;

public class OtModificarCantidadesRequest {
    private List<OtModificarCantidadItemRequest> materiales;

    public List<OtModificarCantidadItemRequest> getMateriales() {
        return materiales;
    }

    public void setMateriales(List<OtModificarCantidadItemRequest> materiales) {
        this.materiales = materiales;
    }
}
