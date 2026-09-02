package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CorteTapCrearRequest {
    @NotBlank(message = "codigoCliente es requerido")
    private String codigoCliente;

    private String tor;

    @NotNull(message = "idTecnico es requerido")
    private Integer idTecnico;

    @NotBlank(message = "tecnico es requerido")
    private String tecnico;

    private String nodoTapBoca;

    private String nodoTapBocaAntiguo;

    private String zonaHfc;

    @NotBlank(message = "estado es requerido")
    private String estado;

    private String observacion;

    private boolean creadoDesdeOt;

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getTor() {
        return tor;
    }

    public void setTor(String tor) {
        this.tor = tor;
    }

    public Integer getIdTecnico() {
        return idTecnico;
    }

    public void setIdTecnico(Integer idTecnico) {
        this.idTecnico = idTecnico;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getNodoTapBoca() {
        return nodoTapBoca;
    }

    public void setNodoTapBoca(String nodoTapBoca) {
        this.nodoTapBoca = nodoTapBoca;
    }

    public String getNodoTapBocaAntiguo() {
        return nodoTapBocaAntiguo;
    }

    public void setNodoTapBocaAntiguo(String nodoTapBocaAntiguo) {
        this.nodoTapBocaAntiguo = nodoTapBocaAntiguo;
    }

    public String getZonaHfc() {
        return zonaHfc;
    }

    public void setZonaHfc(String zonaHfc) {
        this.zonaHfc = zonaHfc;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public boolean isCreadoDesdeOt() {
        return creadoDesdeOt;
    }

    public void setCreadoDesdeOt(boolean creadoDesdeOt) {
        this.creadoDesdeOt = creadoDesdeOt;
    }
}
