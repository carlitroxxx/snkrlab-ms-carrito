package com.snkrlab.carrito.client;

import com.snkrlab.carrito.config.FeignClientConfig;
import com.snkrlab.carrito.dto.DetalleCompraDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "ms-ordenes",
        url = "${APPLICATION_CONFIG_ORDENES_URL:http://localhost:8085}",
        configuration = FeignClientConfig.class
)
public interface OrdenClient {

    @PostMapping("/api/v1/ordenes")
    Long registrarOrden(
            @RequestHeader("X-Internal-Key") String clave,
            @RequestBody List<DetalleCompraDTO> items
    );
}