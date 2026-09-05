package com.snkrlab.carrito.repository;

import com.snkrlab.carrito.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    List<ItemCarrito> findByUsuarioId(String usuarioId);
    Optional<ItemCarrito> findByUsuarioIdAndProductoId(String usuarioId, Long productoId);
    void deleteByUsuarioId(String usuarioId);
}