package com.example.TigoStarSystem.auth.dto;

public class SucursalResponse {
    private final Integer idSucursal;
    private final String sucursal;
    private final String ip;
    private final String baseDeDatos;
    private final String tecnologia;

    public SucursalResponse(Integer idSucursal, String sucursal, String ip, String baseDeDatos) {
        this(idSucursal, sucursal, ip, baseDeDatos, null);
    }

    public SucursalResponse(Integer idSucursal, String sucursal, String ip, String baseDeDatos, String tecnologia) {
        this.idSucursal = idSucursal;
        this.sucursal = sucursal;
        this.ip = ip;
        this.baseDeDatos = baseDeDatos;
        this.tecnologia = tecnologia;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public Integer getIdSucursal() {
        return idSucursal;
    }

    public String getSucursal() {
        return sucursal;
    }

    public String getIp() {
        return ip;
    }

    public String getBaseDeDatos() {
        return baseDeDatos;
    }
}
