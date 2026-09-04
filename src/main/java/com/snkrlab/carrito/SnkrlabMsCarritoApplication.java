package com.snkrlab.carrito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SnkrlabMsCarritoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnkrlabMsCarritoApplication.class, args);
	}

}
