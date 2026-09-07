package com.snkrlab.carrito.client;

import com.snkrlab.carrito.config.FeignClientConfig;
import com.snkrlab.carrito.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "ms-productos",
        url = "${APPLICATION_CONFIG_PRODUCTOS_URL:http://localhost:8082}",
        configuration = FeignClientConfig.class
)
public interface ProductoClient {

    @GetMapping("/api/v1/productos/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Long id);

    @PatchMapping("/api/v1/productos/{id}/stock")
    ProductoDTO reducirStock(@PathVariable("id") Long id, @RequestParam("cantidad") Integer cantidad);
}