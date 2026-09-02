package com.example.TigoStarSystem.pedidotecnico.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class PedidoTecnicoCrearRequest {
    private String observacion;

    @Valid
    @NotEmpty(message = "Debe agregar al menos un material.")
    private List<PedidoTecnicoItemRequest> items;

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<PedidoTecnicoItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PedidoTecnicoItemRequest> items) {
        this.items = items;
    }
}
