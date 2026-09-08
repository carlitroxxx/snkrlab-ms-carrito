package com.snkrlab.carrito.service;

import com.snkrlab.carrito.client.ProductoClient;
import com.snkrlab.carrito.dto.CheckoutDTO;
import com.snkrlab.carrito.dto.ProductoDTO;
import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.repository.ItemCarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.snkrlab.carrito.dto.DetalleCompraDTO;
import com.snkrlab.carrito.dto.ItemCarritoDTO;
import java.util.ArrayList;


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
    public List<ItemCarritoDTO> obtenerPorUsuarioConDetalle(String usuarioId) {
        List<ItemCarrito> items = itemCarritoRepository.findByUsuarioId(usuarioId);
        List<ItemCarritoDTO> resultado = new ArrayList<>();

        for (ItemCarrito item : items) {
            ProductoDTO producto = productoClient.obtenerProductoPorId(item.getProductoId());
            String nombre = producto != null ? producto.getNombre() : "Producto no disponible";
            int subtotal = item.getPrecioUnitario() * item.getCantidad();

            resultado.add(new ItemCarritoDTO(
                    item.getId(),
                    item.getProductoId(),
                    nombre,
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    subtotal
            ));
        }

        return resultado;
    }
    public ItemCarrito agregarItem(ItemCarrito item) {
        ProductoDTO producto = productoClient.obtenerProductoPorId(item.getProductoId());
        if (producto == null) {
            throw new IllegalArgumentException("El producto no existe en el catálogo.");
        }

        Optional<ItemCarrito> itemExistente = itemCarritoRepository.findByUsuarioIdAndProductoId(
                item.getUsuarioId(), item.getProductoId());

        if (itemExistente.isPresent()) {
            ItemCarrito item2 = itemExistente.get();
            item2.setCantidad(item2.getCantidad() + item.getCantidad());
            return itemCarritoRepository.save(item2);
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

    @Transactional
    public CheckoutDTO confirmarCompra(String usuarioId) {
        List<ItemCarrito> items = itemCarritoRepository.findByUsuarioId(usuarioId);

        if (items.isEmpty()) {
            throw new IllegalStateException("El carrito esta vacio, no hay nada que confirmar.");
        }

        List<DetalleCompraDTO> detalle = new ArrayList<>();

        for (ItemCarrito item : items) {
            ProductoDTO producto = productoClient.obtenerProductoPorId(item.getProductoId());
            if (producto == null) {
                throw new IllegalArgumentException("El producto " + item.getProductoId() + " ya no existe en el catalogo.");
            }
            if (producto.getStock() < item.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + producto.getStock() + ", en el carrito: " + item.getCantidad());
            }
            int subtotal = item.getPrecioUnitario() * item.getCantidad();
            detalle.add(new DetalleCompraDTO(
                    item.getProductoId(),
                    producto.getNombre(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    subtotal
            ));
        }

        for (ItemCarrito item : items) {
            productoClient.reducirStock(item.getProductoId(), item.getCantidad());
        }

        int total = items.stream()
                .mapToInt(i -> i.getPrecioUnitario() * i.getCantidad())
                .sum();
        int cantidadItems = items.size();

        itemCarritoRepository.deleteByUsuarioId(usuarioId);

        return new CheckoutDTO("Compra confirmada (simulada, sin pago real).", cantidadItems, total, detalle);
    }

}