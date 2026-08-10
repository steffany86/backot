package com.example.TigoStarSystem.backoffice.nodozona.dto;

public class NodoZonaCrearRequest {
    private String nodosAsociados;
    private String distrito;
    private String zona;

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
}
