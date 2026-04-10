package com.example.TigoStarSystem.auth.dto;

public class AuthLoginResponse {
    private final Integer idUsuario;
    private final String nombre;
    private final String rol;
    private final Integer idRol;
    private final Integer idSucursal;

    public AuthLoginResponse(Integer idUsuario, String nombre, String rol, Integer idRol, Integer idSucursal) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.rol = rol;
        this.idRol = idRol;
        this.idSucursal = idSucursal;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public Integer getIdSucursal() {
        return idSucursal;
    }
}
