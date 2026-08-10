package com.example.TigoStarSystem.cortestap.dto;

public class CorteTapCrearRequest {
    private String codigoCliente;
    private String tor;
    private Integer idTecnico;
    private String tecnico;
    private String sucursal;
    private String nodoTapBoca;

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

    public String getSucursal() {
        return sucursal;
    }

    public void setSucursal(String sucursal) {
        this.sucursal = sucursal;
    }

    public String getNodoTapBoca() {
        return nodoTapBoca;
    }

    public void setNodoTapBoca(String nodoTapBoca) {
        this.nodoTapBoca = nodoTapBoca;
    }
}
