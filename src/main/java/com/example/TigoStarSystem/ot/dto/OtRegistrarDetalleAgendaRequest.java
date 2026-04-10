package com.example.TigoStarSystem.ot.dto;

import java.util.List;

public class OtRegistrarDetalleAgendaRequest {
    private String numeroOrden;
    private Integer idEstado;
    private String observacion;
    private List<OtDetalleMaterialRequest> materiales;

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

    public List<OtDetalleMaterialRequest> getMateriales() {
        return materiales;
    }

    public void setMateriales(List<OtDetalleMaterialRequest> materiales) {
        this.materiales = materiales;
    }
}
