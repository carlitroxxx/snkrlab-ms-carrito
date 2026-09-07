package com.snkrlab.carrito.client;

import com.snkrlab.carrito.config.FeignClientConfig;
import com.snkrlab.carrito.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "ms-productos",
        url = "${APPLICATION_CONFIG_PRODUCTOS_URL:http://localhost:8082}",
        configuration = FeignClientConfig.class
)
public interface ProductoClient {

    @GetMapping("/desarrollo/api/v1/productos/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Long id);

    @PutMapping("/desarrollo/api/v1/productos/{id}/stock")
    ProductoDTO reducirStock(@PathVariable("id") Long id, @RequestParam("cantidad") Integer cantidad);
}