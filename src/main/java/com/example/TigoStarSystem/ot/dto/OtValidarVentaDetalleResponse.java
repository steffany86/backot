package com.example.TigoStarSystem.ot.dto;

import java.time.LocalDate;

public class OtValidarVentaDetalleResponse {
    private final LocalDate fecha;
    private final Integer nroOT;
    private final Integer numeroCliente;
    private final Boolean existeVenta;
    private final Integer cantidadVentas;
    private final Boolean tieneDetalleEnCodigoVenta;
    private final Integer cantidadDetalles;

    public OtValidarVentaDetalleResponse(
            LocalDate fecha,
            Integer nroOT,
            Integer numeroCliente,
            Boolean existeVenta,
            Integer cantidadVentas,
            Boolean tieneDetalleEnCodigoVenta,
            Integer cantidadDetalles) {
        this.fecha = fecha;
        this.nroOT = nroOT;
        this.numeroCliente = numeroCliente;
        this.existeVenta = existeVenta;
        this.cantidadVentas = cantidadVentas;
        this.tieneDetalleEnCodigoVenta = tieneDetalleEnCodigoVenta;
        this.cantidadDetalles = cantidadDetalles;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Integer getNroOT() {
        return nroOT;
    }

    public Integer getNumeroCliente() {
        return numeroCliente;
    }

    public Boolean getExisteVenta() {
        return existeVenta;
    }

    public Integer getCantidadVentas() {
        return cantidadVentas;
    }

    public Boolean getTieneDetalleEnCodigoVenta() {
        return tieneDetalleEnCodigoVenta;
    }

    public Integer getCantidadDetalles() {
        return cantidadDetalles;
    }
}
