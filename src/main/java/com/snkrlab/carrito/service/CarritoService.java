package com.snkrlab.carrito.service;

import com.snkrlab.carrito.client.ProductoClient;
import com.snkrlab.carrito.dto.ProductoDTO;
import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.repository.ItemCarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoClient productoClient;

    public CarritoService(ItemCarritoRepository itemCarritoRepository, ProductoClient productoClient) {
        this.itemCarritoRepository = itemCarritoRepository;
        this.productoClient = productoClient;
    }

    public List<ItemCarrito> obtenerPorUsuario(String usuarioId) {
        return itemCarritoRepository.findByUsuarioId(usuarioId);
    }

    public ItemCarrito agregarItem(ItemCarrito item) {
        ProductoDTO producto = productoClient.obtenerProductoPorId(item.getProductoId());
        if (producto == null) {
            throw new IllegalArgumentException("El producto no existe en el catálogo.");
        }
        item.setPrecioUnitario(producto.getPrecio());
        return itemCarritoRepository.save(item);
    }

    public Optional<ItemCarrito> actualizarItem(String usuarioId, Long id, ItemCarrito itemDetalles) {
        return itemCarritoRepository.findById(id)
                .filter(itemExistente -> itemExistente.getUsuarioId().equals(usuarioId))
                .map(itemExistente -> {
                    itemExistente.setCantidad(itemDetalles.getCantidad());
                    return itemCarritoRepository.save(itemExistente);
                });
    }

    public void eliminarItem(String usuarioId, Long id) {
        itemCarritoRepository.findById(id)
                .filter(item -> item.getUsuarioId().equals(usuarioId))
                .ifPresent(itemCarritoRepository::delete);
    }

    @Transactional
    public void vaciarCarrito(String usuarioId) {
        itemCarritoRepository.deleteByUsuarioId(usuarioId);
    }
}