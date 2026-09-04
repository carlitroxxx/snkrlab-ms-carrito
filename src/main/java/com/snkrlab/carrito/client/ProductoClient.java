package com.snkrlab.carrito.client;

import com.snkrlab.carrito.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-productos", url = "${APPLICATION_CONFIG_PRODUCTOS_URL:http://localhost:8082}")
public interface ProductoClient {

    @GetMapping("/api/v1/productos/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Long id);
}