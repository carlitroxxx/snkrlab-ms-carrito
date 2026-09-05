package com.snkrlab.carrito.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items_carrito")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String usuarioId;
    @Column(nullable = false)
    private Long productoId;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false)
    private Integer precioUnitario;
}