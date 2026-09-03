package com.snkrlab.carrito.service;

import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.repository.ItemCarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {

    private final ItemCarritoRepository itemCarritoRepository;

    public CarritoService(ItemCarritoRepository itemCarritoRepository) {
        this.itemCarritoRepository = itemCarritoRepository;
    }

    public List<ItemCarrito> obtenerPorUsuario(Long usuarioId) {
        return itemCarritoRepository.findByUsuarioId(usuarioId);
    }

    public ItemCarrito agregarItem(ItemCarrito item) {
        return itemCarritoRepository.save(item);
    }

    public Optional<ItemCarrito> actualizarItem(Long id, ItemCarrito itemDetalles) {
        return itemCarritoRepository.findById(id)
                .map(itemExistente -> {
                    itemExistente.setCantidad(itemDetalles.getCantidad());
                    itemExistente.setPrecioUnitario(itemDetalles.getPrecioUnitario());
                    return itemCarritoRepository.save(itemExistente);
                });
    }

    public void eliminarItem(Long id) {
        itemCarritoRepository.deleteById(id);
    }

    @Transactional
    public void vaciarCarrito(Long usuarioId) {
        itemCarritoRepository.deleteByUsuarioId(usuarioId);
    }
}