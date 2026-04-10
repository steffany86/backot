package com.example.TigoStarSystem.ot.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class OtRegistrarRealizadaConCargoRequest {
    @NotBlank(message = "numeroOrden es requerido")
    private String numeroOrden;

    @NotNull(message = "idEstado es requerido")
    private Integer idEstado;

    @NotBlank(message = "observacion es requerida")
    private String observacion;

    @Valid
    private List<OtCargoUsuarioItemRequest> items;

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public Integer getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Integer idEstado) {
        this.idEstado = idEstado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<OtCargoUsuarioItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OtCargoUsuarioItemRequest> items) {
        this.items = items;
    }
}
