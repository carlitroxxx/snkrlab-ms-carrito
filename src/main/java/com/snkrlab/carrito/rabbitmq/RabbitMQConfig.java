package com.snkrlab.carrito.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "carrito.exchange";
    public static final String ROUTING_KEY = "stock.reducir";
    public static final String COLA_STOCK = "productos.stock.queue";

    @Bean
    public DirectExchange carritoExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue stockQueue() {
        return new Queue(COLA_STOCK, true);
    }

    @Bean
    public Binding binding(Queue stockQueue, DirectExchange carritoExchange) {
        return BindingBuilder.bind(stockQueue).to(carritoExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}