package com.lalucha.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ItemCheckoutDto {
    @NotNull(message = "El producto es obligatorio.")
    @Positive(message = "El id de producto debe ser positivo.")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria.")
    @Min(value = 1, message = "La cantidad minima es 1.")
    @Max(value = 50, message = "La cantidad maxima por producto es 50.")
    private Integer cantidad;

    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.")
    private Double precioUnitario;

    public ItemCheckoutDto() {
    }

    public ItemCheckoutDto(Long productoId, Integer cantidad, Double precioUnitario) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
