package com.snkrlab.carrito.service;

import com.snkrlab.carrito.client.ProductoClient;
import com.snkrlab.carrito.dto.CheckoutDTO;
import com.snkrlab.carrito.dto.ProductoDTO;
import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.rabbitmq.RabbitMQConfig;
import com.snkrlab.carrito.rabbitmq.StockReducidoMessage;
import com.snkrlab.carrito.repository.ItemCarritoRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.snkrlab.carrito.dto.DetalleCompraDTO;
import com.snkrlab.carrito.dto.ItemCarritoDTO;
import java.util.ArrayList;
import com.snkrlab.carrito.client.OrdenClient;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CarritoService {

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoClient productoClient;
    private final RabbitTemplate rabbitTemplate;
    private final OrdenClient ordenClient;

    @Value("${ORDENES_INTERNAL_KEY:snkrlab-local-ordenes}")
    private String claveOrdenes;
    public CarritoService(ItemCarritoRepository itemCarritoRepository, ProductoClient productoClient, RabbitTemplate rabbitTemplate, OrdenClient ordenClient) {
        this.itemCarritoRepository = itemCarritoRepository;
        this.productoClient = productoClient;
        this.rabbitTemplate = rabbitTemplate;
        this.ordenClient = ordenClient;
    }

    public List<ItemCarrito> obtenerPorUsuario(String usuarioId) {
        return itemCarritoRepository.findByUsuarioId(usuarioId);
    }
    public List<ItemCarritoDTO> obtenerPorUsuarioConDetalle(String usuarioId) {
        List<ItemCarrito> items = itemCarritoRepository.findByUsuarioId(usuarioId);
        List<ItemCarritoDTO> resultado = new ArrayList<>();

        for (ItemCarrito item : items) {
            String nombre = item.getNombreProducto() != null
                    ? item.getNombreProducto()
                    : "Producto #" + item.getProductoId();
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
            item2.setNombreProducto(producto.getNombre());

            return itemCarritoRepository.save(item2);
        }

        item.setNombreProducto(producto.getNombre());
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
        List<ItemCarrito> items =
                itemCarritoRepository.findByUsuarioId(usuarioId);

        if (items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El carrito está vacío."
            );
        }

        List<DetalleCompraDTO> detalle = new ArrayList<>();
        int total = 0;

        for (ItemCarrito item : items) {
            if (item.getCantidad() == null
                    || item.getCantidad() <= 0
                    || item.getPrecioUnitario() == null
                    || item.getPrecioUnitario() < 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El carrito contiene cantidades o precios inválidos."
                );
            }

            int subtotal;

            try {
                subtotal = Math.multiplyExact(
                        item.getPrecioUnitario(),
                        item.getCantidad()
                );

                total = Math.addExact(total, subtotal);
            } catch (ArithmeticException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El total supera el rango permitido."
                );
            }

            String nombre = item.getNombreProducto() != null
                    ? item.getNombreProducto()
                    : "Producto #" + item.getProductoId();

            detalle.add(new DetalleCompraDTO(
                    item.getProductoId(),
                    nombre,
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    subtotal
            ));
        }

        // Primero se guarda la orden con los datos del carrito.
        Long ordenId;

        try {
            ordenId = ordenClient.registrarOrden(claveOrdenes, detalle);
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo confirmar el registro de la orden. "
                            + "El carrito se conserva.",
                    e
            );
        }

        if (ordenId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Órdenes no devolvió el identificador."
            );
        }

        // El descuento se sigue publicando desde Carrito,
        // en este mismo checkout, como lo tienes actualmente.
        for (ItemCarrito item : items) {
            StockReducidoMessage mensaje = new StockReducidoMessage(
                    item.getProductoId(),
                    item.getCantidad()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    mensaje
            );
        }

        itemCarritoRepository.deleteByUsuarioId(usuarioId);

        return new CheckoutDTO(
                "Orden registrada. Solicitud de descuento de stock enviada.",
                items.size(),
                total,
                detalle,
                ordenId
        );
    }

}