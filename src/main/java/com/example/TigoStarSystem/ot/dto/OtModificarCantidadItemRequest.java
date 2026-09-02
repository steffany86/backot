package com.example.TigoStarSystem.ot.dto;

import java.math.BigDecimal;

public class OtModificarCantidadItemRequest {
    private Long idCodigoVenta;
    private BigDecimal cantidad;

    public Long getIdCodigoVenta() {
        return idCodigoVenta;
    }

    public void setIdCodigoVenta(Long idCodigoVenta) {
        this.idCodigoVenta = idCodigoVenta;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}
