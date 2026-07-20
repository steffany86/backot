package com.example.TigoStarSystem.backoffice.nodozona.dto;

public class NodoDistritoCrearRequest {
    private String nodosAsociados;
    private String distrito;
    private String zona;
    private String distritoNuevo;

    public String getNodosAsociados() {
        return nodosAsociados;
    }

    public void setNodosAsociados(String nodosAsociados) {
        this.nodosAsociados = nodosAsociados;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getDistritoNuevo() {
        return distritoNuevo;
    }

    public void setDistritoNuevo(String distritoNuevo) {
        this.distritoNuevo = distritoNuevo;
    }
}
