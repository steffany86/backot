package com.example.TigoStarSystem.supervision.dto;

import javax.validation.constraints.NotNull;

public class SupervisionSucursalRequest {
    @NotNull(message = "idUsuario es requerido")
    private Integer idUsuario;

    @NotNull(message = "idSucursalUsuario es requerido")
    private Integer idSucursalUsuario;

    @NotNull(message = "idSucursalPermiso es requerido")
    private Integer idSucursalPermiso;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdSucursalUsuario() {
        return idSucursalUsuario;
    }

    public void setIdSucursalUsuario(Integer idSucursalUsuario) {
        this.idSucursalUsuario = idSucursalUsuario;
    }

    public Integer getIdSucursalPermiso() {
        return idSucursalPermiso;
    }

    public void setIdSucursalPermiso(Integer idSucursalPermiso) {
        this.idSucursalPermiso = idSucursalPermiso;
    }
}
