package com.example.TigoStarSystem.pedidotecnico.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PedidoTecnicoItemRequest {
    private Integer idProducto;

    @NotBlank(message = "material es requerido")
    private String material;

    @NotNull(message = "cantidad es requerida")
    @DecimalMin(value = "0.01", message = "cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}
