package com.example.TigoStarSystem.cortetap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CorteTapCrearRequest {
    @NotBlank(message = "codigoCliente es requerido")
    private String codigoCliente;

    @NotBlank(message = "tor es requerido")
    private String tor;

    @NotNull(message = "idTecnico es requerido")
    private Integer idTecnico;

    @NotBlank(message = "tecnico es requerido")
    private String tecnico;

    @NotBlank(message = "sucursal es requerida")
    private String sucursal;

    @NotBlank(message = "nodoTapBoca es requerido")
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

