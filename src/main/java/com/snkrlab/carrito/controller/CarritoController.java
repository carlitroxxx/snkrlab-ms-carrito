package com.snkrlab.carrito.controller;

import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<ItemCarrito>> obtenerPorUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(carritoService.obtenerPorUsuario(usuarioId));
    }

    @PostMapping
    public ResponseEntity<ItemCarrito> agregarItem(@RequestBody ItemCarrito item) {
        ItemCarrito nuevoItem = carritoService.agregarItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoItem);
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<ItemCarrito> actualizarItem(@PathVariable Long id, @RequestBody ItemCarrito item) {
        return carritoService.actualizarItem(id, item)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<Void> eliminarItem(@PathVariable Long id) {
        carritoService.eliminarItem(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuario/{usuarioId}")
    public ResponseEntity<Void> vaciarCarrito(@PathVariable String usuarioId) {
        carritoService.vaciarCarrito(usuarioId);
        return ResponseEntity.noContent().build();
    }
}