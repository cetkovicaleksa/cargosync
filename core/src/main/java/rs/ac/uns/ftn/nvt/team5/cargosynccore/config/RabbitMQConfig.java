package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Warehouse ────────────────────────────────────────────────────────────
    public static final String WAREHOUSE_EXCHANGE       = "warehouse-events";
    public static final String WAREHOUSE_HEARTBEAT_QUEUE   = "warehouse-heartbeat";
    public static final String WAREHOUSE_TEMPERATURE_QUEUE = "warehouse-temperature";
    public static final String WAREHOUSE_HEARTBEAT_KEY     = "warehouse.heartbeat";
    public static final String WAREHOUSE_TEMPERATURE_KEY   = "warehouse.temperature";

    @Bean
    TopicExchange warehouseEventsExchange() {
        return new TopicExchange(WAREHOUSE_EXCHANGE);
    }

    @Bean
    Queue warehouseHeartbeatQueue() {
        return new Queue(WAREHOUSE_HEARTBEAT_QUEUE, true);
    }

    @Bean
    Queue warehouseTemperatureQueue() {
        return new Queue(WAREHOUSE_TEMPERATURE_QUEUE, true);
    }

    @Bean
    Binding warehouseHeartbeatBinding(Queue warehouseHeartbeatQueue, TopicExchange warehouseEventsExchange) {
        return BindingBuilder.bind(warehouseHeartbeatQueue).to(warehouseEventsExchange).with(WAREHOUSE_HEARTBEAT_KEY);
    }

    @Bean
    Binding warehouseTemperatureBinding(Queue warehouseTemperatureQueue, TopicExchange warehouseEventsExchange) {
        return BindingBuilder.bind(warehouseTemperatureQueue).to(warehouseEventsExchange).with(WAREHOUSE_TEMPERATURE_KEY);
    }
}
