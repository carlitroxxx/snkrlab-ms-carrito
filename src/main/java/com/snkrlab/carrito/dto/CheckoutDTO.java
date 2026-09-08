package com.snkrlab.carrito.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDTO {
    private String mensaje;
    private int cantidadItems;
    private int total;
    private List<DetalleCompraDTO> items;
}