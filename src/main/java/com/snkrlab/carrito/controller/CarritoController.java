package com.snkrlab.carrito.controller;

import java.util.List;

import com.snkrlab.carrito.dto.CheckoutDTO;
import com.snkrlab.carrito.dto.ItemCarritoDTO;
import com.snkrlab.carrito.model.ItemCarrito;
import com.snkrlab.carrito.service.CarritoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    @Value("${app.seguridad.cognito-issuer}")
    private String cognitoIssuer;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    private String usuarioIdDesde(Jwt jwt) {
        String emisor = jwt.getClaimAsString("iss");
        boolean esCognito = cognitoIssuer.equals(emisor);

        String identificador = esCognito
                ? jwt.getSubject()
                : jwt.getClaimAsString("oid");

        if (identificador == null || identificador.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El token no contiene el identificador del usuario."
            );
        }

        return esCognito
                ? "cognito:" + identificador
                : identificador;
    }

    @GetMapping
    public ResponseEntity<List<ItemCarritoDTO>> obtenerMiCarrito(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                carritoService.obtenerPorUsuarioConDetalle(
                        usuarioIdDesde(jwt)
                )
        );
    }

    @PostMapping
    public ResponseEntity<ItemCarrito> agregarItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ItemCarrito item) {

        item.setUsuarioId(usuarioIdDesde(jwt));

        ItemCarrito nuevoItem = carritoService.agregarItem(item);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nuevoItem);
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<ItemCarrito> actualizarItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody ItemCarrito item) {

        return carritoService.actualizarItem(
                        usuarioIdDesde(jwt),
                        id,
                        item
                )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<Void> eliminarItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        carritoService.eliminarItem(usuarioIdDesde(jwt), id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito(
            @AuthenticationPrincipal Jwt jwt) {

        carritoService.vaciarCarrito(usuarioIdDesde(jwt));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutDTO> checkout(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                carritoService.confirmarCompra(usuarioIdDesde(jwt))
        );
    }
}