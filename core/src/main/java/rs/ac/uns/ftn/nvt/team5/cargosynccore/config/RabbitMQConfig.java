package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "warehouse-events";
    public static final String HEARTBEAT_QUEUE = "warehouse-heartbeat";
    public static final String TEMPERATURE_QUEUE = "warehouse-temperature";
    public static final String HEARTBEAT_KEY = "warehouse.heartbeat";
    public static final String TEMPERATURE_KEY = "warehouse.temperature";

    @Bean
    TopicExchange warehouseEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue heartbeatQueue() {
        return new Queue(HEARTBEAT_QUEUE, true);
    }

    @Bean
    Queue temperatureQueue() {
        return new Queue(TEMPERATURE_QUEUE, true);
    }

    @Bean
    Binding heartbeatBinding(Queue heartbeatQueue, TopicExchange warehouseEventsExchange) {
        return BindingBuilder.bind(heartbeatQueue).to(warehouseEventsExchange).with(HEARTBEAT_KEY);
    }

    @Bean
    Binding temperatureBinding(Queue temperatureQueue, TopicExchange warehouseEventsExchange) {
        return BindingBuilder.bind(temperatureQueue).to(warehouseEventsExchange).with(TEMPERATURE_KEY);
    }
}
