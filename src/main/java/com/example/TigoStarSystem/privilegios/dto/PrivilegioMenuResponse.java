package com.example.TigoStarSystem.privilegios.dto;

public class PrivilegioMenuResponse {
    private final Integer idMenu;
    private final String nombre;
    private final String nombreMostrar;
    private final Integer nivel;
    private final Integer padre;
    private final boolean asignado;

    public PrivilegioMenuResponse(
            Integer idMenu,
            String nombre,
            String nombreMostrar,
            Integer nivel,
            Integer padre,
            boolean asignado) {
        this.idMenu = idMenu;
        this.nombre = nombre;
        this.nombreMostrar = nombreMostrar;
        this.nivel = nivel;
        this.padre = padre;
        this.asignado = asignado;
    }

    public Integer getIdMenu() {
        return idMenu;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreMostrar() {
        return nombreMostrar;
    }

    public Integer getNivel() {
        return nivel;
    }

    public Integer getPadre() {
        return padre;
    }

    public boolean isAsignado() {
        return asignado;
    }
}
